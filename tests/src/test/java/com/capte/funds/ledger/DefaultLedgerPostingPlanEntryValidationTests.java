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

class DefaultLedgerPostingPlanEntryValidationTests extends LedgerTransactionPostingTestSupport {

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
