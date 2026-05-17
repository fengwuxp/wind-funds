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

}
