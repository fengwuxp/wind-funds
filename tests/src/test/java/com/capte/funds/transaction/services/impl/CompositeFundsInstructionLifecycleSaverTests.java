package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
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

class CompositeFundsInstructionLifecycleSaverTests {

    @Test
    void testBeforePostingShouldDelegateToOnlySupportedSaver() {
        RecordingLifecycleSaver transactionSaver = new RecordingLifecycleSaver("FT_001",
                FundsTransactionEventType.TOPUP);
        RecordingLifecycleSaver frozenOrderSaver = new RecordingLifecycleSaver("FO_001",
                FundsTransactionEventType.FREEZE, FundsTransactionEventType.UNFREEZE);
        CompositeFundsInstructionLifecycleSaver composite = new CompositeFundsInstructionLifecycleSaver(
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

    @Test
    void testBeforePostingShouldRejectWhenNoSaverSupported() {
        RecordingLifecycleSaver transactionSaver = new RecordingLifecycleSaver("FT_001",
                FundsTransactionEventType.TOPUP);
        CompositeFundsInstructionLifecycleSaver composite = new CompositeFundsInstructionLifecycleSaver(
                List.of(transactionSaver));
        FundsInstructionSpec instruction = instruction(FundsInstructionType.BALANCE_CONTROL,
                FundsTransactionEventType.FREEZE);

        assertThat(composite.supports(instruction)).isFalse();
        assertThatThrownBy(() -> composite.beforePosting(instruction, route(FundsTransactionEventType.FREEZE),
                snapshot(FundsTransactionEventType.FREEZE)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到支持的资金指令生命周期保存器");
    }

    @Test
    void testBeforePostingShouldRejectWhenMultipleSaversSupported() {
        RecordingLifecycleSaver first = new RecordingLifecycleSaver("FT_001", FundsTransactionEventType.TOPUP);
        RecordingLifecycleSaver second = new RecordingLifecycleSaver("FT_002", FundsTransactionEventType.TOPUP);
        CompositeFundsInstructionLifecycleSaver composite = new CompositeFundsInstructionLifecycleSaver(
                List.of(first, second));
        FundsInstructionSpec instruction = instruction(FundsInstructionType.DIRECT_TRANSACTION,
                FundsTransactionEventType.TOPUP);

        assertThat(composite.supports(instruction)).isFalse();
        assertThatThrownBy(() -> composite.beforePosting(instruction, route(FundsTransactionEventType.TOPUP),
                snapshot(FundsTransactionEventType.TOPUP)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金指令生命周期保存器不唯一");
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

    private static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver {

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
