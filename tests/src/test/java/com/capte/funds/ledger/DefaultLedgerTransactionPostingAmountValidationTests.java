package com.capte.funds.ledger;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultLedgerTransactionPostingAmountValidationTests extends LedgerTransactionPostingTestSupport {

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
}
