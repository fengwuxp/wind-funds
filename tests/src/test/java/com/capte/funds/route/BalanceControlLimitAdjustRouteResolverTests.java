package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceControlLimitAdjustRouteResolverTests extends BalanceControlFundsInstructionRouteResolverTestSupport {

    /**
     * 场景：信用账户额度调增，释放可用额度。
     * 输入：CREDIT_ACCOUNT，增加 2000，业务场景 LIMIT。
     * 输出：LIMIT 到 AVAILABLE 的额度调整 route。
     * 预期：信用账户 AVAILABLE 增加，route 不引入真实资金主体。
     * 红线：信用额度账户不是真实资金，不得通过平台资金账户平衡。
     */
    @Test
    void testResolveCreditLimitAdjustShouldMoveLimitToAvailable() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.creditAccount("credit_001"), 2_000L, Boolean.TRUE, "LIMIT", "LIMIT_0001"),
                WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("CREDIT_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.LIMIT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
    }

    /**
     * 场景：信用账户额度调减，收回可用额度。
     * 输入：CREDIT_ACCOUNT，减少 900，业务场景 LIMIT。
     * 输出：AVAILABLE 到 LIMIT 的额度调整 route。
     * 预期：信用账户 AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：额度回收不得绕过 AVAILABLE 非负约束形成超额使用。
     */
    @Test
    void testResolveCreditLimitDecreaseShouldConstrainAvailableBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.creditAccount("credit_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 900L, Boolean.FALSE, "LIMIT", "LIMIT_0002"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("CREDIT_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    /**
     * 场景：信用账户额度调减允许受控负 AVAILABLE。
     * 输入：CREDIT_ACCOUNT，减少 900，请求上下文显式允许受控负余额。
     * 输出：AVAILABLE 到 LIMIT 的额度调整 route。
     * 预期：source AVAILABLE 声明 ALLOW_NEGATIVE，由账本 profile 再做最终授权。
     * 红线：profile 允许负余额不代表每笔额度调减都自动放开。
     */
    @Test
    void testResolveCreditLimitDecreaseShouldAllowControlledNegativeWhenContextExplicit() {
        FundsAccountId accountId = FundsRouteTestSupport.creditAccount("credit_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 900L, Boolean.FALSE, "LIMIT", "LIMIT_0003")
                .setContextVariables(allowNegativeContext("CREDIT_AVAILABLE_CONTROLLED_NEGATIVE")),
                WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertConstraint(leg, accountId, LedgerSubjectCode.AVAILABLE, LedgerBalanceConstraintType.ALLOW_NEGATIVE);
        });
    }

    /**
     * 场景：预算组额度调增，释放可用预算。
     * 输入：BUDGET_GROUP，增加 1500，业务场景 BUDGET。
     * 输出：LIMIT 到 AVAILABLE 的额度调整 route。
     * 预期：预算组 AVAILABLE 增加，route 不引入真实资金主体。
     * 红线：预算组是控制主体，不得当作真实资金账户参与外部出入金。
     */
    @Test
    void testResolveBudgetLimitIncreaseShouldMoveLimitToAvailable() {
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                FundsRouteTestSupport.budgetGroup("budget_001"), 1_500L, Boolean.TRUE, "BUDGET", "BUDGET_0002"),
                WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BUDGET_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.LIMIT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
    }

    /**
     * 场景：预算组额度调减，收回可用预算。
     * 输入：BUDGET_GROUP，减少 1500，业务场景 BUDGET。
     * 输出：AVAILABLE 到 LIMIT 的额度调整 route。
     * 预期：预算组 AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：预算回收不得让可用预算静默变负。
     */
    @Test
    void testResolveBudgetLimitAdjustShouldMoveAvailableToLimitOnDecrease() {
        FundsAccountId accountId = FundsRouteTestSupport.budgetGroup("budget_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_500L, Boolean.FALSE, "BUDGET", "BUDGET_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BUDGET_LIMIT_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    /**
     * 场景：预算组额度调减允许受控负 AVAILABLE。
     * 输入：BUDGET_GROUP，减少 1500，请求上下文显式允许受控负余额。
     * 输出：AVAILABLE 到 LIMIT 的预算调整 route。
     * 预期：source AVAILABLE 声明 ALLOW_NEGATIVE，由账本 profile 再做最终授权。
     * 红线：预算超用必须由业务策略显式决策，不得被普通调减静默触发。
     */
    @Test
    void testResolveBudgetLimitAdjustShouldAllowControlledNegativeWhenContextExplicit() {
        FundsAccountId accountId = FundsRouteTestSupport.budgetGroup("budget_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_500L, Boolean.FALSE, "BUDGET", "BUDGET_0003")
                .setContextVariables(budgetAllowNegativeContext()),
                WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertConstraint(leg, accountId, LedgerSubjectCode.AVAILABLE, LedgerBalanceConstraintType.ALLOW_NEGATIVE);
        });
    }

    /**
     * 场景：预算组额度调减允许受控负数但缺预算治理上下文。
     * 输入：BUDGET_GROUP，减少 1500，分别缺预算周期、治理策略或报表标记。
     * 输出：route 解析拒绝。
     * 预期：不生成 ALLOW_NEGATIVE route。
     * 红线：预算超用必须能落到周期、治理策略和报表口径，不能只复用普通资金负余额证据。
     */
    @Test
    void testResolveBudgetLimitAdjustShouldRejectControlledNegativeWithoutBudgetGovernance() {
        assertBudgetAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.BUDGET_PERIOD_ID,
                "预算受控负余额调账缺少预算周期",
                "BUDGET_0004");
        assertBudgetAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.BUDGET_GOVERNANCE_POLICY_CODE,
                "预算受控负余额调账缺少治理策略",
                "BUDGET_0005");
        assertBudgetAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.BUDGET_REPORT_MARKER,
                "预算受控负余额调账缺少报表标记",
                "BUDGET_0006");
    }
}
