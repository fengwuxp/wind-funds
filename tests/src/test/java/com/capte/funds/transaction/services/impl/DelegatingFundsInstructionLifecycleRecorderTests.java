package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegatingFundsInstructionLifecycleRecorderTests {

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

    /**
     * 场景：余额控制冻结指令需要进入冻结单生命周期记录器，而不是标准交易记录器。
     * 输入：一个支持 TOPUP 的交易记录器、一个支持 FREEZE/UNFREEZE 的冻结单记录器。
     * 输出：委托记录器只调用冻结单记录器，并继续把成功或失败结果委托给同一记录器。
     * 预期：组合分发按事实载体唯一命中，冻结/解冻不创建标准 FundsTransaction。
     * 红线：不得因为多个记录器存在而双写生命周期事实。
     */
    @Test
    void testBeforePostingShouldDelegateToOnlySupportedSaver() {
        RecordingLifecycleSaver transactionSaver = new RecordingLifecycleSaver("FT_001",
                FundsTransactionEventType.TOPUP);
        RecordingLifecycleSaver frozenOrderSaver = new RecordingLifecycleSaver("FO_001",
                FundsTransactionEventType.FREEZE, FundsTransactionEventType.UNFREEZE);
        DelegatingFundsInstructionLifecycleRecorder composite = new DelegatingFundsInstructionLifecycleRecorder(
                List.of(transactionSaver, frozenOrderSaver));
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
        assertThat(transactionSaver.beforePostingCalls).hasValue(0);
        assertThat(frozenOrderSaver.beforePostingInstruction.get()).isSameAs(instruction);
        assertThat(frozenOrderSaver.succeededLedgerTransactionSn.get()).isEqualTo("LT_001");
        assertThat(frozenOrderSaver.failedCause.get()).isSameAs(failure);
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
        RecordingLifecycleSaver transactionSaver = new RecordingLifecycleSaver("FT_001",
                FundsTransactionEventType.TOPUP);
        DelegatingFundsInstructionLifecycleRecorder composite = new DelegatingFundsInstructionLifecycleRecorder(
                List.of(transactionSaver));
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
        RecordingLifecycleSaver first = new RecordingLifecycleSaver("FT_001", FundsTransactionEventType.TOPUP);
        RecordingLifecycleSaver second = new RecordingLifecycleSaver("FT_002", FundsTransactionEventType.TOPUP);
        DelegatingFundsInstructionLifecycleRecorder composite = new DelegatingFundsInstructionLifecycleRecorder(
                List.of(first, second));
        FundsInstructionSpec instruction = instruction(FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TOPUP);

        assertThat(composite.supports(instruction)).isFalse();
        assertThatThrownBy(() -> composite.beforePosting(instruction, route(FundsTransactionEventType.TOPUP),
                snapshot(FundsTransactionEventType.TOPUP)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金指令生命周期记录器不唯一");
        assertThat(first.beforePostingCalls).hasValue(0);
        assertThat(second.beforePostingCalls).hasValue(0);
    }

    private static FundsInstructionSpec instruction(FundsInstructionType instructionType,
                                                    FundsTransactionEventType eventType) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1001L)
                .instructionType(instructionType)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(100L, CurrencyIsoCode.USD))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("LIFECYCLE_TEST")
                .businessSn("LIFECYCLE_0001")
                .eventTime(LocalDateTime.of(2026, 5, 15, 10, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(-1L)
                        .operatorType("SYSTEM")
                        .operatorName("SYSTEM")
                        .appName("capte-tests")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of())
                .build();
    }

    private static ResolvedRouteSpec route(FundsTransactionEventType eventType) {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(1001L)
                .routeCode("LIFECYCLE_ROUTE")
                .routeVersion("1.0")
                .businessScene("LIFECYCLE_TEST")
                .businessSn("LIFECYCLE_0001")
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .participants(List.of())
                .legs(List.of())
                .resolvedAt(LocalDateTime.of(2026, 5, 15, 10, 0))
                .contextVariables(Map.of())
                .build();
    }

    private static RouteSnapshotSpec snapshot(FundsTransactionEventType eventType) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1001L)
                .snapshotId("RS_001")
                .snapshotSchemaVersion("1.0")
                .routeCode("LIFECYCLE_ROUTE")
                .routeVersion("1.0")
                .businessScene("LIFECYCLE_TEST")
                .businessSn("LIFECYCLE_0001")
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .participants(List.of())
                .legs(List.of())
                .resolvedAt(LocalDateTime.of(2026, 5, 15, 10, 0))
                .contextVariables(Map.of())
                .build();
    }

    private static final class RecordingLifecycleSaver implements FundsInstructionLifecycleRecorder {

        private final String transactionSn;

        private final List<FundsTransactionEventType> supportedEventTypes;

        private final AtomicInteger beforePostingCalls = new AtomicInteger();

        private final AtomicReference<FundsInstructionSpec> beforePostingInstruction = new AtomicReference<>();

        private final AtomicReference<String> succeededLedgerTransactionSn = new AtomicReference<>();

        private final AtomicReference<Throwable> failedCause = new AtomicReference<>();

        private RecordingLifecycleSaver(String transactionSn, FundsTransactionEventType... supportedEventTypes) {
            this.transactionSn = transactionSn;
            this.supportedEventTypes = List.of(supportedEventTypes);
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return supportedEventTypes.contains(instruction.getEventType());
        }

        @Override
        public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                      @NonNull ResolvedRouteSpec resolvedRoute,
                                                                      @NonNull RouteSnapshotSpec routeSnapshot) {
            beforePostingCalls.incrementAndGet();
            beforePostingInstruction.set(instruction);
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn(transactionSn)
                    .setTransactionDetailSns(List.of())
                    .setCompleted(false);
        }

        @Override
        public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                                  @NonNull FundsInstructionLifecycleResult result,
                                  @Nullable String ledgerTransactionSn) {
            succeededLedgerTransactionSn.set(ledgerTransactionSn);
        }

        @Override
        public void markFailed(@NonNull FundsInstructionSpec instruction,
                               @NonNull FundsInstructionLifecycleResult result,
                               @NonNull Throwable cause) {
            failedCause.set(cause);
        }
    }
}
