package com.wind.funds.route;

import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.route.ImmutableAccountHierarchyFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutableAccountHierarchySnapshotSpec;
import com.wind.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.funds.model.route.ImmutableFundingAllocationDecisionSpec;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.model.route.ImmutableReplayRequestSpec;
import com.wind.funds.model.route.ImmutableRoutingDecisionSpec;
import com.wind.funds.model.route.ImmutableRouteLegSpec;
import com.wind.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.enums.RouteReplayType;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
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

    /**
     * 场景：VCC shared card 原交易完成后，卡换绑到新的子账户或父账户版本，本次退款请求携带当前工具上下文。
     * 输入：原 RouteSnapshot 固化旧卡工具、信用子账户、父账户和层级版本。
     * 输出：回放成功，ResolvedRoute 继续使用原 RouteSnapshot 中的账户层级快照。
     * 预期：funding allocation 的 accountHierarchySnapshot 不被当前工具或当前绑定关系覆盖。
     * 红线：VCC 后续事件必须沿原路径事实回放，不得按当前卡绑定重选子账户或父账户约束。
     */
    @Test
    void testResolveReplayInstructionShouldReuseSnapshotAccountHierarchyForVccSharedCard() {
        SubjectRef cardCreditAccount = creditAccount("VCC-CREDIT-SUB-001");
        SubjectRef parentCreditAccount = creditAccount("VCC-CREDIT-PARENT-001");
        AccountHierarchySnapshotSpec originalHierarchy = accountHierarchySnapshot(cardCreditAccount,
                parentCreditAccount);
        RoutingDecisionSpec originalDecision = vccSharedCardRoutingDecision("ALLOC-VCC-OLD",
                cardCreditAccount,
                originalHierarchy);
        PaymentInstrumentRefSpec originalInstrument = paymentInstrumentRef("CARD-OLD", "old-binding");
        PaymentInstrumentRefSpec currentInstrument = paymentInstrumentRef("CARD-NEW", "new-binding");
        FundsInstructionReferenceSpec reference = ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn("FT-VCC-SHARED-001")
                .build();
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(
                        vccSharedCardRouteSnapshot(originalInstrument, originalDecision, cardCreditAccount)));

        ResolvedRouteSpec resolvedRoute = replayService.resolve(replayInstruction(reference, currentInstrument));

        assertThat(resolvedRoute.getPaymentInstrumentRef()).isSameAs(originalInstrument);
        assertThat(resolvedRoute.getPaymentInstrumentRef().getBindingSnapshot())
                .containsEntry("bindingId", "old-binding");
        assertThat(resolvedRoute.getRoutingDecision().getFundingAllocations()).singleElement()
                .satisfies(allocation -> {
                    assertThat(allocation.getAllocationId()).isEqualTo("ALLOC-VCC-OLD");
                    assertThat(allocation.getSubjectRef().getSubjectId()).isEqualTo("VCC-CREDIT-SUB-001");
                    assertThat(allocation.getAccountHierarchySnapshot()).isNotNull();
                    assertThat(allocation.getAccountHierarchySnapshot().getAccountRef().getSubjectId())
                            .isEqualTo("VCC-CREDIT-SUB-001");
                    assertThat(allocation.getAccountHierarchySnapshot().getParentAccountRef()).isNotNull();
                    assertThat(allocation.getAccountHierarchySnapshot().getParentAccountRef().getSubjectId())
                            .isEqualTo("VCC-CREDIT-PARENT-001");
                    assertThat(allocation.getAccountHierarchySnapshot().getContextVariables())
                            .containsEntry("cardBindingVersion", 1)
                            .containsEntry("cardFundingMode", "SHARED");
                });
    }

    /**
     * 场景：历史含权益原交易发起退款回放，但原 RouteSnapshot 没有原权益快照摘要。
     * 输入：退款指令仅引用原交易，原路径快照 context 为空。
     * 输出：解析器拒绝回放。
     * 预期：无旧权益摘要时按普通历史交易回放，不再要求当前请求携带旧权益快照 DSL。
     * 红线：清理旧 DSL 后，Route Replay 不得依赖当前请求 benefitSnapshot 判断是否含权益。
     */
    @Test
    void testResolveReplayWithoutOriginalBenefitSnapshotSummaryShouldNotRequireLegacySnapshotDsl() {
        FundsInstructionReferenceSpec reference = originalTransactionReference("FT202605190004");
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(
                        routeSnapshot(paymentInstrumentRef("CARD-OLD", "old-binding"))));

        ResolvedRouteSpec resolvedRoute = replayService.resolve(replayInstruction(reference,
                paymentInstrumentRef("CARD-NEW", "new-binding")));

        assertThat(resolvedRoute.getContextVariables())
                .doesNotContainKeys(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID,
                        FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST);
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
     * 场景：含权益退款回放引用带历史权益摘要的原 RouteSnapshot。
     * 输入：原 RouteSnapshot context 保存原权益快照 ID 和稳定摘要，当前请求携带临时上下文。
     * 输出：回放成功，ResolvedRoute context 继承原权益快照摘要。
     * 预期：回放结果继续使用原路径摘要，也不扩散当前请求上下文。
     * 红线：Route Replay 只读原权益快照摘要，不能按当前营销或退款请求重新生成历史权益事实。
     */
    @Test
    void testResolveBenefitReplayShouldReuseOriginalBenefitSnapshotSummary() {
        FundsInstructionReferenceSpec reference = originalTransactionReference("FT202605190005");
        RouteSnapshotSpec routeSnapshot = routeSnapshot(paymentInstrumentRef("CARD-OLD", "old-binding"),
                null,
                routingDecision("ALLOC-OLD", fundingAccount("PAYER-001")),
                benefitSnapshotSummary("BS-ORIGINAL-001", "sha256:original-benefit-digest"));
        DefaultRouteReplayService replayService = new DefaultRouteReplayService(
                new SnapshotFundsTransactionQueryService(routeSnapshot));

        ResolvedRouteSpec resolvedRoute = replayService.resolve(replayInstruction(reference,
                paymentInstrumentRef("CARD-NEW", "new-binding"),
                null,
                Map.of("requestChannel", "mobile")));

        assertThat(resolvedRoute.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, "BS-ORIGINAL-001")
                .containsEntry(FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST,
                        "sha256:original-benefit-digest");
        assertThat(resolvedRoute.getContextVariables())
                .doesNotContainKey("requestChannel");
    }

    @Test
    void testPartialFxRefundShouldUsePaymentSnapshotFacts() {
        SubjectRef payer = fundingAccount("PAYER-FX-001");
        SubjectRef payee = fundingAccount("PAYEE-FX-001");
        RouteLegSpec sourceLeg = ImmutableRouteLegSpec.builder()
                .legId("PAY")
                .sequence(1)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(routeNode(payer, RouteNodeRole.SOURCE))
                .targetNode(routeNode(payee, RouteNodeRole.TARGET))
                .amount(Money.immutable(325L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(1_000L, CurrencyIsoCode.KWD))
                .exchangeRate(new BigDecimal("3.25"))
                .balanceEffectType(LedgerBalanceEffectType.CONSUME)
                .phaseCode(LedgerPhaseCode.SETTLEMENT)
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
                .build();
        RouteSnapshotSpec snapshot = routeSnapshot(null,
                null,
                routingDecision("ALLOC-FX", payer, Money.immutable(325L, CurrencyIsoCode.USD)),
                List.of(participant(RouteParticipantRole.PAYER, payer),
                        participant(RouteParticipantRole.PAYEE, payee)),
                List.of(sourceLeg),
                Map.of());

        ResolvedRouteSpec resolvedRoute = routeReplayService.replay(snapshot,
                ImmutableReplayRequestSpec.builder()
                        .replayType(RouteReplayType.REFUND)
                        .eventType(FundsTransactionEventType.REFUND)
                        .businessScene("FX_REFUND")
                        .businessSn("FX_PARTIAL_REFUND_001")
                        .referenceSnapshotId(snapshot.getSnapshotId())
                        .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                        .originalAmount(Money.immutable(308L, CurrencyIsoCode.KWD))
                        .exchangeRate(new BigDecimal("3.25"))
                        .eventTime(LocalDateTime.of(2026, 5, 19, 1, 0))
                        .contextVariables(Map.of())
                        .build());

        assertThat(resolvedRoute.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getAmount()).isEqualTo(Money.immutable(100L, CurrencyIsoCode.USD));
            assertThat(leg.getOriginalAmount()).isEqualTo(Money.immutable(308L, CurrencyIsoCode.KWD));
            assertThat(leg.getExchangeRate()).isEqualByComparingTo("3.25");
        });

        assertThatThrownBy(() -> routeReplayService.replay(snapshot,
                ImmutableReplayRequestSpec.builder()
                        .replayType(RouteReplayType.REFUND)
                        .eventType(FundsTransactionEventType.REFUND)
                        .businessScene("FX_REFUND")
                        .businessSn("FX_PARTIAL_REFUND_CHANGED_RATE")
                        .referenceSnapshotId(snapshot.getSnapshotId())
                        .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                        .originalAmount(Money.immutable(303L, CurrencyIsoCode.KWD))
                        .exchangeRate(new BigDecimal("3.30"))
                        .eventTime(LocalDateTime.of(2026, 5, 19, 1, 5))
                        .contextVariables(Map.of())
                        .build()))
                .hasMessageContaining("退款汇率必须与原支付快照汇率一致");

        assertThatThrownBy(() -> routeReplayService.replay(snapshot,
                ImmutableReplayRequestSpec.builder()
                        .replayType(RouteReplayType.REFUND)
                        .eventType(FundsTransactionEventType.REFUND)
                        .businessScene("FX_REFUND")
                        .businessSn("FX_PARTIAL_REFUND_FULL_ORIGINAL_AMOUNT")
                        .referenceSnapshotId(snapshot.getSnapshotId())
                        .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                        .originalAmount(Money.immutable(1_000L, CurrencyIsoCode.KWD))
                        .exchangeRate(new BigDecimal("3.25"))
                        .eventTime(LocalDateTime.of(2026, 5, 19, 1, 10))
                        .contextVariables(Map.of())
                        .build()))
                .hasMessageContaining("部分退款原币金额必须小于原支付原币金额");
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
        return replayInstruction(reference, instrumentRef, externalAccountRef, Map.of());
    }

    private FundsInstructionSpec replayInstruction(FundsInstructionReferenceSpec reference,
                                                   PaymentInstrumentRefSpec instrumentRef,
                                                   ExternalAccountRefSpec externalAccountRef,
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

    private RouteSnapshotSpec authorizationRouteSnapshot() {
        SubjectRef payer = fundingAccount("PAYER-001");
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("AUTH-SNAPSHOT-202605190001")
                .snapshotSchemaVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .routeCode(FundsRouteCodes.AUTHORIZATION_STANDARD)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene("AUTHORIZATION")
                .businessSn("AUTHORIZATION_AUTHORIZE")
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTHORIZE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(participant(RouteParticipantRole.PAYER, payer)))
                .legs(List.of(authorizationHoldLeg(payer)))
                .routingDecision(routingDecision("ALLOC-AUTH", payer, Money.immutable(80L, CurrencyIsoCode.USD)))
                .resolvedAt(LocalDateTime.of(2026, 5, 18, 12, 0))
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteLegSpec authorizationHoldLeg(SubjectRef payer) {
        return ImmutableRouteLegSpec.builder()
                .legId("AUTHORIZATION_1")
                .sequence(1)
                .legType(RouteLegType.HOLD)
                .sourceNode(routeNode(payer, RouteNodeRole.SOURCE, LedgerSubjectCode.AVAILABLE))
                .targetNode(routeNode(payer, RouteNodeRole.TARGET, LedgerSubjectCode.AUTHORIZATION))
                .amount(Money.immutable(80L, CurrencyIsoCode.USD))
                .balanceEffectType(LedgerBalanceEffectType.HOLD)
                .phaseCode(LedgerPhaseCode.AUTHORIZATION)
                .replayPolicy(RouteReplayPolicy.PARTIAL_ALLOWED)
                .constraintOverrides(Map.of())
                .contextVariables(Map.of())
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
        return routeSnapshot(paymentInstrumentRef,
                externalAccountRef,
                routingDecision,
                List.of(participant(RouteParticipantRole.PAYER, payer),
                        participant(RouteParticipantRole.PAYEE, payee)),
                List.of(routeLeg(payer, payee)),
                contextVariables);
    }

    private RouteSnapshotSpec routeSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef,
                                            ExternalAccountRefSpec externalAccountRef,
                                            RoutingDecisionSpec routingDecision,
                                            List<RouteParticipantSpec> participants,
                                            List<RouteLegSpec> legs,
                                            Map<String, Object> contextVariables) {
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
                .participants(participants)
                .legs(legs)
                .routingDecision(routingDecision)
                .paymentInstrumentRef(paymentInstrumentRef)
                .externalAccountRef(externalAccountRef)
                .resolvedAt(LocalDateTime.of(2026, 5, 18, 12, 0))
                .contextVariables(contextVariables)
                .build();
    }

    private RouteSnapshotSpec vccSharedCardRouteSnapshot(PaymentInstrumentRefSpec paymentInstrumentRef,
                                                         RoutingDecisionSpec routingDecision,
                                                         SubjectRef cardCreditAccount) {
        SubjectRef merchant = fundingAccount("MERCHANT-001");
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1L)
                .snapshotId("SNAPSHOT-VCC-SHARED-202605190001")
                .snapshotSchemaVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .routeCode(FundsRouteCodes.DIRECT_PAY_STANDARD)
                .routeVersion(FundsRouteCodes.CURRENT_ROUTE_VERSION)
                .businessScene("VCC_SHARED_CARD_PAY")
                .businessSn("VCC-SHARED-PAY-202605190001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .participants(List.of(participant(RouteParticipantRole.PAYER, cardCreditAccount),
                        participant(RouteParticipantRole.PAYEE, merchant)))
                .legs(List.of(routeLeg(cardCreditAccount, merchant)))
                .routingDecision(routingDecision)
                .paymentInstrumentRef(paymentInstrumentRef)
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
        return routeNode(subjectRef, nodeRole, LedgerSubjectCode.AVAILABLE);
    }

    private ImmutableRouteNodeSpec routeNode(SubjectRef subjectRef,
                                             RouteNodeRole nodeRole,
                                             LedgerSubjectCode ledgerSubjectCode) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .ledgerSubjectCode(ledgerSubjectCode)
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

    private SubjectRef creditAccount(String accountId) {
        return ImmutableSubjectRef.builder()
                .tenantId(1L)
                .subjectId(accountId)
                .subjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .currency(CurrencyIsoCode.USD.name())
                .ledgerProfileCode("CREDIT")
                .build();
    }

    private AccountHierarchySnapshotSpec accountHierarchySnapshot(SubjectRef accountRef,
                                                                  SubjectRef parentAccountRef) {
        return ImmutableAccountHierarchySnapshotSpec.builder()
                .accountRef(accountRef)
                .parentAccountRef(parentAccountRef)
                .rootAccountRef(parentAccountRef)
                .contextVariables(Map.of("cardBindingVersion", 1, "cardFundingMode", "SHARED"))
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
        return routingDecision(allocationId, fundingAccount, Money.immutable(10L, CurrencyIsoCode.USD));
    }

    private RoutingDecisionSpec routingDecision(String allocationId, SubjectRef fundingAccount, Money amount) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode("PAYMENT_INSTRUMENT_SNAPSHOT_POLICY")
                .matchedRules(List.of("original-default-funding-source"))
                .selectedProcessor("ORIGINAL_PROCESSOR")
                .selectedCashFundingAccount(fundingAccount.getSubjectId())
                .fundingAllocations(List.of(ImmutableFundingAllocationDecisionSpec.builder()
                        .allocationId(allocationId)
                        .subjectRef(fundingAccount)
                        .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                        .amount(amount)
                        .priority(1)
                        .reason("original funding allocation snapshot")
                        .build()))
                .decisionReason("original route snapshot")
                .contextVariables(Map.of("snapshot", allocationId))
                .build();
    }

    private RoutingDecisionSpec vccSharedCardRoutingDecision(String allocationId,
                                                             SubjectRef cardCreditAccount,
                                                             AccountHierarchySnapshotSpec hierarchySnapshot) {
        return ImmutableRoutingDecisionSpec.builder()
                .policyCode("VCC_SHARED_CARD_ACCOUNT_HIERARCHY_POLICY")
                .matchedRules(List.of("original-vcc-shared-card-account-hierarchy"))
                .selectedProcessor("VCC_ORIGINAL_PROCESSOR")
                .selectedCashFundingAccount(cardCreditAccount.getSubjectId())
                .fundingAllocations(List.of(ImmutableAccountHierarchyFundingAllocationDecisionSpec.builder()
                        .allocationId(allocationId)
                        .subjectRef(cardCreditAccount)
                        .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                        .amount(Money.immutable(10L, CurrencyIsoCode.USD))
                        .accountHierarchySnapshot(hierarchySnapshot)
                        .priority(1)
                        .reason("original VCC shared card account hierarchy snapshot")
                        .build()))
                .decisionReason("original VCC shared card route snapshot")
                .contextVariables(Map.of("snapshot", allocationId))
                .build();
    }

    private FundsInstructionReferenceSpec originalTransactionReference(String referenceSn) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                .referenceSn(referenceSn)
                .build();
    }

    private SubjectRef subjectRef(String subjectId) {
        return ImmutableSubjectRef.builder()
                .subjectId(subjectId)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .currency(CurrencyIsoCode.USD.name())
                .build();
    }

    private Map<String, Object> benefitSnapshotSummary(String benefitSnapshotId, String stableDigest) {
        return Map.of(
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_ID, benefitSnapshotId,
                FundsInstructionContextKeys.BENEFIT_SNAPSHOT_STABLE_DIGEST, stableDigest);
    }

    private static class EmptyFundsTransactionQueryService implements FundsTransactionQueryService {

        @Override
        public Optional<FundsTransactionDTO> queryFundsTransaction(String transactionSn) {
            return Optional.empty();
        }

        @Override
        public Optional<FundsTransactionDTO> findFundsTransactionByBusiness(Long tenantId,
                                                                            String businessScene,
                                                                            String businessSn) {
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
        public Money sumConsumedReplayLegAmount(String referenceTransactionSn,
                                                FundsTransactionEventType eventType,
                                                String replayRefLegId,
                                                CurrencyIsoCode currency,
                                                String excludedBusinessScene,
                                                String excludedBusinessSn) {
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
