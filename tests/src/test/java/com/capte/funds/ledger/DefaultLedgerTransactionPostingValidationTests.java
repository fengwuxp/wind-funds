package com.capte.funds.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingValidationTests extends LedgerTransactionPostingTestSupport {

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

}
