package com.wind.funds.transaction.application.flow;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 费率手续费资金流程测试。
 */
class FundsTransactionRateFeeFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：付款时按交易金额的 1% 收取手续费。
     * 输入：付款方余额 10000，付款 5000，费率 1%。
     * 输出：付款方减少 5050，收款方增加 5000，平台 FEE 增加 50。
     * 预期：费率手续费以币种最小单位计算，并与付款本金原子入账。
     * 红线：手续费不得因主货币单位二次换算被放大。
     */
    @Test
    void testPayWithRateFeeShouldPostExactMinorUnitAmount() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("rate_fee_payee");
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 10_000L, "RATE_FEE_TOPUP");
        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount()));

        FeeSpec feeChargeSpec = FeeSpec.builder()
                .feeType("RATE_FEE")
                .feeRate(new BigDecimal("0.01"))
                .build();
        String transactionSn = directTransactionService.pay(
                payRequest(payer, payee, 5_000L, feeChargeSpec, "PAY_WITH_RATE_FEE"),
                WindOperatorFactory.system());

        BalanceSnapshot after = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5_050L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 5_000L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 50L, CURRENCY));
        LedgerTransaction ledgerTransaction = ledgerTransactionByBusinessSn("PAY_WITH_RATE_FEE");
        assertThat(postingPlansOf(ledgerTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerPhaseCode.SETTLEMENT.name(), LedgerPhaseCode.FEE.name());
        assertThat(ledgerTransaction.getDebitAmount()).isEqualTo(ledgerTransaction.getCreditAmount());
        assertThat(postingPlansOf(ledgerTransaction)).allSatisfy(postingPlan -> {
            assertThat(postingPlan.getDebitAmount()).isEqualTo(postingPlan.getCreditAmount());
        });
        assertThat(entriesOf(ledgerTransaction)).hasSize(4);

        String retryTransactionSn = directTransactionService.pay(
                payRequest(payer, payee, 5_000L, feeChargeSpec, "PAY_WITH_RATE_FEE"),
                WindOperatorFactory.system());
        BalanceSnapshot afterRetry = snapshot(balances(payer, payee, feeAccount()));
        assertThat(retryTransactionSn).isEqualTo(transactionSn);
        assertOnlyBalanceDeltas(after, afterRetry,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("PAY_WITH_RATE_FEE", 3, 2, 4);
        assertLedgerFactsFollowRouteSnapshot("PAY_WITH_RATE_FEE");
    }

    /**
     * 场景：调用方传入空手续费规则，表明本次交易必须收费但没有可收金额。
     * 预期：请求快速失败，余额、资金交易和账本事实均不发生变化。
     */
    @Test
    void testPayWithEmptyFeeChargeSpecShouldFailWithoutFundsFacts() {
        FundsAccountId payer = fundingAccount("empty_fee_payer");
        FundsAccountId payee = fundingAccount("empty_fee_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "EMPTY_FEE_TOPUP");
        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.pay(
                payRequest(payer, payee, 10L, FeeSpec.builder().build(), "PAY_WITH_EMPTY_FEE"),
                WindOperatorFactory.system()))
                .hasMessageContaining("手续费计算结果必须大于 0");

        BalanceSnapshot after = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("PAY_WITH_EMPTY_FEE");
    }

    /**
     * 场景：小额付款按费率计算后舍入为零，且未配置最低手续费。
     * 预期：请求快速失败，不得静默按免手续费交易继续入账。
     */
    @Test
    void testPayWithRoundedZeroRateFeeShouldFailWithoutFundsFacts() {
        FundsAccountId payer = fundingAccount("rounded_zero_fee_payer");
        FundsAccountId payee = fundingAccount("rounded_zero_fee_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "ROUNDED_ZERO_FEE_TOPUP");
        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();
        FeeSpec feeChargeSpec = FeeSpec.builder()
                .feeType("RATE_FEE")
                .feeRate(new BigDecimal("0.01"))
                .build();

        assertThatThrownBy(() -> directTransactionService.pay(
                payRequest(payer, payee, 1L, feeChargeSpec, "PAY_WITH_ROUNDED_ZERO_FEE"),
                WindOperatorFactory.system()))
                .hasMessageContaining("手续费计算结果必须大于 0");

        BalanceSnapshot after = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("PAY_WITH_ROUNDED_ZERO_FEE");
    }

    /**
     * 场景：小额付款按费率舍入为零，但规则配置最低手续费 1 个最小货币单位。
     * 预期：本金与最低手续费原子入账，所有账务计划保持平衡和可追溯。
     */
    @Test
    void testPayWithMinRateFeeShouldChargeOneMinorUnit() {
        FundsAccountId payer = fundingAccount("min_rate_fee_payer");
        FundsAccountId payee = fundingAccount("min_rate_fee_payee");
        ensureLedger(payer, LedgerSubjectCode.AVAILABLE);
        ensureLedger(payee, LedgerSubjectCode.AVAILABLE);
        topup(payer, 100L, "MIN_RATE_FEE_TOPUP");
        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount()));
        FeeSpec feeChargeSpec = FeeSpec.builder()
                .feeType("RATE_FEE")
                .feeRate(new BigDecimal("0.01"))
                .minAmountWithRate(1)
                .build();

        directTransactionService.pay(
                payRequest(payer, payee, 1L, feeChargeSpec, "PAY_WITH_MIN_RATE_FEE"),
                WindOperatorFactory.system());

        BalanceSnapshot after = snapshot(balances(payer, payee, feeAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(payer, LedgerSubjectCode.AVAILABLE, -2L, CURRENCY),
                delta(payee, LedgerSubjectCode.AVAILABLE, 1L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 1L, CURRENCY));
        assertSingleFundsAndLedgerFactsForBusinessSn("PAY_WITH_MIN_RATE_FEE", 3, 2, 4);
        assertLedgerFactsFollowRouteSnapshot("PAY_WITH_MIN_RATE_FEE");
    }

    private FundsTransactionPayRequest payRequest(FundsAccountId payer,
                                                   FundsAccountId payee,
                                                   long amount,
                                                   FeeSpec feeChargeSpec,
                                                   String businessSn) {
        return new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(payee)
                .setPayeeLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setFeeChargeSpec(feeChargeSpec)
                .setBusinessScene("PAY")
                .setBusinessSn(businessSn)
                .setDescription("pay with rate fee");
    }
}
