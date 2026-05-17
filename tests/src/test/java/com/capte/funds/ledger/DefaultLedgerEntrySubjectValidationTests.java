package com.capte.funds.ledger;

import com.capte.funds.support.FundsTransactionTestSupport;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerEntrySubjectValidationTests extends LedgerTransactionPostingTestSupport {

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
