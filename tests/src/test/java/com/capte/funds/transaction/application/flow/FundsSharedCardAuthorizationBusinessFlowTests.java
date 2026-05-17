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
import static org.assertj.core.api.Assertions.assertThat;

class FundsSharedCardAuthorizationBusinessFlowTests extends FundsTransactionBusinessFlowTestSupport {

    /**
     * 场景：共享卡授权同时占用信用账户、预算组和真实资金账户。
     * 输入：信用账户、预算组、真实资金账户各自初始可用余额充足，授权 60。
     * 输出：三个主体的 AVAILABLE/AUTHORIZATION 余额快照。
     * 预期：三个主体都独立从 AVAILABLE 占用到 AUTHORIZATION，并生成独立可追溯的 posting plan。
     * 红线：共享卡授权不得触碰 LIMIT，也不得把多主体授权合并成一笔不透明分录。
     */
    @Test
    void testSharedCardAuthorizationShouldHoldLinkedControlAndFundingSubjects() {
        FundsAccountId credit = creditAccount("credit_shared_001");
        FundsAccountId budgetGroup = budgetGroup("budget_shared_001");
        FundsAccountId funding = fundingAccount("funding_shared_001");
        BalanceSnapshot before = snapshot(balances(credit, budgetGroup, funding));

        authorizeSharedCard(credit, budgetGroup, funding, 60L, "AUTH_SHARED_CARD_AUTHORIZE");

        BalanceSnapshot after = snapshot(balances(credit, budgetGroup, funding));
        assertOnlyBalanceDeltas(before, after,
                delta(credit, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY),
                delta(budgetGroup, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY));
        LedgerTransactionSpec authorizationTransaction = ledgerBook.postedTransactions.getFirst();
        assertEntriesForSubject(authorizationTransaction, credit, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION);
        assertEntriesForSubject(authorizationTransaction, budgetGroup, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION);
        assertEntriesForSubject(authorizationTransaction, funding, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION);
        assertNoLedgerSubject(authorizationTransaction, LedgerSubjectCode.LIMIT);

        assertThat(authorizationTransaction.getPostingPlans()).hasSize(3);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AVAILABLE, 440L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.LIMIT, 300L, CURRENCY);
        assertBucket(ledgerBook.balance(budgetGroup), LedgerSubjectCode.AVAILABLE, 340L, CURRENCY);
        assertBucket(ledgerBook.balance(budgetGroup), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(ledgerBook.balance(funding), LedgerSubjectCode.AVAILABLE, 240L, CURRENCY);
        assertBucket(ledgerBook.balance(funding), LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertPostedTransactions(1);
    }
}
