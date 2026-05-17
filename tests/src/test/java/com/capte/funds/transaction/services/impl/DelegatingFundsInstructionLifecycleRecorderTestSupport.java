package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

final class DelegatingFundsInstructionLifecycleRecorderTestSupport {

    private DelegatingFundsInstructionLifecycleRecorderTestSupport() {
    }

    static FundsInstructionSpec instruction(FundsInstructionType instructionType,
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

    static ResolvedRouteSpec route(FundsTransactionEventType eventType) {
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

    static RouteSnapshotSpec snapshot(FundsTransactionEventType eventType) {
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

    static final class RecordingLifecycleRecorder implements FundsInstructionLifecycleRecorder {

        private final String transactionSn;

        private final List<FundsTransactionEventType> supportedEventTypes;

        private final AtomicInteger beforePostingCalls = new AtomicInteger();

        private final AtomicReference<FundsInstructionSpec> beforePostingInstruction = new AtomicReference<>();

        private final AtomicReference<String> succeededLedgerTransactionSn = new AtomicReference<>();

        private final AtomicReference<Throwable> failedCause = new AtomicReference<>();

        RecordingLifecycleRecorder(String transactionSn, FundsTransactionEventType... supportedEventTypes) {
            this.transactionSn = transactionSn;
            this.supportedEventTypes = List.of(supportedEventTypes);
        }

        AtomicInteger beforePostingCalls() {
            return beforePostingCalls;
        }

        AtomicReference<FundsInstructionSpec> beforePostingInstruction() {
            return beforePostingInstruction;
        }

        AtomicReference<String> succeededLedgerTransactionSn() {
            return succeededLedgerTransactionSn;
        }

        AtomicReference<Throwable> failedCause() {
            return failedCause;
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
