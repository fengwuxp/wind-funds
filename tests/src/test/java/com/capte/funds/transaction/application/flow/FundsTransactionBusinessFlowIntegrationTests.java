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

class FundsTransactionBusinessFlowIntegrationTests extends FundsTransactionBusinessFlowTestSupport {

    /**
     * 场景：用户充值后付款给商户，后续由商户原路退款一部分。
     * 输入：充值 100、付款 40、退款 20。
     * 输出：用户 AVAILABLE、商户 SETTLEMENT、平台 CASH/PREPAYMENT 余额快照。
     * 预期：最终用户可用余额 +80，商户结算余额 +20，平台预收款归零，所有已入账交易借贷平衡。
     */
    @Test
    void testTopupPayRefundShouldKeepLedgerBalances() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId merchant = fundingAccount("merchant_001");
        BalanceSnapshot before = snapshot(balances(user, merchant, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "TOPUP_PAY_REFUND_TOPUP");
        pay(user, merchant, 40L, "TOPUP_PAY_REFUND_PAY");
        refund(user, merchant, 20L, "TOPUP_PAY_REFUND_REFUND");

        BalanceSnapshot after = snapshot(balances(user, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(user, LedgerSubjectCode.AVAILABLE, 80L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 80L, CURRENCY);
        assertBucket(ledgerBook.balance(merchant), LedgerSubjectCode.SETTLEMENT, 20L, CURRENCY);
        assertPostedTransactions(3);
    }

    /**
     * 场景：用户充值后先冻结余额，再确认提现出款。
     * 输入：充值 100、冻结 60、提现 60。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照。
     * 预期：提现成功扣减已冻结的 FROZEN，完成后用户可用余额保留 40，冻结余额归零。
     */
    @Test
    void testTopupFreezeWithdrawShouldKeepLedgerBalances() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "TOPUP_FREEZE_WITHDRAW_TOPUP");
        freeze(user, 60L, "TOPUP_FREEZE_WITHDRAW_FREEZE");
        withdraw(user, 60L, "TOPUP_FREEZE_WITHDRAW_WITHDRAW");

        BalanceSnapshot after = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(3);
    }

    /**
     * 场景：用户充值后一次冻结余额，再按同一冻结事实分两次释放。
     * 输入：充值 100、冻结 60、解冻 20、解冻 40。
     * 输出：用户 AVAILABLE/FROZEN 和平台 CASH/PREPAYMENT 余额快照。
     * 预期：冻结后可用减少且冻结增加，两次解冻只在同主体 AVAILABLE/FROZEN 间回转，最终冻结归零。
     * 红线：多次解冻必须引用原冻结事实回放原路径，不得表达消费、扣划或跨主体价值转移。
     */
    @Test
    void testFreezeOnceUnfreezeTwiceShouldReplayOriginalFreezePath() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "FREEZE_UNFREEZE_TWICE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "FREEZE_UNFREEZE_TWICE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 20L, freezeSn, "FREEZE_UNFREEZE_TWICE_UNFREEZE_1");
        BalanceSnapshot afterFirstUnfreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFirstUnfreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 40L, freezeSn, "FREEZE_UNFREEZE_TWICE_UNFREEZE_2");
        BalanceSnapshot afterSecondUnfreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstUnfreeze, afterSecondUnfreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(ledgerBook.balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(4);
    }

    /**
     * 场景：用户 A 充值后转给 B，B 支付给商户，并把剩余资金冻结后提现。
     * 输入：A 充值 150、A 转 B 100、B 付款 40、B 冻结 60、B 提现 60。
     * 输出：A/B/商户/平台的余额快照。
     * 预期：A 保留 50，B 可用和冻结归零，商户结算余额 +40，平台 CASH 净减少 90。
     */
    @Test
    void testTopupTransferPayWithdrawShouldKeepLedgerBalances() {
        FundsAccountId userA = fundingAccount("funding_user_a");
        FundsAccountId userB = fundingAccount("funding_user_b");
        FundsAccountId merchant = fundingAccount("merchant_001");
        BalanceSnapshot before = snapshot(balances(userA, userB, merchant, cashMappingAccount(), prepaymentAccount()));

        topup(userA, 150L, "TOPUP_TRANSFER_PAY_WITHDRAW_TOPUP");
        transfer(userA, userB, 100L, "TOPUP_TRANSFER_PAY_WITHDRAW_TRANSFER");
        pay(userB, merchant, 40L, "TOPUP_TRANSFER_PAY_WITHDRAW_PAY");
        freeze(userB, 60L, "TOPUP_TRANSFER_PAY_WITHDRAW_FREEZE");
        withdraw(userB, 60L, "TOPUP_TRANSFER_PAY_WITHDRAW_WITHDRAW");

        BalanceSnapshot after = snapshot(balances(userA, userB, merchant, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, after,
                delta(userA, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(userB, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(userB, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -90L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(ledgerBook.balance(userA), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(ledgerBook.balance(userB), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(ledgerBook.balance(merchant), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertPostedTransactions(5);
    }

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
