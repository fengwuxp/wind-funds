package com.capte.funds.transaction;

import com.capte.funds.transaction.services.FundsFrozenOrderService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class FundsFrozenOrderBoundaryTests {

    /**
     * 场景：冻结单是业务审核和撤销引用记录，不是资金编排器的强制依赖。
     * 输入：反射读取 DefaultRoutedFundsInstructionOrchestrator 构造参数。
     * 输出：构造参数中是否存在 FundsFrozenOrderService。
     * 预期：编排器不直接依赖冻结单服务，是否创建冻结单由上层业务决定。
     */
    @Test
    void testFrozenOrderCreationShouldBeOptional() {
        boolean dependsOnFrozenOrderService = Arrays.stream(DefaultRoutedFundsInstructionOrchestrator.class
                        .getDeclaredConstructors())
                .map(Constructor::getParameterTypes)
                .flatMap(Arrays::stream)
                .anyMatch(FundsFrozenOrderService.class::equals);

        assertThat(dependsOnFrozenOrderService).isFalse();
    }
}
