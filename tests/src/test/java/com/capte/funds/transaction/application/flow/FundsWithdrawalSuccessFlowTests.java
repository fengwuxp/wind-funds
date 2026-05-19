package com.capte.funds.transaction.application.flow;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
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
 * 提现成功业务流测试。
 */
class FundsWithdrawalSuccessFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后提交提现，业务层先冻结资金；外部出款成功后交易层确认提现。
     * 输入：充值 100、冻结 60、提现确认 60。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照和账务事实。
     * 预期：提现确认只消耗已冻结资金，用户 AVAILABLE 不被二次扣减，平台 CASH 表达外部出款。
     * 红线：冻结单不表达消费；提现成功必须作为独立资金事实引用冻结单。
     */
    @Test
    void testTopupFreezeThenWithdrawShouldConsumeFrozenBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "WITHDRAW_SUCCESS_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_SUCCESS_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 60L, freezeSn, "WITHDRAW_SUCCESS_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());

        LedgerTransaction withdrawTransaction = ledgerTransactionByBusinessSn("WITHDRAW_SUCCESS_CONFIRM");
        assertThat(entriesOf(withdrawTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.CASH);
        assertThat(postingPlansOf(withdrawTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactly(LedgerPhaseCode.SETTLEMENT.name(), LedgerPhaseCode.FUND_OUT.name());

        assertThat(frozenOrderByBusinessSn("WITHDRAW_SUCCESS_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_SUCCESS_FREEZE").getReleasedAmount()).isZero();
    }

    /**
     * 场景：用户充值后提交提现，业务层冻结本金；外部出款成功时提现确认同时收取固定手续费。
     * 输入：充值 100、冻结 60、提现确认 60、手续费 5。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT/FEE 余额快照和账务事实。
     * 预期：本金从 FROZEN 出款，手续费从 AVAILABLE 独立扣收并进入平台 FEE。
     * 红线：申请阶段只冻结本金；提现手续费不得混入出款本金，也不得二次扣减 FROZEN。
     */
    @Test
    void testTopupFreezeThenWithdrawWithFeeShouldPostSeparateFeeLeg() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));

        topup(user, 100L, "WITHDRAW_WITH_FEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_WITH_FEE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));

        withdrawWithFixedFee(user, 60L, 5L, freezeSn, "WITHDRAW_WITH_FEE_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 35L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);

        assertPostedTransactions(3);
        LedgerTransaction withdrawTransaction = ledgerTransactionByBusinessSn("WITHDRAW_WITH_FEE_CONFIRM");
        assertThat(entriesOf(withdrawTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.CASH,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FEE);
        assertThat(postingPlansOf(withdrawTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactly(
                        LedgerPhaseCode.SETTLEMENT.name(),
                        LedgerPhaseCode.FUND_OUT.name(),
                        LedgerPhaseCode.FEE.name());

        assertThat(frozenOrderByBusinessSn("WITHDRAW_WITH_FEE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_WITH_FEE_FREEZE").getReleasedAmount()).isZero();

        withdrawWithFixedFee(user, 60L, 5L, freezeSn, "WITHDRAW_WITH_FEE_CONFIRM");
        BalanceSnapshot afterDuplicateWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(),
                feeAccount()));
        assertOnlyBalanceDeltas(afterWithdraw, afterDuplicateWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertPostedTransactions(3);
    }
}
