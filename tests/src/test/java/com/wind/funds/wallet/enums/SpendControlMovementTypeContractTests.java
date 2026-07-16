package com.wind.funds.wallet.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spend Rule 控制额度变动类型契约测试。
 */
class SpendControlMovementTypeContractTests {

    /**
     * 场景：额度调整、预留、消耗、退款补偿和释放类活动进入控制额度变动流水。
     * 预期：枚举只包含真实控制额度变动类型，并全部参与预算控制投影。
     * 红线：准入和拒绝决策不得作为控制额度变动类型保留。
     */
    @Test
    void testOnlyControlMovementTypesShouldBeDeclared() {
        EnumSet<SpendControlMovementType> movementTypes = EnumSet.allOf(SpendControlMovementType.class);

        assertThat(movementTypes)
                .containsExactly(
                        SpendControlMovementType.LIMIT_INCREASED,
                        SpendControlMovementType.LIMIT_DECREASED,
                        SpendControlMovementType.RESERVED,
                        SpendControlMovementType.CONSUMED,
                        SpendControlMovementType.REFUND_COMPENSATED,
                        SpendControlMovementType.RELEASED)
                .allSatisfy(type -> {
                    assertThat(type.isControlMovement()).isTrue();
                    assertThat(type.isBudgetProjectionMovement()).isTrue();
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
