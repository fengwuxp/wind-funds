package com.wind.funds.transaction.application.flow;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.core.WritableContextVariables;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertOnlyBalanceDeltas;
import static com.wind.funds.support.FundsBalanceAssertionSupport.delta;
import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
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
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();
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
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_FREEZE_FAIL_TOPUP", 3, 4);
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_FREEZE_FAIL_FREEZE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_FREEZE_FAIL_FREEZE");
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
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();
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
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_FREEZE_CURRENCY_TOPUP", 3, 4);
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_FREEZE_CURRENCY_FREEZE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_FREEZE_CURRENCY_FREEZE");
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
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();
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
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_FREEZE_ZERO_TOPUP", 3, 4);
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_FREEZE_ZERO_FREEZE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_FREEZE_ZERO_FREEZE");
    }

    /**
     * 场景：用户冻结成功后，使用相同业务流水重复提交但把冻结金额改大。
     * 输入：充值 100、第一次冻结 30，随后同业务流水改为冻结 40。
     * 输出：第二次请求被冻结单请求摘要拒绝，AVAILABLE/FROZEN 和既有冻结单保持第一次冻结后的状态。
     * 预期：冻结幂等必须保护金额、主体和 route 摘要，不只按业务键静默短路。
     * 红线：同业务流水不同冻结请求不得新增 ledger transaction，不得改变冻结余额或冻结单金额。
     */
    @Test
    void testFreezeSameBusinessSnWithDifferentAmountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "BALANCE_FREEZE_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 30L, "BALANCE_FREEZE_IDEMPOTENT_FREEZE");
        BalanceSnapshot afterFirstFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFirstFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstFreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> freeze(user, 40L, "BALANCE_FREEZE_IDEMPOTENT_FREEZE"))
                .hasMessageContaining("资金冻结单请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstFreeze, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstFreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 70L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);

        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_FREEZE_IDEMPOTENT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_FREEZE_IDEMPOTENT_FREEZE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_FREEZE_IDEMPOTENT_FREEZE"))
                .satisfies(order -> {
                    assertThat(order.getSn()).isEqualTo(freezeSn);
                    assertThat(order.getAmount()).isEqualTo(30L);
                    assertThat(order.getReleasedAmount()).isZero();
                    assertThat(order.getStatus()).isEqualTo(FundsFrozenOrderStatus.FROZEN);
                });
    }

    /**
     * 场景：用户 USD 资金账户冻结后发起 CNY 解冻请求。
     * 输入：充值 50 USD、冻结 30 USD、解冻请求 10 CNY。
     * 输出：解冻请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结后状态。
     * 预期：解冻只释放原币种冻结余额，错币种由业务层先完成决策或换汇后再提交。
     * 红线：`FundsBalanceControlService` 不承接 FX，不做隐式换汇，不留下释放 route、posting 或账务事实。
     */
    @Test
    void testUnfreezeWithDifferentCurrencyShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_UNFREEZE_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 30L, "BALANCE_UNFREEZE_CURRENCY_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(10L, CurrencyIsoCode.CNY))
                .setReferenceFreezeSn(freezeSn)
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("BALANCE_UNFREEZE_CURRENCY_RELEASE")
                .setDescription("unfreeze with different currency"), WindOperator.system()))
                .hasMessageContaining("amount currency must equal account currency");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_CURRENCY_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_CURRENCY_FREEZE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_CURRENCY_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_CURRENCY_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_UNFREEZE_CURRENCY_RELEASE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_UNFREEZE_CURRENCY_RELEASE");
    }

    /**
     * 场景：用户冻结余额后发起缺少原冻结单引用的解冻请求。
     * 输入：充值 50、冻结 30、解冻 10，但不传 `referenceFreezeSn`。
     * 输出：解冻请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结后状态。
     * 预期：解冻必须沿原冻结单和原路径释放，不允许无引用释放。
     * 红线：缺少原冻结单引用不得退化为空指针，不得生成释放 route、posting 或账务事实。
     */
    @Test
    void testUnfreezeWithoutReferenceFreezeSnShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_UNFREEZE_MISSING_REF_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(user, 30L, "BALANCE_UNFREEZE_MISSING_REF_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(10L, CURRENCY))
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("BALANCE_UNFREEZE_MISSING_REF_RELEASE")
                .setDescription("unfreeze without reference freeze sn"), WindOperator.system()))
                .hasMessageContaining("余额解冻缺少原冻结单引用");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_MISSING_REF_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_MISSING_REF_FREEZE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_MISSING_REF_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_MISSING_REF_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_UNFREEZE_MISSING_REF_RELEASE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_UNFREEZE_MISSING_REF_RELEASE");
    }

    /**
     * 场景：用户冻结余额后引用不存在的冻结单发起解冻。
     * 输入：充值 50、冻结 30、解冻 10，但 `referenceFreezeSn` 指向不存在的冻结单。
     * 输出：解冻请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结后状态。
     * 预期：解冻必须定位原冻结单和原路径快照，未知引用不能释放余额。
     * 红线：未知冻结单引用不得生成释放 route、posting 或账务事实。
     */
    @Test
    void testUnfreezeWithUnknownReferenceFreezeSnShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_UNFREEZE_UNKNOWN_REF_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        freeze(user, 30L, "BALANCE_UNFREEZE_UNKNOWN_REF_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 30L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(10L, CURRENCY))
                .setReferenceFreezeSn("FREEZE_ORDER_NOT_EXISTS")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_RELEASE")
                .setDescription("unfreeze with unknown reference freeze sn"), WindOperator.system()))
                .hasMessageContaining("RouteSnapshot 回放事件未找到原路径快照");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_FREEZE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_RELEASE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_UNFREEZE_UNKNOWN_REF_RELEASE");
    }

    /**
     * 场景：用户 A 冻结余额后，用户 B 引用用户 A 的冻结单发起解冻。
     * 输入：A 充值 50、冻结 30，B 解冻 10 并传入 A 的 `referenceFreezeSn`。
     * 输出：解冻请求失败，A/B AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持冻结后状态。
     * 预期：解冻引用的原冻结单主体必须与请求账户一致。
     * 红线：不得跨主体释放冻结余额，不得生成释放 route、posting 或账务事实。
     */
    @Test
    void testUnfreezeWithDifferentAccountReferenceShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId anotherUser = fundingAccount("funding_user_another");
        ensureLedger(anotherUser, LedgerSubjectCode.AVAILABLE);
        ensureLedger(anotherUser, LedgerSubjectCode.FROZEN);
        BalanceSnapshot before = snapshot(balances(user, anotherUser, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_UNFREEZE_ACCOUNT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, anotherUser, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 30L, "BALANCE_UNFREEZE_ACCOUNT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, anotherUser, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -30L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 30L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(anotherUser)
                .setAmount(Money.immutable(10L, CURRENCY))
                .setReferenceFreezeSn(freezeSn)
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("BALANCE_UNFREEZE_ACCOUNT_RELEASE")
                .setDescription("unfreeze with different account reference"), WindOperator.system()))
                .hasMessageContaining("冻结单引用主体与请求账户不一致");

        BalanceSnapshot afterFailure = snapshot(balances(user, anotherUser, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(anotherUser, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(anotherUser), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_ACCOUNT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_ACCOUNT_FREEZE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_ACCOUNT_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_ACCOUNT_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_UNFREEZE_ACCOUNT_RELEASE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_UNFREEZE_ACCOUNT_RELEASE");
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
        LedgerFactSnapshot afterFreezeFacts = ledgerFactSnapshot();
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
        assertLedgerTransactionFactsUnchanged(afterFreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 20L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 30L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_ZERO_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_ZERO_FREEZE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_ZERO_FREEZE").getStatus())
                .isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_ZERO_FREEZE").getReleasedAmount()).isZero();
        assertThat(frozenOrderExistsByBusinessSn("BALANCE_UNFREEZE_ZERO_RELEASE")).isFalse();
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_UNFREEZE_ZERO_RELEASE");
    }

    /**
     * 场景：用户部分解冻成功后，使用相同业务流水重复提交但把解冻金额改大。
     * 输入：充值 100、冻结 70、第一次解冻 20，随后同业务流水改为解冻 30。
     * 输出：第二次请求被冻结单请求摘要拒绝，AVAILABLE/FROZEN、原冻结单和释放记录保持第一次解冻后的状态。
     * 预期：解冻幂等必须保护原冻结引用、解冻金额、主体和 route 摘要。
     * 红线：同业务流水不同解冻请求不得新增 ledger transaction，不得重复释放或污染原冻结单释放金额。
     */
    @Test
    void testUnfreezeSameBusinessSnWithDifferentAmountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");

        BalanceSnapshot beforeTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        topup(user, 100L, "BALANCE_UNFREEZE_IDEMPOTENT_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(beforeTopup, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -100L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        String freezeSn = freeze(user, 70L, "BALANCE_UNFREEZE_IDEMPOTENT_FREEZE");
        BalanceSnapshot afterFreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, -70L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 70L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        unfreeze(user, 20L, freezeSn, "BALANCE_UNFREEZE_IDEMPOTENT_RELEASE");
        BalanceSnapshot afterFirstUnfreeze = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFreeze, afterFirstUnfreeze,
                delta(user, LedgerSubjectCode.AVAILABLE, 20L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, -20L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        LedgerFactSnapshot afterFirstUnfreezeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> unfreeze(user, 30L, freezeSn,
                "BALANCE_UNFREEZE_IDEMPOTENT_RELEASE"))
                .hasMessageContaining("资金冻结单请求参数不一致");

        BalanceSnapshot afterConflict = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterFirstUnfreeze, afterConflict,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterFirstUnfreezeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 50L, CURRENCY);

        assertPostedTransactions(3);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_IDEMPOTENT_TOPUP", 3, 4);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_IDEMPOTENT_FREEZE", 0, 0, 1, 2);
        assertFundsAndLedgerFactsForBusinessSn("BALANCE_UNFREEZE_IDEMPOTENT_RELEASE", 0, 0, 1, 2);
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_IDEMPOTENT_FREEZE"))
                .satisfies(order -> {
                    assertThat(order.getAmount()).isEqualTo(70L);
                    assertThat(order.getReleasedAmount()).isEqualTo(20L);
                    assertThat(order.getStatus()).isEqualTo(FundsFrozenOrderStatus.PARTIALLY_RELEASED);
                });
        assertThat(frozenOrderByBusinessSn("BALANCE_UNFREEZE_IDEMPOTENT_RELEASE"))
                .satisfies(order -> {
                    assertThat(order.getAmount()).isEqualTo(20L);
                    assertThat(order.getReleasedAmount()).isZero();
                    assertThat(order.getStatus()).isEqualTo(FundsFrozenOrderStatus.RELEASED);
                });
    }

    /**
     * 场景：用户充值后发起 0 金额余额调账请求。
     * 输入：充值 50、调账 0，并给齐调账原因、凭证和审批引用。
     * 输出：调账请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持调账前状态。
     * 预期：0 金额不能进入余额调账指令、route、posting 或账本分录生命周期。
     * 红线：调账不能用 0 金额绕过审计上下文、制造幂等占位或无账务观察事件。
     */
    @Test
    void testBalanceAdjustWithZeroAmountShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_ADJUST_ZERO_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(0L, CURRENCY))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("BALANCE_ADJUST")
                .setBusinessSn("BALANCE_ADJUST_ZERO_ADJUST")
                .setAdjustReason("customer service balance adjust")
                .setAdjustEvidenceRef("EVIDENCE_BALANCE_ADJUST_ZERO")
                .setApprovalRef("APPROVAL_BALANCE_ADJUST_ZERO")
                .setDescription("balance adjust with zero amount"), WindOperator.system()))
                .hasMessageContaining("fundsInstruction.amount must be positive");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_ADJUST_ZERO_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_ZERO_ADJUST");
    }

    /**
     * 场景：用户 USD 资金账户发起 CNY 余额调账请求。
     * 输入：充值 50 USD、调账 10 CNY，并给齐调账原因、凭证和审批引用。
     * 输出：调账请求失败，用户 AVAILABLE/FROZEN 与平台 CASH/PREPAYMENT 余额保持调账前状态。
     * 预期：余额调账只接受同币种余额桶控制，错币种由业务层先完成决策或换汇后再提交。
     * 红线：`FundsBalanceControlService` 不承接 FX，不做隐式换汇，不留下 route、posting 或账务事实。
     */
    @Test
    void testBalanceAdjustWithDifferentCurrencyShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));

        topup(user, 50L, "BALANCE_ADJUST_CURRENCY_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(10L, CurrencyIsoCode.CNY))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("BALANCE_ADJUST")
                .setBusinessSn("BALANCE_ADJUST_CURRENCY_ADJUST")
                .setAdjustReason("customer service balance adjust")
                .setAdjustEvidenceRef("EVIDENCE_BALANCE_ADJUST_CURRENCY")
                .setApprovalRef("APPROVAL_BALANCE_ADJUST_CURRENCY")
                .setDescription("balance adjust with different currency"), WindOperator.system()))
                .hasMessageContaining("amount currency must equal account currency");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterTopup, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_ADJUST_CURRENCY_TOPUP", 3, 4);
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_CURRENCY_ADJUST");
    }

    /**
     * 场景：信用账户发起额度调增后再调减。
     * 输入：信用账户额度调增 100、调减 40，并给齐调账原因、凭证和审批引用。
     * 输出：只调整该信用账户 LIMIT/AVAILABLE 控制账本，AUTHORIZATION、资金账户和平台资金账本不变。
     * 预期：信用额度调整与资金账户余额调整分离，不经由平台调整挂账户承载价值转移。
     * 红线：额度调整不得污染真实资金账户、平台 CASH/PREPAYMENT 或授权占用账本。
     */
    @Test
    void testCreditLimitAdjustShouldAffectOnlyCreditControlLedgers() {
        FundsAccountId credit = creditAccount("credit_limit_flow");
        FundsAccountId funding = fundingAccount("funding_user");
        ensureCreditAccount(credit);
        BalanceSnapshot before = snapshot(balances(credit, funding, cashMappingAccount(), prepaymentAccount()));

        adjustBalance(credit, 100L, true, "CREDIT_LIMIT_ADJUST_INCREASE");

        BalanceSnapshot afterIncrease = snapshot(balances(credit, funding, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterIncrease,
                delta(credit, LedgerSubjectCode.LIMIT, 100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        adjustBalance(credit, 40L, false, "CREDIT_LIMIT_ADJUST_DECREASE");

        BalanceSnapshot afterDecrease = snapshot(balances(credit, funding, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterIncrease, afterDecrease,
                delta(credit, LedgerSubjectCode.LIMIT, -40L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, -40L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertBucket(balance(credit), LedgerSubjectCode.LIMIT, 60L, CURRENCY);
        assertBucket(balance(credit), LedgerSubjectCode.AVAILABLE, 60L, CURRENCY);
        assertBucket(balance(credit), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(funding), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(funding), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(funding), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertPostedTransactions(2);
        assertSingleFundsAndLedgerFactsForBusinessSn("CREDIT_LIMIT_ADJUST_INCREASE", 1, 2);
        assertSingleFundsAndLedgerFactsForBusinessSn("CREDIT_LIMIT_ADJUST_DECREASE", 1, 2);
    }

    /**
     * 场景：历史调用方继续通过资金余额控制入口调整预算组额度。
     * 输入：预算组额度调增 300，并给齐调账原因、凭证和审批引用。
     * 输出：旧入口前置拒绝，不生成资金交易、route、posting、LedgerEntry、账本交易或余额投影事实。
     * 预期：预算额度调整必须迁移到预算控制活动和预算控制投影入口。
     * 红线：预算组不得再通过余额控制写入任何 ledger entry。
     */
    @Test
    void testBudgetLimitAdjustShouldRejectLegacyBalanceControlEntry() {
        FundsAccountId budget = budgetGroup("budget_limit_flow");
        FundsAccountId funding = fundingAccount("funding_user");
        ensureBudgetGroupWithoutLedgers(budget);
        BalanceSnapshot before = snapshot(balances(budget, funding, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> adjustBalance(budget, 300L, true, "BUDGET_LIMIT_ADJUST_INCREASE"))
                .hasMessageContaining("预算组额度调整已迁移到预算控制活动");

        BalanceSnapshot afterFailure = snapshot(balances(budget, funding, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(budget, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("BUDGET_LIMIT_ADJUST_INCREASE");
        assertPostedTransactions(0);
    }

    /**
     * 场景：信用账户额度调增后发起超过可调下限的调减。
     * 输入：信用账户额度调增 100、调减 140，并给齐调账原因、凭证和审批引用。
     * 输出：调减失败，信用账户 LIMIT/AVAILABLE/AUTHORIZATION 与资金账户、平台账本保持调减前状态。
     * 预期：信用额度调减必须校验可调下限，不得让 LIMIT 或 AVAILABLE 透支。
     * 红线：失败调减不得污染真实资金账户、平台 CASH/PREPAYMENT 或留下半成功账务事实。
     */
    @Test
    void testCreditLimitDecreaseExceedingAvailableShouldLeaveNoSideEffects() {
        FundsAccountId credit = creditAccount("credit_limit_dec_fail");
        FundsAccountId funding = fundingAccount("funding_user");
        ensureCreditAccount(credit);
        BalanceSnapshot before = snapshot(balances(credit, funding, cashMappingAccount(), prepaymentAccount()));

        adjustBalance(credit, 100L, true, "CREDIT_LIMIT_DECREASE_FAIL_INCREASE");
        BalanceSnapshot afterIncrease = snapshot(balances(credit, funding, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot afterIncreaseFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(before, afterIncrease,
                delta(credit, LedgerSubjectCode.LIMIT, 100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> adjustBalance(credit, 140L, false,
                "CREDIT_LIMIT_DECREASE_FAIL_DECREASE"))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterFailure = snapshot(balances(credit, funding, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(afterIncrease, afterFailure,
                delta(credit, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(credit, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterIncreaseFacts);
        assertBucket(balance(credit), LedgerSubjectCode.LIMIT, 100L, CURRENCY);
        assertBucket(balance(credit), LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance(credit), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(balance(funding), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(funding), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(funding), LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("CREDIT_LIMIT_DECREASE_FAIL_INCREASE", 1, 2);
        assertFailedFundsTransactionWithoutLedgerFacts("CREDIT_LIMIT_DECREASE_FAIL_DECREASE");
    }

    /**
     * 场景：历史调用方继续通过资金余额控制入口调减预算组额度。
     * 输入：预算组额度调减 260，并给齐调账原因、凭证和审批引用。
     * 输出：旧入口前置拒绝，预算组、资金账户和平台账本均无变化。
     * 预期：预算额度调减下限校验应由预算控制活动入口负责，不再通过 ledger 余额桶表达。
     * 红线：失败路径不得污染真实资金账户、平台 CASH/PREPAYMENT 或留下半成功账务事实。
     */
    @Test
    void testBudgetLimitDecreaseShouldRejectLegacyBalanceControlEntryAndLeaveNoSideEffects() {
        FundsAccountId budget = budgetGroup("budget_limit_dec_fail");
        FundsAccountId funding = fundingAccount("funding_user");
        ensureBudgetGroupWithoutLedgers(budget);
        BalanceSnapshot before = snapshot(balances(budget, funding, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> adjustBalance(budget, 260L, false,
                "BUDGET_LIMIT_DECREASE_FAIL_DECREASE"))
                .hasMessageContaining("预算组额度调整已迁移到预算控制活动");

        BalanceSnapshot afterFailure = snapshot(balances(budget, funding, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(budget, LedgerSubjectCode.LIMIT, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(budget, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(funding, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertNoFundsOrLedgerFactsForBusinessSn("BUDGET_LIMIT_DECREASE_FAIL_DECREASE");
        assertPostedTransactions(0);
    }

    /**
     * 场景：用户充值后发起超过可用余额的减少调账请求。
     * 输入：充值 50、减少调账 80，并给齐调账原因、凭证和审批引用。
     * 输出：调账请求失败，用户 AVAILABLE/FROZEN 与平台调整挂账余额保持调账前状态。
     * 预期：默认调账减少必须遵守 AVAILABLE 不可为负约束。
     * 红线：余额调账不能借人工审批语义绕过余额约束、透支客户资金或留下半成功账务事实。
     */
    @Test
    void testBalanceDecreaseAdjustWithInsufficientAvailableBalanceShouldLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        FundsAccountId adjustmentAccount = fundingAccount("platform_adjustment");
        ensureLedger(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT);
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), adjustmentAccount));

        topup(user, 50L, "BALANCE_ADJUST_DECREASE_FAIL_TOPUP");
        BalanceSnapshot afterTopup = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), adjustmentAccount));
        LedgerFactSnapshot afterTopupFacts = ledgerFactSnapshot();
        assertOnlyBalanceDeltas(before, afterTopup,
                delta(user, LedgerSubjectCode.AVAILABLE, 50L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, -50L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY));

        assertThatThrownBy(() -> balanceControlService.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(user)
                .setAmount(Money.immutable(80L, CURRENCY))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("BALANCE_ADJUST")
                .setBusinessSn("BALANCE_ADJUST_DECREASE_FAIL_ADJUST")
                .setAdjustReason("customer service balance decrease adjust")
                .setAdjustEvidenceRef("EVIDENCE_BALANCE_ADJUST_DECREASE_FAIL")
                .setApprovalRef("APPROVAL_BALANCE_ADJUST_DECREASE_FAIL")
                .setDescription("balance decrease adjust exceeds available"), WindOperator.system()))
                .hasMessageContaining("账本余额不足");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount(), adjustmentAccount));
        assertOnlyBalanceDeltas(afterTopup, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY),
                delta(adjustmentAccount, LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(afterTopupFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 50L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertBucket(balance(adjustmentAccount), LedgerSubjectCode.ADJUSTMENT, 0L, CURRENCY);
        assertPostedTransactions(1);
        assertSingleFundsAndLedgerFactsForBusinessSn("BALANCE_ADJUST_DECREASE_FAIL_TOPUP", 3, 4);
        assertFailedFundsTransactionWithoutLedgerFacts("BALANCE_ADJUST_DECREASE_FAIL_ADJUST");
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
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

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
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_AUDIT_MISSING_REASON");
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_AUDIT_MISSING_EVIDENCE");
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_AUDIT_MISSING_APPROVAL");
    }

    /**
     * 场景：余额调账审计字段齐全，但扩展上下文携带原始银行账户号字段。
     * 输入：调账请求包含原因、凭证、审批引用，并在 contextVariables 中写入 bankAccountNo。
     * 输出：调账请求被拒绝，余额桶和账务事实不变化。
     * 预期：请求扩展上下文不得进入资金交易事实、route snapshot 或账务事实。
     * 红线：原始外部账户号不得通过普通余额控制上下文落库。
     */
    @Test
    void testBalanceAdjustWithSensitiveContextVariablesShouldRejectAndLeaveNoSideEffects() {
        FundsAccountId user = fundingAccount("funding_user");
        BalanceSnapshot before = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot beforeFacts = ledgerFactSnapshot();

        assertThatThrownBy(() -> balanceControlService.adjust(balanceAdjustRequest(user,
                "BALANCE_ADJUST_SENSITIVE_CONTEXT")
                .setAdjustReason("customer service balance adjust")
                .setAdjustEvidenceRef("EVIDENCE_BALANCE_ADJUST_SENSITIVE_CONTEXT")
                .setApprovalRef("APPROVAL_BALANCE_ADJUST_SENSITIVE_CONTEXT")
                .setContextVariables(WritableContextVariables.of(Map.of("externalAccount",
                        Map.of("bankAccountNo", "123456789012")))), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");
        assertThatThrownBy(() -> balanceControlService.adjust(balanceAdjustRequest(user,
                "BALANCE_ADJUST_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setAdjustReason("customer service balance adjust")
                .setAdjustEvidenceRef("EVIDENCE_BALANCE_ADJUST_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setApprovalRef("APPROVAL_BALANCE_ADJUST_SENSITIVE_CONTEXT_IBAN_VALUE")
                .setContextVariables(WritableContextVariables.of(Map.of("processorPayload",
                        Map.of("networkReference", "GB82WEST12345698765432")))), WindOperator.system()))
                .hasMessageContaining("contextVariables must not contain sensitive funds transaction fields");

        BalanceSnapshot afterFailure = snapshot(balances(user, cashMappingAccount(), prepaymentAccount()));
        assertOnlyBalanceDeltas(before, afterFailure,
                delta(user, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY),
                delta(user, LedgerSubjectCode.FROZEN, 0L, CURRENCY),
                delta(cashMappingAccount(), LedgerSubjectCode.CASH, 0L, CURRENCY),
                delta(prepaymentAccount(), LedgerSubjectCode.PREPAYMENT, 0L, CURRENCY));
        assertLedgerTransactionFactsUnchanged(beforeFacts);
        assertBucket(balance(user), LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(balance(user), LedgerSubjectCode.FROZEN, 0L, CURRENCY);
        assertPostedTransactions(0);
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_SENSITIVE_CONTEXT");
        assertNoFundsOrLedgerFactsForBusinessSn("BALANCE_ADJUST_SENSITIVE_CONTEXT_IBAN_VALUE");
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
