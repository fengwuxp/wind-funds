package com.capte.funds.transaction.services;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FundsInstructionLifecycleRecorderContractTests {

    /**
     * 场景：生命周期记录器作为写侧端口，防止交易事实查询能力重新混入 recorder。
     * 输入：反射读取 FundsInstructionLifecycleRecorder 声明的方法。
     * 输出：方法名称集合和返回类型集合。
     * 预期：契约只保留 beforePosting、markSucceeded、markFailed，不出现 query/find/get/list 等查询职责。
     * 红线：生命周期记录器不得承载交易视图查询、route replay 查询或账本查询职责。
     */
    @Test
    void testLifecycleRecorderShouldNotContainQueryResponsibilities() {
        Method[] methods = FundsInstructionLifecycleRecorder.class.getDeclaredMethods();
        List<String> methodNames = Arrays.stream(methods)
                .map(Method::getName)
                .toList();

        assertThat(methodNames)
                .containsExactlyInAnyOrder("supports", "beforePosting", "markSucceeded", "markFailed");
        assertThat(methodNames)
                .noneMatch(FundsInstructionLifecycleRecorderContractTests::isQueryMethodName);
        assertThat(Arrays.stream(methods).map(Method::getReturnType).toList())
                .containsExactlyInAnyOrder(Boolean.TYPE, FundsInstructionLifecycleResult.class, Void.TYPE, Void.TYPE);
    }

    /**
     * 场景：P0-G 命名治理中，旧 Saver 契约作为兼容别名继续保留。
     * 输入：反射读取 FundsInstructionLifecycleSaver 和 FundsInstructionLifecycleRecorder 类型关系。
     * 输出：旧接口的废弃标记与父接口。
     * 预期：新代码可依赖 Recorder；旧调用方仍可通过 Saver 编译。
     * 红线：兼容别名不得重新声明方法或扩展查询职责。
     */
    @Test
    void testLifecycleSaverShouldBeDeprecatedCompatibilityAliasOfRecorder() {
        assertThat(FundsInstructionLifecycleSaver.class)
                .isAssignableTo(FundsInstructionLifecycleRecorder.class)
                .hasAnnotation(Deprecated.class);
        assertThat(FundsInstructionLifecycleSaver.class.getDeclaredMethods()).isEmpty();
    }

    private static boolean isQueryMethodName(String methodName) {
        return methodName.startsWith("query")
                || methodName.startsWith("find")
                || methodName.startsWith("get")
                || methodName.startsWith("list")
                || methodName.startsWith("count")
                || methodName.startsWith("search");
    }
}
