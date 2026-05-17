package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DelegatingFundsInstructionLifecycleRecorderCompatibilityTests {

    /**
     * 场景：P0-G 命名治理中，组合分发实现从 Saver 正名为 Delegating Recorder。
     * 输入：新的 DelegatingFundsInstructionLifecycleRecorder 与旧 CompositeFundsInstructionLifecycleSaver。
     * 输出：旧类作为废弃兼容别名继承新主类。
     * 预期：Spring 主 Bean 和新调用方依赖 Recorder 命名，旧源码调用方仍可编译。
     * 红线：不得注册两个组合生命周期分发 Bean，也不得破坏每个指令只命中一个记录器的约束。
     */
    @Test
    void testCompositeSaverShouldRemainDeprecatedCompatibilityAlias() {
        assertThat(DelegatingFundsInstructionLifecycleRecorder.class)
                .isAssignableTo(FundsInstructionLifecycleRecorder.class)
                .isAssignableTo(FundsInstructionLifecycleSaver.class);
        assertThat(CompositeFundsInstructionLifecycleSaver.class)
                .isAssignableTo(DelegatingFundsInstructionLifecycleRecorder.class);
        assertThat(CompositeFundsInstructionLifecycleSaver.class.isAnnotationPresent(Deprecated.class))
                .isTrue();
    }
}
