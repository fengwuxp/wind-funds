package com.capte.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.capte.funds.support.FundsBalanceAssertionSupport.delta;
import static com.capte.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 余额控制失败业务流测试。
 */
class FundsBalanceControlFailureFlowTests extends FundsTransactionFlowTestSupport {

    /**
     * 场景：用户充值后发起超过可用余额的冻结请求。
     * 输入：充值 50、冻结 80。
     * 输出：冻结请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结前状态。
     * 预期：失败路径不生成冻结单、不生成新的 ledger transaction。
     * 红线：冻结只能控制已经存在的可用余额，不得透支、不得留下半成功事实。
     */
    @Test
    void testFreezeWithInsufficientAvailableBalanceShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_FREEZE_FAIL_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> freeze(user, 80L, "BALANCE_FREEZE_FAIL_FREEZE"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_FREEZE_FAIL_FREEZE")).isFalse();
    }

    /**
     * 场景：用户 USD 资金账户发起 CNY 冻结请求。
     * 输入：充值 50 USD、冻结请求 10 CNY。
     * 输出：冻结请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结前状态。
     * 预期：余额控制只接受同币种余额桶控制，错币种由业务层先完成决策或换汇后再提交。
     * 红线：`FundsBalanceControlService` 不承接 FX，不做隐式换汇，不留下冻结单或账务事实。
     */
    @Test
    void testFreezeWithDifferentCurrencyShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_FREEZE_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(10L, CurrencyIsoCode.CNY))
                .setBusinessScene("FREEZE")
                .setBusinessSn("BALANCE_FREEZE_CURRENCY_FREEZE")
                .setDescription("freeze with different currency"), WindOperator.system()))
                .hasMessageContaining("amount currency must equal account currency");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_FREEZE_CURRENCY_FREEZE")).isFalse();
    }

    /**
     * 场景：用户充值后发起 0 金额冻结请求。
     * 输入：充值 50、冻结 0。
     * 输出：冻结请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结前状态。
     * 预期：0 金额不能进入资金指令、route、posting 或冻结单生命周期。
     * 红线：余额控制不能用 0 金额伪造冻结动作、幂等占位或无账务观察事件。
     */
    @Test
    void testFreezeWithZeroAmountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_FREEZE_ZERO_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(0L, CURRENCY))
                .setBusinessScene("FREEZE")
                .setBusinessSn("BALANCE_FREEZE_ZERO_FREEZE")
                .setDescription("freeze with zero amount"), WindOperator.system()))
                .hasMessageContaining("fundsInstruction.amount must be positive");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_FREEZE_ZERO_FREEZE")).isFalse();
    }

    /**
     * 场景：用户冻结余额后发起 0 金额解冻请求。
     * 输入：充值 50、冻结 30、解冻 0。
     * 输出：解冻请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结后状态。
     * 预期：0 金额不能进入解冻指令、route、posting 或冻结单释放生命周期。
     * 红线：解冻不能用 0 金额制造释放动作、幂等占位或无账务观察事件。
     */
    @Test
    void testUnfreezeWithZeroAmountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_UNFREEZE_ZERO_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 30L, "BALANCE_UNFREEZE_ZERO_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(0L, CURRENCY))
                .setReferenceFreezeSn(freezeSn)
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("BALANCE_UNFREEZE_ZERO_RELEASE")
                .setDescription("unfreeze with zero amount"), WindOperator.system()))
                .hasMessageContaining("fundsInstruction.amount must be positive");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);
        assertPostedTransactions(2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_ZERO_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_ZERO_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_UNFREEZE_ZERO_RELEASE")).isFalse();
    }

    /**
     * 场景：资金账户余额调账请求缺少调账原因、凭证或审批引用。
     * 输入：同一个账户分别提交缺少原因、缺少凭证、缺少审批引用的调账请求。
     * 输出：三个请求均被拒绝，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持不变。
     * 预期：余额调账必须携带审计上下文，不能绕过原因、证据和审批约束。
     * 红线：调账失败不得写入 ledger transaction，不得改变余额桶。
     */
    @Test
    void testBalanceAdjustWithoutAuditContextShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        assertThatThrownBy(() -> balanceControlService.adjust(balanceAdjustRequest(user,
                "BALANCE_ADJUST_AUDIT_MISSING_REASON"), WindOperator.system()))
                .hasMessageContaining("余额调账缺少调账原因");
        assertThatThrownBy(() -> balanceControlService.adjust(balanceAdjustRequest(user,
                "BALANCE_ADJUST_AUDIT_MISSING_EVIDENCE")
                .setAdjustReason("customer service balance adjust"), WindOperator.system()))
                .hasMessageContaining("余额调账缺少调账凭证");
        assertThatThrownBy(() -> balanceControlService.adjust(balanceAdjustRequest(user,
                "BALANCE_ADJUST_AUDIT_MISSING_APPROVAL")
                .setAdjustReason("customer service balance adjust")
                .setAdjustEvidenceRef("EVIDENCE_BALANCE_ADJUST_001"), WindOperator.system()))
                .hasMessageContaining("余额调账缺少审批引用");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(0);
    }

    private static FundsBalanceAdjustRequest balanceAdjustRequest(FundsAccountId accountId, String businessSn) {
        return new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(10L, CURRENCY))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("BALANCE_ADJUST")
                .setBusinessSn(businessSn)
                .setDescription("balance adjust without audit context");
    }
}
