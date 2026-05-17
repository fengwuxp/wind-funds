package com.capte.funds.route;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableReplayRequestSpec;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class DefaultRouteReplayServiceTestSupport {

    private DefaultRouteReplayServiceTestSupport() {
    }

    static RouteSnapshotSpec authorizationSnapshot() {
        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(sharedCardInstruction(
                FundsTransactionEventType.AUTHORIZE));
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    static RouteSnapshotSpec authorizationSettlementSnapshot() {
        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(sharedCardInstruction(
                FundsTransactionEventType.SETTLE));
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    static RouteSnapshotSpec fullOnlySnapshot() {
        ResolvedRouteSpec route = FundsRouteTestSupport.balanceControlRouteResolver()
                .resolve(balanceControlInstruction(FundsTransactionEventType.FREEZE,
                        "BALANCE_FREEZE", "FREEZE_0001",
                        Map.of(FundsInstructionContextKeys.ACCOUNT_ID,
                                FundsRouteTestSupport.fundingAccount("funding_001"))));
        RouteLegSpec leg = route.getLegs().getFirst();
        RouteLegSpec fullOnlyLeg = ImmutableRouteLegSpec.builder()
                .legId(leg.getLegId())
                .sequence(leg.getSequence())
                .legType(leg.getLegType())
                .sourceNode(leg.getSourceNode())
                .targetNode(leg.getTargetNode())
                .amount(leg.getAmount())
                .originalAmount(leg.getOriginalAmount())
                .exchangeRate(leg.getExchangeRate())
                .balanceEffectType(leg.getBalanceEffectType())
                .phaseCode(leg.getPhaseCode())
                .periodType(leg.getPeriodType())
                .periodId(leg.getPeriodId())
                .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                .replayRefLegId(leg.getReplayRefLegId())
                .constraintOverrides(leg.getConstraintOverrides())
                .description(leg.getDescription())
                .contextVariables(leg.getContextVariables())
                .build();
        ResolvedRouteSpec fullOnlyRoute = ImmutableResolvedRouteSpec.builder()
                .tenantId(route.getTenantId())
                .routeCode(route.getRouteCode())
                .routeVersion(route.getRouteVersion())
                .businessScene(route.getBusinessScene())
                .businessSn(route.getBusinessSn())
                .instructionType(route.getInstructionType())
                .eventType(route.getEventType())
                .transactionType(route.getTransactionType())
                .participants(route.getParticipants())
                .legs(List.of(fullOnlyLeg))
                .routingDecision(route.getRoutingDecision())
                .paymentInstrumentRef(route.getPaymentInstrumentRef())
                .externalAccountRef(route.getExternalAccountRef())
                .platformAccounts(route.getPlatformAccounts())
                .resolvedAt(route.getResolvedAt())
                .expiresAt(route.getExpiresAt())
                .description(route.getDescription())
                .contextVariables(route.getContextVariables())
                .build();
        return new DefaultRouteSnapshotFactory().createSnapshot(fullOnlyRoute);
    }

    static RouteSnapshotSpec transferWithFeeSnapshot() {
        FundsInstructionSpec instruction = FundsRouteTestSupport.transactionInstructionConverter()
                .convertToTransferInstruction(new FundsTransactionTransferRequest()
                        .setPayerAccountId(FundsRouteTestSupport.fundingAccount("funding_001"))
                        .setPayeeAccountId(FundsRouteTestSupport.fundingAccount("funding_002"))
                        .setTransactionAmount(FundsRouteTestSupport.transactionAmount(500L))
                        .setFeeSpec(FundsRouteTestSupport.fixedFeeSpec(30L))
                        .setBusinessScene("TRANSFER")
                        .setBusinessSn("TRANSFER_0001"), WindOperator.system());
        ResolvedRouteSpec route = FundsRouteTestSupport.transferRouteResolver().resolve(instruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    static RouteSnapshotSpec copySnapshot(RouteSnapshotSpec snapshot,
                                          String snapshotSchemaVersion,
                                          List<RouteLegSpec> legs) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(snapshot.getTenantId())
                .snapshotId(snapshot.getSnapshotId())
                .snapshotSchemaVersion(snapshotSchemaVersion)
                .routeCode(snapshot.getRouteCode())
                .routeVersion(snapshot.getRouteVersion())
                .businessScene(snapshot.getBusinessScene())
                .businessSn(snapshot.getBusinessSn())
                .instructionType(snapshot.getInstructionType())
                .eventType(snapshot.getEventType())
                .transactionType(snapshot.getTransactionType())
                .participants(snapshot.getParticipants())
                .legs(legs)
                .routingDecision(snapshot.getRoutingDecision())
                .paymentInstrumentRef(snapshot.getPaymentInstrumentRef())
                .externalAccountRef(snapshot.getExternalAccountRef())
                .platformAccounts(snapshot.getPlatformAccounts())
                .resolvedAt(snapshot.getResolvedAt())
                .expiresAt(snapshot.getExpiresAt())
                .description(snapshot.getDescription())
                .contextVariables(snapshot.getContextVariables())
                .build();
    }

    static RouteLegSpec copyLegWithReplayPolicy(RouteLegSpec leg, RouteReplayPolicy replayPolicy) {
        return ImmutableRouteLegSpec.builder()
                .legId(leg.getLegId())
                .sequence(leg.getSequence())
                .legType(leg.getLegType())
                .sourceNode(leg.getSourceNode())
                .targetNode(leg.getTargetNode())
                .amount(leg.getAmount())
                .originalAmount(leg.getOriginalAmount())
                .exchangeRate(leg.getExchangeRate())
                .balanceEffectType(leg.getBalanceEffectType())
                .phaseCode(leg.getPhaseCode())
                .periodType(leg.getPeriodType())
                .periodId(leg.getPeriodId())
                .replayPolicy(replayPolicy)
                .replayRefLegId(leg.getReplayRefLegId())
                .constraintOverrides(leg.getConstraintOverrides())
                .description(leg.getDescription())
                .contextVariables(leg.getContextVariables())
                .build();
    }

    static RouteLegSpec copyLegWithExchangeSnapshot(RouteLegSpec leg,
                                                    Money originalAmount,
                                                    BigDecimal exchangeRate) {
        return ImmutableRouteLegSpec.builder()
                .legId(leg.getLegId())
                .sequence(leg.getSequence())
                .legType(leg.getLegType())
                .sourceNode(leg.getSourceNode())
                .targetNode(leg.getTargetNode())
                .amount(leg.getAmount())
                .originalAmount(originalAmount)
                .exchangeRate(exchangeRate)
                .balanceEffectType(leg.getBalanceEffectType())
                .phaseCode(leg.getPhaseCode())
                .periodType(leg.getPeriodType())
                .periodId(leg.getPeriodId())
                .replayPolicy(leg.getReplayPolicy())
                .replayRefLegId(leg.getReplayRefLegId())
                .constraintOverrides(leg.getConstraintOverrides())
                .description(leg.getDescription())
                .contextVariables(leg.getContextVariables())
                .build();
    }

    static ImmutableReplayRequestSpec replayRequest(RouteReplayType replayType,
                                                    Money amount) {
        return replayRequest(replayType, null, amount);
    }

    static ImmutableReplayRequestSpec replayRequest(RouteReplayType replayType,
                                                    FundsTransactionEventType eventType,
                                                    Money amount) {
        return ImmutableReplayRequestSpec.builder()
                .replayType(replayType)
                .eventType(eventType)
                .businessScene("CARD_REPLAY")
                .businessSn(replayType.name() + "_0001")
                .amount(amount)
                .originalAmount(amount)
                .eventTime(LocalDateTime.of(2026, 5, 9, 12, 30))
                .operator(systemActor())
                .contextVariables(Map.of())
                .build();
    }

    static ImmutableFundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }

    static void assertNoLimitNodes(ResolvedRouteSpec route) {
        assertThat(route.getLegs())
                .allSatisfy(leg -> assertThat(LedgerSubjectCode.LIMIT)
                        .isNotIn(leg.getSourceNode().getLedgerSubjectCode(),
                                leg.getTargetNode().getLedgerSubjectCode()));
    }

    private static FundsInstructionSpec sharedCardInstruction(FundsTransactionEventType eventType) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(FundsInstructionContextKeys.ACCOUNT_ID, FundsRouteTestSupport.creditAccount("credit_001"));
        context.put(FundsInstructionContextKeys.APPROVED, Boolean.TRUE);
        context.put(FundsInstructionContextKeys.LINKED_BUDGET_GROUP_ID,
                FundsRouteTestSupport.budgetGroup("budget_001"));
        context.put(FundsInstructionContextKeys.LINKED_FUNDING_ACCOUNT_ID,
                FundsRouteTestSupport.fundingAccount("funding_001"));
        return instruction(FundsInstructionType.AUTHORIZATION_TRANSACTION, eventType,
                eventType == FundsTransactionEventType.AUTH_REFUND
                        ? DefaultFundsTransactionType.REFUND
                        : DefaultFundsTransactionType.PAY,
                "CARD_AUTH", eventType.name() + "_0001", "shared card", context);
    }

    private static FundsInstructionSpec balanceControlInstruction(FundsTransactionEventType eventType,
                                                                  String businessScene,
                                                                  String businessSn,
                                                                  Map<String, Object> context) {
        return instruction(FundsInstructionType.BALANCE_CONTROL, eventType,
                DefaultFundsTransactionType.ADJUSTMENT, businessScene, businessSn, "freeze", context);
    }

    private static FundsInstructionSpec instruction(FundsInstructionType instructionType,
                                                    FundsTransactionEventType eventType,
                                                    DefaultFundsTransactionType transactionType,
                                                    String businessScene,
                                                    String businessSn,
                                                    String description,
                                                    Map<String, Object> context) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(instructionType)
                .eventType(eventType)
                .transactionType(transactionType)
                .amount(FundsRouteTestSupport.amount(600L))
                .originalAmount(FundsRouteTestSupport.amount(600L))
                .exchangeRate(BigDecimal.ONE)
                .businessScene(businessScene)
                .businessSn(businessSn)
                .eventTime(LocalDateTime.now())
                .description(description)
                .operator(systemActor())
                .contextVariables(Map.copyOf(context))
                .build();
    }
}
