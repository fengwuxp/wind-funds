package com.wind.funds.transaction.application.flow;

import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多主体直接交易组合业务流测试。
 */
class FundsTransferPayWithdrawChainFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：A 充值后转给 B，B 付款给普通收款方，随后 B 发起提现并由外部出款确认。
     * 输入：A 充值 200、A 转 B 120、B 付款 50、B 冻结 40、B 提现确认 40。
     * 输出：A、B、收款方、平台 CASH/PREPAYMENT 的余额快照和账务事实。
     * 预期：B 的付款与提现都只消耗自身余额，提现必须先冻结再确认出款。
     * 红线：跨主体组合链路不能串账；提现确认不得二次扣减 B 的 AVAILABLE。
     */
    @Test
    void testTopupTransferPayThenWithdrawShouldKeepEachSubjectBalanceIndependent() {
        FundsAccountId accountA = fundingAccount("funding_user");
        FundsAccountId accountB = fundingAccount("funding_b");
        FundsAccountId merchant = fundingAccount("merchant_payee");
        ensureLedger(accountB, LedgerSubjectCode.AVAILABLE);
        ensureLedger(accountB, LedgerSubjectCode.FROZEN);
        ensureLedger(merchant, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(accountA, accountB, merchant, cashMappingAccount(),
                prepaymentAccount()));

        topup(accountA, 200L, "CHAIN_TRANSFER_PAY_WITHDRAW_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(accountA, accountB, merchant, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(accountA, LedgerSubjectCode.AVAILABLE, 200L, CURRENCY),
                delta(accountA, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -200L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        transfer(accountA, accountB, 120L, "CHAIN_TRANSFER_PAY_WITHDRAW_TRANSFER");
        BalanceSnapshot afterTransfer = snapshot(balances(accountA, accountB, merchant, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterTransfer,
                delta(accountA, LedgerSubjectCode.AVAILABLE, -120L, CURRENCY),
                delta(accountA, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.AVAILABLE, 120L, CURRENCY),
                delta(accountB, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(accountB, merchant, LedgerSubjectCode.SETTLEMENT, 50L, "CHAIN_TRANSFER_PAY_WITHDRAW_PAY");
        BalanceSnapshot afterPay = snapshot(balances(accountA, accountB, merchant, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTransfer, afterPay,
                delta(accountA, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountA, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.AVAILABLE, -50L, CURRENCY),
                delta(accountB, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(accountB, 40L, "CHAIN_TRANSFER_PAY_WITHDRAW_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(accountA, accountB, merchant, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFreeze,
                delta(accountA, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountA, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(accountB, LedgerSubjectCode.FROZEN, 40L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(accountB, 40L, freezeSn, "CHAIN_TRANSFER_PAY_WITHDRAW_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(accountA, accountB, merchant, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(accountA, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountA, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(accountB, LedgerSubjectCode.FROZEN, -40L, CURRENCY),
                delta(merchant, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(accountA), LedgerSubjectCode.AVAILABLE, 80L, CURRENCY);
        assertBucket(balance(accountA), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(accountB), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(accountB), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(merchant), LedgerSubjectCode.SETTLEMENT, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_840L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(5);
        assertSingleFundsAndLedgerFactsForBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("CHAIN_TRANSFER_PAY_WITHDRAW_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_TRANSFER", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("CHAIN_TRANSFER_PAY_WITHDRAW_TRANSFER");
        assertSingleFundsAndLedgerFactsForBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_PAY", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("CHAIN_TRANSFER_PAY_WITHDRAW_PAY");
        assertFundsAndLedgerFactsForBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_CONFIRM", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("CHAIN_TRANSFER_PAY_WITHDRAW_CONFIRM");
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.TRANSFER.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());

        LedgerTransaction transferTransaction = ledgerTransactionByBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_TRANSFER");
        assertThat(entriesOf(transferTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(transferTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.TRANSFER.name());

        LedgerTransaction payTransaction = ledgerTransactionByBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_PAY");
        assertThat(entriesOf(payTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.SETTLEMENT);
        assertThat(postingPlansOf(payTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.SETTLEMENT.name());

        LedgerTransaction freezeTransaction = ledgerTransactionByBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_FREEZE");
        assertThat(freezeTransaction.getTransactionType())
                .isEqualTo(DefaultFundsTransactionType.BALANCE_CONTROL.name());
        assertThat(entriesOf(freezeTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN);
        assertThat(entriesOf(freezeTransaction).stream()
                .map(LedgerEntry::getSubjectId)
                .toList())
                .containsOnly(accountB.id());
        assertThat(postingPlansOf(freezeTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.FREEZE.name());

        LedgerTransaction withdrawTransaction = ledgerTransactionByBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_CONFIRM");
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

        assertThat(frozenOrderByBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_FREEZE").getState())
                .isEqualTo(FundsFrozenOrderState.FROZEN);
        assertThat(frozenOrderByBusinessSn("CHAIN_TRANSFER_PAY_WITHDRAW_FREEZE").getReleasedAmount()).isZero();
    }
}
