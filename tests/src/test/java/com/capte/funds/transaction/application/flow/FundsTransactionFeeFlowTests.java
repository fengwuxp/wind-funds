package com.capte.funds.transaction.application.flow;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 手续费组合业务流测试。
 */
class FundsTransactionFeeFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后付款并收取手续费，随后发起本金退款，再单独退回手续费。
     * 输入：充值 100、付款 70、固定手续费 5、本金退款 30、手续费退回 5。
     * 输出：付款方 AVAILABLE、收款方 SETTLEMENT、平台 FEE/CASH/PREPAYMENT 余额快照和账务事实。
     * 预期：主交易本金和费用拆 leg；普通退款不退费；`refundFee` 只回放费用路径。
     * 红线：手续费不能混入本金退款，也不能在普通退款时自动退回。
     */
    @Test
    void testPayWithFeeThenRefundPrincipalAndFeeShouldReplayDifferentRouteLegs() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_flow_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 100L, "FEE_FLOW_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L, "FEE_FLOW_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -75L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 30L, "FEE_FLOW_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_FLOW_PAY").getFundsTransactionSn();
        refundFee(payer, 5L, feeSourceTransactionSn, "FEE_FLOW_FEE_REFUND");
        BalanceSnapshot afterFeeRefund = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRefund, afterFeeRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, -5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.REFUND.name(),
                        FundsTransactionEventType.FEE_REFUND.name());

        LedgerTransaction payTransaction = ledgerTransactionByBusinessSn("FEE_FLOW_PAY");
        assertThat(entriesOf(payTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.SETTLEMENT,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FEE);
        assertThat(postingPlansOf(payTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactly(LedgerPhaseCode.SETTLEMENT.name(), LedgerPhaseCode.FEE.name());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("FEE_FLOW_REFUND");
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());

        LedgerTransaction feeRefundTransaction = ledgerTransactionByBusinessSn("FEE_FLOW_FEE_REFUND");
        assertThat(entriesOf(feeRefundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.FEE, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(feeRefundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());
    }
}
