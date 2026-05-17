package com.capte.funds.transaction.application.flow;

import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

class FundsAuthorizationBusinessFlowTests extends FundsTransactionBusinessFlowTestSupport {

    /**
     * 场景：外部授权问询通过后，发生部分撤销、部分结算，再对结算事实做部分退款。
     * 输入：信用账户初始可用 500，授权 100，撤销 20，结算 60，退款 30。
     * 输出：信用账户 AVAILABLE/AUTHORIZATION、平台 SETTLEMENT 余额快照。
     * 预期：授权只占用 AVAILABLE 到 AUTHORIZATION；撤销释放 20；结算捕获 60；退款回补 30。
     * 红线：普通授权结算和退款不得触碰 LIMIT，且每次回放只使用本次金额。
     */
    @Test
    void testAuthorizationPartialReversalSettleRefundShouldKeepLedgerBalances() {
        FundsAccountId credit = creditAccount("credit_auth_001");
        FundsAccountId settlement = settlementAccount();
        BalanceSnapshot before = snapshot(balances(credit, settlement));

        String authorizationSn = authorize(credit, 100L, "AUTH_PARTIAL_CHAIN_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(credit, settlement));
        assertOnlyBalanceDeltas(before, afterAuthorize,
                delta(credit, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerTransactionSpec authorizationTransaction = ledgerBook.postedTransactions.getFirst();
        assertEntriesForSubject(authorizationTransaction, credit, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION);
        assertNoLedgerSubject(authorizationTransaction, LedgerSubjectCode.LIMIT);

        reversal(credit, 20L, authorizationSn, "AUTH_PARTIAL_CHAIN_REVERSAL");
        BalanceSnapshot afterReversal = snapshot(balances(credit, settlement));
        assertOnlyBalanceDeltas(afterAuthorize, afterReversal,
                delta(credit, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, -20L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerTransactionSpec reversalTransaction = ledgerBook.postedTransactions.get(1);
        assertEntriesForSubject(reversalTransaction, credit, LedgerSubjectCode.AUTHORIZATION,
                LedgerSubjectCode.AVAILABLE);
        assertNoLedgerSubject(reversalTransaction, LedgerSubjectCode.LIMIT);

        String settlementSn = settle(credit, 60L, authorizationSn, "AUTH_PARTIAL_CHAIN_SETTLE");
        BalanceSnapshot afterSettlement = snapshot(balances(credit, settlement));
        assertOnlyBalanceDeltas(afterReversal, afterSettlement,
                delta(credit, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, -60L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, 60L, CURRENCY));
        LedgerTransactionSpec settlementTransaction = ledgerBook.postedTransactions.get(2);
        assertEntriesForSubject(settlementTransaction, credit, LedgerSubjectCode.AUTHORIZATION);
        assertEntriesForSubject(settlementTransaction, settlement, LedgerSubjectCode.SETTLEMENT);
        assertNoLedgerSubject(settlementTransaction, LedgerSubjectCode.LIMIT);

        authRefund(credit, 30L, settlementSn, "AUTH_PARTIAL_CHAIN_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(credit, settlement));
        assertOnlyBalanceDeltas(afterSettlement, afterRefund,
                delta(credit, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY));
        LedgerTransactionSpec refundTransaction = ledgerBook.postedTransactions.get(3);
        assertEntriesForSubject(refundTransaction, credit, LedgerSubjectCode.AVAILABLE);
        assertEntriesForSubject(refundTransaction, settlement, LedgerSubjectCode.SETTLEMENT);
        assertNoLedgerSubject(refundTransaction, LedgerSubjectCode.LIMIT);

        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AVAILABLE, 450L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AUTHORIZATION, 20L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.LIMIT, 100L, CURRENCY);
        assertBucket(ledgerBook.balance(settlement), LedgerSubjectCode.SETTLEMENT, 30L, CURRENCY);
        assertPostedTransactions(4);
    }

    /**
     * 场景：外部授权问询通过后直接按原授权全额结算。
     * 输入：信用账户初始可用 500，授权 100，结算 100。
     * 输出：信用账户 AVAILABLE/AUTHORIZATION、平台 SETTLEMENT 余额快照。
     * 预期：授权占用后直接消费 AUTHORIZATION，结算目标入平台 SETTLEMENT。
     * 红线：授权直接结算必须回放原授权路径，不得重新占用 AVAILABLE，也不得触碰 LIMIT。
     */
    @Test
    void testAuthorizationDirectSettleShouldReplayOriginalRouteWithoutLimit() {
        FundsAccountId credit = creditAccount("credit_auth_001");
        FundsAccountId settlement = settlementAccount();
        BalanceSnapshot before = snapshot(balances(credit, settlement));

        String authorizationSn = authorize(credit, 100L, "AUTH_DIRECT_SETTLE_AUTHORIZE");
        BalanceSnapshot afterAuthorize = snapshot(balances(credit, settlement));
        assertOnlyBalanceDeltas(before, afterAuthorize,
                delta(credit, LedgerSubjectCode.AVAILABLE, -100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 100L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        LedgerTransactionSpec authorizationTransaction = ledgerBook.postedTransactions.getFirst();
        assertEntriesForSubject(authorizationTransaction, credit, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION);
        assertNoLedgerSubject(authorizationTransaction, LedgerSubjectCode.LIMIT);

        String settlementSn = settle(credit, 100L, authorizationSn, "AUTH_DIRECT_SETTLE_SETTLE");
        BalanceSnapshot afterSettlement = snapshot(balances(credit, settlement));
        assertThat(settlementSn).isNotBlank();
        assertOnlyBalanceDeltas(afterAuthorize, afterSettlement,
                delta(credit, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, -100L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, 100L, CURRENCY));
        LedgerTransactionSpec settlementTransaction = ledgerBook.postedTransactions.get(1);
        assertEntriesForSubject(settlementTransaction, credit, LedgerSubjectCode.AUTHORIZATION);
        assertEntriesForSubject(settlementTransaction, settlement, LedgerSubjectCode.SETTLEMENT);
        assertNoLedgerSubject(settlementTransaction, LedgerSubjectCode.LIMIT);

        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AVAILABLE, 400L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.LIMIT, 100L, CURRENCY);
        assertBucket(ledgerBook.balance(settlement), LedgerSubjectCode.SETTLEMENT, 100L, CURRENCY);
        assertPostedTransactions(2);
    }

    /**
     * 场景：外部授权问询被拒绝。
     * 输入：信用账户初始可用 500，授权请求 100，授权结果为拒绝。
     * 输出：信用账户 AVAILABLE/AUTHORIZATION/LIMIT、平台 SETTLEMENT 余额快照。
     * 预期：拒绝事实不生成入账交易，余额保持不变，route snapshot 不包含 route leg。
     * 红线：授权拒付不得改余额、不得生成 entry，也不得按争议拒付或授权结算路径处理。
     */
    @Test
    void testAuthorizationDeclinedShouldNotPostLedgerOrChangeBalances() {
        FundsAccountId credit = creditAccount("credit_auth_001");
        FundsAccountId settlement = settlementAccount();
        BalanceSnapshot before = snapshot(balances(credit, settlement));

        String transactionSn = authorizeDeclined(credit, 100L, "insufficient_funds",
                "AUTH_DECLINED_NO_POSTING");

        BalanceSnapshot after = snapshot(balances(credit, settlement));
        assertThat(transactionSn).isNotBlank();
        assertOnlyBalanceDeltas(before, after,
                delta(credit, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(settlement, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY));
        RouteSnapshotSpec declinedRoute = lifecycleSaver.routeSnapshots.get(transactionSn);
        assertThat(declinedRoute).isNotNull();
        assertThat(declinedRoute.getLegs()).isEmpty();
        assertThat(ledgerBook.postedTransactions).isEmpty();
        assertThat(lifecycleSaver.succeededLedgerTransactionSns).isEmpty();
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AVAILABLE, 500L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(ledgerBook.balance(credit), LedgerSubjectCode.LIMIT, 100L, CURRENCY);
        assertBucket(ledgerBook.balance(settlement), LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY);
    }

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
