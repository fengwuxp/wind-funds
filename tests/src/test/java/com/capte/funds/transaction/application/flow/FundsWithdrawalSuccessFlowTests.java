package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.ledger.dal.entities.LedgerEntry;
import com.capte.funds.ledger.dal.entities.LedgerPostingPlan;
import com.capte.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 提现成功业务流测试。
 */
class FundsWithdrawalSuccessFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后提交提现，业务层先冻结资金；外部出款成功后交易层确认提现。
     * 输入：充值 100、冻结 60、提现确认 60。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 余额快照和账务事实。
     * 预期：提现确认只消耗已冻结资金，用户 AVAILABLE 不被二次扣减，平台 CASH 表达外部出款。
     * 红线：冻结单不表达消费；提现成功必须作为独立资金事实引用冻结单。
     */
    @Test
    void testTopupFreezeThenWithdrawShouldConsumeFrozenBalance() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 100L, "WITHDRAW_SUCCESS_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_SUCCESS_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 60L, freezeSn, "WITHDRAW_SUCCESS_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());

        LedgerTransaction withdrawTransaction = ledgerTransactionByBusinessSn("WITHDRAW_SUCCESS_CONFIRM");
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

        assertThat(frozenOrderByBusinessSn("WITHDRAW_SUCCESS_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_SUCCESS_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_SUCCESS_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_SUCCESS_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_SUCCESS_CONFIRM", 3, 4);
    }

    /**
     * 场景：用户充值后提交提现，业务层冻结本金；外部出款成功时提现确认同时收取固定手续费。
     * 输入：充值 100、冻结 60、提现确认 60、手续费 5。
     * 输出：用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT/FEE 余额快照和账务事实。
     * 预期：本金从 FROZEN 出款，手续费从 AVAILABLE 独立扣收并进入平台 FEE。
     * 红线：申请阶段只冻结本金；提现手续费不得混入出款本金，也不得二次扣减 FROZEN。
     */
    @Test
    void testTopupFreezeThenWithdrawWithFeeShouldPostSeparateFeeLeg() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));

        topup(user, 100L, "WITHDRAW_WITH_FEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_WITH_FEE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));

        withdrawWithFixedFee(user, 60L, 5L, freezeSn, "WITHDRAW_WITH_FEE_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), feeAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 35L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);

        assertPostedTransactions(3);
        LedgerTransaction withdrawTransaction = ledgerTransactionByBusinessSn("WITHDRAW_WITH_FEE_CONFIRM");
        assertThat(entriesOf(withdrawTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.PREPAYMENT,
                        LedgerSubjectCode.CASH,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FEE);
        assertThat(postingPlansOf(withdrawTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactly(
                        LedgerPhaseCode.SETTLEMENT.name(),
                        LedgerPhaseCode.FUND_OUT.name(),
                        LedgerPhaseCode.FEE.name());

        assertThat(frozenOrderByBusinessSn("WITHDRAW_WITH_FEE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_WITH_FEE_FREEZE").getReleasedAmount()).isZero();

        withdrawWithFixedFee(user, 60L, 5L, freezeSn, "WITHDRAW_WITH_FEE_CONFIRM");
        BalanceSnapshot afterDuplicateWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(),
                feeAccount()));
        assertOnlyBalanceDeltas(afterWithdraw, afterDuplicateWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY));
        assertPostedTransactions(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_WITH_FEE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_WITH_FEE_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_WITH_FEE_CONFIRM", 4, 3, 6);
    }

    /**
     * 场景：提现确认请求把敏感账户值放入扩展上下文。
     * 输入：用户充值 100、冻结 60 后，提现 contextVariables 含嵌套 IBAN 值。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现入口在构造指令前阻断敏感上下文，不生成资金交易事实和账务事实。
     * 红线：提现确认不得把 IBAN、完整账户号等敏感值通过普通上下文落库。
     */
    @Test
    void testWithdrawWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_SENSITIVE_CONTEXT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("withdraw with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_SENSITIVE_CONTEXT_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_SENSITIVE_CONTEXT_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_SENSITIVE_CONTEXT_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：提现确认缺少提现账户。
     * 输入：用户充值 100、冻结 60 后，提现确认未传 accountId。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现确认必须明确消费哪个账户的冻结余额，缺主体不能进入 route 和 ledger。
     * 红线：缺提现账户不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testWithdrawWithoutAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_MISSING_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_MISSING_ACCOUNT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setPayeeId(FundsAccountId.immutable("external_bank_missing_withdraw_account",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_MISSING_ACCOUNT_CONFIRM")
                .setDescription("withdraw without account"), WindOperator.system()))
                .hasMessageContaining("提现账户不能为空");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_ACCOUNT_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_ACCOUNT_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_ACCOUNT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_ACCOUNT_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_MISSING_ACCOUNT_CONFIRM");
    }

    /**
     * 场景：提现确认同时缺少提现账户和外部收款方。
     * 输入：用户充值 100、冻结 60 后，提现确认未传 accountId 和 payeeId。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现确认必须先明确消费哪个账户的冻结余额，不能用外部收款方校验掩盖缺提现账户。
     * 红线：缺提现账户不能生成 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testWithdrawWithoutAccountAndPayeeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_CONFIRM")
                .setDescription("withdraw without account and payee"), WindOperator.system()))
                .hasMessageContaining("提现账户不能为空");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_MISSING_ACCOUNT_AND_PAYEE_CONFIRM");
    }

    /**
     * 场景：提现确认缺少原冻结流水号。
     * 输入：用户充值 100、冻结 60 后，提现确认未传 referenceFreezeSn。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现确认必须明确消费哪一笔冻结单，缺冻结流水不能进入 route 和 ledger。
     * 红线：缺冻结流水不能泄露到底层 reference 构造异常，不得释放冻结或生成提现账务事实。
     */
    @Test
    void testWithdrawWithoutReferenceFreezeSnShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_MISSING_FREEZE_REF_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(user, 60L, "WITHDRAW_MISSING_FREEZE_REF_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("external_bank_missing_withdraw_freeze_ref",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_MISSING_FREEZE_REF_CONFIRM")
                .setDescription("withdraw without reference freeze sn"), WindOperator.system()))
                .hasMessageContaining("提现冻结流水号不能为空");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_FREEZE_REF_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_FREEZE_REF_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_FREEZE_REF_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_FREEZE_REF_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_MISSING_FREEZE_REF_CONFIRM");
    }

    /**
     * 场景：提现确认同时缺少原冻结流水号和外部收款方。
     * 输入：用户充值 100、冻结 60 后，提现确认未传 referenceFreezeSn 和 payeeId。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现确认必须先绑定被消费的冻结事实，不能用外部收款方校验掩盖缺冻结流水。
     * 红线：缺冻结流水不能生成出款 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testWithdrawWithoutReferenceFreezeSnAndPayeeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(user, 60L, "WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_CONFIRM")
                .setDescription("withdraw without reference freeze sn and payee"), WindOperator.system()))
                .hasMessageContaining("提现冻结流水号不能为空");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_FREEZE")
                .getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_MISSING_FREEZE_REF_AND_PAYEE_CONFIRM");
    }

    /**
     * 场景：提现确认缺少外部收款方。
     * 输入：用户充值 100、冻结 60 后，提现确认未传 payeeId。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现确认必须明确外部出款对象，缺外部收款方不能进入 route 和 ledger。
     * 红线：缺外部收款方不能以 NPE 或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testWithdrawWithoutPayeeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_MISSING_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_MISSING_PAYEE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_MISSING_PAYEE_CONFIRM")
                .setDescription("withdraw without payee"), WindOperator.system()))
                .hasMessageContaining("提现外部收款方不能为空");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_PAYEE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_MISSING_PAYEE_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_PAYEE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_MISSING_PAYEE_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_MISSING_PAYEE_CONFIRM");
    }

    /**
     * 场景：USD 资金账户冻结后收到 CNY 提现确认。
     * 输入：充值 100 USD、冻结 60 USD，随后用同一冻结流水确认提现 60 CNY。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持冻结后的状态。
     * 预期：提现确认只能消费同币种冻结余额，FX 必须由业务层显式完成后再提交资金指令。
     * 红线：提现不得隐式换汇，不得释放冻结、生成出款 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testWithdrawWithDifferentCurrencyShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_CURRENCY_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("external_bank_withdraw_currency",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CurrencyIsoCode.CNY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_CURRENCY_CONFIRM")
                .setDescription("withdraw with different currency"), WindOperator.system()))
                .hasMessageContaining("transactionAmount.amount currency must equal account currency");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_CURRENCY_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_CURRENCY_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_CURRENCY_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_CURRENCY_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_CURRENCY_CONFIRM");
    }

    /**
     * 场景：用户存在冻结余额，但提现确认引用了不存在的冻结单。
     * 输入：充值 100、冻结 60，随后提现确认 60，但 `referenceFreezeSn` 指向不存在的冻结单。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持冻结后的状态。
     * 预期：提现必须按原冻结单消费冻结余额，不能只因账户存在 FROZEN 余额就允许出款。
     * 红线：未知冻结单引用不得生成提现 route、posting、ledger entry 或外部出款事实。
     */
    @Test
    void testWithdrawWithUnknownReferenceFreezeSnShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_UNKNOWN_REF_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(user, 60L, "WITHDRAW_UNKNOWN_REF_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> withdraw(user, 60L, "FREEZE_ORDER_NOT_EXISTS", "WITHDRAW_UNKNOWN_REF_CONFIRM"))
                .hasMessageContaining("提现引用冻结单不存在");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_UNKNOWN_REF_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_UNKNOWN_REF_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_UNKNOWN_REF_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_UNKNOWN_REF_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_UNKNOWN_REF_CONFIRM");
    }

    /**
     * 场景：用户 A 与用户 B 都存在冻结余额，用户 B 提现时引用用户 A 的冻结单。
     * 输入：A/B 各充值 100、冻结 60，B 提现确认 60，但 `referenceFreezeSn` 指向 A 的冻结单。
     * 输出：提现请求被拒绝；A/B AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 保持冻结后的状态。
     * 预期：提现引用的原冻结单主体必须与提现账户一致。
     * 红线：不得跨主体消费冻结余额，不得生成提现 route、posting、ledger entry 或外部出款事实。
     */
    @Test
    void testWithdrawWithDifferentAccountReferenceFreezeSnShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId anotherUser = fundingAccount("funding_user_another");
        ensureLedger(anotherUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(anotherUser, LedgerSubjectCode.FROZEN);

        BalanceSnapshot beforeUserTopup = snapshot(balances(user, anotherUser, cashMappingAccount(),
                prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_ACCOUNT_REF_TOPUP");
        BalanceSnapshot afterUserTopup = snapshot(balances(user, anotherUser, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeUserTopup, afterUserTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_ACCOUNT_REF_FREEZE");
        BalanceSnapshot afterUserFreeze = snapshot(balances(user, anotherUser, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterUserTopup, afterUserFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        topup(anotherUser, 100L, "WITHDRAW_ACCOUNT_REF_ANOTHER_TOPUP");
        BalanceSnapshot afterAnotherTopup = snapshot(balances(user, anotherUser, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterUserFreeze, afterAnotherTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(anotherUser, 60L, "WITHDRAW_ACCOUNT_REF_ANOTHER_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, anotherUser, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterAnotherTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> withdraw(anotherUser, 60L, freezeSn, "WITHDRAW_ACCOUNT_REF_CONFIRM"))
                .hasMessageContaining("提现引用冻结单主体与请求账户不一致");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, anotherUser, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_ACCOUNT_REF_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_ACCOUNT_REF_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderByBusinessSn("WITHDRAW_ACCOUNT_REF_ANOTHER_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_ACCOUNT_REF_ANOTHER_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_ACCOUNT_REF_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_ACCOUNT_REF_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_ACCOUNT_REF_ANOTHER_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_ACCOUNT_REF_ANOTHER_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_ACCOUNT_REF_CONFIRM");
    }

    /**
     * 场景：提现确认把内部资金账户作为收款方。
     * 输入：用户充值 100、冻结 60，随后用另一个资金账户作为提现 payeeId。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN、内部收款方和平台账户余额保持冻结后的状态。
     * 预期：提现只能引用外部账户作为出款对象，系统内价值转移必须走 transfer。
     * 红线：内部账户不能被伪装成提现收款方，不得释放冻结、生成出款 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testWithdrawToInternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId internalPayee = fundingAccount("withdraw_internal_payee");
        ensureLedger(internalPayee, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(user, internalPayee, cashMappingAccount(),
                prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_INTERNAL_PAYEE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, internalPayee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(internalPayee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_INTERNAL_PAYEE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, internalPayee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(internalPayee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(internalPayee)
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_INTERNAL_PAYEE_CONFIRM")
                .setDescription("withdraw to internal account"), WindOperator.system()))
                .hasMessageContaining("提现外部收款方必须是外部账户");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, internalPayee, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(internalPayee, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(internalPayee), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_INTERNAL_PAYEE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_INTERNAL_PAYEE_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_INTERNAL_PAYEE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_INTERNAL_PAYEE_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_INTERNAL_PAYEE_CONFIRM");
    }

    /**
     * 场景：提现确认把外部账户作为被冻结和出款的账户主体。
     * 输入：用户充值并冻结 60 后，提现确认 accountId 使用外部银行账户，referenceFreezeSn 使用用户冻结单。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：提现账户必须是内部可记账主体，外部账户只能作为外部收款方引用。
     * 红线：外部账户不得成为提现 route、posting、ledger entry 或余额投影主体。
     */
    @Test
    void testWithdrawFromExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId externalAccount = FundsAccountId.immutable("external_bank_withdraw_account",
                DefaultFundsAccountType.EXTERNAL_BANK);

        BalanceSnapshot beforeTopup = snapshot(balances(user, externalAccount, cashMappingAccount(),
                prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_EXTERNAL_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, externalAccount, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_EXTERNAL_ACCOUNT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, externalAccount, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(externalAccount)
                .setPayeeId(FundsAccountId.immutable("external_bank_withdraw_payee",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_EXTERNAL_ACCOUNT_CONFIRM")
                .setDescription("withdraw from external account"), WindOperator.system()))
                .hasMessageContaining("提现账户不能是外部账户");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, externalAccount, cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_EXTERNAL_ACCOUNT_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_EXTERNAL_ACCOUNT_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_EXTERNAL_ACCOUNT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_EXTERNAL_ACCOUNT_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_EXTERNAL_ACCOUNT_CONFIRM");
    }

    /**
     * 场景：提现确认把完整 IBAN 伪装成外部收款账户引用 ID。
     * 输入：用户充值并冻结 60 后，提现 payeeId.id 为完整 IBAN。
     * 输出：提现请求被拒绝；用户 AVAILABLE/FROZEN 和平台账户余额保持冻结后的状态。
     * 预期：外部账户引用快照构造期阻断敏感原文，不释放冻结，不生成出款资金事实或账务事实。
     * 红线：外部账户引用字段不得保存完整银行账户号、IBAN 或其他敏感原文。
     */
    @Test
    void testWithdrawToRawExternalAccountIdShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_RAW_EXTERNAL_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_RAW_EXTERNAL_ACCOUNT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("GB82WEST12345698765432",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_RAW_EXTERNAL_ACCOUNT_CONFIRM")
                .setDescription("withdraw to raw external account id"), WindOperator.system()))
                .hasMessageContaining("externalAccountNo must be masked or token reference");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 60L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_RAW_EXTERNAL_ACCOUNT_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_RAW_EXTERNAL_ACCOUNT_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_RAW_EXTERNAL_ACCOUNT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_RAW_EXTERNAL_ACCOUNT_FREEZE", 0, 0, 1, 2);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_RAW_EXTERNAL_ACCOUNT_CONFIRM");
    }

    /**
     * 场景：提现确认使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 100、冻结 60、提现确认 60，随后同流水同金额重试，再同流水改金额为 61。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额和账务事实保持第一次提现后的状态。
     * 预期：提现确认幂等必须保护冻结引用、提现金额、外部收款方和出款 route 摘要。
     * 红线：同业务流水不同提现请求不得重复释放冻结、重复出款或污染账务事实。
     */
    @Test
    void testWithdrawSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_IDEMPOTENT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstWithdrawSn = directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_IDEMPOTENT_CONFIRM")
                .setDescription("idempotent withdraw"), WindOperator.system());
        BalanceSnapshot afterFirstWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFirstWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstWithdrawFacts = ledgerFactSnapshot();

        String retryWithdrawSn = directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(60L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_IDEMPOTENT_CONFIRM")
                .setDescription("idempotent withdraw"), WindOperator.system());

        assertThat(retryWithdrawSn).isEqualTo(firstWithdrawSn);
        BalanceSnapshot afterRetryWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstWithdraw, afterRetryWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstWithdrawFacts);
        assertThatThrownBy(() -> directTransactionService.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(user)
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn(freezeSn)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(61L, CURRENCY)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_IDEMPOTENT_CONFIRM")
                .setDescription("idempotent withdraw"), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryWithdraw, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstWithdrawFacts);

        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());
        assertThat(fundsTransactionDetails(firstWithdrawSn)).hasSize(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_IDEMPOTENT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_IDEMPOTENT_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_IDEMPOTENT_CONFIRM", 3, 4);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_IDEMPOTENT_FREEZE").getReleasedAmount()).isZero();
    }

    /**
     * 场景：同一账户存在两笔冻结，第一笔冻结已提现确认后，又用第一笔冻结单和新的业务流水再次提现。
     * 输入：充值 160、冻结 60、冻结 70、用第一笔冻结提现 60，再次用第一笔冻结提现 60。
     * 输出：第二次提现失败；第二笔冻结余额不得被第一笔冻结来源借用。
     * 预期：提现确认必须按 `referenceFreezeSn` 维度累计消费，不能只看账户 FROZEN 聚合余额。
     * 红线：同一冻结来源不得被不同业务流水重复关闭或重复出款。
     */
    @Test
    void testWithdrawSameFreezeSourceTwiceShouldRejectEvenWhenAnotherFrozenBalanceExists() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 160L, "WITHDRAW_DUP_SOURCE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 160L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -160L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstFreezeSn = freeze(user, 60L, "WITHDRAW_DUP_SOURCE_FREEZE_1");
        BalanceSnapshot afterFirstFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(user, 70L, "WITHDRAW_DUP_SOURCE_FREEZE_2");
        BalanceSnapshot afterSecondFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstFreeze, afterSecondFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 60L, firstFreezeSn, "WITHDRAW_DUP_SOURCE_CONFIRM_1");
        BalanceSnapshot afterFirstWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterSecondFreeze, afterFirstWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstWithdrawFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> withdraw(user, 60L, firstFreezeSn, "WITHDRAW_DUP_SOURCE_CONFIRM_2"))
                .hasMessageContaining("冻结单剩余可提现金额不足");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstWithdraw, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstWithdrawFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 70L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_DUP_SOURCE_FREEZE_1").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_DUP_SOURCE_FREEZE_1").getReleasedAmount()).isZero();
        assertThat(frozenOrderByBusinessSn("WITHDRAW_DUP_SOURCE_FREEZE_2").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_DUP_SOURCE_FREEZE_2").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_DUP_SOURCE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_DUP_SOURCE_FREEZE_1", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_DUP_SOURCE_FREEZE_2", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_DUP_SOURCE_CONFIRM_1", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_DUP_SOURCE_CONFIRM_2");
    }

    /**
     * 场景：同一冻结单已部分提现，随后用新的业务流水再次提现并超过剩余冻结金额。
     * 输入：充值 120、冻结 90、首次提现 40、再次提现 60。
     * 输出：第二次提现失败；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持首次提现后的状态。
     * 预期：提现确认必须按 `referenceFreezeSn` 维度累计消费剩余金额，不能只看账户 FROZEN 聚合余额。
     * 红线：同一冻结来源累计提现不得超过原冻结金额；失败请求不得生成出款 route、posting 或账务事实。
     */
    @Test
    void testWithdrawSameFreezeSourcePartiallyThenExceedRemainingShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 120L, "WITHDRAW_PARTIAL_SOURCE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 120L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -120L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 90L, "WITHDRAW_PARTIAL_SOURCE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -90L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 90L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 40L, freezeSn, "WITHDRAW_PARTIAL_SOURCE_CONFIRM_1");
        BalanceSnapshot afterFirstWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFirstWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstWithdrawFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> withdraw(user, 60L, freezeSn, "WITHDRAW_PARTIAL_SOURCE_CONFIRM_2"))
                .hasMessageContaining("冻结单剩余可提现金额不足");

        BalanceSnapshot afterRejectedWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstWithdraw, afterRejectedWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstWithdrawFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_920L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());
        assertThat(frozenOrderByBusinessSn("WITHDRAW_PARTIAL_SOURCE_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("WITHDRAW_PARTIAL_SOURCE_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_SOURCE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_SOURCE_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_SOURCE_CONFIRM_1", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_SOURCE_CONFIRM_2");
    }

    /**
     * 场景：同一冻结单已部分提现，随后同冻结来源解冻超过剩余可处理金额。
     * 输入：充值 120、冻结 90、提现 40、解冻 60。
     * 输出：解冻失败；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持首次提现后的状态。
     * 预期：提现关闭金额与解冻释放金额必须共享同一冻结来源累计上限。
     * 红线：同一冻结来源不得被提现和解冻累计处理超过原冻结金额；失败请求不得生成解冻账务事实。
     */
    @Test
    void testUnfreezeAfterPartialWithdrawExceedingFreezeSourceRemainingShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 120L, "WITHDRAW_PARTIAL_CLOSE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 120L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -120L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 90L, "WITHDRAW_PARTIAL_CLOSE_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -90L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 90L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 40L, freezeSn, "WITHDRAW_PARTIAL_CLOSE_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -40L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 40L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterWithdrawFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> unfreeze(user, 60L, freezeSn, "WITHDRAW_PARTIAL_CLOSE_RELEASE"))
                .hasMessageContaining("RouteSnapshot leg 回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterRejectedRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterWithdraw, afterRejectedRelease,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterWithdrawFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 50L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_920L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FREEZE.name(),
                        FundsTransactionEventType.WITHDRAW.name());
        assertThat(frozenOrderExistsByBusinessSn("WITHDRAW_PARTIAL_CLOSE_RELEASE"))
                .isFalse();
        assertThat(frozenOrderByBusinessSn("WITHDRAW_PARTIAL_CLOSE_FREEZE").getReleasedAmount()).isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_CLOSE_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_CLOSE_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_CLOSE_CONFIRM", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_PARTIAL_CLOSE_RELEASE");
    }

    /**
     * 场景：用户充值、冻结并提现成功后，又收到同一冻结来源的撤销或拒绝解冻请求。
     * 输入：充值 100、冻结 60、提现确认 60、随后解冻 60。
     * 输出：解冻请求失败；用户 AVAILABLE/FROZEN、平台 CASH/PREPAYMENT 保持提现成功后的状态。
     * 预期：已确认出款不能被简单解冻；失败请求不得生成解冻账务事实或冻结释放记录。
     * 红线：提现成功后的拒绝、撤销或回滚应进入后续差错/追偿流程，不得回写冻结语义。
     */
    @Test
    void testUnfreezeAfterSuccessfulWithdrawShouldFailAndKeepBalanceUnchanged() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "WITHDRAW_UNFREEZE_AFTER_SUCCESS_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 60L, "WITHDRAW_UNFREEZE_AFTER_SUCCESS_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -60L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        withdraw(user, 60L, freezeSn, "WITHDRAW_UNFREEZE_AFTER_SUCCESS_CONFIRM");
        BalanceSnapshot afterWithdraw = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterWithdraw,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -60L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 60L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterWithdrawFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> unfreeze(user, 60L, freezeSn,
                "WITHDRAW_UNFREEZE_AFTER_SUCCESS_RELEASE"))
                .hasMessageContaining("RouteSnapshot leg 回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterRejectedRelease = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterWithdraw, afterRejectedRelease,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterWithdrawFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_960L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(frozenOrderExistsByBusinessSn("WITHDRAW_UNFREEZE_AFTER_SUCCESS_RELEASE"))
                .isFalse();
        assertThat(frozenOrderByBusinessSn("WITHDRAW_UNFREEZE_AFTER_SUCCESS_FREEZE").getReleasedAmount())
                .isZero();
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_UNFREEZE_AFTER_SUCCESS_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("WITHDRAW_UNFREEZE_AFTER_SUCCESS_FREEZE", 0, 0, 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("WITHDRAW_UNFREEZE_AFTER_SUCCESS_CONFIRM", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("WITHDRAW_UNFREEZE_AFTER_SUCCESS_RELEASE");
    }
}
