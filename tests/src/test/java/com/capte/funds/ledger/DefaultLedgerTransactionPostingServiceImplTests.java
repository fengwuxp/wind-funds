package com.capte.funds.ledger;

import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingServiceImplTests extends LedgerTransactionPostingTestSupport {

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

}
