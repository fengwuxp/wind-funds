package com.wind.funds.transaction.services.impl;

import com.wind.jackson.WindJson;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableRouteParticipantSpec;
import com.wind.funds.route.model.ImmutableRouteSnapshotSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.enums.RouteReplayPolicy;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.spec.FundsInstructionReferenceSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.operator.WindOperator;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import tools.jackson.core.type.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 冻结单生命周期兼容契约测试。
 */
class DefaultFundsFrozenOrderLifecycleSaverTests {

    private static final long TENANT_ID = 1L;

    private static final String ACCOUNT_ID = "FUNDING-001";

    private static final String FREEZE_TYPE = "FREEZE";

    private static final String FROZEN_ORDER_REQUEST_HASH = "frozenOrderRequestHash";

    private static final LocalDateTime EVENT_TIME = LocalDateTime.of(2026, 5, 14, 10, 0);

    /**
     * 场景：升级前的冻结单保存了 ADJUSTMENT 完整摘要，升级后以 BALANCE_CONTROL 重试。
     * 预期：真实 beforePosting 接受旧摘要并复用原冻结单。
     * 红线：instruction 或 route 任一 transactionType 未归一，重试必须失败。
     */
    @Test
    void testFreezeShouldAcceptLegacyAdjustmentRequestHash() {
        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.FREEZE,
                "FREEZE-BIZ-001", 30L, null);
        RouteSnapshotSpec routeSnapshot = routeSnapshot(instruction);
        FundsFrozenOrder order = order("FO-FREEZE-001", instruction, null,
                legacyRequestHash(instruction, routeSnapshot), legacyRouteSnapshotJson(routeSnapshot));
        FundsFrozenOrderMapper mapper = mock(FundsFrozenOrderMapper.class);
        when(mapper.selectOneByQuery(any())).thenReturn(order);

        FundsInstructionLifecycleResult result = new DefaultFundsFrozenOrderLifecycleSaver(mapper)
                .beforePosting(instruction, mock(ResolvedRouteSpec.class), routeSnapshot);

