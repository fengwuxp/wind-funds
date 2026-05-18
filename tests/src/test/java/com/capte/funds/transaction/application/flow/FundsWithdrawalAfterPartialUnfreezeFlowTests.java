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
 * 部分解冻后提现业务流测试。
 */
class FundsWithdrawalAfterPartialUnfreezeFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后冻结提现资金，随后部分解冻，最终只对剩余冻结金额确认出款。
     * 输入：充值 100、冻结 80、解冻 30、提现确认 50。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照和账务事实。
     * 预期：已解冻金额回到 AVAILABLE；提现只能消耗剩余 FROZEN，不能再次扣减 AVAILABLE。
     * 红线：冻结单释放状态只表达解冻，不表达后续提现消费。
     */
    @Test
    void testTopupFreezePartialUnfreezeThenWithdrawShouldConsumeRemainingFrozenBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "WITHDRAW_PARTIAL_UNFREEZE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 80L, "WITHDRAW_PARTIAL_UNFREEZE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -80L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 80L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 30L, freezeSn, "WITHDRAW_PARTIAL_UNFREEZE_RELEASE");
        BalanceSnapshot afterRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRelease,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 50L, freezeSn, "WITHDRAW_PARTIAL_UNFREEZE_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRelease, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.UNFREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());

        LedgerTransaction releaseTransaction = ledgerTransactionByBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_RELEASE");
        assertThat(entriesOf(releaseTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.FROZEN, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(releaseTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.UNFREEZE.name());

        LedgerTransaction withdrawTransaction = ledgerTransactionByBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_CONFIRM");
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

        assertThat(frozenOrderByBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.PARTIALLY_RELEASED);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_FREEZE").getReleasedAmount()).isEqualTo(30L);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_RELEASE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);
    }
}
