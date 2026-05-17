package com.capte.funds.transaction.application.flow;

import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;

class FundsBalanceControlBusinessFlowTests extends FundsTransactionBusinessFlowTestSupport {

    /**
     * 场景：运营在同一批次内对普通资金账户、信用账户和预算组做调账/调额。
     * 输入：资金账户调增 30、信用账户可用额度调增 100、预算组可用预算调减 40。
     * 输出：资金账户 AVAILABLE、平台 ADJUSTMENT、信用/预算 LIMIT 与 AVAILABLE 控制桶。
     * 预期：普通资金调账通过平台 ADJUSTMENT 平衡，信用/预算只在自身控制桶内调整。
     * 红线：普通资金调账不得触碰 LIMIT，LIMIT 只出现在 LIMIT_ADJUST 路径。
     */
    @Test
    void testFundingCreditBudgetAdjustShouldKeepControlBoundaries() {
        FundsAccountId funding = fundingAccount("funding_adjust_user");
        FundsAccountId credit = creditAccount("credit_001");
        FundsAccountId budgetGroup = budgetGroup("budget_001");
        FundsAccountId adjustment = adjustmentAccount();
        BalanceSnapshot before = snapshot(balances(funding, credit, budgetGroup, adjustment));

        adjust(funding, 30L, true, "ADJUST", "ADJUST_COMBO_FUNDING");
        BalanceSnapshot afterFundingAdjust = snapshot(balances(funding, credit, budgetGroup, adjustment));
        assertOnlyBalanceDeltas(before, afterFundingAdjust,
                delta(funding, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(adjustment, LedgerSubjectCode.ADJUSTMENT, -30L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
        LedgerTransactionSpec fundingAdjust = ledgerBook.postedTransactions.getFirst();
        assertEntriesForSubject(fundingAdjust, funding, LedgerSubjectCode.AVAILABLE);
        assertEntriesForSubject(fundingAdjust, adjustment, LedgerSubjectCode.ADJUSTMENT);
        assertNoLedgerSubject(fundingAdjust, LedgerSubjectCode.LIMIT);

        adjust(credit, 100L, true, "LIMIT", "ADJUST_COMBO_CREDIT");
        BalanceSnapshot afterCreditAdjust = snapshot(balances(funding, credit, budgetGroup, adjustment));
        assertOnlyBalanceDeltas(afterFundingAdjust, afterCreditAdjust,
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(adjustment, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, -100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY));
        LedgerTransactionSpec creditAdjust = ledgerBook.postedTransactions.get(1);
        assertEntriesForSubject(creditAdjust, credit, LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE);
        assertNoEntriesForSubject(creditAdjust, adjustment);

        adjust(budgetGroup, 40L, false, "BUDGET", "ADJUST_COMBO_BUDGET");
        BalanceSnapshot afterBudgetAdjust = snapshot(balances(funding, credit, budgetGroup, adjustment));
        assertOnlyBalanceDeltas(afterCreditAdjust, afterBudgetAdjust,
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(adjustment, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.LIMIT, 40L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY));
        LedgerTransactionSpec budgetAdjust = ledgerBook.postedTransactions.get(2);
        assertEntriesForSubject(budgetAdjust, budgetGroup, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT);
        assertNoEntriesForSubject(budgetAdjust, adjustment);

        assertPostedTransactions(3);
    }

}
