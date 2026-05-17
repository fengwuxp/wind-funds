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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceControlFundingAdjustRouteResolverTests extends BalanceControlFundsInstructionRouteResolverTestSupport {

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
}
