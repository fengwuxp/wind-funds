package com.capte.funds.route;

import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.transaction.support.FundsRouteCodes;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.integration.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.integration.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RouteSnapshot 回放解析边界测试。
 */
class DefaultRouteReplayServiceTests {

    private static final String MISSING_REPLAY_REFERENCE_MESSAGE = "RouteSnapshot 回放事件缺少原路径引用";

    private static final String MISSING_ROUTE_SNAPSHOT_MESSAGE = "RouteSnapshot 回放事件未找到原路径快照";

    private final DefaultRouteReplayService routeReplayService = new DefaultRouteReplayService(
            new EmptyFundsTransactionQueryService());

    /**
     * 场景：业务侧构造退款、撤销、解冻等 replay 事件，但没有传入原交易或冻结单引用。
     * 输入：`REFUND` 事件、无 `FundsInstructionReferenceSpec`。
     * 输出：解析器拒绝回放。
     * 预期：异常信息明确指向缺少原路径引用，而不是空指针。
     * 红线：Route Replay 不得在缺少原始 RouteSnapshot 定位依据时退化为重新路由。
     */
    @Test
    void testResolveReplayInstructionWithoutReferenceShouldFailClearly() {
        assertThatThrownBy(() -> routeReplayService.resolve(replayInstruction(null)))
                .hasMessageContaining(MISSING_REPLAY_REFERENCE_MESSAGE);
    }

    /**
     * 场景：业务侧传入 replay 引用对象，但引用流水号为空。
     * 输入：`REFUND` 事件、`ORIGINAL_TRANSACTION` 引用、空 `referenceSn`。
     * 输出：解析器拒绝回放。
     * 预期：异常信息明确指向缺少原路径引用，而不是查询空单号或重新路由。
     * 红线：Route Replay 必须依赖可定位的原始 RouteSnapshot。
     */
    @Test
    void testResolveReplayInstructionWithBlankReferenceSnShouldFailClearly() {
        FundsInstructionReferenceSpec reference = new BlankReferenceSpec();

        assertThatThrownBy(() -> routeReplayService.resolve(replayInstruction(reference)))
                .hasMessageContaining(MISSING_REPLAY_REFERENCE_MESSAGE);
    }

    /**
     * 场景：业务侧传入有效原交易引用，但系统无法查询到原交易的 RouteSnapshot。
     * 输入：`REFUND` 事件、`ORIGINAL_TRANSACTION` 引用、存在格式的 `referenceSn`。
     * 输出：解析器拒绝回放。
     * 预期：异常信息明确指向缺少原路径快照，并带上原交易流水号。
     * 红线：Route Replay 缺原路径快照时不得按当前绑定关系重新路由。
     */
    @Test
    void testResolveReplayInstructionWithoutRouteSnapshotShouldFailClearly() {
        FundsInstructionReferenceSpec reference = ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn("FT202605190001")
                .build();

        assertThatThrownBy(() -> routeReplayService.resolve(replayInstruction(reference)))
                .hasMessageContaining(MISSING_ROUTE_SNAPSHOT_MESSAGE)
                .hasMessageContaining("FT202605190001");
    }

    /**
     * 场景：原交易完成后支付工具发生换绑，业务侧用新的工具引用发起退款回放。
     * 输入：原 RouteSnapshot 持有旧工具快照，退款指令持有新工具引用。
     * 输出：回放成功，ResolvedRoute 使用原 RouteSnapshot 中的工具快照。
     * 预期：回放结果的 `paymentInstrumentRef` 与原快照一致，不被当前指令覆盖。
     * 红线：Route Replay 必须按原路径事实回放，不得因当前绑定或当前工具引用重新选路。
     */
    @Test
    void testResolveReplayInstructionShouldReuseSnapshotPaymentInstrumentRef() {
        PaymentInstrumentRefSpec originalInstrument = paymentInstrumentRef("CARD-OLD", "old-binding");
        PaymentInstrumentRefSpec currentInstrument = paymentInstrumentRef("CARD-NEW", "new-binding");
        FundsInstructionReferenceSpec reference = ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn("FT202605190002")
                .build();
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(routeSnapshot(originalInstrument)));

        ResolvedRouteSpec resolvedRoute = replayService.resolve(replayInstruction(reference, currentInstrument));

