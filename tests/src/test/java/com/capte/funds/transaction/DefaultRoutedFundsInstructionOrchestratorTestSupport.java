package com.capte.funds.transaction;

import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

abstract class DefaultRoutedFundsInstructionOrchestratorTestSupport {

    protected static ResolvedRouteSpec route(boolean withLedgerLeg) {
        return route(withLedgerLeg, RouteReplayPolicy.FULL_ONLY);
    }

    protected static ResolvedRouteSpec route(boolean withLedgerLeg, RouteReplayPolicy replayPolicy) {
        List<RouteLegSpec> legs = withLedgerLeg ? List.of(new SimpleRouteLeg(replayPolicy)) : List.of();
        return new SimpleResolvedRoute(legs);
    }

    protected static ResolvedRouteSpec feeRoute() {
        return new SimpleResolvedRoute(List.of(new SimpleRouteLeg(RouteReplayPolicy.FULL_ONLY,
                LedgerPhaseCode.FEE)));
    }

    protected static RouteResolver replayRouteResolver(RecordingTransactionQueryService transactionQueryService,
                                                       RecordingRouteResolver fallbackRouteResolver) {
        return new CompositeRouteResolver(List.of(
                new DefaultRouteReplayService(transactionQueryService),
                new NonReplayRouteResolver(fallbackRouteResolver)
        ));
    }

    static final class NonReplayRouteResolver implements RouteResolver {

        private final RecordingRouteResolver delegate;

        NonReplayRouteResolver(RecordingRouteResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return instruction.getReference() == null;
        }

