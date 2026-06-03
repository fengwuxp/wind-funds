package com.wind.funds.dsl;

import com.wind.funds.model.FundsContextVariables;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金 DSL 扩展上下文通用契约测试。
 */
class FundsContextVariablesContractTests {

    /**
     * 场景：调用方在顶层扩展上下文中传入空白 key。
     * 预期：统一快照入口立即拒绝。
     * 红线：contextVariables 会进入 route、账务、审计和投影链路，不能保存不可解释的字段名。
     */
    @Test
    void testContextVariablesShouldRejectBlankTopLevelKey() {
        assertThatThrownBy(() -> FundsContextVariables.immutableCopy(Map.of(" ", "blank-key")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables key must be text");
    }

    /**
     * 场景：调用方在嵌套扩展上下文中传入非文本 key。
     * 预期：统一快照入口递归拒绝。
     * 红线：嵌套 Map 会被序列化、日志、导出和回放消费，不能用非 JSON 字段名绕过契约治理。
     */
    @Test
    void testContextVariablesShouldRejectNestedNonTextKey() {
        Map<Object, Object> processorPayload = new LinkedHashMap<>();
        processorPayload.put(1001L, "non-text-key");

        assertThatThrownBy(() -> FundsContextVariables.immutableCopy(
                Map.of("processorPayload", processorPayload)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contextVariables key must be text");
    }
}
