package com.capte.funds.route;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceControlFundsInstructionRouteResolverTests {

    private FundsBalanceControlInstructionConverter converter;

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
        converter = FundsRouteTestSupport.balanceControlInstructionConverter();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    /**
     * 场景：运营对普通资金账户做余额调增。
     * 输入：FUNDING_ACCOUNT，增加 1000，业务场景 ADJUST。
     * 输出：平台 ADJUSTMENT 账户到用户 AVAILABLE 的调账 route。
     * 预期：route 携带平台调整账户快照，用户 AVAILABLE 增加。
     * 红线：资金调增必须通过平台 ADJUSTMENT 平衡，不得凭空增加用户余额。
     */
    @Test
    void testResolveFundingBalanceAdjustShouldUsePlatformAdjustment() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_000L, Boolean.TRUE, "ADJUST", "ADJUST_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("FUNDING_BALANCE_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.ADJUST);
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.ADJUSTMENT);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getBalanceEffectType()).isEqualTo(LedgerBalanceEffectType.INCREASE);
            assertThat(leg.getPhaseCode()).isEqualTo(LedgerPhaseCode.ADJUSTMENT);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getAdjustmentFundingAccount()).isNotNull();
    }

    /**
     * 场景：运营对普通资金账户做余额调减。
     * 输入：FUNDING_ACCOUNT，减少 1000，业务场景 ADJUST。
     * 输出：用户 AVAILABLE 到平台 ADJUSTMENT 账户的调账 route。
     * 预期：用户 AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：余额调减不得让负 AVAILABLE 静默产生。
     */
    @Test
    void testResolveFundingBalanceDecreaseShouldConstrainAvailableBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_000L, Boolean.FALSE, "ADJUST", "ADJUST_0002"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("FUNDING_BALANCE_ADJUST_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.ADJUSTMENT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getPlatformAccounts()).isNotNull();
        assertThat(route.getPlatformAccounts().getAdjustmentFundingAccount()).isNotNull();
    }

    /**
     * 场景：运营对普通资金账户做受控负余额调减。
     * 输入：FUNDING_ACCOUNT，减少 1000，请求上下文显式允许受控负余额。
     * 输出：用户 AVAILABLE 到平台 ADJUSTMENT 账户的调账 route。
     * 预期：用户 AVAILABLE 作为 source，并声明本次可按 profile 受控为负。
     * 红线：负 AVAILABLE 必须有显式业务决策开关，不得静默放开。
     */
    @Test
    void testResolveFundingBalanceDecreaseShouldAllowControlledNegativeWhenContextExplicit() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_000L, Boolean.FALSE, "ADJUST", "ADJUST_0003")
                .setContextVariables(allowNegativeContext("FUNDING_AVAILABLE_CONTROLLED_NEGATIVE")),
                WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.ADJUSTMENT,
                    LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
            assertConstraint(leg, accountId, LedgerSubjectCode.AVAILABLE, LedgerBalanceConstraintType.ALLOW_NEGATIVE);
        });
    }

    /**
     * 场景：运营尝试只用布尔开关放开普通资金账户负余额。
     * 输入：FUNDING_ACCOUNT，减少 1000，只有 allowNegativeBalance=true。
     * 输出：route 解析拒绝。
     * 预期：缺少策略编码、审批依据或原因时，不生成 ALLOW_NEGATIVE route。
     * 红线：受控负余额不能退化成单个布尔后门。
     */
    @Test
    void testResolveFundingBalanceDecreaseShouldRejectAllowNegativeWithoutPolicyEvidence() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_000L, Boolean.FALSE, "ADJUST", "ADJUST_0004")
                .setContextVariables(context(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE, Boolean.TRUE)),
                WindOperator.system());

        assertThatThrownBy(() -> FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("受控负余额调账缺少策略编码");
    }

    /**
     * 场景：运营尝试放开受控负余额但治理证据不完整。
     * 输入：FUNDING_ACCOUNT，减少 1000，分别缺少风险状态、单笔上限、累计上限或账龄起点。
     * 输出：route 解析拒绝。
     * 预期：治理证据缺失时，不生成 ALLOW_NEGATIVE route。
     * 红线：受控负余额必须有上限、风险状态和账龄治理，不能只靠审批文本放开。
     */
    @Test
    void testResolveFundingBalanceDecreaseShouldRejectAllowNegativeWithoutGovernanceEvidence() {
        assertAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS,
                "受控负余额调账缺少风险状态",
                "ADJUST_0005");
        assertAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT,
                "受控负余额调账缺少单笔上限",
                "ADJUST_0006");
        assertAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT,
                "受控负余额调账缺少累计上限",
                "ADJUST_0007");
        assertAllowNegativeContextMissingShouldFail(
                FundsInstructionContextKeys.NEGATIVE_AVAILABLE_AGING_STARTED_AT,
                "受控负余额调账缺少账龄起点",
                "ADJUST_0008");
    }

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

    /**
     * 场景：冻结普通资金账户可用余额。
     * 输入：FUNDING_ACCOUNT，冻结 600，业务场景 FREEZE。
     * 输出：AVAILABLE 到 FROZEN 的余额控制 route。
     * 预期：AVAILABLE 作为 source，并声明本次更新后必须非负。
     * 红线：冻结只做同主体余额桶控制，不表达消费或跨主体资金转移。
     */
    @Test
    void testResolveFreezeShouldConstrainAvailableBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToFreezeInstruction(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(600L))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BALANCE_FREEZE_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.HOLD, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN,
                    LedgerBalanceEffectType.HOLD, LedgerPhaseCode.FREEZE);
            assertThat(leg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.PARTIAL_ALLOWED);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.AVAILABLE);
        });
    }

    /**
     * 场景：释放普通资金账户冻结余额。
     * 输入：FUNDING_ACCOUNT，解冻 600，引用 FREEZE_0001。
     * 输出：FROZEN 到 AVAILABLE 的余额控制 route。
     * 预期：FROZEN 作为 source，并声明本次更新后必须非负。
     * 红线：解冻不得释放超过冻结桶剩余余额。
     */
    @Test
    void testResolveUnfreezeShouldReleaseFrozenBalance() {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToUnfreezeInstruction(new FundsBalanceUnfreezeRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(600L))
                .setReferenceFreezeSn("FREEZE_0001")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("UNFREEZE_0001"), WindOperator.system());

        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction);

        assertThat(route.getRouteCode()).isEqualTo("BALANCE_UNFREEZE_STANDARD");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertLeg(leg, RouteLegType.RELEASE, LedgerSubjectCode.FROZEN, LedgerSubjectCode.AVAILABLE,
                    LedgerBalanceEffectType.RELEASE, LedgerPhaseCode.UNFREEZE);
            assertMustNotBeNegative(leg, accountId, LedgerSubjectCode.FROZEN);
        });
    }

    /**
     * 场景：route 解析只负责生成冻结路径，冻结单由生命周期服务独立管理。
     * 输入：BalanceControlFundsInstructionRouteResolver 构造器签名。
     * 输出：构造器不依赖 FundsFrozenOrderService。
     * 预期：route resolver 不直接创建或更新 FrozenOrder。
     * 红线：冻结事实载体不得和 route 解析职责耦合。
     */
    @Test
    void testFrozenOrderCreationShouldBeOptionalForFreezeRoute() {
        boolean dependsOnFrozenOrderService = Arrays.stream(BalanceControlFundsInstructionRouteResolver.class
                        .getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .flatMap(Arrays::stream)
                .map(Class::getSimpleName)
                .anyMatch("FundsFrozenOrderService"::equals);

        assertThat(dependsOnFrozenOrderService).isFalse();
    }

    private static void assertLeg(RouteLegSpec leg,
                                  RouteLegType legType,
                                  LedgerSubjectCode sourceLedgerSubjectCode,
                                  LedgerSubjectCode targetLedgerSubjectCode,
                                  LedgerBalanceEffectType balanceEffectType,
                                  LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(sourceLedgerSubjectCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(targetLedgerSubjectCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(balanceEffectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    private static void assertMustNotBeNegative(RouteLegSpec leg,
                                                FundsAccountId accountId,
                                                LedgerSubjectCode ledgerSubjectCode) {
        assertConstraint(leg, accountId, ledgerSubjectCode, LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    private static void assertConstraint(RouteLegSpec leg,
                                         FundsAccountId accountId,
                                         LedgerSubjectCode ledgerSubjectCode,
                                         LedgerBalanceConstraintType constraintType) {
        assertThat(leg.getConstraintOverrides())
                .containsEntry(accountId.type() + ":" + accountId.id() + ":" + ledgerSubjectCode.name(),
                        constraintType);
    }

    private static WritableContextVariables context(String name, Object value) {
        return new TestContextVariables().putVariable(name, value);
    }

    private static WritableContextVariables allowNegativeContext(String policyCode) {
        return new TestContextVariables()
                .putVariable(FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE, Boolean.TRUE)
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE, policyCode)
                .putVariable(FundsInstructionContextKeys.APPROVAL_REF, "APPROVAL_0001")
                .putVariable(FundsInstructionContextKeys.ADJUST_REASON, "controlled negative balance test")
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS, "IN_GOVERNANCE")
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT,
                        FundsRouteTestSupport.amount(5_000L))
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT,
                        FundsRouteTestSupport.amount(20_000L))
                .putVariable(FundsInstructionContextKeys.NEGATIVE_AVAILABLE_AGING_STARTED_AT,
                        LocalDateTime.of(2026, 5, 16, 10, 0));
    }

    private static WritableContextVariables budgetAllowNegativeContext() {
        return allowNegativeContext("BUDGET_AVAILABLE_CONTROLLED_NEGATIVE")
                .putVariable(FundsInstructionContextKeys.BUDGET_PERIOD_ID, "BUDGET_2026_M05")
                .putVariable(FundsInstructionContextKeys.BUDGET_GOVERNANCE_POLICY_CODE,
                        "BUDGET_OVERUSE_GOVERNANCE")
                .putVariable(FundsInstructionContextKeys.BUDGET_REPORT_MARKER, "BUDGET_REPORT_2026_M05");
    }

    private void assertAllowNegativeContextMissingShouldFail(String missingKey,
                                                             String expectedMessage,
                                                             String businessSn) {
        FundsAccountId accountId = FundsRouteTestSupport.fundingAccount("funding_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_000L, Boolean.FALSE, "ADJUST", businessSn)
                .setContextVariables(allowNegativeContext("FUNDING_AVAILABLE_CONTROLLED_NEGATIVE")
                        .removeVariable(missingKey)),
                WindOperator.system());

        assertThatThrownBy(() -> FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(expectedMessage);
    }

    private void assertBudgetAllowNegativeContextMissingShouldFail(String missingKey,
                                                                   String expectedMessage,
                                                                   String businessSn) {
        FundsAccountId accountId = FundsRouteTestSupport.budgetGroup("budget_001");
        FundsInstructionSpec instruction = converter.convertToAdjustInstruction(adjustRequest(
                accountId, 1_500L, Boolean.FALSE, "BUDGET", businessSn)
                .setContextVariables(budgetAllowNegativeContext()
                        .removeVariable(missingKey)),
                WindOperator.system());

        assertThatThrownBy(() -> FundsRouteTestSupport.balanceControlRouteResolver().resolve(instruction))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(expectedMessage);
    }

    private static FundsBalanceAdjustRequest adjustRequest(FundsAccountId accountId,
                                                           long amount,
                                                           Boolean increase,
                                                           String businessScene,
                                                           String businessSn) {
        return new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(FundsRouteTestSupport.amount(amount))
                .setIncrease(increase)
                .setBusinessScene(businessScene)
                .setBusinessSn(businessSn)
                .setAdjustReason("adjust reason")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn);
    }

    private static final class TestContextVariables implements WritableContextVariables {

        private final Map<String, Object> variables = new HashMap<>();

        @Override
        public WritableContextVariables putVariable(String name, Object val) {
            variables.put(name, val);
            return this;
        }

        @Override
        public WritableContextVariables removeVariable(String name) {
            variables.remove(name);
            return this;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.copyOf(variables);
        }
    }
}
