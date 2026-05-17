package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.capte.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorderTestSupport.RecordingLifecycleRecorder;
import static com.capte.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorderTestSupport.instruction;
import static com.capte.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorderTestSupport.route;
import static com.capte.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorderTestSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegatingFundsInstructionLifecycleRecorderTests {

    /**
     * 场景：余额控制冻结指令需要进入冻结单生命周期记录器，而不是标准交易记录器。
     * 输入：一个支持 TOPUP 的交易记录器、一个支持 FREEZE/UNFREEZE 的冻结单记录器。
     * 输出：委托记录器只调用冻结单记录器，并继续把成功或失败结果委托给同一记录器。
     * 预期：组合分发按事实载体唯一命中，冻结/解冻不创建标准 FundsTransaction。
     * 红线：不得因为多个记录器存在而双写生命周期事实。
     */
    @Test
    void testBeforePostingShouldDelegateToOnlySupportedSaver() {
        RecordingLifecycleRecorder transactionRecorder = new RecordingLifecycleRecorder("FT_001",
                FundsTransactionEventType.TOPUP);
        RecordingLifecycleRecorder frozenOrderRecorder = new RecordingLifecycleRecorder("FO_001",
                FundsTransactionEventType.FREEZE, FundsTransactionEventType.UNFREEZE);
        DelegatingFundsInstructionLifecycleRecorder composite = new DelegatingFundsInstructionLifecycleRecorder(
                List.of(transactionRecorder, frozenOrderRecorder));
        FundsInstructionSpec instruction = instruction(FundsInstructionType.BALANCE_CONTROL,
                FundsTransactionEventType.FREEZE);
        ResolvedRouteSpec route = route(FundsTransactionEventType.FREEZE);
        RouteSnapshotSpec snapshot = snapshot(FundsTransactionEventType.FREEZE);

        FundsInstructionLifecycleResult result = composite.beforePosting(instruction, route, snapshot);
        composite.markSucceeded(instruction, result, "LT_001");
        RuntimeException failure = new RuntimeException("posting failed");
        composite.markFailed(instruction, result, failure);

        assertThat(composite.supports(instruction)).isTrue();
        assertThat(result.getTransactionSn()).isEqualTo("FO_001");
        assertThat(transactionRecorder.beforePostingCalls()).hasValue(0);
        assertThat(frozenOrderRecorder.beforePostingInstruction().get()).isSameAs(instruction);
        assertThat(frozenOrderRecorder.succeededLedgerTransactionSn().get()).isEqualTo("LT_001");
        assertThat(frozenOrderRecorder.failedCause().get()).isSameAs(failure);
    }

    /**
     * 场景：余额控制指令没有任何生命周期记录器支持。
     * 输入：只有 TOPUP 交易记录器，传入 FREEZE 指令。
     * 输出：委托记录器拒绝处理。
     * 预期：编排器不会在事实载体缺失时继续入账。
     * 红线：不得静默跳过生命周期事实记录。
     */
    @Test
    void testBeforePostingShouldRejectWhenNoSaverSupported() {
        RecordingLifecycleRecorder transactionRecorder = new RecordingLifecycleRecorder("FT_001",
                FundsTransactionEventType.TOPUP);
        DelegatingFundsInstructionLifecycleRecorder composite = new DelegatingFundsInstructionLifecycleRecorder(
                List.of(transactionRecorder));
        FundsInstructionSpec instruction = instruction(FundsInstructionType.BALANCE_CONTROL,
                FundsTransactionEventType.FREEZE);

        assertThat(composite.supports(instruction)).isFalse();
        assertThatThrownBy(() -> composite.beforePosting(instruction, route(FundsTransactionEventType.FREEZE),
                snapshot(FundsTransactionEventType.FREEZE)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到支持的资金指令生命周期记录器");
    }

    /**
     * 场景：同一个资金指令被多个生命周期记录器声明支持。
     * 输入：两个都支持 TOPUP 的记录器。
     * 输出：委托记录器拒绝处理，且不调用任意一个记录器。
     * 预期：每个资金指令必须且只能命中一个生命周期事实载体。
     * 红线：不得出现重复创建 FundsTransaction 或 FrozenOrder 的双写路径。
     */
    @Test
    void testBeforePostingShouldRejectWhenMultipleSaversSupported() {
        RecordingLifecycleRecorder first = new RecordingLifecycleRecorder("FT_001", FundsTransactionEventType.TOPUP);
        RecordingLifecycleRecorder second = new RecordingLifecycleRecorder("FT_002", FundsTransactionEventType.TOPUP);
        DelegatingFundsInstructionLifecycleRecorder composite = new DelegatingFundsInstructionLifecycleRecorder(
                List.of(first, second));
        FundsInstructionSpec instruction = instruction(FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TOPUP);

        assertThat(composite.supports(instruction)).isFalse();
        assertThatThrownBy(() -> composite.beforePosting(instruction, route(FundsTransactionEventType.TOPUP),
                snapshot(FundsTransactionEventType.TOPUP)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金指令生命周期记录器不唯一");
        assertThat(first.beforePostingCalls()).hasValue(0);
        assertThat(second.beforePostingCalls()).hasValue(0);
    }
}