        assertThat(result.getTransactionSn()).isEqualTo(order.getSn());
        assertThat(result.isCompleted()).isTrue();
    }

    /**
     * 场景：升级前的解冻单保存了 ADJUSTMENT 完整摘要，升级后以 BALANCE_CONTROL 重试。
     * 预期：真实 beforePosting 接受旧摘要并复用原解冻记录。
     * 红线：解冻引用、instruction 或 route 的旧摘要语义不得在升级后漂移。
     */
    @Test
    void testUnfreezeShouldAcceptLegacyAdjustmentRequestHash() {
        FundsFrozenOrder original = originalFreezeOrder();
        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.UNFREEZE,
                "UNFREEZE-BIZ-001", 20L, original.getSn());
        RouteSnapshotSpec routeSnapshot = routeSnapshot(instruction);
        FundsFrozenOrder release = order("FO-UNFREEZE-001", instruction, original.getSn(),
                legacyRequestHash(instruction, routeSnapshot), legacyRouteSnapshotJson(routeSnapshot));
        FundsFrozenOrderMapper mapper = mock(FundsFrozenOrderMapper.class);
        when(mapper.selectOneByQuery(any())).thenReturn(original, release, original);

        FundsInstructionLifecycleResult result = new DefaultFundsFrozenOrderLifecycleSaver(mapper)
                .beforePosting(instruction, mock(ResolvedRouteSpec.class), routeSnapshot);

        assertThat(result.getTransactionSn()).isEqualTo(release.getSn());
        assertThat(result.isCompleted()).isTrue();
    }

    /**
     * 场景：解冻记录首次创建时需要保留本次释放路径。
     * 预期：解冻记录的上下文保存本次 UNFREEZE RouteSnapshot，而不是只保存原冻结单引用。
     * 红线：恢复时不得用原 FREEZE 路径伪装本次 UNFREEZE 路径。
     */
    @Test
    void testUnfreezeShouldPersistCurrentRouteSnapshot() {
        FundsFrozenOrder original = originalFreezeOrder();
        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.UNFREEZE,
                "UNFREEZE-BIZ-ROUTE-001", 20L, original.getSn());
        RouteSnapshotSpec routeSnapshot = routeSnapshot(instruction);
        FundsFrozenOrderMapper mapper = mock(FundsFrozenOrderMapper.class);
        when(mapper.selectOneByQuery(any())).thenReturn(original, null, original);
        doAnswer(invocation -> {
            FundsFrozenOrder order = invocation.getArgument(0);
            order.setId(1L);
            return 1;
        }).when(mapper).insertSelective(any());

        new DefaultFundsFrozenOrderLifecycleSaver(mapper)
                .beforePosting(instruction, mock(ResolvedRouteSpec.class), routeSnapshot);

        ArgumentCaptor<FundsFrozenOrder> captor = ArgumentCaptor.forClass(FundsFrozenOrder.class);
        verify(mapper).insertSelective(captor.capture());
        Map<String, Object> context = WindJson.parseObject(
                captor.getValue().getContextVariables(), new TypeReference<>() {
                });
        assertThat(context)
                .containsEntry(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, original.getSn())
                .containsEntry(FundsInstructionContextKeys.ROUTE_SNAPSHOT,
                        RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot));
    }

    private FundsInstructionSpec instruction(FundsTransactionEventType eventType,
                                             String businessSn,
                                             long amount,
                                             String referenceFreezeSn) {
        FundsInstructionReferenceSpec reference = referenceFreezeSn == null ? null
                : ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.FREEZE_ORDER)
                .referenceSn(referenceFreezeSn)
                .contextVariables(Map.of())
                .build();
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.BALANCE_CONTROL)
                .amount(Money.immutable(amount, CurrencyIsoCode.USD))
                .accountId(FundsAccountId.immutable(ACCOUNT_ID, FundsSubjectType.FUNDING_ACCOUNT.name()))
                .reference(reference)
                .businessScene(eventType.name())
                .businessSn(businessSn)
                .eventTime(EVENT_TIME)
                .operator(mock(WindOperator.class))
                .contextVariables(Map.of())
                .build();
    }

    private RouteSnapshotSpec routeSnapshot(FundsInstructionSpec instruction) {
        ImmutableSubjectRef subjectRef = ImmutableSubjectRef.builder()
                .tenantId(TENANT_ID)
                .subjectId(ACCOUNT_ID)
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .currency(CurrencyIsoCode.USD.name())
                .build();
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(TENANT_ID)
                .snapshotId(instruction.getBusinessSn() + "_ROUTE")
                .snapshotSchemaVersion("1.0")
                .routeCode(instruction.getEventType().name())
                .routeVersion("1.0")
                .businessScene(instruction.getBusinessScene())
                .businessSn(instruction.getBusinessSn())
                .instructionType(instruction.getInstructionType())
                .eventType(instruction.getEventType())
                .transactionType(instruction.getTransactionType())
                .participants(List.of(ImmutableRouteParticipantSpec.builder()
                        .participantRole(RouteParticipantRole.PAYER)
                        .subjectRef(subjectRef)
                        .currency(CurrencyIsoCode.USD.name())
                        .amount(instruction.getAmount())
                        .contextVariables(Map.of())
                        .build()))
                .legs(List.of(ImmutableRouteLegSpec.builder()
                        .legId(instruction.getEventType().name())
                        .sequence(1)
                        .legType(instruction.getEventType() == FundsTransactionEventType.FREEZE
                                ? RouteLegType.HOLD : RouteLegType.RELEASE)
                        .sourceNode(routeNode(subjectRef, RouteNodeRole.SOURCE))
                        .targetNode(routeNode(subjectRef, RouteNodeRole.TARGET))
                        .amount(instruction.getAmount())
                        .originalAmount(instruction.getAmount())
                        .exchangeRate(java.math.BigDecimal.ONE)
                        .replayPolicy(RouteReplayPolicy.FULL_ONLY)
                        .contextVariables(Map.of())
                        .build()))
                .resolvedAt(EVENT_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private FundsFrozenOrder order(String sn,
                                   FundsInstructionSpec instruction,
                                   String referenceFreezeSn,
                                   String requestHash,
                                   String persistedRouteSnapshot) {
        Map<String, Object> context = new TreeMap<>();
        context.put(FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE, instruction.getEventType().name());
        context.put(FROZEN_ORDER_REQUEST_HASH, requestHash);
        if (persistedRouteSnapshot != null) {
            context.put(FundsInstructionContextKeys.ROUTE_SNAPSHOT, persistedRouteSnapshot);
        }
        if (referenceFreezeSn != null) {
            context.put(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, referenceFreezeSn);
        }
        FundsFrozenOrder result = new FundsFrozenOrder();
        result.setSn(sn);
        result.setTenantId(TENANT_ID);
        result.setSubjectId(ACCOUNT_ID);
        result.setSubjectType(FundsSubjectType.FUNDING_ACCOUNT);
        result.setFreezeType(FREEZE_TYPE);
        result.setBusinessScene(instruction.getBusinessScene());
        result.setBusinessSn(instruction.getBusinessSn());
        result.setAmount(instruction.getAmount().getAmount());
        result.setReleasedAmount(0L);
        result.setCurrency(instruction.getAmount().getCurrency());
        result.setState(FundsFrozenOrderState.RELEASED);
        result.setContextVariables(WindJson.toJsonString(context));
        return result;
    }

    private FundsFrozenOrder originalFreezeOrder() {
        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.FREEZE,
                "ORIGINAL-FREEZE-BIZ-001", 30L, null);
        return order("FO-ORIGINAL-001", instruction, null, "unused", null);
    }

    private String legacyRequestHash(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> values = new TreeMap<>();
        values.put(ImmutableFundsInstructionSpec.Fields.tenantId, instruction.getTenantId());
        values.put(ImmutableFundsInstructionSpec.Fields.instructionType, instruction.getInstructionType().name());
        values.put(ImmutableFundsInstructionSpec.Fields.eventType, instruction.getEventType().name());
        values.put(ImmutableFundsInstructionSpec.Fields.transactionType,
                DefaultFundsTransactionType.ADJUSTMENT.name());
        values.put(ImmutableFundsInstructionSpec.Fields.amount, instruction.getAmount().getAmount());
        values.put("currency", instruction.getAmount().getCurrency().name());
        values.put(ImmutableFundsInstructionSpec.Fields.businessScene, instruction.getBusinessScene());
        values.put(ImmutableFundsInstructionSpec.Fields.businessSn, instruction.getBusinessSn());
        values.put(ImmutableFundsInstructionReferenceSpec.Fields.referenceSn,
                instruction.getReference() == null ? null : instruction.getReference().getReferenceSn());
        values.put(ImmutableSubjectRef.Fields.subjectId, ACCOUNT_ID);
        values.put(ImmutableSubjectRef.Fields.subjectType, FundsSubjectType.FUNDING_ACCOUNT.name());
        values.put("freezeType", FREEZE_TYPE);
        values.put("route", legacyRouteHashSummary(routeSnapshot));
        return FundsStableHashSupport.sha256Json(values);
    }

    private Map<String, Object> legacyRouteHashSummary(RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> values = legacyRouteSummary(routeSnapshot);
        values.remove(ImmutableRouteSnapshotSpec.Fields.snapshotId);
        values.remove(ImmutableRouteSnapshotSpec.Fields.resolvedAt);
        values.remove(ImmutableRouteSnapshotSpec.Fields.expiresAt);
        return FundsStableHashSupport.stableHashMap(values);
    }

    private String legacyRouteSnapshotJson(RouteSnapshotSpec routeSnapshot) {
        return WindJson.toJsonString(legacyRouteSummary(routeSnapshot));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> legacyRouteSummary(RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> values = new TreeMap<>(RouteSnapshotJsonSupport.routeSummary(routeSnapshot));
        values.put(ImmutableRouteSnapshotSpec.Fields.transactionType,
                DefaultFundsTransactionType.ADJUSTMENT.name());
        List<Map<String, Object>> legs = ((List<Map<String, Object>>) values.get(
                ImmutableRouteSnapshotSpec.Fields.legs)).stream()
                .<Map<String, Object>>map(TreeMap::new)
                .toList();
        Map<String, Object> leg = legs.getFirst();
        leg.put("balanceEffectType", routeSnapshot.getEventType() == FundsTransactionEventType.FREEZE
                ? "HOLD" : "RELEASE");
        leg.put("phaseCode", routeSnapshot.getEventType().name());
        leg.put("periodType", "LIFETIME");
        leg.put("periodId", "LIFETIME");
        leg.put("constraintOverrides", Map.of());
        Map<String, Object> sourceNode = new TreeMap<>((Map<String, Object>) leg.get("sourceNode"));
        Map<String, Object> targetNode = new TreeMap<>((Map<String, Object>) leg.get("targetNode"));
        sourceNode.put("ledgerSubjectCode", routeSnapshot.getEventType() == FundsTransactionEventType.FREEZE
                ? "AVAILABLE" : "FROZEN");
        targetNode.put("ledgerSubjectCode", routeSnapshot.getEventType() == FundsTransactionEventType.FREEZE
                ? "FROZEN" : "AVAILABLE");
        leg.put("sourceNode", sourceNode);
        leg.put("targetNode", targetNode);
        values.put(ImmutableRouteSnapshotSpec.Fields.legs, legs);
        return values;
    }

    private ImmutableRouteNodeSpec routeNode(ImmutableSubjectRef subjectRef, RouteNodeRole role) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(subjectRef)
                .nodeRole(role)
                .build();
    }

}
