package com.capte.funds.transaction.application.flow;

import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);

        assertPostedTransactions(3);
        LedgerTransactionSpec releaseTransaction = ledgerBook.postedTransactions.get(2);
        assertThat(entriesOf(releaseTransaction).stream()
                .map(LedgerEntrySpec::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.FROZEN, LedgerSubjectCode.AVAILABLE);
        assertThat(releaseTransaction.getPostingPlans().stream()
                .map(LedgerPostingPlanSpec::getPostingPhases)
                .flatMap(List::stream)
                .map(phase -> phase.getPhaseCode())
                .toList())
                .containsOnly(LedgerPhaseCode.UNFREEZE);
    }

    private static List<LedgerEntrySpec> entriesOf(LedgerTransactionSpec transaction) {
        return transaction.getPostingPlans().stream()
                .map(LedgerPostingPlanSpec::getEntries)
                .flatMap(List::stream)
                .toList();
    }
}
