package com.capte.funds.route;

import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
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
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitComponentSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitRefundPolicySpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsBenefitSnapshotSpec;
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
import com.wind.integration.funds.spec.transaction.FundsBenefitComponentSpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.integration.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.integration.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.integration.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.integration.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.integration.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.integration.funds.transaction.enums.FundsBenefitType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final String MISSING_ORIGINAL_BENEFIT_SNAPSHOT_MESSAGE =
            "RouteSnapshot 回放缺少原权益快照摘要";

    private static final String ROUTE_REPLAY_SERVICE_SOURCE =
            "transaction/transaction-impl/src/main/java/com/capte/funds/route/DefaultRouteReplayService.java";

    private static final List<String> CURRENT_ROUTE_SELECTION_TOKENS = List.of(
            "TransferFundsInstructionRouteResolver",
            "AuthorizationFundsInstructionRouteResolver",
            "BalanceControlFundsInstructionRouteResolver",
            "CompositeRouteResolver",
            "PlatformAccountRouteSupport",
            "PaymentInstrumentService",
            "SpendSubjectFundingRelationService",
            "FundingAccountService",
            "FundsSubjectBalanceQueryService");

    private final DefaultRouteReplayService routeReplayService = new DefaultRouteReplayService(
            new EmptyFundsTransactionQueryService());

    /**
     * 场景：开发者维护 Route Replay 实现时误接入当前选路解析器、账户关系或支付工具查询。
     * 输入：DefaultRouteReplayService 生产源码。
     * 输出：不依赖当前路由解析器、当前平台账户解析、当前支付工具或当前资金来源关系服务。
     * 预期：回放入口只读取原 RouteSnapshot 和原引用事实，不能具备按当前绑定关系重算路径的能力。
     * 红线：缺原路径快照时必须明确失败，不得通过 fallback resolver 或当前关系服务重新选路。
     */
    @Test
    void testRouteReplayServiceShouldNotDependOnCurrentRouteSelectionPorts() throws IOException {
        String source = Files.readString(workspaceRoot().resolve(ROUTE_REPLAY_SERVICE_SOURCE));

        List<String> violations = CURRENT_ROUTE_SELECTION_TOKENS.stream()
                .filter(source::contains)
                .toList();

        assertThat(violations)
                .as("Route Replay must not depend on current route selection or rebinding services")
                .isEmpty();
    }

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

    /**
     * 场景：含权益原交易发起退款回放，但原 RouteSnapshot 没有原权益快照摘要。
     * 输入：退款指令携带业务层本次退款决策引用的 benefitSnapshot，原路径快照 context 为空。
     * 输出：解析器拒绝回放。
     * 预期：错误明确指向缺少原权益快照摘要，并带上原交易流水号。
     * 红线：含权益退款不得用当前请求权益结果替代原交易权益快照，也不得静默按当前营销规则重算。
     */
    @Test
    void testResolveBenefitReplayWithoutOriginalBenefitSnapshotShouldFail() {
        FundsBenefitSnapshotSpec currentBenefitSnapshot = benefitSnapshot("BS-CURRENT-001", 9000L, 1000L);
        FundsInstructionReferenceSpec reference = originalTransactionReference("FT202605190004");
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(
                        routeSnapshot(paymentInstrumentRef("CARD-OLD", "old-binding"))));

        assertThatThrownBy(() -> replayService.resolve(replayInstruction(reference,
                paymentInstrumentRef("CARD-NEW", "new-binding"),
                null,
                currentBenefitSnapshot)))
                .hasMessageContaining(MISSING_ORIGINAL_BENEFIT_SNAPSHOT_MESSAGE)
                .hasMessageContaining("FT202605190004");
    }

    /**
     * 场景：原 RouteSnapshot 已被标记为含权益交易，但只保存了权益快照 ID，缺少稳定摘要。
     * 输入：退款指令不携带新的权益结果，原路径快照 context 仅有 `benefitSnapshotId`。
     * 输出：解析器拒绝回放。
     * 预期：错误明确指向缺少原权益快照摘要，并带上原交易流水号。
     * 红线：只保存部分权益线索不能被当成可回放的原权益快照事实。
     */
    @Test
    void testResolveBenefitReplayWithPartialOriginalBenefitSnapshotSummaryShouldFail() {
        FundsInstructionReferenceSpec reference = originalTransactionReference("FT202605190006");
        RouteSnapshotSpec routeSnapshot = routeSnapshot(paymentInstrumentRef("CARD-OLD", "old-binding"),
                null,
                routingDecision("ALLOC-OLD", fundingAccount("PAYER-001")),
                Map.of(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-PARTIAL-001"));
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(routeSnapshot));

        assertThatThrownBy(() -> replayService.resolve(replayInstruction(reference)))
                .hasMessageContaining(MISSING_ORIGINAL_BENEFIT_SNAPSHOT_MESSAGE)
                .hasMessageContaining("FT202605190006");
    }

    /**
     * 场景：含权益退款回放引用原 RouteSnapshot，当前请求也携带了新的权益快照和请求上下文。
     * 输入：原 RouteSnapshot context 只保存原权益快照 ID 和稳定摘要，当前请求携带另一份权益快照和 accountId。
     * 输出：回放成功，ResolvedRoute context 继承原权益快照摘要。
     * 预期：回放结果不使用当前请求的权益摘要覆盖原路径事实，也不扩散当前请求上下文。
     * 红线：Route Replay 只读原权益快照摘要，不能按当前营销或退款请求重新生成历史权益事实或携带临时请求字段。
     */
    @Test
    void testResolveBenefitReplayShouldReuseOriginalBenefitSnapshotSummary() {
        FundsBenefitSnapshotSpec originalBenefitSnapshot = benefitSnapshot("BS-ORIGINAL-001", 9000L, 1000L);
        FundsBenefitSnapshotSpec currentBenefitSnapshot = benefitSnapshot("BS-CURRENT-001", 8000L, 2000L);
        FundsInstructionReferenceSpec reference = originalTransactionReference("FT202605190005");
        RouteSnapshotSpec routeSnapshot = routeSnapshot(paymentInstrumentRef("CARD-OLD", "old-binding"),
                null,
                routingDecision("ALLOC-OLD", fundingAccount("PAYER-001")),
                benefitSnapshotSummary(originalBenefitSnapshot));
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(routeSnapshot));

        ResolvedRouteSpec resolvedRoute = replayService.resolve(replayInstruction(reference,
                paymentInstrumentRef("CARD-NEW", "new-binding"),
                null,
                currentBenefitSnapshot,
                Map.of(FundsInstructionContextKeys.ACCOUNT_ID, "PAYER-CURRENT", "requestChannel", "mobile")));

        assertThat(resolvedRoute.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-001")
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST,
                        originalBenefitSnapshot.getStableDigest());
        assertThat(resolvedRoute.getContextVariables())
                .doesNotContainEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-CURRENT-001")
                .doesNotContainEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST,
                        currentBenefitSnapshot.getStableDigest())
                .doesNotContainKeys(FundsInstructionContextKeys.ACCOUNT_ID, "requestChannel");
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
        return replayInstruction(reference, instrumentRef, externalAccountRef, null);
    }

    private FundsInstructionSpec replayInstruction(FundsInstructionReferenceSpec reference,
                                                   PaymentInstrumentRefSpec instrumentRef,
                                                   ExternalAccountRefSpec externalAccountRef,
                                                   FundsBenefitSnapshotSpec benefitSnapshot) {
        return replayInstruction(reference, instrumentRef, externalAccountRef, benefitSnapshot, Map.of());
    }

    private FundsInstructionSpec replayInstruction(FundsInstructionReferenceSpec reference,
                                                   PaymentInstrumentRefSpec instrumentRef,
                                                   ExternalAccountRefSpec externalAccountRef,
                                                   FundsBenefitSnapshotSpec benefitSnapshot,
                                                   Map<String, Object> contextVariables) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                .instrumentRef(instrumentRef)
                .externalAccountRef(externalAccountRef)
                .reference(reference)
                .benefitSnapshot(benefitSnapshot)
                .businessScene("REFUND")
                .businessSn("REPLAY_MISSING_REFERENCE")
                .eventTime(LocalDateTime.of(2026, 5, 19, 0, 0))
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(1L)
                        .operatorType("SYSTEM")
                        .appName("wind-funds-tests")
                        .build())
                .contextVariables(contextVariables)
                .build();
    }

    private RouteSnapshotSpec routeSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef) {
        return routeSnapshot(paymentInstrumentRef, null, routingDecision("ALLOC-DEFAULT", fundingAccount("PAYER-001")));
    }

    private RouteSnapshotSpec routeSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef,
                                            ExternalAccountRefSpec externalAccountRef,
                                            RoutingDecisionSpec routingDecision) {
        return routeSnapshot(paymentInstrumentRef, externalAccountRef, routingDecision, Map.of());
    }

    private RouteSnapshotSpec routeSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef,
                                            ExternalAccountRefSpec externalAccountRef,
                                            RoutingDecisionSpec routingDecision,
                                            Map<String, Object> contextVariables) {
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
                .contextVariables(contextVariables)
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

    private FundsInstructionReferenceSpec originalTransactionReference(String referenceSn) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn(referenceSn)
                .build();
    }

    private FundsBenefitSnapshotSpec benefitSnapshot(String benefitSnapshotId,
                                                     long userPayAmount,
                                                     long discountAmount) {
        return ImmutableFundsBenefitSnapshotSpec.builder()
                .benefitSnapshotId(benefitSnapshotId)
                .benefitGroupSn("BG-ORDER-001")
                .orderSn("ORDER-001")
                .pricingSnapshotSn("PRICE-001")
                .orderAmount(Money.immutable(10_000L, CurrencyIsoCode.USD))
                .userPayAmount(Money.immutable(userPayAmount, CurrencyIsoCode.USD))
                .merchantReceivableAmount(Money.immutable(userPayAmount, CurrencyIsoCode.USD))
                .components(List.of(merchantDiscountComponent(benefitSnapshotId, discountAmount)))
                .decisionSource("ORDER_PRICING")
                .decisionTraceId("TRACE-" + benefitSnapshotId)
                .contextVariables(Map.of())
                .build();
    }

    private FundsBenefitComponentSpec merchantDiscountComponent(String benefitSnapshotId, long amount) {
        return ImmutableFundsBenefitComponentSpec.builder()
                .componentSn("BC-" + benefitSnapshotId)
                .sequence(1)
                .benefitType(FundsBenefitType.MERCHANT_COUPON)
                .componentType(FundsBenefitComponentType.MERCHANT_DISCOUNT)
                .closureRole(FundsBenefitAmountClosureRole.ORDER_DISCOUNT_CLOSURE)
                .amount(Money.immutable(amount, CurrencyIsoCode.USD))
                .ledgerEffect(FundsBenefitLedgerEffect.NO_LEDGER)
                .fundingNature(FundsBenefitFundingNature.MERCHANT_BORNE)
                .bearerSubjectRef(fundingAccount("MERCHANT-001"))
                .beneficiarySubjectRef(fundingAccount("PAYER-001"))
                .benefitReference(ImmutableFundsBenefitReferenceSpec.builder()
                        .couponId("COUPON-001")
                        .ruleVersion("merchant-rule-v3")
                        .externalDecisionId("pricing-decision-001")
                        .contextVariables(Map.of())
                        .build())
                .refundPolicy(ImmutableFundsBenefitRefundPolicySpec.builder()
                        .partialRefundStrategy(FundsBenefitPartialRefundStrategy.ITEM_LINE_BASED)
                        .dispositions(List.of(FundsBenefitRefundDisposition.NO_REFUND,
                                FundsBenefitRefundDisposition.REDUCE_MERCHANT_RECEIVABLE))
                        .refundRuleVersion("merchant-refund-v3")
                        .refundPolicyCode("MERCHANT_COUPON_NO_RETURN")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of())
                .build();
    }

    private Map<String, Object> benefitSnapshotSummary(FundsBenefitSnapshotSpec snapshot) {
        return Map.of(
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, snapshot.getBenefitSnapshotId(),
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST, snapshot.getStableDigest());
    }

    private Path workspaceRoot() {
        String multiModuleDir = System.getProperty("maven.multiModuleProjectDirectory");
        if (StringUtils.hasText(multiModuleDir)) {
            return Path.of(multiModuleDir);
        }
        Path current = Path.of("").toAbsolutePath();
        if ("tests".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
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
