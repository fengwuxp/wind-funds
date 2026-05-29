package com.capte.funds.transaction.application.flow;

import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
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
 * 提现拒绝或撤销业务流测试。
 */
class FundsWithdrawalRejectionFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后提交提现，业务层先冻结资金；随后通道拒绝或用户撤销提现。
     * 输入：充值 100、冻结 60、解冻 60。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照。
     * 预期：撤销或拒绝只释放冻结资金，用户可用回到充值后的 100，冻结归零。
     * 红线：拒绝/撤销不得生成提现 FUND_OUT 出款事实，不得改变平台现金和预收款口径。
     */
    @Test
    void testTopupFreezeThenWithdrawalRejectedShouldOnlyUnfreeze() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "WITHDRAW_REJECTED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_REJECTED_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 60L, freezeSn, "WITHDRAW_REJECTED_UNFREEZE");
        BalanceSnapshot afterRejected = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejected,
                delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .doesNotContain(FundsTransactionEventType.WITHDRAW.name());

        LedgerTransaction freezeTransaction = ledgerTransactionByBusinessSn("WITHDRAW_REJECTED_FREEZE");
        assertThat(entriesOf(freezeTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN);
        assertThat(entriesOf(freezeTransaction).stream()
                .map(LedgerEntry::getSubjectId)
                .toList())
                .containsOnly(user.id());
        assertThat(postingPlansOf(freezeTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.FREEZE.name());

        LedgerTransaction releaseTransaction = ledgerTransactionByBusinessSn("WITHDRAW_REJECTED_UNFREEZE");
        assertThat(entriesOf(releaseTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.FROZEN, LedgerSubjectCode.AVAILABLE);
        assertThat(entriesOf(releaseTransaction).stream()
                .map(LedgerEntry::getSubjectId)
                .toList())
                .containsOnly(user.id());
        assertThat(postingPlansOf(releaseTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.UNFREEZE.name());

        assertThat(frozenOrderByBusinessSn("WITHDRAW_REJECTED_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_REJECTED_FREEZE").getReleasedAmount()).isEqualTo(60L);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_REJECTED_UNFREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);
        LedgerFactSnapshot afterRejectedFacts = ledgerFactSnapshot();

        unfreeze(user, 60L, freezeSn, "WITHDRAW_REJECTED_UNFREEZE");
        BalanceSnapshot afterDuplicateUnfreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRejected, afterDuplicateUnfreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertPostedTransactions(3);
        assertLedgerTransactionFactsUnchanged(afterRejectedFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_REJECTED_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_REJECTED_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_REJECTED_UNFREEZE", 0, 0, 1, 2);
    }
}
