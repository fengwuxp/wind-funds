package com.capte.funds.transaction.boundary;

import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.capte.funds.transaction.services.FundsFrozenOrderService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

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

    /**
     * 场景：冻结单只表达同主体 AVAILABLE 与 FROZEN 的控制事实。
     * 输入：反射读取 FundsFrozenOrderService 与 FundsFrozenOrderServiceImpl 的公开方法。
     * 输出：公开方法名中的禁止消费、结算、扣划语义。
     * 预期：冻结单 API 不提供 consume、settle、deduct 等后续价值转移能力。
     * 红线：提现出款确认、追偿、退款或调账必须作为独立资金事实引用冻结单。
     */
    @Test
    void testFrozenOrderServiceShouldNotExposeConsumptionOrDeductionApi() {
        Set<String> forbiddenFragments = Set.of("consume", "consumption", "settle", "deduct");

        assertThat(publicMethodNames(FundsFrozenOrderService.class))
                .allSatisfy(methodName -> assertThat(containsAny(methodName, forbiddenFragments))
                        .as("FundsFrozenOrderService should not expose consumption or deduction API: %s", methodName)
                        .isFalse());
        assertThat(publicMethodNames(com.capte.funds.transaction.services.impl.FundsFrozenOrderServiceImpl.class))
                .allSatisfy(methodName -> assertThat(containsAny(methodName, forbiddenFragments))
                        .as("FundsFrozenOrderServiceImpl should not expose consumption or deduction API: %s", methodName)
                        .isFalse());
    }

    private static Set<String> publicMethodNames(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .filter(method -> method.getDeclaringClass() != Object.class)
                .map(Method::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static boolean containsAny(String value, Set<String> fragments) {
        return fragments.stream().anyMatch(value::contains);
    }
}
