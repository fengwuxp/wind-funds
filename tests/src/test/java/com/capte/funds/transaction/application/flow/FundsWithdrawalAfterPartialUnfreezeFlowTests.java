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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_RELEASE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_UNFREEZE_CONFIRM", 3, 4);
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

    /**
     * 场景：同一冻结单已部分解冻，随后用新的业务流水提现并超过剩余冻结金额。
     * 输入：充值 120、冻结 90、解冻 40、提现 60。
     * 输出：提现失败；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持解冻后的状态。
     * 预期：提现确认必须把已解冻金额计入同一冻结来源累计处理上限。
     * 红线：同一冻结来源累计解冻和提现不得超过原冻结金额；失败请求不得生成出款账务事实。
     */
    @Test
    void testWithdrawAfterPartialUnfreezeExceedingFreezeSourceRemainingShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        topup(user, 120L, "WITHDRAW_AFTER_RELEASE_TOPUP");
        String freezeSn = freeze(user, 90L, "WITHDRAW_AFTER_RELEASE_FREEZE");
        unfreeze(user, 40L, freezeSn, "WITHDRAW_AFTER_RELEASE_UNFREEZE");
        BalanceSnapshot afterRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterReleaseFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> withdraw(user, 60L, freezeSn, "WITHDRAW_AFTER_RELEASE_CONFIRM"))
                .hasMessageContaining("冻结单剩余可提现金额不足");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRelease, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 70L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_880L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_AFTER_RELEASE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_AFTER_RELEASE_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_AFTER_RELEASE_UNFREEZE", 0, 0, 1, 2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.UNFREEZE.name());
        assertThat(frozenOrderExistsByBusinessSn("WITHDRAW_AFTER_RELEASE_CONFIRM"))
                .isFalse();
        assertThat(frozenOrderByBusinessSn("WITHDRAW_AFTER_RELEASE_FREEZE"))
                .satisfies(order -> {
                    assertThat(order.getStatus()).isEqualTo(FundsFrozenOrderStatus.PARTIALLY_RELEASED);
                    assertThat(order.getReleasedAmount()).isEqualTo(40L);
                });
        assertThat(frozenOrderByBusinessSn("WITHDRAW_AFTER_RELEASE_UNFREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);
        assertLedgerTransactionFactsUnchanged(afterReleaseFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_AFTER_RELEASE_CONFIRM");
    }

    /**
     * 场景：同一冻结单已全额解冻，随后用新的业务流水再次确认提现。
     * 输入：充值 100、冻结 60、全额解冻 60、提现确认 60。
     * 输出：提现失败；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持解冻后的状态。
     * 预期：提现确认必须把全额解冻计入同一冻结来源累计处理上限。
     * 红线：已释放冻结来源不得再次出款；失败请求不得生成提现 route、posting 或账务事实。
     */
    @Test
    void testWithdrawAfterFullUnfreezeShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        topup(user, 100L, "WITHDRAW_AFTER_FULL_RELEASE_TOPUP");
        String freezeSn = freeze(user, 60L, "WITHDRAW_AFTER_FULL_RELEASE_FREEZE");
        unfreeze(user, 60L, freezeSn, "WITHDRAW_AFTER_FULL_RELEASE_UNFREEZE");
        BalanceSnapshot afterRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterReleaseFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> withdraw(user, 60L, freezeSn, "WITHDRAW_AFTER_FULL_RELEASE_CONFIRM"))
                .hasMessageContaining("冻结单剩余可提现金额不足");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRelease, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_UNFREEZE", 0, 0, 1, 2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.UNFREEZE.name());
        assertThat(frozenOrderExistsByBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_CONFIRM"))
                .isFalse();
        assertThat(frozenOrderByBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_FREEZE"))
                .satisfies(order -> {
                    assertThat(order.getStatus()).isEqualTo(FundsFrozenOrderStatus.RELEASED);
                    assertThat(order.getReleasedAmount()).isEqualTo(60L);
                });
        assertThat(frozenOrderByBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_UNFREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);
        assertLedgerTransactionFactsUnchanged(afterReleaseFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_AFTER_FULL_RELEASE_CONFIRM");
    }

    /**
     * 场景：用户充值后冻结资金，随后分两次解冻，并尝试超过剩余冻结金额的第三次解冻。
     * 输入：充值 120、冻结 90、解冻 20、解冻 30、超额解冻 50。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照和冻结单释放状态。
     * 预期：两次解冻逐步释放冻结金额，超额解冻失败且无余额副作用。
     * 红线：累计解冻不得超过原冻结剩余金额；失败请求不得生成新的入账事实。
     */
    @Test
    void testTopupFreezeMultipleUnfreezeThenExceedShouldKeepBalanceUnchanged() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 120L, "BALANCE_MULTI_UNFREEZE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 120L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -120L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 90L, "BALANCE_MULTI_UNFREEZE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -90L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 90L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 20L, freezeSn, "BALANCE_MULTI_UNFREEZE_RELEASE_1");
        BalanceSnapshot afterFirstRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFirstRelease,
                delta(user, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 30L, freezeSn, "BALANCE_MULTI_UNFREEZE_RELEASE_2");
        BalanceSnapshot afterSecondRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstRelease, afterSecondRelease,
                delta(user, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 80L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 40L, CURRENCY);
        LedgerFactSnapshot afterSecondReleaseFacts = ledgerFactSnapshot();

        assertThat(frozenOrderByBusinessSn("BALANCE_MULTI_UNFREEZE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.PARTIALLY_RELEASED);
        assertThat(frozenOrderByBusinessSn("BALANCE_MULTI_UNFREEZE_FREEZE").getReleasedAmount()).isEqualTo(50L);
        assertThat(frozenOrderByBusinessSn("BALANCE_MULTI_UNFREEZE_RELEASE_1").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);
        assertThat(frozenOrderByBusinessSn("BALANCE_MULTI_UNFREEZE_RELEASE_2").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.RELEASED);

        assertThatThrownBy(() -> unfreeze(user, 50L, freezeSn, "BALANCE_MULTI_UNFREEZE_EXCEED"))
                .hasMessageContaining("冻结单剩余可释放金额不足");
        BalanceSnapshot afterExceed = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterSecondRelease, afterExceed,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertPostedTransactions(4);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_MULTI_UNFREEZE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_MULTI_UNFREEZE_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_MULTI_UNFREEZE_RELEASE_1", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_MULTI_UNFREEZE_RELEASE_2", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_MULTI_UNFREEZE_FREEZE").getReleasedAmount()).isEqualTo(50L);
        assertLedgerTransactionFactsUnchanged(afterSecondReleaseFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_MULTI_UNFREEZE_EXCEED");
    }
}
