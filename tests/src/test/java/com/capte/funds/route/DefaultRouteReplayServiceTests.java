package com.capte.funds.route;

import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.common.exception.BaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.authorizationSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.authorizationSettlementSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.fullOnlySnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.replayRequest;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.systemActor;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.transferWithFeeSnapshot;
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
    void testResolveShouldReplaySavedSnapshotForSupportedLifecycleEvents() {
        List<ResolveCase> cases = List.of(
                resolveCase(FundsTransactionEventType.REVERSAL, FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        "AUTH_0001", authorizationSnapshot(), FundsRouteTestSupport.amount(100L),
                        FundsRouteCodes.AUTHORIZATION_REVERSAL_REPLAY),
                resolveCase(FundsTransactionEventType.SETTLE, FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        "AUTH_0002", authorizationSnapshot(), FundsRouteTestSupport.amount(100L),
                        FundsRouteCodes.AUTHORIZATION_SETTLE_REPLAY),
                resolveCase(FundsTransactionEventType.AUTH_REFUND, FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        "SETTLE_0001", authorizationSettlementSnapshot(), FundsRouteTestSupport.amount(100L),
                        FundsRouteCodes.AUTHORIZATION_REFUND_REPLAY),
                resolveCase(FundsTransactionEventType.CHARGEBACK, FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        "SETTLE_0002", authorizationSettlementSnapshot(), FundsRouteTestSupport.amount(100L),
                        FundsRouteCodes.CHARGEBACK_REPLAY),
                resolveCase(FundsTransactionEventType.REFUND, FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        "TRANSFER_0001", transferWithFeeSnapshot(), FundsRouteTestSupport.amount(500L),
                        FundsRouteCodes.DIRECT_REFUND_REPLAY),
                resolveCase(FundsTransactionEventType.FEE_REFUND, FundsInstructionReferenceType.ORIGINAL_TRANSACTION,
                        "TRANSFER_0002", transferWithFeeSnapshot(), FundsRouteTestSupport.amount(30L),
                        FundsRouteCodes.DIRECT_REFUND_REPLAY),
                resolveCase(FundsTransactionEventType.UNFREEZE, FundsInstructionReferenceType.FREEZE_ORDER,
                        "FREEZE_0001", fullOnlySnapshot(), FundsRouteTestSupport.amount(600L),
                        FundsRouteCodes.BALANCE_UNFREEZE_REPLAY)
        );

        for (ResolveCase testCase : cases) {
            RecordingTransactionQueryService queryService = new RecordingTransactionQueryService()
                    .saveSnapshot(testCase.referenceType(), testCase.referenceSn(), testCase.snapshot());

            ResolvedRouteSpec route = new DefaultRouteReplayService(queryService).resolve(instruction(testCase));

            assertThat(route.getRouteCode())
                    .as("eventType = %s", testCase.eventType())
                    .isEqualTo(testCase.expectedRouteCode());
            assertThat(route.getEventType()).isEqualTo(testCase.eventType());
            assertThat(route.getLegs()).isNotEmpty();
            assertThat(route.getLegs())
                    .allSatisfy(leg -> assertThat(leg.getReplayRefLegId()).isNotBlank());
            assertThat(queryService.sumConsumedReplayLegCalls).isNotEmpty();
        }
    }

    @Test
    void testResolveShouldRejectMissingOriginalRouteSnapshot() {
        RecordingTransactionQueryService queryService = new RecordingTransactionQueryService();
        FundsInstructionSpec instruction = instruction(resolveCase(FundsTransactionEventType.REFUND,
                FundsInstructionReferenceType.ORIGINAL_TRANSACTION, "MISSING_TXN", transferWithFeeSnapshot(),
                FundsRouteTestSupport.amount(100L), FundsRouteCodes.DIRECT_REFUND_REPLAY));

        assertThatThrownBy(() -> new DefaultRouteReplayService(queryService).resolve(instruction))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到原路径快照")
                .hasMessageContaining("MISSING_TXN");
        assertThat(queryService.transactionSnapshotQueries).containsExactly("MISSING_TXN");
    }

    private static ResolveCase resolveCase(FundsTransactionEventType eventType,
                                           FundsInstructionReferenceType referenceType,
                                           String referenceSn,
                                           RouteSnapshotSpec snapshot,
                                           Money amount,
                                           String expectedRouteCode) {
        return new ResolveCase(eventType, referenceType, referenceSn, snapshot, amount, expectedRouteCode);
    }

    private static FundsInstructionSpec instruction(ResolveCase testCase) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(FundsRouteTestSupport.TENANT_ID)
                .instructionType(instructionType(testCase.eventType()))
                .eventType(testCase.eventType())
                .transactionType(transactionType(testCase.eventType()))
                .amount(testCase.amount())
                .originalAmount(testCase.amount())
                .exchangeRate(BigDecimal.ONE)
                .businessScene("ROUTE_REPLAY_RESOLVE")
                .businessSn(testCase.eventType().name() + "_RESOLVE_0001")
                .reference(ImmutableFundsInstructionReferenceSpec.builder()
                        .referenceType(testCase.referenceType())
                        .referenceSn(testCase.referenceSn())
                        .build())
                .eventTime(LocalDateTime.of(2026, 5, 18, 9, 30))
                .operator(systemActor())
                .contextVariables(Map.of())
                .build();
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

    private record ResolveCase(FundsTransactionEventType eventType,
                               FundsInstructionReferenceType referenceType,
                               String referenceSn,
                               RouteSnapshotSpec snapshot,
                               Money amount,
                               String expectedRouteCode) {
    }

    private static final class RecordingTransactionQueryService implements FundsTransactionQueryService {

        private final Map<String, RouteSnapshotSpec> transactionSnapshots = new LinkedHashMap<>();

        private final Map<String, RouteSnapshotSpec> freezeOrderSnapshots = new LinkedHashMap<>();

        private final List<String> transactionSnapshotQueries = new ArrayList<>();

        private final List<String> sumConsumedReplayLegCalls = new ArrayList<>();

        private RecordingTransactionQueryService saveSnapshot(FundsInstructionReferenceType referenceType,
                                                              String referenceSn,
                                                              RouteSnapshotSpec snapshot) {
            if (referenceType == FundsInstructionReferenceType.FREEZE_ORDER) {
                freezeOrderSnapshots.put(referenceSn, snapshot);
            } else {
                transactionSnapshots.put(referenceSn, snapshot);
            }
            return this;
        }

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
            return false;
        }

        @Override
        public @NonNull Money sumConsumedReplayLegAmount(@NonNull String referenceTransactionSn,
                                                         @NonNull FundsTransactionEventType eventType,
                                                         @NonNull String replayRefLegId,
                                                         @NonNull CurrencyIsoCode currency) {
            sumConsumedReplayLegCalls.add(referenceTransactionSn + ":" + eventType + ":" + replayRefLegId);
            return Money.immutable(0L, currency);
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            transactionSnapshotQueries.add(transactionSn);
            return Optional.ofNullable(transactionSnapshots.get(transactionSn));
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.ofNullable(freezeOrderSnapshots.get(freezeOrderSn));
        }
    }
}
