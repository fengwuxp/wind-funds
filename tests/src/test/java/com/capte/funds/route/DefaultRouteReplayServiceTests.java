package com.capte.funds.route;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.model.route.ImmutableReplayRequestSpec;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
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
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRouteReplayServiceTests {

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testReplayReleaseShouldReverseAuthorizationLegs() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L)));

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REVERSAL_REPLAY");
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.RELEASE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getSourceNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AUTHORIZATION);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AVAILABLE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getBalanceEffectType())
                .containsOnly(LedgerBalanceEffectType.RELEASE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getAmount().getAmount())
                .containsOnly(200L);
    }

    @Test
    void testReplayAuthorizationSettlementShouldConsumeControlAndFundingSubjects() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.AUTHORIZATION_SETTLEMENT, FundsRouteTestSupport.amount(300L)));

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.SETTLE);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.SETTLEMENT);
        assertNoLimitNodes(route);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getPhaseCode())
                .containsOnly(LedgerPhaseCode.SETTLEMENT);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.CONSUME);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getReplayRefLegId())
                .containsExactly("AUTHORIZATION_1", "AUTHORIZATION_2", "AUTHORIZATION_3");
        assertThat(route.getParticipants()).hasSize(4);
        assertThat(route.getParticipants())
                .filteredOn(participant -> participant.getSubjectRef().getSubjectId().equals("funding_001"))
                .singleElement()
                .satisfies(participant -> assertThat(participant.getParticipantRole())
                        .isEqualTo(RouteParticipantRole.REAL_FUNDING_SOURCE));
    }

    @Test
    void testReplayAuthorizationRefundShouldRestoreOriginalRoute() {
        RouteSnapshotSpec captureSnapshot = new DefaultRouteSnapshotFactory().createSnapshot(
                FundsRouteTestSupport.authorizationRouteResolver().resolve(sharedCardInstruction(
                        FundsTransactionEventType.SETTLE)));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(captureSnapshot,
                replayRequest(RouteReplayType.AUTHORIZATION_REFUND, FundsRouteTestSupport.amount(100L)));

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REFUND_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
        assertThat(route.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.RESTORE);
        assertNoLimitNodes(route);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AVAILABLE);
    }

    @Test
    void testReplayChargebackShouldUseIndependentChargebackPhase() {
        RouteSnapshotSpec captureSnapshot = new DefaultRouteSnapshotFactory().createSnapshot(
                FundsRouteTestSupport.authorizationRouteResolver().resolve(sharedCardInstruction(
                        FundsTransactionEventType.SETTLE)));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(captureSnapshot,
                replayRequest(RouteReplayType.CHARGEBACK, FundsRouteTestSupport.amount(100L)));

        assertThat(route.getRouteCode()).isEqualTo("CHARGEBACK_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(route.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.RESTORE);
        assertNoLimitNodes(route);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getPhaseCode())
                .containsOnly(LedgerPhaseCode.CHARGEBACK);
    }

    @Test
    void testReplayRefundShouldExcludeFeeLegsByDefault() {
        RouteSnapshotSpec snapshot = transferWithFeeSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.REFUND, FundsTransactionEventType.REFUND,
                        FundsRouteTestSupport.amount(500L)));

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.RESTORE);
            assertThat(leg.getReplayRefLegId()).isEqualTo("TRANSFER");
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getSubjectRef().getSubjectId())
                .containsExactlyInAnyOrder("funding_001", "funding_002");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getAmount().getAmount())
                .containsOnly(500L);
    }

    @Test
    void testReplayFeeRefundShouldOnlyIncludeFeeLegs() {
        RouteSnapshotSpec snapshot = transferWithFeeSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.FEE_REFUND, FundsTransactionEventType.FEE_REFUND,
                        FundsRouteTestSupport.amount(30L)));

        assertThat(route.getRouteCode()).isEqualTo("DIRECT_REFUND_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.FEE_REFUND);
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.RESTORE);
            assertThat(leg.getReplayRefLegId()).isEqualTo("FEE");
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.FEE);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getSubjectRef().getSubjectId())
                .containsExactlyInAnyOrder("funding_001", "platform_fee");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getAmount().getAmount())
                .containsOnly(30L);
        assertThat(route.getParticipants())
                .filteredOn(participant -> participant.getSubjectRef().getSubjectId().equals("platform_fee"))
                .singleElement()
                .satisfies(participant -> assertThat(participant.getParticipantRole())
                        .isEqualTo(RouteParticipantRole.FEE_RECEIVER));
    }

    @Test
    void testReplayShouldRejectPartialAmountWhenLegRequiresFullReplay() {
        RouteSnapshotSpec snapshot = fullOnlySnapshot();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("仅支持全量回放");
    }

    @Test
    void testReplayOnceShouldAllowOnlyFullReplayAndKeepSourceLegReference() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteLegSpec sourceLeg = copyLegWithReplayPolicy(sourceSnapshot.getLegs().getFirst(),
                RouteReplayPolicy.REPLAY_ONCE);
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of(sourceLeg));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, sourceLeg.getAmount()));

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.REPLAY_ONCE);
            assertThat(leg.getReplayRefLegId()).isEqualTo(sourceLeg.getLegId());
            assertThat(leg.getAmount()).isEqualTo(sourceLeg.getAmount());
        });
    }

    @Test
    void testReplayOnceShouldRejectPartialReplayAmount() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteLegSpec sourceLeg = copyLegWithReplayPolicy(sourceSnapshot.getLegs().getFirst(),
                RouteReplayPolicy.REPLAY_ONCE);
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of(sourceLeg));

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, Money.immutable(
                        sourceLeg.getAmount().getAmount() - 1, sourceLeg.getAmount().getCurrency()))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("仅支持全量回放");
    }

    @Test
    void testReplayShouldSkipNonReplayableLegs() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();
        RouteLegSpec nonReplayableLeg = copyLegWithReplayPolicy(snapshot.getLegs().getFirst(),
                RouteReplayPolicy.NON_REPLAYABLE);
        RouteSnapshotSpec adjustedSnapshot = copySnapshot(snapshot, snapshot.getSnapshotSchemaVersion(),
                List.of(nonReplayableLeg, snapshot.getLegs().get(1), snapshot.getLegs().get(2)));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(adjustedSnapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L)));

        assertThat(route.getLegs()).hasSize(2);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getReplayRefLegId())
                .containsExactly("AUTHORIZATION_2", "AUTHORIZATION_3");
    }

    @Test
    void testReplayShouldRejectAmountGreaterThanSourceLeg() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(700L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("回放金额不能大于原 RouteLeg 金额");
    }

    @Test
    void testReplayShouldRejectDifferentCurrency() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, Money.immutable(100L, CurrencyIsoCode.EUR))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("回放金额币种必须与原 RouteLeg 一致");
    }

    @Test
    void testReplayShouldRejectUnknownSnapshotSchemaVersion() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, "v3", sourceSnapshot.getLegs());

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("snapshotSchemaVersion");
    }

    @Test
    void testReplayShouldReuseOriginalExchangeSnapshotInsteadOfRequote() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteLegSpec sourceLeg = sourceSnapshot.getLegs().getFirst();
        Money sourceOriginalAmount = Money.immutable(540L, CurrencyIsoCode.EUR);
        BigDecimal sourceExchangeRate = new BigDecimal("1.111111");
        RouteLegSpec exchangedLeg = copyLegWithExchangeSnapshot(sourceLeg, sourceOriginalAmount, sourceExchangeRate);
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of(exchangedLeg));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                ImmutableReplayRequestSpec.builder()
                        .replayType(RouteReplayType.RELEASE_HOLD)
                        .businessScene("CARD_REPLAY")
                        .businessSn("REPLAY_EXCHANGE_0001")
                        .amount(FundsRouteTestSupport.amount(200L))
                        .originalAmount(Money.immutable(999L, CurrencyIsoCode.EUR))
                        .exchangeRate(new BigDecimal("9.999999"))
                        .replayLegIds(List.of(sourceLeg.getLegId()))
                        .eventTime(LocalDateTime.of(2026, 5, 9, 12, 30))
                        .operator(systemActor())
                        .contextVariables(Map.of())
                        .build());

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getAmount()).isEqualTo(FundsRouteTestSupport.amount(200L));
            assertThat(leg.getOriginalAmount()).isEqualTo(sourceOriginalAmount);
            assertThat(leg.getExchangeRate()).isEqualByComparingTo(sourceExchangeRate);
            assertThat(leg.getReplayRefLegId()).isEqualTo(sourceLeg.getLegId());
        });
    }

    private RouteSnapshotSpec authorizationSnapshot() {
        ResolvedRouteSpec route = FundsRouteTestSupport.authorizationRouteResolver().resolve(sharedCardInstruction(
                FundsTransactionEventType.AUTHORIZE));
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    private static void assertNoLimitNodes(ResolvedRouteSpec route) {
        assertThat(route.getLegs())
                .allSatisfy(leg -> assertThat(LedgerSubjectCode.LIMIT)
                        .isNotIn(leg.getSourceNode().getLedgerSubjectCode(),
                                leg.getTargetNode().getLedgerSubjectCode()));
    }

    private RouteSnapshotSpec fullOnlySnapshot() {
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

    private RouteSnapshotSpec transferWithFeeSnapshot() {
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

    private RouteSnapshotSpec copySnapshot(RouteSnapshotSpec snapshot,
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

    private RouteLegSpec copyLegWithReplayPolicy(RouteLegSpec leg, RouteReplayPolicy replayPolicy) {
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

    private RouteLegSpec copyLegWithExchangeSnapshot(RouteLegSpec leg,
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

    private FundsInstructionSpec sharedCardInstruction(FundsTransactionEventType eventType) {
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

    private FundsInstructionSpec balanceControlInstruction(FundsTransactionEventType eventType,
                                                           String businessScene,
                                                           String businessSn,
                                                           Map<String, Object> context) {
        return instruction(FundsInstructionType.BALANCE_CONTROL, eventType,
                DefaultFundsTransactionType.ADJUSTMENT, businessScene, businessSn, "freeze", context);
    }

    private FundsInstructionSpec instruction(FundsInstructionType instructionType,
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

    private ImmutableReplayRequestSpec replayRequest(RouteReplayType replayType,
                                                     Money amount) {
        return replayRequest(replayType, null, amount);
    }

    private ImmutableReplayRequestSpec replayRequest(RouteReplayType replayType,
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

    private static ImmutableFundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }
}