        assertThat(resolvedRoute.getPaymentInstrumentRef()).isSameAs(originalInstrument);
        assertThat(resolvedRoute.getPaymentInstrumentRef().getInstrumentId()).isEqualTo("CARD-OLD");
        assertThat(resolvedRoute.getPaymentInstrumentRef().getBindingSnapshot())
                .containsEntry("bindingId", "old-binding");
        assertThat(resolvedRoute.getPaymentInstrumentRef()).isNotSameAs(currentInstrument);
    }

    /**
     * 场景：原交易完成后外部收款账户和默认资金来源关系发生变化，业务侧用当前账户上下文发起退款回放。
     * 输入：原 RouteSnapshot 持有旧外部账户和旧资金来源决策，退款指令持有新的外部账户引用。
     * 输出：回放成功，ResolvedRoute 继续使用原 RouteSnapshot 中的外部账户和 funding allocation。
     * 预期：外部账户、路由决策和资金来源分配均来自原快照，不被当前请求上下文覆盖。
     * 红线：Route Replay 必须按原路径事实回放，不得因当前收款账户或默认资金来源变化重新选路。
     */
    @Test
    void testResolveReplayInstructionShouldReuseSnapshotExternalAccountAndFundingAllocation() {
        PaymentInstrumentRefSpec originalInstrument = paymentInstrumentRef("CARD-OLD", "old-binding");
        PaymentInstrumentRefSpec currentInstrument = paymentInstrumentRef("CARD-NEW", "new-binding");
        ExternalAccountRefSpec originalExternalAccount = externalAccountRef("EXT-OLD", "ach-old");
        ExternalAccountRefSpec currentExternalAccount = externalAccountRef("EXT-NEW", "ach-new");
        RoutingDecisionSpec originalDecision = routingDecision("ALLOC-OLD", fundingAccount("PAYER-001"));
        FundsInstructionReferenceSpec reference = ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn("FT202605190003")
                .build();
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(routeSnapshot(originalInstrument, originalExternalAccount,
                        originalDecision)));

        ResolvedRouteSpec resolvedRoute = replayService.resolve(replayInstruction(reference, currentInstrument,
                currentExternalAccount));

        assertThat(resolvedRoute.getPaymentInstrumentRef()).isSameAs(originalInstrument);
        assertThat(resolvedRoute.getExternalAccountRef()).isSameAs(originalExternalAccount);
        assertThat(resolvedRoute.getExternalAccountRef()).isNotSameAs(currentExternalAccount);
        assertThat(resolvedRoute.getRoutingDecision()).isSameAs(originalDecision);
        assertThat(resolvedRoute.getRoutingDecision().getFundingAllocations()).singleElement()
                .satisfies(allocation -> {
                    assertThat(allocation.getAllocationId()).isEqualTo("ALLOC-OLD");
                    assertThat(allocation.getSubjectRef().getSubjectId()).isEqualTo("PAYER-001");
                    assertThat(allocation.getReason()).isEqualTo("original funding allocation snapshot");
                });
    }

    private FundsInstructionSpec replayInstruction(FundsInstructionReferenceSpec reference) {
        return replayInstruction(reference, null);
    }

    private FundsInstructionSpec replayInstruction(FundsInstructionReferenceSpec reference,
                                                   PaymentInstrumentRefSpec instrumentRef) {
        return replayInstruction(reference, instrumentRef, null);
    }

    private FundsInstructionSpec replayInstruction(FundsInstructionReferenceSpec reference,
                                                   PaymentInstrumentRefSpec instrumentRef,
                                                   ExternalAccountRefSpec externalAccountRef) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                .instrumentRef(instrumentRef)
                .externalAccountRef(externalAccountRef)
                .reference(reference)
                .businessScene("REFUND")
                .businessSn("REPLAY_MISSING_REFERENCE")
                .eventTime(LocalDateTime.of(2026, 5, 19, 0, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(1L)
                        .operatorType("SYSTEM")
                        .appName("wind-funds-tests")
                        .build())
                .contextVariables(Map.of())
                .build();
    }

    private RouteSnapshotSpec routeSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef) {
        return routeSnapshot(paymentInstrumentRef, null, routingDecision("ALLOC-DEFAULT", fundingAccount("PAYER-001")));
    }

    private RouteSnapshotSpec routeSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef,
                                            ExternalAccountRefSpec externalAccountRef,
                                            RoutingDecisionSpec routingDecision) {
        SubjectRef payer = fundingAccount("PAYER-001");
        SubjectRef payee = fundingAccount("PAYEE-001");
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("SNAPSHOT-202605190002")
                .snapshotSchemaVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .routeCode(FundsRouteCodes.DIRECT_PAY_STANDARD)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene("PAY")
                .businessSn("PAY-202605190002")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(participant(RouteParticipantRole.PAYER, payer),
                        participant(RouteParticipantRole.PAYEE, payee)))
                .legs(List.of(routeLeg(payer, payee)))
                .routingDecision(routingDecision)
                .paymentInstrumentRef(paymentInstrumentRef)
                .externalAccountRef(externalAccountRef)
                .resolvedAt(LocalDateTime.of(2026, 5, 18, 12, 0))
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteLegSpec routeLeg(SubjectRef payer, SubjectRef payee) {
        return ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(1)
                .legType(RouteLegType.CONSUME)
                .sourceNode(routeNode(payer, RouteNodeRole.SOURCE))
                .targetNode(routeNode(payee, RouteNodeRole.TARGET))
                .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteNodeSpec routeNode(SubjectRef subjectRef, RouteNodeRole nodeRole) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(nodeRole)
                .build();
    }

    private ImmutableRouteParticipantSpec participant(RouteParticipantRole role, SubjectRef subjectRef) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(role)
                .subjectRef(subjectRef)
                .currency(CurrencyIsoCode.USD.name())
                .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                .contextVariables(Map.of())
                .build();
    }

    private SubjectRef fundingAccount(String accountId) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(accountId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .currency(CurrencyIsoCode.USD.name())
                .ledgerProfileCode("DEFAULT")
                .build();
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(String instrumentId, String bindingId) {
        return ImmutablePaymentInstrumentRefSpec.builder()
                .instrumentId(instrumentId)
                .instrumentType("CARD")
                .instrumentNo("**** 4242")
                .ownerId("PAYER-001")
                .ownerType("USER")
                .tenantId(1L)
                .currency(CurrencyIsoCode.USD.name())
                .status("ACTIVE")
                .bindingSnapshot(Map.of("bindingId", bindingId))
                .build();
    }

    private ExternalAccountRefSpec externalAccountRef(String externalAccountId, String channelCode) {
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(externalAccountId)
                .externalAccountType("ACH_ACCOUNT")
                .externalAccountNo("token:" + externalAccountId)
                .providerCode("ACH_PROVIDER")
                .channelCode(channelCode)
                .currency(CurrencyIsoCode.USD.name())
                .countryCode("US")
                .contextVariables(Map.of("snapshot", externalAccountId))
                .build();
    }

    private RoutingDecisionSpec routingDecision(String allocationId, SubjectRef fundingAccount) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode("PAYMENT_INSTRUMENT_SNAPSHOT_POLICY")
                .matchedRules(List.of("original-default-funding-source"))
                .selectedProcessor("ORIGINAL_PROCESSOR")
                .selectedCashFundingAccount(fundingAccount.getSubjectId())
                .fundingAllocations(List.of(ImmutableFundingAllocationDecisionSpec.builder()
                        .allocationId(allocationId)
                        .subjectRef(fundingAccount)
                        .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                        .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                        .priority(1)
                        .reason("original funding allocation snapshot")
                        .build()))
                .decisionReason("original route snapshot")
                .contextVariables(Map.of("snapshot", allocationId))
                .build();
    }

    private static class EmptyFundsTransactionQueryService implements FundsTransactionQueryService {

        @Override
        public Optional<FundsTransactionDTO> queryFundsTransaction(String transactionSn) {
            return Optional.empty();
        }

        @Override
        public List<FundsTransactionDetailDTO> queryFundsTransactionDetails(String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(String referenceTransactionSn,
                                            FundsTransactionEventType eventType,
                                            String replayRefLegId) {
            return false;
        }

        @Override
        public Money sumConsumedReplayLegAmount(String referenceTransactionSn,
                                                FundsTransactionEventType eventType,
                                                String replayRefLegId,
                                                CurrencyIsoCode currency) {
            return Money.immutable(0L, currency);
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(String transactionSn) {
            return Optional.empty();
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(String freezeOrderSn) {
            return Optional.empty();
        }
    }

    private static final class SnapshotFundsTransactionQueryService extends EmptyFundsTransactionQueryService {

        private final RouteSnapshotSpec routeSnapshot;

        private SnapshotFundsTransactionQueryService(RouteSnapshotSpec routeSnapshot) {
            this.routeSnapshot = routeSnapshot;
        }

        @Override
        public Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(String transactionSn) {
            return Optional.of(routeSnapshot);
        }
    }

    private static final class BlankReferenceSpec implements FundsInstructionReferenceSpec {

        @Override
        public @NonNull FundsInstructionReferenceType getReferenceType() {
            return FundsInstructionReferenceType.ORIGINAL_TRANSACTION;
        }

        @Override
        public @Nullable String getReferenceSn() {
            return " ";
        }

        @Override
        public @Nullable String getReferenceBusinessSn() {
            return null;
        }

        @Override
        public @Nullable String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public @Nullable String getExternalTransactionId() {
            return null;
        }

        @Override
        public @Nullable String getAuthCode() {
            return null;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }
}