        @Override
        public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
            return delegate.resolve(instruction);
        }
    }

    static final class RecordingRouteResolver implements RouteResolver {

        final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        final ResolvedRouteSpec route;

        RecordingRouteResolver(ResolvedRouteSpec route) {
            this.route = route;
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
            this.instruction.set(instruction);
            return route;
        }
    }

    static final class RecordingLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec> {

        final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        final AtomicReference<String> fundsTransactionSn = new AtomicReference<>();

        final AtomicReference<ResolvedRouteSpec> route = new AtomicReference<>();

        private final boolean unsupported;

        RecordingLedgerPostingAssembler(boolean unsupported) {
            this.unsupported = unsupported;
        }

        @Override
        public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                       @NonNull String fundsTransactionSn,
                                                       @NonNull ResolvedRouteSpec resolvedRoute) {
            if (unsupported) {
                throw new IllegalArgumentException("Not found supported LedgerPostingAssembler");
            }
            this.instruction.set(instruction);
            this.fundsTransactionSn.set(fundsTransactionSn);
            this.route.set(resolvedRoute);
            return LedgerTransactionSpecFactory.createLedgerTransaction(instruction, fundsTransactionSn,
                    ledgerTransactionSn -> {
                        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(
                                LedgerPhaseCode.TRANSFER,
                                List.of(entry(ledgerTransactionSn, EntrySide.DEBIT),
                                        entry(ledgerTransactionSn, EntrySide.CREDIT))
                        );
                        return List.of(LedgerTransactionSpecFactory.postingPlan(
                                LedgerPostingIntentType.TRANSFER,
                                ledgerTransactionSn,
                                null,
                                LedgerBalanceEffectType.CONSUME,
                                List.of(phase)
                        ));
                    });
        }

        @Override
        public boolean supports(@NonNull ResolvedRouteSpec resolvedRoute) {
            return !unsupported;
        }
    }

    private static LedgerEntrySpec entry(String ledgerTransactionSn, EntrySide entrySide) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                entrySide == EntrySide.DEBIT ? "funding_001" : "funding_002",
                "FUNDING_ACCOUNT",
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                ledgerTransactionSn,
                "TRANSFER",
                "BIZ_0001",
                100L,
                CurrencyIsoCode.USD,
                LocalDateTime.of(2026, 5, 9, 12, 0)
        ).setBalanceEffectType(LedgerBalanceEffectType.CONSUME)
                .setIntent(LedgerPostingIntentType.TRANSFER)
                .setPhaseCode(LedgerPhaseCode.TRANSFER)
                .setContextVariables(Map.of());
    }

    static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver {

        final AtomicReference<FundsInstructionSpec> beforePostingInstruction = new AtomicReference<>();

        final AtomicReference<ResolvedRouteSpec> beforePostingRoute = new AtomicReference<>();

        final AtomicReference<RouteSnapshotSpec> beforePostingSnapshot = new AtomicReference<>();

        final AtomicReference<String> succeededLedgerTransactionSn = new AtomicReference<>();

        final AtomicReference<Throwable> failedCause = new AtomicReference<>();

        private final boolean completed;

        private final String lifecycleSn;

        RecordingLifecycleSaver(boolean completed) {
            this(completed, "FT_001");
        }

        RecordingLifecycleSaver(boolean completed, String lifecycleSn) {
            this.completed = completed;
            this.lifecycleSn = lifecycleSn;
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                             @NonNull ResolvedRouteSpec resolvedRoute,
                                                             @NonNull RouteSnapshotSpec routeSnapshot) {
            beforePostingInstruction.set(instruction);
            beforePostingRoute.set(resolvedRoute);
            beforePostingSnapshot.set(routeSnapshot);
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn(lifecycleSn)
                    .setTransactionDetailSns(List.of("FTD_001", "FTD_002"))
                    .setCompleted(completed);
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

    static final class RecordingTransactionQueryService implements FundsTransactionQueryService {

        final AtomicReference<RouteSnapshotSpec> routeSnapshot = new AtomicReference<>();

        final AtomicReference<RouteSnapshotSpec> freezeOrderRouteSnapshot = new AtomicReference<>();

        final AtomicReference<String> consumedReplayLegId = new AtomicReference<>();

        final AtomicReference<Money> consumedReplayLegAmount = new AtomicReference<>();

        @Override
        public @NonNull Optional<FundsTransactionDTO> queryFundsTransaction(@NonNull String transactionSn) {
            return Optional.empty();
        }

        @Override
        public @NonNull List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(@NonNull String referenceTransactionSn,
                                            @NonNull FundsTransactionEventType eventType,
                                            @NonNull String replayRefLegId) {
            return replayRefLegId.equals(consumedReplayLegId.get());
        }

        @Override
        public @NonNull Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                                         @NonNull FundsTransactionEventType eventType,
                                                         @NonNull String replayRefLegId,
                                                         @NonNull CurrencyIsoCode currency) {
            Money consumed = consumedReplayLegAmount.get();
            return consumed == null ? Money.immutable(0L, currency) : consumed;
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            return Optional.ofNullable(routeSnapshot.get());
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.ofNullable(freezeOrderRouteSnapshot.get());
        }
    }

    static final class RecordingPostingService implements LedgerTransactionPostingService {

        final AtomicReference<LedgerTransactionSpec> transaction = new AtomicReference<>();

        private final boolean fail;

        RecordingPostingService(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void post(LedgerTransactionSpec transaction) {
            this.transaction.set(transaction);
            if (fail) {
                throw new IllegalStateException("posting failed");
            }
        }
    }

    static class SimpleInstruction implements FundsInstructionSpec {

        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TOPUP;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CurrencyIsoCode.USD);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return getAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public @Nullable PaymentInstrumentRefSpec getInstrumentRef() {
            return null;
        }

        @Override
        public @Nullable ExternalAccountRefSpec getExternalAccountRef() {
            return null;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return null;
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "BIZ_0001";
        }

        @Override
        public @NonNull LocalDateTime getEventTime() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @Nullable String getDescription() {
            return "test";
        }

        @Override
        public @NonNull FundsOperationActorSpec getOperator() {
            return systemActor();
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private static FundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }

    static final class ReferencedInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        ReferencedInstruction(FundsTransactionEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.REFUND;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return ImmutableFundsInstructionReferenceSpec.builder()
                    .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                    .referenceSn("FT_ORIGINAL")
                    .referenceBusinessSn("BIZ_ORIGINAL")
                    .contextVariables(Map.of())
                    .build();
        }
    }

    static final class FreezeOrderReferencedInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        FreezeOrderReferencedInstruction(FundsTransactionEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.ADJUSTMENT;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return ImmutableFundsInstructionReferenceSpec.builder()
                    .referenceType(FundsInstructionReferenceType.FREEZE_ORDER)
                    .referenceSn("FO_001")
                    .contextVariables(Map.of())
                    .build();
        }
    }

    static final class BalanceControlInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        BalanceControlInstruction(FundsTransactionEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.BALANCE_CONTROL;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.ADJUSTMENT;
        }
    }

    static final class DirectRefundInstruction extends SimpleInstruction {

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.REFUND;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.REFUND;
        }
    }

    static final class SimpleResolvedRoute implements ResolvedRouteSpec {

        private final List<RouteLegSpec> legs;

        SimpleResolvedRoute(List<RouteLegSpec> legs) {
            this.legs = legs;
        }

        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "WALLET_DIRECT";
        }

        @Override
        public @NonNull String getRouteVersion() {
            return "v2";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "BIZ_0001";
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TOPUP;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of();
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return legs;
        }

        @Override
        public @NonNull LocalDateTime getResolvedAt() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static final class SimpleRouteLeg implements RouteLegSpec {

        private final RouteReplayPolicy replayPolicy;

        private final RouteNodeSpec sourceNode = new SimpleRouteNode("funding_001", RouteNodeRole.SOURCE);

        private final RouteNodeSpec targetNode = new SimpleRouteNode("funding_002", RouteNodeRole.TARGET);

        private final LedgerPhaseCode phaseCode;

        SimpleRouteLeg(RouteReplayPolicy replayPolicy) {
            this(replayPolicy, LedgerPhaseCode.TRANSFER);
        }

        SimpleRouteLeg(RouteReplayPolicy replayPolicy, LedgerPhaseCode phaseCode) {
            this.replayPolicy = replayPolicy;
            this.phaseCode = phaseCode;
        }

        @Override
        public @NonNull String getLegId() {
            return "LEG_001";
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.INTERNAL_TRANSFER;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return sourceNode;
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return targetNode;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CurrencyIsoCode.USD);
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return LedgerBalanceEffectType.CONSUME;
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return phaseCode;
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return AccountBalancePeriodType.LIFETIME;
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return Map.of();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return replayPolicy;
        }
    }

    static final class SimpleRouteNode implements RouteNodeSpec {

        private final SubjectRef subjectRef;

        private final RouteNodeRole nodeRole;

        SimpleRouteNode(String subjectId, RouteNodeRole nodeRole) {
            this.subjectRef = new SimpleSubjectRef(subjectId);
            this.nodeRole = nodeRole;
        }

        @Override
        public @NonNull RouteNodeType getNodeType() {
            return RouteNodeType.SUBJECT;
        }

        @Override
        public @NonNull SubjectRef getSubjectRef() {
            return subjectRef;
        }

        @Override
        public @NonNull LedgerSubjectCode getLedgerSubjectCode() {
            return LedgerSubjectCode.AVAILABLE;
        }

        @Override
        public @NonNull RouteNodeRole getNodeRole() {
            return nodeRole;
        }
    }

    static final class SimpleSubjectRef implements SubjectRef {

        private final String subjectId;

        SimpleSubjectRef(String subjectId) {
            this.subjectId = subjectId;
        }

        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public @NonNull String getSubjectId() {
            return subjectId;
        }

        @Override
        public @NonNull FundsSubjectType getSubjectType() {
            return FundsSubjectType.FUNDING_ACCOUNT;
        }
    }
}
