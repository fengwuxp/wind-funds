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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 手续费组合业务流测试。
 */
class FundsTransactionFeeFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：账户没有可用余额时直接收取独立手续费。
     * 输入：用户未充值，提交独立手续费 5。
     * 输出：请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额均不变化。
     * 预期：无受控负余额策略时，独立手续费不能把用户可用余额打成负数。
     * 红线：手续费失败不能留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testStandaloneFeeWithoutBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        assertThatThrownBy(() -> fee(payer, 5L, "FEE_NO_BALANCE_CHARGE"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("FEE_NO_BALANCE_CHARGE");
    }

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

    /**
     * 场景：业务侧发起手续费退回，但传入的原费用交易流水不存在。
     * 输入：用户充值 100、付款 70 并收取手续费 5，随后退费 5 且 `feeSourceTransactionSn` 指向未知交易。
     * 输出：退费失败，付款方、收款方、平台 FEE/CASH/PREPAYMENT 余额保持付款后状态。
     * 预期：手续费退回必须定位原 route snapshot 和原 FEE leg，未知引用不能退化为当前路径重新路由。
     * 红线：缺原路径快照的退费不得生成 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testFeeRefundWithUnknownSourceTransactionShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_refund_no_src_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        topup(payer, 100L, "FEE_REFUND_UNKNOWN_SOURCE_TOPUP");
        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L,
                "FEE_REFUND_UNKNOWN_SOURCE_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        assertThatThrownBy(() -> refundFee(payer, 5L, "FUNDS_TRANSACTION_NOT_EXISTS",
                "FEE_REFUND_UNKNOWN_SOURCE_RETURN"))
                .hasMessageContaining("RouteSnapshot 回放事件未找到原路径快照");

        BalanceSnapshot afterFailure = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 25L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_UNKNOWN_SOURCE_RETURN");
    }

    /**
     * 场景：用户付款产生固定手续费，全额退费后再次尝试退回同一原交易手续费。
     * 输入：原交易手续费 5、第一次手续费退款 5、平台手续费账户另有足额余额、第二次手续费退款 5。
     * 输出：第二次退费失败，付款方、收款方、平台手续费和现金账户余额均保持不变。
     * 预期：手续费退款按原交易 FEE leg 回放，同一原交易累计退费金额不得超过原交易手续费。
     * 红线：不能因为平台手续费账户余额充足，就允许对同一原交易超额退费或落下失败账务事实。
     */
    @Test
    void testRepeatedFeeRefundForSameTransactionShouldLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_refund_exceed_payee");
        FundsAccountId reservePayer = fundingAccount("fee_refund_reserve_user");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(reservePayer, LedgerSubjectCode.AVAILABLE);

        topup(payer, 100L, "FEE_REFUND_EXCEED_TOPUP");
        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L,
                "FEE_REFUND_EXCEED_PAY");
        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_EXCEED_PAY")
                .getFundsTransactionSn();
        refundFee(payer, 5L, feeSourceTransactionSn, "FEE_REFUND_EXCEED_FIRST_RETURN");

        topup(reservePayer, 100L, "FEE_REFUND_EXCEED_RESERVE_TOPUP");
        payWithFixedFee(reservePayer, payee, LedgerSubjectCode.SETTLEMENT, 10L, 20L,
                "FEE_REFUND_EXCEED_RESERVE_PAY");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));

        assertThatThrownBy(() -> refundFee(payer, 5L, feeSourceTransactionSn,
                "FEE_REFUND_EXCEED_SECOND_RETURN"))
                .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterFailure = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 80L, CURRENCY);
        assertBucket(balance(reservePayer), LedgerSubjectCode.AVAILABLE, 70L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 20L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(5);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.FEE_REFUND.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_EXCEED_SECOND_RETURN");
    }
}
