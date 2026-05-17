package com.capte.funds.transaction.application.flow;

import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsTransactionFeeBusinessFlowTests extends FundsTransactionBusinessFlowTestSupport {

    /**
     * 场景：用户充值后带业务显式手续费付款，后续商户对付款主金额做部分退款，再退回手续费。
     * 输入：充值 100、付款 40、固定手续费 5、本金退款 20、手续费退款 5。
     * 输出：用户 AVAILABLE、商户 SETTLEMENT、平台 FEE 和平台 CASH/PREPAYMENT 余额快照。
     * 预期：普通退款只回补付款主金额，费用退款只回放 fee leg，最终平台费用归零。
     * 红线：手续费由交易层请求显式传入并独立入账，普通退款不默认退费，费用退款不得冲销本金 leg。
     */
    @Test
    void testTopupPayWithFeeRefundShouldKeepLedgerBalances() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId merchant = fundingAccount("merchant_001");
        FundsAccountId fee = feeAccount();
        BalanceSnapshot before = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "TOPUP_PAY_FEE_REFUND_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String paySn = pay(user, merchant, 40L, fixedFeeSpec(5L), "TOPUP_PAY_FEE_REFUND_PAY");
        BalanceSnapshot afterPay = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(user, LedgerSubjectCode.AVAILABLE, -45L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerTransactionSpec payTransaction = ledgerBook.postedTransactions.get(1);
        assertEntriesForSubject(payTransaction, user, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.AVAILABLE);
        assertEntriesForSubject(payTransaction, merchant, LedgerSubjectCode.SETTLEMENT);
        assertEntriesForSubject(payTransaction, fee, LedgerSubjectCode.FEE);

        refund(user, merchant, 20L, "TOPUP_PAY_FEE_REFUND_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(user, merchant, fee, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, -20L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerTransactionSpec refundTransaction = ledgerBook.postedTransactions.get(2);
        assertEntriesForSubject(refundTransaction, user, LedgerSubjectCode.AVAILABLE);
        assertEntriesForSubject(refundTransaction, merchant, LedgerSubjectCode.SETTLEMENT);
        assertNoEntriesForSubject(refundTransaction, fee);

        refundFee(user, 5L, paySn, "TOPUP_PAY_FEE_REFUND_FEE_REFUND");
        BalanceSnapshot afterFeeRefund = snapshot(balances(user, merchant, fee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRefund, afterFeeRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, -5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerTransactionSpec feeRefundTransaction = ledgerBook.postedTransactions.get(3);
        assertEntriesForSubject(feeRefundTransaction, user, LedgerSubjectCode.AVAILABLE);
        assertEntriesForSubject(feeRefundTransaction, fee, LedgerSubjectCode.FEE);
        assertNoEntriesForSubject(feeRefundTransaction, merchant);

        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 80L, CURRENCY);
        assertBucket(ledgerBook.balance(merchant), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);
        assertBucket(ledgerBook.balance(fee), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertPostedTransactions(4);
    }

    /**
     * 场景：手续费退回累计金额超过原手续费事实。
     * 输入：充值 100、付款 40、固定手续费 5、先全额退费 5、再重复退费 5。
     * 输出：第二次退费失败，余额保持第一次退费后的状态。
     * 预期：手续费退回基于原 fee leg 累计上限校验，失败前不生成新账本交易。
     * 红线：费用退款不得超过原收费金额。
     */
    @Test
    void testFeeRefundShouldRejectWhenCumulativeAmountExceedsOriginalFee() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId merchant = fundingAccount("merchant_001");
        FundsAccountId fee = feeAccount();

        topup(user, 100L, "FEE_REFUND_LIMIT_TOPUP");
        String paySn = pay(user, merchant, 40L, fixedFeeSpec(5L), "FEE_REFUND_LIMIT_PAY");
        refundFee(user, 5L, paySn, "FEE_REFUND_LIMIT_FIRST");
        BalanceSnapshot afterFirstFeeRefund = snapshot(balances(user, merchant, fee, cashMappingAccount(),
                prepaymentAccount()));

        assertThatThrownBy(() -> refundFee(user, 5L, paySn, "FEE_REFUND_LIMIT_SECOND"))
                .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterRejectedFeeRefund = snapshot(balances(user, merchant, fee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstFeeRefund, afterRejectedFeeRefund,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(fee, LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertPostedTransactions(3);
    }

}
