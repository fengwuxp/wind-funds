package com.wind.funds.wallet.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spend Rule 控制额度变动类型契约测试。
 */
class SpendControlActivityTypeContractTests {

    /**
     * 场景：历史准入和拒绝活动类型仍存在于枚举中。
     * 预期：它们只解释为决策记录兼容类型，不参与新的控制额度变动流水。
     * 红线：不得再次把准入或拒绝证据写入预算控制投影。
     */
    @Test
    void testDecisionRecordCompatibilityTypesShouldNotBeControlMovements() {
        EnumSet<SpendControlActivityType> decisionRecordCompatibilityTypes = EnumSet.of(
                SpendControlActivityType.ADMISSION_RECORDED,
                SpendControlActivityType.REJECTED_RECORDED);

        assertThat(decisionRecordCompatibilityTypes)
                .allSatisfy(type -> {
                    assertThat(type.getProductSemantic()).isEqualTo("SpendRuleDecisionRecord");
                    assertThat(type.isDecisionRecordCompatibilityActivity()).isTrue();
                    assertThat(type.isBudgetProjectionActivity()).isFalse();
                    assertThat(type.isControlMovementActivity()).isFalse();
                    assertThat(type.isLimitAdjustmentActivity()).isFalse();
                    assertThat(type.isReleaseActivity()).isFalse();
                });
    }

    /**
     * 场景：额度调整、预留、消耗、退款补偿和释放类活动进入控制额度变动流水。
     * 预期：这些类型统一解释为 SpendControlMovement，并参与预算控制投影。
     * 红线：控制额度变动流水不得退化成规则决策记录。
     */
    @Test
    void testControlMovementTypesShouldParticipateInBudgetProjection() {
        EnumSet<SpendControlActivityType> movementTypes = EnumSet.complementOf(EnumSet.of(
                SpendControlActivityType.ADMISSION_RECORDED,
                SpendControlActivityType.REJECTED_RECORDED));

        assertThat(movementTypes)
                .allSatisfy(type -> {
                    assertThat(type.getProductSemantic()).isEqualTo("SpendControlMovement");
                    assertThat(type.isControlMovementActivity()).isTrue();
                    assertThat(type.isBudgetProjectionActivity()).isTrue();
                    assertThat(type.isDecisionRecordCompatibilityActivity()).isFalse();
                });
    }

    /**
     * 场景：预算控制额度调整需要额外审计字段。
     * 预期：只有调增和调减被归类为额度调整流水。
     */
    @Test
    void testLimitAdjustmentTypesShouldBeExplicitSubsetOfControlMovements() {
        assertThat(SpendControlActivityType.LIMIT_INCREASED.isLimitAdjustmentActivity()).isTrue();
        assertThat(SpendControlActivityType.LIMIT_DECREASED.isLimitAdjustmentActivity()).isTrue();
        assertThat(EnumSet.complementOf(EnumSet.of(
                        SpendControlActivityType.LIMIT_INCREASED,
                        SpendControlActivityType.LIMIT_DECREASED)))
                .noneMatch(SpendControlActivityType::isLimitAdjustmentActivity);
    }

    /**
     * 场景：交易失败、过期或撤销后释放已占用控制额度。
     * 预期：释放、过期和撤销是释放类流水，仍然属于控制额度变动。
     */
    @Test
    void testReleaseTypesShouldBeExplicitSubsetOfControlMovements() {
        EnumSet<SpendControlActivityType> releaseTypes = EnumSet.of(
                SpendControlActivityType.RELEASED,
                SpendControlActivityType.EXPIRED,
                SpendControlActivityType.REVERSED);

        assertThat(releaseTypes)
                .allSatisfy(type -> {
                    assertThat(type.isReleaseActivity()).isTrue();
                    assertThat(type.isControlMovementActivity()).isTrue();
                });
        assertThat(EnumSet.complementOf(releaseTypes))
                .noneMatch(SpendControlActivityType::isReleaseActivity);
    }
}
