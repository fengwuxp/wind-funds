package com.capte.funds.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingCurrencyValidationTests extends LedgerTransactionPostingTestSupport {

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

}
