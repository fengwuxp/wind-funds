package com.capte.funds.ledger;

import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.dto.LedgerEntryDTO;
import com.capte.funds.ledger.dto.LedgerTransactionCreateResult;
import com.capte.funds.ledger.dto.LedgerTransactionDTO;
import com.capte.funds.ledger.query.LedgerEntryQuery;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.query.LedgerTransactionQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.capte.funds.ledger.request.UpdateLedgerTransactionRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.ledger.service.LedgerTransactionService;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.wind.common.exception.BaseException;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import com.wind.integration.funds.ledger.LedgerBalanceProjectionService;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingServiceImplTests {

    /**
     * 场景：已确认账本交易完成入账。
     * 输入：一笔 POSTED 转账交易，包含平衡的 AVAILABLE 借贷分录。
     * 输出：创建账本交易，并把分录交给余额投影服务。
     * 预期：账本交易可追溯，posting plan 平衡，余额投影收到完整分录。
     * 红线：不得绕过账本交易事实直接更新余额投影。
     */
    @Test
    void testPostShouldCreateLedgerTransactionAndProjectEntries() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        service.post(transaction);

        assertThat(transactionService.createdTransactions).containsExactly(transaction);
        assertThat(projectionService.projectedEntries).hasSize(1);
        assertThat(projectionService.projectedEntries.getFirst()).hasSize(2);
    }

    /**
     * 场景：重复入账请求命中账本交易幂等。
     * 输入：账本交易创建服务返回未新建，表示交易事实已存在。
     * 输出：入账服务跳过余额投影。
     * 预期：幂等重放不重复写余额投影，已存在交易只保留一次资金影响。
     * 红线：不得因为重复请求导致同一 ledgerTransactionSn 被重复投影。
     */
    @Test
    void testPostShouldSkipProjectionWhenLedgerTransactionAlreadyExists() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService(false);
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        service.post(transaction);

        assertThat(transactionService.createdTransactions).containsExactly(transaction);
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：入账请求缺少账本交易对象。
     * 输入：LedgerTransactionSpec 为 null。
     * 输出：入账服务在任何持久化动作前拒绝。
     * 预期：错误明确提示账本交易不能为空，且不创建交易、不投影余额。
     * 红线：不得用 NPE 或半写入状态暴露空交易输入。
     */
    @Test
    void testPostShouldRejectNullLedgerTransactionBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));

        assertThatThrownBy(() -> service.post(null))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易不能为空");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易缺少交易流水号。
     * 输入：LedgerTransactionSpec.sn 为空白字符串，posting plan 和 entry 均完整。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：账本交易必须先具备可追溯流水号，再进入状态、金额和账务计划校验。
     * 红线：不得生成缺少账本交易流水的账本事实、posting plan 或余额投影。
     */
    @Test
    void testPostShouldRejectBlankLedgerTransactionSnBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerPostingPlanSpec plan = postingPlan("LE_000000000021", List.of(
                entry(EntrySide.DEBIT, "LE_000000000021"),
                entry(EntrySide.CREDIT, "LE_000000000021")
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(" ", List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易流水号不能为空");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易缺少交易级金额。
     * 输入：LedgerTransactionSpec.amount 为 null，但 posting plan 分录完整。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：错误信息能定位到 ledgerTransactionSn，且不读取后续币种或落库。
     * 红线：账本交易不得在缺少交易级金额时进入入账和余额投影链路。
     */
    @Test
    void testPostShouldRejectTransactionWithoutAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000017";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, null, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易金额不能为空")
                .hasMessageContaining("LE_000000000017");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易金额为 0。
     * 输入：LedgerTransactionSpec.amount 为 USD 0，posting plan 分录完整且金额为正。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：交易级金额必须大于 0，不能只依赖 entry 金额校验。
     * 红线：不得产生零金额账本交易事实污染审计和幂等链路。
     */
    @Test
    void testPostShouldRejectNonPositiveTransactionAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000025";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(
                ledgerTransactionSn,
                Money.immutable(0L, CurrencyIsoCode.USD),
                Money.immutable(100L, CurrencyIsoCode.USD),
                BigDecimal.ONE,
                List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易金额必须大于 0")
                .hasMessageContaining("LE_000000000025");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易原始金额为 0。
     * 输入：LedgerTransactionSpec.originalAmount 为 USD 0，交易金额和分录金额均为正。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：原始金额必须大于 0，保证换汇和原币追溯字段可解释。
     * 红线：不得让缺少有效原币金额的账本交易进入持久化。
     */
    @Test
    void testPostShouldRejectNonPositiveOriginalAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000026";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(
                ledgerTransactionSn,
                Money.immutable(100L, CurrencyIsoCode.USD),
                Money.immutable(0L, CurrencyIsoCode.USD),
                BigDecimal.ONE,
                List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易原始金额必须大于 0")
                .hasMessageContaining("LE_000000000026");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易汇率为 0。
     * 输入：LedgerTransactionSpec.exchangeRate 为 0，交易金额和分录金额均为正。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：汇率必须大于 0，避免后续原币和账本币推导失真。
     * 红线：不得让零汇率或负汇率的账本交易进入持久化。
     */
    @Test
    void testPostShouldRejectNonPositiveExchangeRateBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000027";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(
                ledgerTransactionSn,
                Money.immutable(100L, CurrencyIsoCode.USD),
                Money.immutable(100L, CurrencyIsoCode.USD),
                BigDecimal.ZERO,
                List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易汇率必须大于 0")
                .hasMessageContaining("LE_000000000027");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易仍处于待处理状态。
     * 输入：LedgerTransactionSpec.status 为 PENDING，posting plan 和 entry 均完整。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：只有已确认可入账的 POSTED 交易才能进入账本写入口。
     * 红线：不得把待处理、失败、撤销或结算后状态的交易再次作为原始入账事实写入。
     */
    @Test
    void testPostShouldRejectNonPostedTransactionBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000018";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(
                ledgerTransactionSn, List.of(plan), CurrencyIsoCode.USD, LedgerTransactionStatus.PENDING);

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易状态不允许入账")
                .hasMessageContaining("LE_000000000018")
                .hasMessageContaining(LedgerTransactionStatus.PENDING.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易缺少账务计划。
     * 输入：LedgerTransactionSpec.postingPlans 为空集合。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每笔账本交易必须带有可解释的 posting plan。
     * 红线：不得持久化没有账务计划和资金影响明细的账本事实。
     */
    @Test
    void testPostShouldRejectEmptyPostingPlansBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = uncheckedTransaction("LE_000000000009", List.of());

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易 postingPlans 不能为空")
                .hasMessageContaining("LE_000000000009");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易 postingPlans 集合中混入空计划。
     * 输入：postingPlans 非空，但第一项 posting plan 为 null。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：错误信息能定位到 ledgerTransactionSn，且不创建账本交易、不投影余额。
     * 红线：账本交易不得持久化结构不完整、无法追溯到具体账务计划的入账请求。
     */
    @Test
    void testPostShouldRejectNullPostingPlanBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000015";
        List<LedgerPostingPlanSpec> plans = new ArrayList<>();
        plans.add(null);
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, plans);

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划不能为空")
                .hasMessageContaining("LE_000000000015");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账务计划缺少计划流水号。
     * 输入：posting plan.planId 为空白字符串，交易流水和分录流水均完整。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每个 posting plan 必须有稳定 planId 作为落库、摘要和审计锚点。
     * 红线：不得让不可追溯的账务计划进入账本持久化。
     */
    @Test
    void testPostShouldRejectBlankPostingPlanIdBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000022";
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(ledgerTransactionSn, " ", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划流水号不能为空")
                .hasMessageContaining("LE_000000000022");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账务计划绑定到另一个账本交易流水。
     * 输入：LedgerTransactionSpec.sn 为 LE_000000000019，但 posting plan.ledgerTransactionSn 为 LE_WRONG_PLAN。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每个 posting plan 必须属于当前账本交易，错误信息能定位到 planId。
     * 红线：不得让同一笔账本交易持有其他交易的账务计划，避免审计和幂等链路断裂。
     */
    @Test
    void testPostShouldRejectPostingPlanLedgerTransactionSnMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000019";
        LedgerPostingPlanSpec plan = uncheckedPostingPlan("LE_WRONG_PLAN", "WRONG_PLAN_SN", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划交易流水与账本交易流水不一致")
                .hasMessageContaining("WRONG_PLAN_SN")
                .hasMessageContaining("LE_000000000019")
                .hasMessageContaining("LE_WRONG_PLAN");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账务计划缺少绑定的账本交易流水。
     * 输入：posting plan.ledgerTransactionSn 为空白字符串，交易流水和分录流水均完整。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：posting plan 必须显式绑定所属账本交易流水。
     * 红线：不得让账务计划脱离所属账本交易独立落库。
     */
    @Test
    void testPostShouldRejectBlankPostingPlanLedgerTransactionSnBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000023";
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(" ", "BLANK_PLAN_TRANSACTION_SN", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划交易流水不能为空")
                .hasMessageContaining("BLANK_PLAN_TRANSACTION_SN")
                .hasMessageContaining("LE_000000000023");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易包含空的 posting plan。
     * 输入：postingPlans 非空，但其中一个 plan 没有任何 ledger entry。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每个 posting plan 至少包含一组可解释的借贷分录。
     * 红线：不得持久化没有资金影响明细的账本交易计划。
     */
    @Test
    void testPostShouldRejectPostingPlanWithoutEntriesBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000013";
        LedgerPostingPlanSpec emptyPlan = uncheckedPostingPlan(ledgerTransactionSn, "EMPTY_PLAN", List.of());
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(emptyPlan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划 entries 不能为空")
                .hasMessageContaining("EMPTY_PLAN")
                .hasMessageContaining("LE_000000000013");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账务计划分录集合中混入空分录。
     * 输入：posting plan 内第一条 entry 为 null，第二条为正常分录。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：错误信息能定位到 planId 和 ledgerTransactionSn。
     * 红线：账本入账前置校验不得让空分录以 NPE 或后置持久化错误暴露。
     */
    @Test
    void testPostShouldRejectNullLedgerEntryBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000014";
        List<LedgerEntrySpec> entries = new ArrayList<>();
        entries.add(null);
        entries.add(entry(EntrySide.CREDIT, ledgerTransactionSn));
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(ledgerTransactionSn, "NULL_ENTRY", entries);
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录不能为空")
                .hasMessageContaining("NULL_ENTRY")
                .hasMessageContaining("LE_000000000014");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账务计划内的账本分录缺少交易流水。
     * 输入：posting plan 属于 LE_000000000024，但第一条 entry.ledgerTransactionSn 为空白字符串。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每条 entry 必须显式绑定所属账本交易流水。
     * 红线：不得让账本分录脱离交易流水进入余额投影或持久化链路。
     */
    @Test
    void testPostShouldRejectBlankEntryLedgerTransactionSnBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000024";
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(ledgerTransactionSn, "BLANK_ENTRY_TRANSACTION_SN", List.of(
                entry(EntrySide.DEBIT, " "),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录交易流水不能为空")
                .hasMessageContaining("BLANK_ENTRY_TRANSACTION_SN")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账务计划内的账本分录绑定到另一个账本交易流水。
     * 输入：posting plan 属于 LE_000000000020，但第一条 entry.ledgerTransactionSn 为 LE_WRONG_ENTRY。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每条 entry 必须属于当前账本交易和当前 plan，错误信息能定位到主体和账目。
     * 红线：不得让账本分录脱离当前交易流水写入，否则交易、计划、分录无法稳定追溯。
     */
    @Test
    void testPostShouldRejectEntryLedgerTransactionSnMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000020";
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(ledgerTransactionSn, "WRONG_ENTRY_SN", List.of(
                entry(EntrySide.DEBIT, "LE_WRONG_ENTRY"),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录交易流水与账本交易流水不一致")
                .hasMessageContaining("WRONG_ENTRY_SN")
                .hasMessageContaining("LE_000000000020")
                .hasMessageContaining("LE_WRONG_ENTRY")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本入账找不到支持账户的余额投影服务。
     * 输入：posting plan 分录完整，但所有 LedgerBalanceProjectionService 均不支持该账户。
     * 输出：入账服务拒绝继续执行。
     * 预期：不创建账本交易，不产生无法落到余额桶的投影动作。
     * 红线：不得在余额投影职责缺失时只写账本交易事实，造成账实链路断裂。
     */
    @Test
    void testPostShouldFailWhenNoProjectionServiceSupportsAccount() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(false);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到支持的账本余额投影服务");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：同一账户被多个余额投影服务同时支持。
     * 输入：posting plan 分录完整，两个投影服务都声明支持。
     * 输出：入账服务拒绝继续执行。
     * 预期：投影服务路由必须唯一，避免同一分录被重复或分叉处理。
     * 红线：不得让余额投影服务选择存在歧义的账本交易落库。
     */
    @Test
    void testPostShouldFailWhenMultipleProjectionServicesSupportAccount() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService firstProjectionService = new RecordingProjectionService(true);
        RecordingProjectionService secondProjectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(firstProjectionService, secondProjectionService));
        LedgerTransactionSpec transaction = transaction();

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本余额投影服务不唯一");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(firstProjectionService.projectedEntries).isEmpty();
        assertThat(secondProjectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：交易总借贷平衡，但单个账务计划不平衡。
     * 输入：两个 posting plan 相互抵消，交易整体平衡，但每个 plan 内部不平衡。
     * 输出：入账服务拒绝第一个不平衡 plan。
     * 预期：平衡校验必须落到每个 posting plan，而不是只看交易总额。
     * 红线：不得用跨计划抵消掩盖单个资金事件的账务不平衡。
     */
    @Test
    void testPostShouldRejectUnbalancedPostingPlanEvenWhenTransactionIsBalanced() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000002";
        LedgerPostingPlanSpec debitHeavyPlan = uncheckedPostingPlan(ledgerTransactionSn, "DEBIT_HEAVY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 100L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 1L)
        ));
        LedgerPostingPlanSpec creditHeavyPlan = uncheckedPostingPlan(ledgerTransactionSn, "CREDIT_HEAVY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 1L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 100L)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(debitHeavyPlan, creditHeavyPlan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(debitHeavyPlan.isBalanced()).isFalse();
        assertThat(creditHeavyPlan.isBalanced()).isFalse();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账务计划不平衡")
                .hasMessageContaining("DEBIT_HEAVY");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：同一账务计划内出现多币种分录。
     * 输入：posting plan 借方为 USD，贷方为 EUR。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：单个 posting plan 的借贷分录必须使用同一币种。
     * 红线：不得在未定义换汇事件和汇率规则时把多币种分录记入同一账务计划。
     */
    @Test
    void testPostShouldRejectCurrencyMismatchInsidePostingPlanBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000007";
        LedgerPostingPlanSpec mixedCurrencyPlan = uncheckedPostingPlan(ledgerTransactionSn, "MIXED_CURRENCY", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 100L, CurrencyIsoCode.USD),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 100L, CurrencyIsoCode.EUR)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(mixedCurrencyPlan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("记账计划币种不一致")
                .hasMessageContaining("MIXED_CURRENCY")
                .hasMessageContaining(CurrencyIsoCode.USD.name())
                .hasMessageContaining(CurrencyIsoCode.EUR.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本交易金额币种和实际记账分录币种不一致。
     * 输入：交易金额为 EUR，posting plan 内分录均为 USD。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：交易级金额币种必须和所有 posting plan 币种一致。
     * 红线：不得产生交易事实币种与账本事实币种不一致的入账记录。
     */
    @Test
    void testPostShouldRejectTransactionCurrencyMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000012";
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan), CurrencyIsoCode.EUR);

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本交易币种与记账计划币种不一致")
                .hasMessageContaining("LE_000000000012")
                .hasMessageContaining(CurrencyIsoCode.EUR.name())
                .hasMessageContaining(CurrencyIsoCode.USD.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录缺少金额。
     * 输入：posting plan 内借方分录 amount 为 null，贷方分录正常。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：错误信息能定位到 planId、ledgerTransactionSn、主体和账目。
     * 红线：不得让缺少金额的分录进入币种、平衡或持久化阶段。
     */
    @Test
    void testPostShouldRejectEntryWithoutAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000016";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutAmount = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutAmount.setAmount(null);
        LedgerPostingPlanSpec plan = uncheckedPostingPlan(ledgerTransactionSn, "MISSING_AMOUNT", List.of(
                entryWithoutAmount,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(plan));

        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录金额不能为空")
                .hasMessageContaining("MISSING_AMOUNT")
                .hasMessageContaining("LE_000000000016")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录金额为 0。
     * 输入：posting plan 借贷分录金额均为 0，形式上仍然平衡。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：账本分录金额必须大于 0，平衡不等于有效资金事实。
     * 红线：不得产生零金额账本事实污染交易追踪和余额投影。
     */
    @Test
    void testPostShouldRejectNonPositiveEntryAmountBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000008";
        LedgerPostingPlanSpec zeroAmountPlan = uncheckedPostingPlan(ledgerTransactionSn, "ZERO_AMOUNT", List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn, 0L),
                entry(EntrySide.CREDIT, ledgerTransactionSn, 0L)
        ));
        LedgerTransactionSpec transaction = uncheckedTransaction(ledgerTransactionSn, List.of(zeroAmountPlan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(zeroAmountPlan.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录金额必须大于 0")
                .hasMessageContaining("ZERO_AMOUNT")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录缺少 ledgerId。
     * 输入：posting plan 借方分录未绑定账本 ID，贷方分录正常。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：每条分录必须绑定具体账本，才能校验主体、账目和 profile。
     * 红线：不得把没有 ledgerId 的分录交给余额投影或账本持久化。
     */
    @Test
    void testPostShouldRejectEntryWithoutLedgerIdBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000003";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutLedgerId = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutLedgerId.setLedgerId(null);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entryWithoutLedgerId,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录 ledgerId 不能为空")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录绑定的 ledgerId 与分录主体不一致。
     * 输入：分录 subjectId 为 funding_001，但 ledgerId 指向 funding_002 的账本。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：ledgerId、subjectId、subjectType、ledgerSubjectCode 必须共同指向同一账本。
     * 红线：不得让主体 A 的资金变化记入主体 B 的账本。
     */
    @Test
    void testPostShouldRejectLedgerBindingMismatchBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        RecordingLedgerService ledgerService = defaultLedgerService();
        ledgerService.addLedger(ledger(2L, "funding_002", FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, CurrencyIsoCode.USD));
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, ledgerService, List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000010";
        FundsTransactionTestSupport.MutableLedgerEntrySpec mismatchedEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        mismatchedEntry.setLedgerId(2L);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                mismatchedEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体与账本主体不一致")
                .hasMessageContaining("ledgerId = 2");
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：分录要求允许负余额，但账本 profile 不允许。
     * 输入：AVAILABLE 分录设置 ALLOW_NEGATIVE，账本 profile 为不可负。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：分录级余额约束不得突破账本 profile 的余额控制红线。
     * 红线：不得通过 posting plan 参数绕过账本账户余额约束。
     */
    @Test
    void testPostShouldRejectAllowNegativeWhenLedgerProfileDisallowsNegativeBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000011";
        FundsTransactionTestSupport.MutableLedgerEntrySpec allowNegativeEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        allowNegativeEntry.setBalanceConstraintType(LedgerBalanceConstraintType.ALLOW_NEGATIVE);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                allowNegativeEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本 profile 不允许负余额")
                .hasMessageContaining("ledgerId = 1")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：外部账户被作为账本分录主体入账。
     * 输入：分录 subjectType 为 EXTERNAL_ACCOUNT，subjectId 为 external_bank_001。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：账本只记录受管资金主体，外部账户只能作为 route/通道语义存在。
     * 红线：不得为外部银行账户直接创建内部账本事实。
     */
    @Test
    void testPostShouldRejectExternalAccountEntryBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000004";
        FundsTransactionTestSupport.MutableLedgerEntrySpec externalEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        externalEntry.setSubjectId("external_bank_001");
        externalEntry.setSubjectType(RouteNodeType.EXTERNAL_ACCOUNT.name());
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                externalEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining("external_bank_001")
                .hasMessageContaining(RouteNodeType.EXTERNAL_ACCOUNT.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录缺少主体类型。
     * 输入：分录 subjectId 存在，但 subjectType 为 null。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：主体类型必须明确，才能判断是否允许入账及如何投影余额。
     * 红线：不得把主体语义不完整的分录写入账本。
     */
    @Test
    void testPostShouldRejectEntryWithoutSubjectTypeBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000005";
        FundsTransactionTestSupport.MutableLedgerEntrySpec entryWithoutSubjectType = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        entryWithoutSubjectType.setSubjectType(null);
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                entryWithoutSubjectType,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining("funding_001")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    /**
     * 场景：账本分录主体类型未知。
     * 输入：分录 subjectType 为 UNKNOWN_ACCOUNT。
     * 输出：入账服务在创建账本交易前拒绝。
     * 预期：只有资金底座明确允许的主体类型才能进入账本写入口。
     * 红线：不得把未知主体类型作为内部资金主体入账。
     */
    @Test
    void testPostShouldRejectUnknownSubjectTypeBeforeCreatingLedgerTransaction() {
        RecordingLedgerTransactionService transactionService = new RecordingLedgerTransactionService();
        RecordingProjectionService projectionService = new RecordingProjectionService(true);
        DefaultLedgerTransactionPostingServiceImpl service = new DefaultLedgerTransactionPostingServiceImpl(
                transactionService, defaultLedgerService(), List.of(projectionService));
        String ledgerTransactionSn = "LE_000000000006";
        FundsTransactionTestSupport.MutableLedgerEntrySpec unknownSubjectTypeEntry = entry(
                EntrySide.DEBIT, ledgerTransactionSn);
        unknownSubjectTypeEntry.setSubjectType("UNKNOWN_ACCOUNT");
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, List.of(
                unknownSubjectTypeEntry,
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        ));
        LedgerTransactionSpec transaction = transaction(ledgerTransactionSn, List.of(plan));

        assertThat(transaction.isBalanced()).isTrue();
        assertThatThrownBy(() -> service.post(transaction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("账本分录主体类型不允许入账")
                .hasMessageContaining("UNKNOWN_ACCOUNT")
                .hasMessageContaining(LedgerSubjectCode.AVAILABLE.name());
        assertThat(transactionService.createdTransactions).isEmpty();
        assertThat(projectionService.projectedEntries).isEmpty();
    }

    private LedgerTransactionSpec transaction() {
        String ledgerTransactionSn = "LE_000000000001";
        List<LedgerEntrySpec> entries = List.of(
                entry(EntrySide.DEBIT, ledgerTransactionSn),
                entry(EntrySide.CREDIT, ledgerTransactionSn)
        );
        LedgerPostingPlanSpec plan = postingPlan(ledgerTransactionSn, entries);
        return transaction(ledgerTransactionSn, List.of(plan));
    }

    private LedgerTransactionSpec transaction(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
        return transaction(ledgerTransactionSn, plans, CurrencyIsoCode.USD);
    }

    private LedgerTransactionSpec transaction(String ledgerTransactionSn,
                                              List<LedgerPostingPlanSpec> plans,
                                              CurrencyIsoCode currency) {
        return transaction(ledgerTransactionSn, plans, currency, LedgerTransactionStatus.POSTED);
    }

    private LedgerTransactionSpec transaction(String ledgerTransactionSn,
                                              List<LedgerPostingPlanSpec> plans,
                                              CurrencyIsoCode currency,
                                              LedgerTransactionStatus status) {
        return LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec.builder()
                .sn(ledgerTransactionSn)
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .status(status)
                .amount(Money.immutable(100L, currency))
                .originalAmount(Money.immutable(100L, currency))
                .exchangeRate(BigDecimal.ONE)
                .businessSn("TRANSFER_000000000001")
                .businessScene("TRANSFER")
                .transactionTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .description("transfer")
                .postingPlans(plans)
                .contextVariables(Map.of())
                .build();
    }

    private FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide, String ledgerTransactionSn) {
        return entry(entrySide, ledgerTransactionSn, 100L);
    }

    private FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide,
                                                                     String ledgerTransactionSn,
                                                                     long amount) {
        return entry(entrySide, ledgerTransactionSn, amount, CurrencyIsoCode.USD);
    }

    private FundsTransactionTestSupport.MutableLedgerEntrySpec entry(EntrySide entrySide,
                                                                     String ledgerTransactionSn,
                                                                     long amount,
                                                                     CurrencyIsoCode currency) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                "funding_001",
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                entrySide,
                ledgerTransactionSn,
                "TRANSFER",
                "TRANSFER_000000000001",
                amount,
                currency,
                LocalDateTime.of(2026, 5, 10, 10, 0)
        ).setLedgerId(1L);
    }

    private LedgerPostingPlanSpec postingPlan(String ledgerTransactionSn, List<LedgerEntrySpec> entries) {
        return LedgerTransactionSpecFactory.postingPlan(LedgerPostingIntentType.TRANSFER,
                ledgerTransactionSn, List.of(LedgerTransactionSpecFactory.postingPhase(
                        LedgerPhaseCode.TRANSFER, entries)));
    }

    private RecordingLedgerService defaultLedgerService() {
        RecordingLedgerService ledgerService = new RecordingLedgerService();
        ledgerService.addLedger(ledger(1L, "funding_001", FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE, LedgerSubjectCategory.ASSET, CurrencyIsoCode.USD));
        return ledgerService;
    }

    private LedgerDTO ledger(Long id,
                             String subjectId,
                             String subjectType,
                             LedgerSubjectCode ledgerSubjectCode,
                             LedgerSubjectCategory ledgerSubjectCategory,
                             CurrencyIsoCode currency) {
        return new LedgerDTO()
                .setId(id)
                .setSubjectId(subjectId)
                .setSubjectType(subjectType)
                .setLedgerSubjectCode(ledgerSubjectCode)
                .setLedgerSubjectCategory(ledgerSubjectCategory)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(false)
                .setDebitAmount(0L)
                .setCreditAmount(0L)
                .setCurrency(currency);
    }

    private LedgerPostingPlanSpec uncheckedPostingPlan(String ledgerTransactionSn,
                                                       String planId,
                                                       List<LedgerEntrySpec> entries) {
        return new UncheckedLedgerPostingPlanSpec(planId, ledgerTransactionSn, entries);
    }

    private LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn, List<LedgerPostingPlanSpec> plans) {
        return uncheckedTransaction(ledgerTransactionSn, Money.immutable(100L, CurrencyIsoCode.USD), plans);
    }

    private LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn,
                                                       Money amount,
                                                       List<LedgerPostingPlanSpec> plans) {
        Money originalAmount = amount == null ? null : Money.immutable(amount.getAmount(), amount.getCurrency());
        return uncheckedTransaction(ledgerTransactionSn, amount, originalAmount, BigDecimal.ONE, plans);
    }

    private LedgerTransactionSpec uncheckedTransaction(String ledgerTransactionSn,
                                                       Money amount,
                                                       Money originalAmount,
                                                       BigDecimal exchangeRate,
                                                       List<LedgerPostingPlanSpec> plans) {
        return new UncheckedLedgerTransactionSpec(ledgerTransactionSn, amount, originalAmount, exchangeRate, plans);
    }

    private static final class UncheckedLedgerPostingPlanSpec implements LedgerPostingPlanSpec {

        private final String planId;

        private final String ledgerTransactionSn;

        private final List<LedgerEntrySpec> entries;

        private UncheckedLedgerPostingPlanSpec(String planId,
                                               String ledgerTransactionSn,
                                               List<LedgerEntrySpec> entries) {
            this.planId = planId;
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.entries = entries;
        }

        @Override
        public @NonNull String getPlanId() {
            return planId;
        }

        @Override
        public @NonNull String getLedgerTransactionSn() {
            return ledgerTransactionSn;
        }

        @Override
        public @NonNull LedgerPostingIntentType getIntent() {
            return LedgerPostingIntentType.TRANSFER;
        }

        @Override
        public @NonNull List<LedgerEntrySpec> getEntries() {
            return entries;
        }

        @Override
        public @NonNull List<LedgerPostingPhaseSpec> getPostingPhases() {
            return List.of();
        }
    }

    private static final class UncheckedLedgerTransactionSpec implements LedgerTransactionSpec {

        private final String ledgerTransactionSn;

        private final Money amount;

        private final Money originalAmount;

        private final BigDecimal exchangeRate;

        private final List<LedgerPostingPlanSpec> plans;

        private UncheckedLedgerTransactionSpec(String ledgerTransactionSn,
                                               Money amount,
                                               Money originalAmount,
                                               BigDecimal exchangeRate,
                                               List<LedgerPostingPlanSpec> plans) {
            this.ledgerTransactionSn = ledgerTransactionSn;
            this.amount = amount;
            this.originalAmount = originalAmount;
            this.exchangeRate = exchangeRate;
            this.plans = plans;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getSn() {
            return ledgerTransactionSn;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TRANSFER;
        }

        @Override
        public @NonNull LedgerTransactionStatus getStatus() {
            return LedgerTransactionStatus.POSTED;
        }

        @Override
        public @NonNull Money getAmount() {
            return amount;
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return originalAmount;
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return exchangeRate;
        }

        @Override
        public String getBusinessSn() {
            return "TRANSFER_000000000001";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public @NonNull LocalDateTime getTransactionTime() {
            return LocalDateTime.of(2026, 5, 10, 10, 0);
        }

        @Override
        public String getDescription() {
            return "transfer";
        }

        @Override
        public @NonNull List<LedgerPostingPlanSpec> getPostingPlans() {
            return plans;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private static final class RecordingProjectionService implements LedgerBalanceProjectionService {

        private final boolean supported;

        private final List<List<LedgerEntrySpec>> projectedEntries = new ArrayList<>();

        private RecordingProjectionService(boolean supported) {
            this.supported = supported;
        }

        @Override
        public void project(@NonNull List<LedgerEntrySpec> entries) {
            projectedEntries.add(entries);
        }

        @Override
        public boolean supports(@NonNull FundsAccountId accountId) {
            return supported;
        }
    }

    private static final class RecordingLedgerService implements LedgerService {

        private final Map<Long, LedgerDTO> ledgers = new LinkedHashMap<>();

        private void addLedger(LedgerDTO ledger) {
            ledgers.put(ledger.getId(), ledger);
        }

        @Override
        public @NonNull Long createLedger(@NonNull CreateLedgerRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateLedgerBalance(@NonNull UpdateLedgerBalanceRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteLedgerByIds(@NonNull Long... ids) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull LedgerDTO getLedgerById(@NonNull Long id) {
            LedgerDTO ledger = ledgers.get(id);
            if (ledger == null) {
                throw new BaseException("账户账本不存在");
            }
            return ledger;
        }

        @Override
        public @NonNull List<LedgerDTO> getLedgerByIds(@NonNull Collection<Long> ids) {
            return ids.stream()
                    .map(this::getLedgerById)
                    .toList();
        }

        @Override
        public @NonNull WindPagination<LedgerDTO> queryLedgers(@NonNull LedgerQuery query,
                                                               @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingLedgerTransactionService implements LedgerTransactionService {

        private final List<LedgerTransactionSpec> createdTransactions = new ArrayList<>();

        private final boolean createResultCreated;

        private RecordingLedgerTransactionService() {
            this(true);
        }

        private RecordingLedgerTransactionService(boolean createResultCreated) {
            this.createResultCreated = createResultCreated;
        }

        @Override
        public @NonNull LedgerTransactionCreateResult createLedgerTransaction(@NonNull LedgerTransactionSpec transaction) {
            createdTransactions.add(transaction);
            return new LedgerTransactionCreateResult()
                    .setLedgerTransactionId(1L)
                    .setCreated(createResultCreated);
        }

        @Override
        public void updateLedgerTransaction(@NonNull UpdateLedgerTransactionRequest request) {
        }

        @Override
        public void deleteLedgerTransactionByIds(@NonNull Long... ids) {
        }

        @Override
        public @NonNull LedgerTransactionDTO getLedgerTransactionById(@NonNull Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull WindPagination<LedgerTransactionDTO> queryAccountLedgerTransactions(
                @NonNull LedgerTransactionQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull LedgerEntryDTO getLedgerEntryById(@NonNull Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NonNull WindPagination<LedgerEntryDTO> queryLedgerEntries(
                @NonNull LedgerEntryQuery query,
                @NonNull WindQuery<? extends QueryOrderField> options) {
            throw new UnsupportedOperationException();
        }
    }
}
