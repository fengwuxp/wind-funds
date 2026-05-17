package com.capte.funds.route;

import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RouteReplaySupportTests {

    @Test
    void testIsReplayInstructionShouldSupportReplayEventsWithInternalReference() {
        for (FundsTransactionEventType eventType : replayEvents()) {
            FundsInstructionReferenceType referenceType = eventType == FundsTransactionEventType.UNFREEZE
                    ? FundsInstructionReferenceType.FREEZE_ORDER
                    : FundsInstructionReferenceType.ORIGINAL_TRANSACTION;

            assertThat(RouteReplaySupport.isReplayInstruction(instruction(eventType, referenceType, "REF_0001")))
                    .as("eventType = %s", eventType)
                    .isTrue();
        }
    }

    @Test
    void testIsReplayInstructionShouldRejectReplayEventWithoutReference() {
        assertThat(RouteReplaySupport.isReplayInstruction(instruction(FundsTransactionEventType.REFUND,
                null, null))).isFalse();
    }

    @Test
    void testIsReplayInstructionShouldRejectBlankReferenceSn() {
        assertThat(RouteReplaySupport.isReplayInstruction(instruction(FundsTransactionEventType.REFUND,
                FundsInstructionReferenceType.ORIGINAL_TRANSACTION, " "))).isFalse();
    }

    @Test
    void testIsReplayInstructionShouldRejectExternalTransactionReference() {
        assertThat(RouteReplaySupport.isReplayInstruction(instruction(FundsTransactionEventType.REFUND,
                FundsInstructionReferenceType.EXTERNAL_TRANSACTION, "EXT_0001"))).isFalse();
    }

    @Test
    void testIsReplayInstructionShouldRejectNonReplayEventEvenWithReference() {
        assertThat(RouteReplaySupport.isReplayInstruction(instruction(FundsTransactionEventType.PAY,
                FundsInstructionReferenceType.ORIGINAL_TRANSACTION, "PAY_0001"))).isFalse();
    }

    private static FundsTransactionEventType[] replayEvents() {
        return new FundsTransactionEventType[]{
                FundsTransactionEventType.REVERSAL,
                FundsTransactionEventType.SETTLE,
                FundsTransactionEventType.AUTH_REFUND,
                FundsTransactionEventType.CHARGEBACK,
                FundsTransactionEventType.REFUND,
                FundsTransactionEventType.FEE_REFUND,
                FundsTransactionEventType.UNFREEZE
        };
    }

    private static FundsInstructionSpec instruction(FundsTransactionEventType eventType,
                                                    FundsInstructionReferenceType referenceType,
                                                    String referenceSn) {
        var builder = ImmutableFundsInstructionSpec.builder()
                .tenantId(FundsRouteTestSupport.TENANT_ID)
                .instructionType(instructionType(eventType))
                .eventType(eventType)
                .transactionType(transactionType(eventType))
                .amount(FundsRouteTestSupport.amount(100L))
                .originalAmount(FundsRouteTestSupport.amount(100L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene("ROUTE_REPLAY_SUPPORT")
                .businessSn(eventType.name() + "_0001")
                .eventTime(LocalDateTime.of(2026, 5, 17, 22, 30))
                .operator(systemActor())
                .contextVariables(Map.of());
        if (referenceType != null) {
            builder.reference(ImmutableFundsInstructionReferenceSpec.builder()
                    .referenceType(referenceType)
                    .referenceSn(referenceSn)
                    .build());
        }
        return builder.build();
    }

    private static FundsInstructionType instructionType(FundsTransactionEventType eventType) {
        return switch (eventType) {
            case REVERSAL, SETTLE, AUTH_REFUND, CHARGEBACK -> FundsInstructionType.AUTHORIZATION_TRANSACTION;
            case UNFREEZE -> FundsInstructionType.BALANCE_CONTROL;
            default -> FundsInstructionType.DIRECT_TRANSACTION;
        };
    }

    private static DefaultFundsTransactionType transactionType(FundsTransactionEventType eventType) {
        return switch (eventType) {
            case FEE_REFUND -> DefaultFundsTransactionType.FEE;
            case REFUND, AUTH_REFUND, CHARGEBACK -> DefaultFundsTransactionType.REFUND;
            case UNFREEZE -> DefaultFundsTransactionType.ADJUSTMENT;
            default -> DefaultFundsTransactionType.PAY;
        };
    }

    private static ImmutableFundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }
}
