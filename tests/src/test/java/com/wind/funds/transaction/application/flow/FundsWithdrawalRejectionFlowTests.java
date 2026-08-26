package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerState;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.request.UpdateLedgerStateRequest;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
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
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_REJECTED_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        Long frozenLedgerId = findLedger(user, LedgerSubjectCode.FROZEN).orElseThrow().getId();
        ledgerService.updateLedgerState(new UpdateLedgerStateRequest()
                .setId(frozenLedgerId)
                .setState(LedgerState.SUSPENDED));

        unfreeze(user, 60L, freezeSn, "WITHDRAW_REJECTED_UNFREEZE");
        BalanceSnapshot afterRejected = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejected,
                delta(user, LedgerSubjectCode.AVAILABLE, 60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
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
        assertThat(postingPlansOf(freezeTransaction)).singleElement().satisfies(plan -> {
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.HOLD.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.WITHIN_SUBJECT.name());
        });
        assertThat(entriesOf(freezeTransaction)).allSatisfy(entry -> {
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.HOLD.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.WITHIN_SUBJECT.name());
        });

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
        assertThat(postingPlansOf(releaseTransaction)).singleElement().satisfies(plan -> {
            assertThat(plan.getIntent()).isEqualTo(LedgerPostingIntentType.RELEASE.name());
            assertThat(plan.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_RELEASE.name());
        });
        assertThat(entriesOf(releaseTransaction)).allSatisfy(entry -> {
            assertThat(entry.getIntent()).isEqualTo(LedgerPostingIntentType.RELEASE.name());
            assertThat(entry.getPostingScope()).isEqualTo(LedgerPostingScope.CONTROL_RELEASE.name());
        });

        RouteSnapshotSpec freezeSnapshot = fundsTransactionQueryService
                .findRouteSnapshotByFreezeOrderSn(TENANT_ID, freezeSn)
                .orElseThrow();
        assertThat(freezeSnapshot.getLegs()).singleElement();
        String releaseSn = frozenOrderByBusinessSn("WITHDRAW_REJECTED_UNFREEZE").getSn();
        RouteSnapshotSpec releaseSnapshot = fundsTransactionQueryService
                .findRouteSnapshotByFreezeOrderSn(TENANT_ID, releaseSn)
                .orElseThrow();
        assertThat(releaseSnapshot.getLegs()).singleElement().satisfies(releaseLeg ->
                assertThat(releaseLeg.getReplayRefLegId()).isEqualTo(freezeSnapshot.getLegs().getFirst().getLegId()));
        assertThat(postingPlansOf(freezeTransaction)).singleElement().satisfies(plan ->
                assertThat(plan.getRouteLegId()).isEqualTo(freezeSnapshot.getLegs().getFirst().getLegId()));
        assertThat(postingPlansOf(releaseTransaction)).singleElement().satisfies(plan ->
                assertThat(plan.getRouteLegId()).isEqualTo(releaseSnapshot.getLegs().getFirst().getLegId()));

        assertThat(frozenOrderByBusinessSn("WITHDRAW_REJECTED_FREEZE").getState())
                .isEqualTo(FundsFrozenOrderState.RELEASED);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_REJECTED_FREEZE").getReleasedAmount()).isEqualTo(60L);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_REJECTED_UNFREEZE").getState())
                .isEqualTo(FundsFrozenOrderState.RELEASED);
        assertThat(ledgerService.getLedgerById(frozenLedgerId).getState()).isEqualTo(LedgerState.SUSPENDED);
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
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_100L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertPostedTransactions(3);
        assertLedgerTransactionFactsUnchanged(afterRejectedFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_REJECTED_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_REJECTED_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_REJECTED_UNFREEZE", 0, 0, 1, 2);
    }
}
