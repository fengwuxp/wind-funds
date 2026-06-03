package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.core.WritableContextVariables;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.transaction.enums.DefaultFeeType;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.Money;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 手续费组合业务流测试。
 */
class FundsTransactionFeeFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：账户没有可用余额时直接收取独立手续费。
     * 输入：用户未充值，提交独立手续费 5。
     * 输出：请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额均不变化。
     * 预期：无受控负余额策略时，独立手续费不能把用户可用余额打成负数。
     * 红线：手续费失败不能留下 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testStandaloneFeeWithoutBalanceShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> fee(payer, 5L, "FEE_NO_BALANCE_CHARGE"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(0);
        assertFailedFundsTransactionWithoutLedgerFacts("FEE_NO_BALANCE_CHARGE");
    }

    /**
     * 场景：独立手续费缺少支出账户。
     * 输入：业务侧提交手续费 5，但不传 accountId。
     * 输出：请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额均不变化。
     * 预期：独立手续费必须明确支出账户，缺主体不能进入 route 和 ledger。
     * 红线：缺支出账户不能以底层账户查询异常或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testStandaloneFeeWithoutAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_MISSING_ACCOUNT_CHARGE")
                .setDescription("fee without account"), WindOperator.system()))
                .hasMessageContaining("手续费支出账户不能为空");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_MISSING_ACCOUNT_CHARGE");
    }

    /**
     * 场景：独立手续费把外部账户作为支出账户。
     * 输入：业务侧提交手续费 5，但 accountId 是外部银行账户。
     * 输出：请求被拒绝；平台 FEE/CASH/PREPAYMENT 余额均不变化。
     * 预期：外部账户只能作为出入金引用或快照，不能成为手续费 ledger subject。
     * 红线：外部账户不得生成手续费 route、posting、ledger entry 或余额投影。
     */
    @Test
    void testStandaloneFeeFromExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId externalAccount = FundsAccountId.immutable("external_fee_account",
                DefaultFundsAccountType.EXTERNAL_BANK);
        BalanceSnapshot before = snapshot(balances(externalAccount, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(externalAccount)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_EXTERNAL_ACCOUNT_CHARGE")
                .setDescription("fee from external account"), WindOperator.system()))
                .hasMessageContaining("手续费支出账户不能是外部账户");

        BalanceSnapshot afterFailure = snapshot(balances(externalAccount, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 10_000L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_EXTERNAL_ACCOUNT_CHARGE");
    }

    /**
     * 场景：独立手续费缺少手续费类型。
     * 输入：用户充值 50 后，提交独立手续费 5 但不传 feeType。
     * 输出：请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额保持充值后的状态。
     * 预期：独立手续费必须明确费用类型，缺类型不能进入 route 和 ledger。
     * 红线：缺手续费类型不能泄露为底层 Map 构造异常，不得生成手续费资金事实或账务事实。
     */
    @Test
    void testStandaloneFeeWithoutFeeTypeShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot beforeTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 50L, "FEE_MISSING_TYPE_TOPUP");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, beforeFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_MISSING_TYPE_CHARGE")
                .setDescription("fee without type"), WindOperator.system()))
                .hasMessageContaining("手续费类型不能为空");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_MISSING_TYPE_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_MISSING_TYPE_TOPUP");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_MISSING_TYPE_CHARGE");
    }

    /**
     * 场景：独立手续费收取和手续费退回请求把敏感账户值放入扩展上下文。
     * 输入：手续费扣款 contextVariables 含嵌套 IBAN 值；有效手续费扣款后，退费 contextVariables 含嵌套 IBAN 值。
     * 输出：两次敏感请求均被拒绝；用户和平台手续费余额保持最近一次成功事实后的状态。
     * 预期：手续费交易各入口在构造指令前统一阻断敏感上下文，不生成资金交易事实和账务事实。
     * 红线：IBAN、完整账户号等敏感值不得通过手续费上下文落库。
     */
    @Test
    void testFeeAndFeeRefundWithSensitiveContextVariablesShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(payer, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("fee with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedFee = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterRejectedFee,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);

        topup(payer, 50L, "FEE_REFUND_SENSITIVE_CONTEXT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRejectedFee, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        fee(payer, 5L, "FEE_REFUND_SENSITIVE_CONTEXT_SOURCE");
        BalanceSnapshot afterFee = snapshot(balances(payer, feeAccount(), cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFee,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFeeFacts = ledgerFactSnapshot();

        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_SENSITIVE_CONTEXT_SOURCE")
                .getFundsTransactionSn();
        assertThatThrownBy(() -> directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeSourceTransactionSn(feeSourceTransactionSn)
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432"))))
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn("FEE_REFUND_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setDescription("fee refund with sensitive IBAN value"), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterRejectedFeeRefund = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFee, afterRejectedFeeRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFeeFacts);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 45L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FEE_CHARGE.name());
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_SENSITIVE_CONTEXT_IBAN_VALUE");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_SENSITIVE_CONTEXT_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_SENSITIVE_CONTEXT_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_SENSITIVE_CONTEXT_SOURCE", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_SENSITIVE_CONTEXT_SOURCE");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_SENSITIVE_CONTEXT_IBAN_VALUE");
    }

    /**
     * 场景：手续费退回缺少到账账户。
     * 输入：充值 50、独立手续费 5，随后退回手续费 5 但不传 accountId。
     * 输出：退费请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额保持原手续费后的状态。
     * 预期：手续费退回必须明确到账账户，缺主体不能进入 route replay 和 ledger。
     * 红线：缺到账账户不能以底层账户查询异常或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testFeeRefundWithoutAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot beforeTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 50L, "FEE_REFUND_MISSING_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        fee(payer, 5L, "FEE_REFUND_MISSING_ACCOUNT_SOURCE");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, beforeFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_MISSING_ACCOUNT_SOURCE")
                .getFundsTransactionSn();

        assertThatThrownBy(() -> directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeSourceTransactionSn(feeSourceTransactionSn)
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn("FEE_REFUND_MISSING_ACCOUNT_RETURN")
                .setDescription("fee refund without account"), WindOperator.system()))
                .hasMessageContaining("手续费退回到账账户不能为空");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 45L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_MISSING_ACCOUNT_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_MISSING_ACCOUNT_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_MISSING_ACCOUNT_SOURCE", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_MISSING_ACCOUNT_SOURCE");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_MISSING_ACCOUNT_RETURN");
    }

    /**
     * 场景：手续费退回把外部账户作为到账账户。
     * 输入：充值 50、独立手续费 5，随后退回手续费 5 但 accountId 是外部银行账户。
     * 输出：退费请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额保持原手续费后的状态。
     * 预期：手续费退回到账账户必须是内部可入账主体，外部账户不能成为 ledger subject。
     * 红线：外部账户不得生成退费 route replay、posting、ledger entry 或余额投影。
     */
    @Test
    void testFeeRefundToExternalAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId externalAccount = FundsAccountId.immutable("external_fee_refund_account",
                DefaultFundsAccountType.EXTERNAL_BANK);
        BalanceSnapshot beforeTopup = snapshot(balances(payer, externalAccount, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 50L, "FEE_REFUND_EXTERNAL_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, externalAccount, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        fee(payer, 5L, "FEE_REFUND_EXTERNAL_ACCOUNT_SOURCE");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, externalAccount, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, beforeFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();
        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_EXTERNAL_ACCOUNT_SOURCE")
                .getFundsTransactionSn();

        assertThatThrownBy(() -> directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAccountId(externalAccount)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeSourceTransactionSn(feeSourceTransactionSn)
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn("FEE_REFUND_EXTERNAL_ACCOUNT_RETURN")
                .setDescription("fee refund to external account"), WindOperator.system()))
                .hasMessageContaining("手续费退回到账账户不能是外部账户");

        BalanceSnapshot afterFailure = snapshot(balances(payer, externalAccount, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(externalAccount, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 45L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_EXTERNAL_ACCOUNT_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_EXTERNAL_ACCOUNT_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_EXTERNAL_ACCOUNT_SOURCE", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_EXTERNAL_ACCOUNT_SOURCE");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_EXTERNAL_ACCOUNT_RETURN");
    }

    /**
     * 场景：手续费退回缺少原费用交易流水。
     * 输入：充值 50、独立手续费 5，随后退回手续费 5 但不传 feeSourceTransactionSn。
     * 输出：退费请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额保持原手续费后的状态。
     * 预期：手续费退回必须明确原费用交易流水，缺原路径不能进入 route replay 和 ledger。
     * 红线：缺原费用交易流水不能以下游 route replay 泛化错误或半截账务事实形式泄露到生产链路。
     */
    @Test
    void testFeeRefundWithoutSourceTransactionShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot beforeTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 50L, "FEE_REFUND_MISSING_SOURCE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        fee(payer, 5L, "FEE_REFUND_MISSING_SOURCE_CHARGE");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, beforeFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn("FEE_REFUND_MISSING_SOURCE_RETURN")
                .setDescription("fee refund without source transaction"), WindOperator.system()))
                .hasMessageContaining("手续费退回原费用交易流水不能为空");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 45L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_MISSING_SOURCE_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_MISSING_SOURCE_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_MISSING_SOURCE_CHARGE", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_MISSING_SOURCE_CHARGE");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_MISSING_SOURCE_RETURN");
    }

    /**
     * 场景：手续费退回同时缺少原费用交易流水和到账账户。
     * 输入：充值 50、独立手续费 5，随后退回手续费 5 但不传 feeSourceTransactionSn 和 accountId。
     * 输出：退费请求被拒绝；用户 AVAILABLE/FROZEN 和平台 FEE 余额保持原手续费后的状态。
     * 预期：手续费退回必须先绑定被退回的原费用事实，不能用到账账户校验掩盖缺原路径。
     * 红线：缺原费用交易流水不能进入 route replay、posting、ledger entry 或余额投影。
     */
    @Test
    void testFeeRefundWithoutSourceTransactionAndAccountShouldRejectAndLeaveNoLedgerSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        BalanceSnapshot beforeTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 50L, "FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        fee(payer, 5L, "FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_CHARGE");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, beforeFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAmount(Money.immutable(5L, CURRENCY))
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn("FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_RETURN")
                .setDescription("fee refund without source transaction and account"), WindOperator.system()))
                .hasMessageContaining("手续费退回原费用交易流水不能为空");

        BalanceSnapshot afterFailure = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 45L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_CHARGE", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_CHARGE");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_MISSING_SOURCE_AND_ACCOUNT_RETURN");
    }

    /**
     * 场景：独立手续费使用相同业务流水重复提交，第二次请求摘要一致时复用原交易，摘要不一致时拒绝。
     * 输入：充值 50、手续费 5 使用业务流水 `FEE_IDEMPOTENT_CHARGE`，随后同流水同金额重试，再同流水改金额为 6。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额、route 和账务事实保持第一次手续费后的状态。
     * 预期：独立手续费和直接交易主流程一样受 `tenantId + businessScene + businessSn + requestHash` 保护。
     * 红线：同业务流水不同手续费请求不得重复扣款、重写 route、追加 posting 或污染余额投影。
     */
    @Test
    void testStandaloneFeeSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        topup(payer, 50L, "FEE_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String firstFeeSn = directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_IDEMPOTENT_CHARGE")
                .setDescription("idempotent fee"), WindOperator.system());
        BalanceSnapshot afterFirstFee = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstFee,
                delta(payer, LedgerSubjectCode.AVAILABLE, -5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstFeeFacts = ledgerFactSnapshot();
        String firstRouteSnapshot = routeSnapshotJson("FEE_IDEMPOTENT_CHARGE");

        String retryFeeSn = directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(5L, CURRENCY))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_IDEMPOTENT_CHARGE")
                .setDescription("idempotent fee"), WindOperator.system());

        assertThat(retryFeeSn).isEqualTo(firstFeeSn);
        BalanceSnapshot afterRetryFee = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstFee, afterRetryFee,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstFeeFacts);
        assertRouteSnapshotUnchanged("FEE_IDEMPOTENT_CHARGE", firstRouteSnapshot);

        assertThatThrownBy(() -> directTransactionService.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(Money.immutable(6L, CURRENCY))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_IDEMPOTENT_CHARGE")
                .setDescription("idempotent fee"), WindOperator.system()))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryFee, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstFeeFacts);
        assertRouteSnapshotUnchanged("FEE_IDEMPOTENT_CHARGE", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 45L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_950L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.FEE_CHARGE.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_IDEMPOTENT_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_IDEMPOTENT_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_IDEMPOTENT_CHARGE", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_IDEMPOTENT_CHARGE");
    }

    /**
     * 场景：手续费退回使用相同业务流水重复提交，第二次请求摘要一致时复用原退费，摘要不一致时拒绝。
     * 输入：充值 100、付款 70 并收取手续费 5，退费 5 使用业务流水 `FEE_REFUND_IDEMPOTENT_RETURN` 后重试，再把规则上下文从 RULE-A 改为 RULE-B。
     * 输出：同摘要重试返回同一资金交易流水；摘要冲突抛错；余额、route 和账务事实保持第一次退费后的状态。
     * 预期：`refundFee` 按原 FEE leg 回放，同时由请求摘要保护幂等。
     * 红线：同业务流水不同退费请求不得重复退费、重写 route、追加 posting 或污染余额投影。
     */
    @Test
    void testFeeRefundSameBusinessSnWithDifferentRequestShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_refund_idem_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        topup(payer, 100L, "FEE_REFUND_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L,
                "FEE_REFUND_IDEMPOTENT_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -75L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_IDEMPOTENT_PAY")
                .getFundsTransactionSn();
        String firstFeeRefundSn = refundFeeWithContext(payer, 5L, feeSourceTransactionSn,
                "FEE_REFUND_IDEMPOTENT_RETURN", "RULE-A");
        BalanceSnapshot afterFirstFeeRefund = snapshot(balances(payer, payee, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFirstFeeRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, -5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstFeeRefundFacts = ledgerFactSnapshot();
        String firstRouteSnapshot = routeSnapshotJson("FEE_REFUND_IDEMPOTENT_RETURN");

        String retryFeeRefundSn = refundFeeWithContext(payer, 5L, feeSourceTransactionSn,
                "FEE_REFUND_IDEMPOTENT_RETURN", "RULE-A");

        assertThat(retryFeeRefundSn).isEqualTo(firstFeeRefundSn);
        BalanceSnapshot afterRetryFeeRefund = snapshot(balances(payer, payee, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstFeeRefund, afterRetryFeeRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstFeeRefundFacts);
        assertRouteSnapshotUnchanged("FEE_REFUND_IDEMPOTENT_RETURN", firstRouteSnapshot);

        assertThatThrownBy(() -> refundFeeWithContext(payer, 5L, feeSourceTransactionSn,
                "FEE_REFUND_IDEMPOTENT_RETURN", "RULE-B"))
                .hasMessageContaining("资金交易明细请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRetryFeeRefund, afterConflict,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstFeeRefundFacts);
        assertRouteSnapshotUnchanged("FEE_REFUND_IDEMPOTENT_RETURN", firstRouteSnapshot);

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(3);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.FEE_REFUND.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_IDEMPOTENT_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_IDEMPOTENT_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_IDEMPOTENT_PAY", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_IDEMPOTENT_PAY");
        assertFeeRefundFactsWithFundsTransaction("FEE_REFUND_IDEMPOTENT_RETURN", feeSourceTransactionSn);
    }

    /**
     * 场景：用户充值后付款并收取手续费，随后发起本金退款，再单独退回手续费。
     * 输入：充值 100、付款 70、固定手续费 5、本金退款 30、手续费退回 5。
     * 输出：付款方 AVAILABLE、收款方 SETTLEMENT、平台 FEE/CASH/PREPAYMENT 余额快照和账务事实。
     * 预期：主交易本金和费用拆 leg；普通退款不退费；`refundFee` 只回放费用路径。
     * 红线：手续费不能混入本金退款，也不能在普通退款时自动退回。
     */
    @Test
    void testPayWithFeeThenRefundPrincipalAndFeeShouldReplayDifferentRouteLegs() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_flow_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot before = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));

        topup(payer, 100L, "FEE_FLOW_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L, "FEE_FLOW_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -75L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        refund(payer, payee, LedgerSubjectCode.SETTLEMENT, 30L, "FEE_FLOW_REFUND");
        BalanceSnapshot afterRefund = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 30L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, -30L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_FLOW_PAY").getFundsTransactionSn();
        refundFee(payer, 5L, feeSourceTransactionSn, "FEE_FLOW_FEE_REFUND");
        BalanceSnapshot afterFeeRefund = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterRefund, afterFeeRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, -5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(payer), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 40L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(4);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.REFUND.name(),
                        FundsTransactionEventType.FEE_REFUND.name());

        LedgerTransaction payTransaction = ledgerTransactionByBusinessSn("FEE_FLOW_PAY");
        assertThat(entriesOf(payTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.SETTLEMENT,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FEE);
        assertThat(postingPlansOf(payTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsExactly(LedgerPhaseCode.SETTLEMENT.name(), LedgerPhaseCode.FEE.name());

        LedgerTransaction refundTransaction = ledgerTransactionByBusinessSn("FEE_FLOW_REFUND");
        assertThat(entriesOf(refundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.SETTLEMENT, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(refundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());

        LedgerTransaction feeRefundTransaction = ledgerTransactionByBusinessSn("FEE_FLOW_FEE_REFUND");
        assertThat(entriesOf(feeRefundTransaction).stream()
                .map(LedgerEntry::getLedgerSubjectCode)
                .toList())
                .containsExactlyInAnyOrder(LedgerSubjectCode.FEE, LedgerSubjectCode.AVAILABLE);
        assertThat(postingPlansOf(feeRefundTransaction).stream()
                .map(LedgerPostingPlan::getPhaseCode)
                .toList())
                .containsOnly(LedgerPhaseCode.REFUND.name());
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_FLOW_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_FLOW_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_FLOW_PAY", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_FLOW_PAY");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_FLOW_REFUND", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_FLOW_REFUND");
        assertFeeRefundFactsWithFundsTransaction("FEE_FLOW_FEE_REFUND", feeSourceTransactionSn);
    }

    /**
     * 场景：业务侧发起手续费退回，但传入的原费用交易流水不存在。
     * 输入：用户充值 100、付款 70 并收取手续费 5，随后退费 5 且 `feeSourceTransactionSn` 指向未知交易。
     * 输出：退费失败，付款方、收款方、平台 FEE/CASH/PREPAYMENT 余额保持付款后状态。
     * 预期：手续费退回必须定位原 route snapshot 和原 FEE leg，未知引用不能退化为当前路径重新路由。
     * 红线：缺原路径快照的退费不得生成 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testFeeRefundWithUnknownSourceTransactionShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_refund_no_src_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        topup(payer, 100L, "FEE_REFUND_UNKNOWN_SOURCE_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L,
                "FEE_REFUND_UNKNOWN_SOURCE_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -75L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> refundFee(payer, 5L, "FUNDS_TRANSACTION_NOT_EXISTS",
                "FEE_REFUND_UNKNOWN_SOURCE_RETURN"))
                .hasMessageContaining("RouteSnapshot 回放事件未找到原路径快照");

        BalanceSnapshot afterFailure = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 25L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 5L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertLedgerTransactionFactsUnchanged(afterPayFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_UNKNOWN_SOURCE_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_UNKNOWN_SOURCE_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_UNKNOWN_SOURCE_PAY", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_UNKNOWN_SOURCE_PAY");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_UNKNOWN_SOURCE_RETURN");
    }

    /**
     * 场景：业务侧发起手续费退回，但原交易存在且没有手续费 leg。
     * 输入：用户充值 100、付款 70 且未收取手续费，随后以该付款交易作为原费用交易发起退费 5。
     * 输出：退费失败，付款方、收款方、平台 FEE/CASH/PREPAYMENT 余额保持付款后状态。
     * 预期：手续费退回必须定位原交易 FEE leg，不能把普通付款路径当成费用路径回放。
     * 红线：原交易没有费用 leg 时不得生成退费 route、posting、ledger entry 或余额投影副作用。
     */
    @Test
    void testFeeRefundWithSourceTransactionWithoutFeeLegShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("no_fee_payee");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        topup(payer, 100L, "FEE_REFUND_NO_FEE_LEG_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        pay(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, "FEE_REFUND_NO_FEE_LEG_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterPayFacts = ledgerFactSnapshot();
        String nonFeeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_NO_FEE_LEG_PAY")
                .getFundsTransactionSn();

        assertThatThrownBy(() -> refundFee(payer, 5L, nonFeeSourceTransactionSn,
                "FEE_REFUND_NO_FEE_LEG_RETURN"))
                .hasMessageContaining("手续费退回原交易没有可回放费用路径");

        BalanceSnapshot afterFailure = snapshot(balances(payer, payee, feeAccount(), cashMappingAccount(),
                prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 0L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_900L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);
        assertPostedTransactions(2);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertLedgerTransactionFactsUnchanged(afterPayFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_NO_FEE_LEG_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_NO_FEE_LEG_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_NO_FEE_LEG_PAY", 2, 2);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_NO_FEE_LEG_PAY");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_NO_FEE_LEG_RETURN");
    }

    /**
     * 场景：用户付款产生固定手续费，全额退费后再次尝试退回同一原交易手续费。
     * 输入：原交易手续费 5、第一次手续费退款 5、平台手续费账户另有足额余额、第二次手续费退款 5。
     * 输出：第二次退费失败，付款方、收款方、平台手续费和现金账户余额均保持不变。
     * 预期：手续费退款按原交易 FEE leg 回放，同一原交易累计退费金额不得超过原交易手续费。
     * 红线：不能因为平台手续费账户余额充足，就允许对同一原交易超额退费或落下失败账务事实。
     */
    @Test
    void testRepeatedFeeRefundForSameTransactionShouldLeaveNoSideEffects() {
        FundsAccountId payer = fundingAccount("funding_user");
        FundsAccountId payee = fundingAccount("fee_refund_exceed_payee");
        FundsAccountId reservePayer = fundingAccount("fee_refund_reserve_user");
        ensureLedger(payee, LedgerSubjectCode.SETTLEMENT);
        ensureLedger(reservePayer, LedgerSubjectCode.AVAILABLE);

        BalanceSnapshot beforeTopup = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        topup(payer, 100L, "FEE_REFUND_EXCEED_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        payWithFixedFee(payer, payee, LedgerSubjectCode.SETTLEMENT, 70L, 5L,
                "FEE_REFUND_EXCEED_PAY");
        BalanceSnapshot afterPay = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterPay,
                delta(payer, LedgerSubjectCode.AVAILABLE, -75L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 70L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String feeSourceTransactionSn = ledgerTransactionByBusinessSn("FEE_REFUND_EXCEED_PAY")
                .getFundsTransactionSn();
        refundFee(payer, 5L, feeSourceTransactionSn, "FEE_REFUND_EXCEED_FIRST_RETURN");
        BalanceSnapshot afterFirstFeeRefund = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterPay, afterFirstFeeRefund,
                delta(payer, LedgerSubjectCode.AVAILABLE, 5L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, -5L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        topup(reservePayer, 100L, "FEE_REFUND_EXCEED_RESERVE_TOPUP");
        BalanceSnapshot afterReserveTopup = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstFeeRefund, afterReserveTopup,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        payWithFixedFee(reservePayer, payee, LedgerSubjectCode.SETTLEMENT, 10L, 20L,
                "FEE_REFUND_EXCEED_RESERVE_PAY");
        BalanceSnapshot beforeFailure = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterReserveTopup, beforeFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 10L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot beforeFailureFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> refundFee(payer, 5L, feeSourceTransactionSn,
                "FEE_REFUND_EXCEED_SECOND_RETURN"))
                .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");

        BalanceSnapshot afterFailure = snapshot(balances(payer, payee, reservePayer, feeAccount(),
                cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeFailure, afterFailure,
                delta(payer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(payer, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(payee, LedgerSubjectCode.SETTLEMENT, 0L, CURRENCY),
                delta(reservePayer, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(feeAccount(), LedgerSubjectCode.FEE, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertBucket(balance(payer), LedgerSubjectCode.AVAILABLE, 30L, CURRENCY);
        assertBucket(balance(payee), LedgerSubjectCode.SETTLEMENT, 80L, CURRENCY);
        assertBucket(balance(reservePayer), LedgerSubjectCode.AVAILABLE, 70L, CURRENCY);
        assertBucket(balance(feeAccount()), LedgerSubjectCode.FEE, 20L, CURRENCY);
        assertBucket(balance(cashMappingAccount()), LedgerSubjectCode.CASH, 9_800L, CURRENCY);
        assertBucket(balance(prepaymentAccount()), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY);

        assertPostedTransactions(5);
        assertThat(ledgerTransactions().stream()
                .map(LedgerTransaction::getEventType)
                .toList())
                .containsExactly(
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name(),
                        FundsTransactionEventType.FEE_REFUND.name(),
                        FundsTransactionEventType.TOPUP.name(),
                        FundsTransactionEventType.PAY.name());
        assertLedgerTransactionFactsUnchanged(beforeFailureFacts);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_EXCEED_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_EXCEED_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_EXCEED_PAY", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_EXCEED_PAY");
        assertFeeRefundFactsWithFundsTransaction("FEE_REFUND_EXCEED_FIRST_RETURN", feeSourceTransactionSn);
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_EXCEED_RESERVE_TOPUP", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_EXCEED_RESERVE_TOPUP");
        assertSingleFundsAndLedgerFactsForBusinessSn("FEE_REFUND_EXCEED_RESERVE_PAY", 3, 4);
        assertLedgerFactsFollowRouteSnapshot("FEE_REFUND_EXCEED_RESERVE_PAY");
        assertNoFundsOrLedgerFactsForBusinessSn("FEE_REFUND_EXCEED_SECOND_RETURN");
    }

    private void assertRouteSnapshotUnchanged(String businessSn, String expectedRouteSnapshot) {
        assertThat(routeSnapshotJson(businessSn))
                .as("fee route snapshot must not be rewritten for idempotent businessSn %s", businessSn)
                .isEqualTo(expectedRouteSnapshot);
    }

    private String routeSnapshotJson(String businessSn) {
        return fundsTransactionsByBusinessSn(businessSn).getFirst().getRouteSnapshot();
    }

    private String refundFeeWithContext(FundsAccountId accountId,
                                        long amount,
                                        String feeSourceTransactionSn,
                                        String businessSn,
                                        String ruleVersion) {
        return directTransactionService.refundFee(new FundsTransactionFeeRefundRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setFeeSourceTransactionSn(feeSourceTransactionSn)
                .setContextVariables(WritableContextVariables.of(Map.of("ruleVersion", ruleVersion)))
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn(businessSn)
                .setDescription("idempotent fee refund"), WindOperator.system());
    }

    private void assertFeeRefundFactsWithFundsTransaction(String businessSn, String sourceTransactionSn) {
        var transactions = fundsTransactionsByBusinessSn(businessSn);
        assertThat(transactions)
                .as("funds transactions for fee refund businessSn %s", businessSn)
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
                    assertThat(transaction.getStatus())
                            .isIn(FundsTransactionStatus.OPEN, FundsTransactionStatus.CLOSED);
                    assertThat(transaction.getReferenceTransactionSn()).isEqualTo(sourceTransactionSn);
                    assertThat(transaction.getRouteSnapshot()).isNotBlank();
                });
        String transactionSn = transactions.getFirst().getSn();
        var details = fundsTransactionDetailsByBusinessSn(businessSn);
        assertThat(details)
                .as("funds transaction details for fee refund businessSn %s", businessSn)
                .hasSize(2);
        assertThat(details.stream()
                .map(detail -> detail.getEventType())
                .toList())
                .as("funds transaction detail event types for fee refund businessSn %s", businessSn)
                .containsOnly(FundsTransactionEventType.FEE_REFUND);
        assertThat(details.stream()
                .map(detail -> detail.getFundsEffectType())
                .toList())
                .as("funds transaction detail effect types for fee refund businessSn %s", businessSn)
                .containsOnly(FundsEffectType.RETURN);
        assertThat(details.stream()
                .map(detail -> detail.getStatus())
                .toList())
                .as("funds transaction detail statuses for fee refund businessSn %s", businessSn)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED);
        assertThat(details.stream()
                .map(detail -> detail.getParticipantRole())
                .toList())
                .as("funds transaction detail roles for fee refund businessSn %s", businessSn)
                .containsExactlyInAnyOrder(RouteParticipantRole.PAYER, RouteParticipantRole.FEE_RECEIVER);
        assertThat(ledgerTransactionsForBusinessSn(businessSn))
                .as("ledger transactions for fee refund businessSn %s", businessSn)
                .singleElement()
                .satisfies(transaction -> {
                    var postingPlans = postingPlansOf(transaction);
                    var entries = entriesByBusinessSn(businessSn);
                    var routeSnapshot = fundsTransactionQueryService
                            .findRouteSnapshotByTransactionSn(transaction.getFundsTransactionSn())
                            .orElseThrow(() -> new AssertionError("fee refund route snapshot not found: "
                                    + businessSn));
                    var replayLegs = routeSnapshot.getLegs();
                    var postingPlanSns = postingPlans.stream()
                            .map(LedgerPostingPlan::getSn)
                            .toList();
                    assertThat(transaction.getStatus()).isEqualTo(LedgerTransactionStatus.POSTED);
                    assertThat(transaction.getFundsTransactionSn()).isEqualTo(transactionSn);
                    assertThat(transaction.getEventType())
                            .isEqualTo(FundsTransactionEventType.FEE_REFUND.name());
                    assertThat(routeSnapshot.getRouteCode()).isEqualTo(FundsRouteCodes.DIRECT_REFUND_REPLAY);
                    assertThat(routeSnapshot.getSnapshotId()).isEqualTo(businessSn + "_ROUTE");
                    assertThat(routeSnapshot.getSnapshotSchemaVersion())
                            .isEqualTo(FundsRouteCodes.CURRENT_ROUTE_VERSION);
                    assertThat(routeSnapshot.getBusinessSn()).isEqualTo(businessSn);
                    assertThat(routeSnapshot.getEventType()).isEqualTo(FundsTransactionEventType.FEE_REFUND);
                    assertThat(routeSnapshot.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
                    assertThat(routeSnapshot.getContextVariables())
                            .as("fee refund route snapshot must not carry request context for %s", businessSn)
                            .isEmpty();
                    assertThat(replayLegs)
                            .as("fee refund must replay only source fee leg for %s", businessSn)
                            .singleElement()
                            .satisfies(leg -> {
                                assertThat(leg.getReplayRefLegId()).isEqualTo("FEE");
                                assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.REFUND);
                                assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.RESTORE);
                            });
                    assertThat(details)
                            .as("funds transaction details must point to fee refund ledger transaction for businessSn %s",
                                    businessSn)
                            .allSatisfy(detail -> {
                                assertThat(detail.getTransactionSn()).isEqualTo(transaction.getFundsTransactionSn());
                                assertThat(detail.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
                            });
                    assertThat(postingPlans)
                            .as("posting plans for fee refund businessSn %s", businessSn)
                            .hasSize(1);
                    assertThat(postingPlans.stream()
                            .map(LedgerPostingPlan::getRouteLegId)
                            .toList())
                            .as("fee refund posting must use replay route leg for businessSn %s", businessSn)
                            .containsExactlyElementsOf(replayLegs.stream()
                                    .map(RouteLegSpec::getLegId)
                                    .toList());
                    assertThat(postingPlans)
                            .as("posting plans must point to source funds transaction for fee refund businessSn %s",
                                    businessSn)
                            .allSatisfy(plan -> {
                                assertThat(plan.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
                                assertThat(plan.getFundsTransactionSn()).isEqualTo(transaction.getFundsTransactionSn());
                                RouteLegSpec replayLeg = replayLegs.getFirst();
                                assertThat(plan.getAmount()).isEqualTo(replayLeg.getAmount().getAmount());
                                assertThat(plan.getCurrency()).isEqualTo(replayLeg.getAmount().getCurrency());
                                assertThat(plan.getBalanceEffectType())
                                        .isEqualTo(replayLeg.getBalanceEffectType().name());
                                assertThat(plan.getPhaseCode()).isEqualTo(replayLeg.getPhaseCode().name());
                            });
                    assertThat(postingPlans.stream()
                            .map(LedgerPostingPlan::getPhaseCode)
                            .toList())
                            .as("posting plans for fee refund businessSn %s", businessSn)
                            .containsOnly(LedgerPhaseCode.REFUND.name());
                    assertThat(entries)
                            .as("ledger entries for fee refund businessSn %s", businessSn)
                            .hasSize(2);
                    assertThat(entries)
                            .as("ledger entries must point to ledger transaction and posting plan for fee refund businessSn %s",
                                    businessSn)
                            .allSatisfy(entry -> {
                                assertThat(entry.getLedgerTransactionSn()).isEqualTo(transaction.getSn());
                                assertThat(entry.getFundsTransactionSn()).isEqualTo(transaction.getFundsTransactionSn());
                                assertThat(entry.getPostingPlanSn()).isIn(postingPlanSns);
                            });
                    RouteLegSpec replayLeg = replayLegs.getFirst();
                    assertThat(entries.stream()
                            .map(entry -> new FeeRefundRouteNodeKey(entry.getSubjectId(), entry.getSubjectType(),
                                    entry.getLedgerSubjectCode(), entry.getEntrySide()))
                            .toList())
                            .as("fee refund entries must follow replay route leg nodes for businessSn %s", businessSn)
                            .containsExactlyInAnyOrder(
                                    FeeRefundRouteNodeKey.debit(replayLeg),
                                    FeeRefundRouteNodeKey.credit(replayLeg));
                    assertThat(entries.stream()
                            .map(LedgerEntry::getLedgerSubjectCode)
                            .toList())
                            .as("ledger entries for fee refund businessSn %s", businessSn)
                            .containsExactlyInAnyOrder(LedgerSubjectCode.FEE, LedgerSubjectCode.AVAILABLE);
                });
    }

    private record FeeRefundRouteNodeKey(String subjectId,
                                         String subjectType,
                                         LedgerSubjectCode ledgerSubjectCode,
                                         EntrySide entrySide) {

        private static FeeRefundRouteNodeKey debit(RouteLegSpec routeLeg) {
            return new FeeRefundRouteNodeKey(routeLeg.getSourceNode().getSubjectRef().getSubjectId(),
                    routeLeg.getSourceNode().getSubjectRef().getSubjectType().name(),
                    routeLeg.getSourceNode().getLedgerSubjectCode(), EntrySide.DEBIT);
        }

        private static FeeRefundRouteNodeKey credit(RouteLegSpec routeLeg) {
            return new FeeRefundRouteNodeKey(routeLeg.getTargetNode().getSubjectRef().getSubjectId(),
                    routeLeg.getTargetNode().getSubjectRef().getSubjectType().name(),
                    routeLeg.getTargetNode().getLedgerSubjectCode(), EntrySide.CREDIT);
        }
    }
}
