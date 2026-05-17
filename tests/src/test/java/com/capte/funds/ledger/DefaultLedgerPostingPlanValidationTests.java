package com.capte.funds.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerPostingPlanValidationTests extends LedgerTransactionPostingTestSupport {

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
}
