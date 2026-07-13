package com.wind.funds.wallet.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spend Rule 控制额度变动类型契约测试。
 */
class SpendControlMovementTypeContractTests {

    /**
     * 场景：历史准入和拒绝活动类型仍存在于枚举中。
     * 预期：它们只解释为决策记录兼容类型，不参与新的控制额度变动流水。
     * 红线：不得再次把准入或拒绝证据写入预算控制投影。
     */
    @Test
    void testDecisionRecordCompatibilityTypesShouldNotBeControlMovements() {
        EnumSet<SpendControlMovementType> decisionRecordCompatibilityTypes = EnumSet.of(
                SpendControlMovementType.ADMISSION_RECORDED,
                SpendControlMovementType.REJECTED_RECORDED);

        assertThat(decisionRecordCompatibilityTypes)
                .allSatisfy(type -> {
                    assertThat(type.getProductSemantic()).isEqualTo("SpendRuleDecisionRecord");
                    assertThat(type.isDecisionRecordType()).isTrue();
                    assertThat(type.isBudgetProjectionMovement()).isFalse();
                    assertThat(type.isControlMovement()).isFalse();
                    assertThat(type.isLimitAdjustmentMovement()).isFalse();
                    assertThat(type.isReleaseMovement()).isFalse();
                });
    }

    /**
     * 场景：额度调整、预留、消耗、退款补偿和释放类活动进入控制额度变动流水。
     * 预期：这些类型统一解释为 SpendControlMovement，并参与预算控制投影。
     * 红线：控制额度变动流水不得退化成规则决策记录。
     */
    @Test
    void testControlMovementTypesShouldParticipateInBudgetProjection() {
        EnumSet<SpendControlMovementType> movementTypes = EnumSet.complementOf(EnumSet.of(
                SpendControlMovementType.ADMISSION_RECORDED,
                SpendControlMovementType.REJECTED_RECORDED));

        assertThat(movementTypes)
                .allSatisfy(type -> {
                    assertThat(type.getProductSemantic()).isEqualTo("SpendControlMovement");
                    assertThat(type.isControlMovement()).isTrue();
                    assertThat(type.isBudgetProjectionMovement()).isTrue();
                    assertThat(type.isDecisionRecordType()).isFalse();
                });
    }

    /**
     * 场景：预算控制额度调整需要额外审计字段。
     * 预期：只有调增和调减被归类为额度调整流水。
     */
    @Test
    void testLimitAdjustmentTypesShouldBeExplicitSubsetOfControlMovements() {
        assertThat(SpendControlMovementType.LIMIT_INCREASED.isLimitAdjustmentMovement()).isTrue();
        assertThat(SpendControlMovementType.LIMIT_DECREASED.isLimitAdjustmentMovement()).isTrue();
        assertThat(EnumSet.complementOf(EnumSet.of(
                        SpendControlMovementType.LIMIT_INCREASED,
                        SpendControlMovementType.LIMIT_DECREASED)))
                .noneMatch(SpendControlMovementType::isLimitAdjustmentMovement);
    }

    /**
     * 场景：收到可信完成或撤销事实后释放已占用控制额度。
     * 预期：只有 RELEASED 是释放类流水，超时不落控制流水，撤销来源由业务引用表达。
     */
    @Test
    void testReleaseTypesShouldBeExplicitSubsetOfControlMovements() {
        EnumSet<SpendControlMovementType> releaseTypes = EnumSet.of(SpendControlMovementType.RELEASED);

        assertThat(releaseTypes)
                .allSatisfy(type -> {
                    assertThat(type.isReleaseMovement()).isTrue();
                    assertThat(type.isControlMovement()).isTrue();
                });
        assertThat(EnumSet.complementOf(releaseTypes))
                .noneMatch(SpendControlMovementType::isReleaseMovement);
    }
}
