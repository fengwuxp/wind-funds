package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.capte.funds.transaction.support.FundsStableHashSupport;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 默认冻结单生命周期保存服务。
 *
 * @author Codex
 * @date 2026-05-14
 */
@Service
@AllArgsConstructor
public class DefaultFundsFrozenOrderLifecycleSaver implements FundsInstructionLifecycleRecorder {

    private static final WindSequenceType FUNDS_FROZEN_ORDER_SEQUENCE_TYPE = WindSequenceType.immutable(
            "FUNDS_FROZEN_ORDER", "FO", 6);

    private static final String FROZEN_ORDER_REQUEST_HASH = "frozenOrderRequestHash";

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

    @Override
    public boolean supports(@NonNull FundsInstructionSpec instruction) {
        return instruction.getEventType() == FundsTransactionEventType.FREEZE
                || instruction.getEventType() == FundsTransactionEventType.UNFREEZE;
    }

    @Override
    public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                  @NonNull ResolvedRouteSpec resolvedRoute,
                                                                  @NonNull RouteSnapshotSpec routeSnapshot) {
        FundsFrozenOrder order = findOrCreateFrozenOrder(instruction, routeSnapshot);
        return new FundsInstructionLifecycleResult()
                .setTransactionSn(order.getSn())
                .setTransactionDetailSns(List.of())
                .setLedgerTransactionSn(order.getFreezeLedgerTransactionSn())
                .setCompleted(isCompleted(order, instruction.getEventType()));
    }

    @Override
    public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                              @NonNull FundsInstructionLifecycleResult result,
                              @Nullable String ledgerTransactionSn) {
        FundsFrozenOrder order = findFrozenOrderBySn(result.getTransactionSn());
        FundsTransactionEventType eventType = resolveEventType(order);
        if (isCompleted(order, eventType)) {
            return;
        }
        if (eventType == FundsTransactionEventType.UNFREEZE) {
            markUnfreezeSucceeded(order, ledgerTransactionSn);
        } else {
            order.setFreezeLedgerTransactionSn(ledgerTransactionSn);
            order.setStatus(FundsFrozenOrderStatus.FROZEN);
            AssertUtils.isTrue(fundsFrozenOrderMapper.update(order) == 1,
                    "更新资金冻结单生命周期状态失败，sn = {}", order.getSn());
        }
    }

    @Override
    public void markFailed(@NonNull FundsInstructionSpec instruction,
                           @NonNull FundsInstructionLifecycleResult result,
                           @NonNull Throwable cause) {
        findFrozenOrderBySn(result.getTransactionSn());
    }

    private FundsFrozenOrder findOrCreateFrozenOrder(FundsInstructionSpec instruction,
                                                     RouteSnapshotSpec routeSnapshot) {
        FundsFrozenOrder result = findFrozenOrderByBusiness(instruction);
        if (result != null) {
            assertSameRequest(result, instruction, routeSnapshot);
            return result;
        }
        if (instruction.getEventType() == FundsTransactionEventType.UNFREEZE) {
            return createUnfreezeRecord(instruction, routeSnapshot);
        }
        return createFrozenOrder(instruction, routeSnapshot);
    }

    private FundsFrozenOrder createFrozenOrder(FundsInstructionSpec instruction,
                                               RouteSnapshotSpec routeSnapshot) {
        RouteParticipantSpec participant = resolveParticipant(routeSnapshot);
        SubjectRef subjectRef = participant.getSubjectRef();
        FundsFrozenOrder entity = new FundsFrozenOrder();
        entity.setSn(TemporalSequenceFactory.hourNext(FUNDS_FROZEN_ORDER_SEQUENCE_TYPE));
        entity.setTenantId(instruction.getTenantId());
        entity.setSubjectId(subjectRef.getSubjectId());
        entity.setSubjectType(subjectRef.getSubjectType());
        entity.setFreezeType(resolveFreezeType(instruction));
        entity.setBusinessScene(instruction.getBusinessScene());
        entity.setBusinessSn(instruction.getBusinessSn());
        entity.setAmount(instruction.getAmount().getAmount());
        entity.setReleasedAmount(0L);
        entity.setCurrency(instruction.getAmount().getCurrency());
        entity.setStatus(FundsFrozenOrderStatus.CREATED);
        entity.setDescription(instruction.getDescription());
        entity.setContextVariables(toLifecycleContext(instruction, Map.of(
                FROZEN_ORDER_REQUEST_HASH, computeRequestHash(instruction, routeSnapshot),
                FundsInstructionContextKeys.ROUTE_SNAPSHOT,
                RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot)
        )));
        fundsFrozenOrderMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金冻结单生命周期记录失败");
        return entity;
    }

    private FundsFrozenOrder createUnfreezeRecord(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
        FundsFrozenOrder originalOrder = findReferencedFrozenOrder(instruction.getReference());
        assertEnoughReleasableAmount(originalOrder, instruction.getAmount().getAmount());
        FundsFrozenOrder entity = new FundsFrozenOrder();
        entity.setSn(TemporalSequenceFactory.hourNext(FUNDS_FROZEN_ORDER_SEQUENCE_TYPE));
        entity.setTenantId(instruction.getTenantId());
        entity.setSubjectId(originalOrder.getSubjectId());
        entity.setSubjectType(originalOrder.getSubjectType());
        entity.setFreezeType(originalOrder.getFreezeType());
        entity.setBusinessScene(instruction.getBusinessScene());
        entity.setBusinessSn(instruction.getBusinessSn());
        entity.setAmount(instruction.getAmount().getAmount());
        entity.setReleasedAmount(0L);
        entity.setCurrency(instruction.getAmount().getCurrency());
        entity.setStatus(FundsFrozenOrderStatus.CREATED);
        entity.setDescription(instruction.getDescription());
        entity.setContextVariables(toLifecycleContext(instruction,
                Map.of(
                        FROZEN_ORDER_REQUEST_HASH, computeRequestHash(instruction, routeSnapshot),
                        FundsInstructionContextKeys.REFERENCE_FREEZE_SN, originalOrder.getSn())));
        fundsFrozenOrderMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金解冻生命周期记录失败");
        return entity;
    }

    private FundsFrozenOrder findFrozenOrderByBusiness(FundsInstructionSpec instruction) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref)
                .where(ref.tenantId.eq(instruction.getTenantId()))
                .and(ref.freezeType.eq(resolveFreezeType(instruction)))
                .and(ref.businessScene.eq(instruction.getBusinessScene()))
                .and(ref.businessSn.eq(instruction.getBusinessSn()));
        return fundsFrozenOrderMapper.selectOneByQuery(wrapper);
    }

    private FundsFrozenOrder findFrozenOrderBySn(String sn) {
        FundsFrozenOrderNameRefs ref = FundsFrozenOrderNameRefs.fundsFrozenOrder;
        QueryWrapper wrapper = QueryWrapper.create().from(ref).where(ref.sn.eq(sn));
        FundsFrozenOrder result = fundsFrozenOrderMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "资金冻结单生命周期记录不存在，sn = {}", sn);
        return result;
    }

    private FundsFrozenOrder findReferencedFrozenOrder(@Nullable FundsInstructionReferenceSpec reference) {
        AssertUtils.notNull(reference, "解冻事件必须引用冻结单");
        AssertUtils.hasText(reference.getReferenceSn(), "解冻事件必须引用冻结单");
        return findFrozenOrderBySn(reference.getReferenceSn());
    }

    private RouteParticipantSpec resolveParticipant(RouteSnapshotSpec routeSnapshot) {
        AssertUtils.isFalse(routeSnapshot.getParticipants().isEmpty(), "RouteSnapshot participants 不能为空");
        return routeSnapshot.getParticipants().getFirst();
    }

    private boolean isCompleted(FundsFrozenOrder order, FundsTransactionEventType eventType) {
        if (eventType == FundsTransactionEventType.UNFREEZE) {
            return order.getStatus() == FundsFrozenOrderStatus.RELEASED
                    || order.getStatus() == FundsFrozenOrderStatus.CLOSED;
        }
        return order.getStatus() == FundsFrozenOrderStatus.FROZEN
                || order.getStatus() == FundsFrozenOrderStatus.RELEASED
                || order.getStatus() == FundsFrozenOrderStatus.CLOSED;
    }

    private void assertSameRequest(FundsFrozenOrder order,
                                   FundsInstructionSpec instruction,
                                   RouteSnapshotSpec routeSnapshot) {
        String requestHash = requestHash(order);
        if (StringUtils.hasText(requestHash)) {
            AssertUtils.isTrue(Objects.equals(requestHash, computeRequestHash(instruction, routeSnapshot)),
                    "资金冻结单请求参数不一致，sn = {}", order.getSn());
            return;
        }
        AssertUtils.isTrue(Objects.equals(order.getAmount(), instruction.getAmount().getAmount())
                        && order.getCurrency() == instruction.getAmount().getCurrency()
                        && order.getSubjectId().equals(subjectId(instruction, routeSnapshot))
                        && order.getSubjectType().name().equals(subjectType(instruction, routeSnapshot))
                        && resolveEventType(order) == instruction.getEventType()
                        && Objects.equals(referenceFreezeSn(order), referenceSn(instruction.getReference())),
                "资金冻结单请求参数不一致，sn = {}", order.getSn());
    }

    private void markUnfreezeSucceeded(FundsFrozenOrder releaseRecord,
                                       @Nullable String ledgerTransactionSn) {
        releaseRecord.setFreezeLedgerTransactionSn(ledgerTransactionSn);
        releaseRecord.setReleaseTime(LocalDateTime.now());
        releaseRecord.setStatus(FundsFrozenOrderStatus.RELEASED);
        AssertUtils.isTrue(fundsFrozenOrderMapper.update(releaseRecord) == 1,
                "更新资金解冻生命周期状态失败，sn = {}", releaseRecord.getSn());

        FundsFrozenOrder originalOrder = findReferencedFrozenOrder(releaseRecord.getContextVariables());
        assertEnoughReleasableAmount(originalOrder, releaseRecord.getAmount());
        Long releasedAmount = originalOrder.getReleasedAmount() + releaseRecord.getAmount();
        originalOrder.setReleasedAmount(releasedAmount);
        originalOrder.setReleaseTime(releaseRecord.getReleaseTime());
        originalOrder.setStatus(releasedAmount >= originalOrder.getAmount()
                ? FundsFrozenOrderStatus.RELEASED
                : FundsFrozenOrderStatus.PARTIALLY_RELEASED);
        AssertUtils.isTrue(fundsFrozenOrderMapper.update(originalOrder) == 1,
                "更新原冻结单释放金额失败，sn = {}", originalOrder.getSn());
    }

    private FundsFrozenOrder findReferencedFrozenOrder(String contextVariables) {
        AssertUtils.hasText(contextVariables, "解冻生命周期记录缺少原冻结单引用上下文");
        JSONObject values = JSON.parseObject(contextVariables);
        String referenceFreezeSn = values.getString(FundsInstructionContextKeys.REFERENCE_FREEZE_SN);
        AssertUtils.hasText(referenceFreezeSn, "解冻生命周期记录缺少原冻结单引用");
        return findFrozenOrderBySn(referenceFreezeSn);
    }

    private void assertEnoughReleasableAmount(FundsFrozenOrder originalOrder, Long releaseAmount) {
        long remainingAmount = remainingReleasableAmount(originalOrder);
        AssertUtils.notNull(releaseAmount,
                "冻结单剩余可释放金额不足，sn = {}，remainingAmount = {}，amount = {}",
                originalOrder.getSn(), remainingAmount, releaseAmount);
        AssertUtils.isTrue(releaseAmount <= remainingAmount,
                "冻结单剩余可释放金额不足，sn = {}，remainingAmount = {}，amount = {}",
                originalOrder.getSn(), remainingAmount, releaseAmount);
    }

    private long remainingReleasableAmount(FundsFrozenOrder order) {
        return order.getAmount() - defaultAmount(order.getReleasedAmount());
    }

    private long defaultAmount(Long amount) {
        return amount == null ? 0L : amount;
    }

    private FundsTransactionEventType resolveEventType(FundsFrozenOrder order) {
        if (!StringUtils.hasText(order.getContextVariables())) {
            return FundsTransactionEventType.FREEZE;
        }
        JSONObject values = JSON.parseObject(order.getContextVariables());
        String eventType = values.getString(FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE);
        return StringUtils.hasText(eventType) ? FundsTransactionEventType.valueOf(eventType)
                : FundsTransactionEventType.FREEZE;
    }

    private String resolveFreezeType(FundsInstructionSpec instruction) {
        if (instruction.getEventType() == FundsTransactionEventType.UNFREEZE) {
            return findReferencedFrozenOrder(instruction.getReference()).getFreezeType();
        }
        Object freezeType = instruction.getContextVariables().get(FundsInstructionContextKeys.FREEZE_TYPE);
        return freezeType == null ? instruction.getBusinessScene() : freezeType.toString();
    }

    private String toLifecycleContext(FundsInstructionSpec instruction, Map<String, Object> extraContext) {
        Map<String, Object> values = new LinkedHashMap<>(instruction.getContextVariables());
        values.put(FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE, instruction.getEventType().name());
        values.putAll(extraContext);
        return JSON.toJSONString(values);
    }

    private String requestHash(FundsFrozenOrder order) {
        if (!StringUtils.hasText(order.getContextVariables())) {
            return null;
        }
        JSONObject values = JSON.parseObject(order.getContextVariables());
        return values.getString(FROZEN_ORDER_REQUEST_HASH);
    }

    private String referenceFreezeSn(FundsFrozenOrder order) {
        if (!StringUtils.hasText(order.getContextVariables())) {
            return null;
        }
        JSONObject values = JSON.parseObject(order.getContextVariables());
        return values.getString(FundsInstructionContextKeys.REFERENCE_FREEZE_SN);
    }

    private String referenceSn(FundsInstructionReferenceSpec reference) {
        return reference == null ? null : reference.getReferenceSn();
    }

    private String subjectId(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
        if (routeSnapshot != null && !routeSnapshot.getParticipants().isEmpty()) {
            return routeSnapshot.getParticipants().getFirst().getSubjectRef().getSubjectId();
        }
        Object accountId = instruction.getContextVariables().get(FundsInstructionContextKeys.ACCOUNT_ID);
        return accountId == null ? null : JSON.parseObject(JSON.toJSONString(accountId)).getString("id");
    }

    private String subjectType(FundsInstructionSpec instruction, RouteSnapshotSpec routeSnapshot) {
        if (routeSnapshot != null && !routeSnapshot.getParticipants().isEmpty()) {
            return routeSnapshot.getParticipants().getFirst().getSubjectRef().getSubjectType().name();
        }
        Object accountId = instruction.getContextVariables().get(FundsInstructionContextKeys.ACCOUNT_ID);
        return accountId == null ? null : JSON.parseObject(JSON.toJSONString(accountId)).getString("type");
    }

    private String computeRequestHash(FundsInstructionSpec instruction, @Nullable RouteSnapshotSpec routeSnapshot) {
        Map<String, Object> values = new TreeMap<>();
        values.put("tenantId", instruction.getTenantId());
        values.put("instructionType", instruction.getInstructionType().name());
        values.put("eventType", instruction.getEventType().name());
        values.put("transactionType", instruction.getTransactionType().name());
        values.put("amount", instruction.getAmount().getAmount());
        values.put("currency", instruction.getAmount().getCurrency().name());
        values.put("businessScene", instruction.getBusinessScene());
        values.put("businessSn", instruction.getBusinessSn());
        values.put("referenceSn", referenceSn(instruction.getReference()));
        values.put("subjectId", subjectId(instruction, routeSnapshot));
        values.put("subjectType", subjectType(instruction, routeSnapshot));
        values.put("freezeType", resolveFreezeType(instruction));
        values.put("route", routeHashSummary(routeSnapshot));
        return FundsStableHashSupport.sha256Json(values);
    }

    private Map<String, Object> routeHashSummary(@Nullable RouteSnapshotSpec routeSnapshot) {
        if (routeSnapshot == null) {
            return Map.of();
        }
        Map<String, Object> values = new TreeMap<>(RouteSnapshotJsonSupport.routeSummary(routeSnapshot));
        values.remove("snapshotId");
        values.remove("resolvedAt");
        values.remove("expiresAt");
        return FundsStableHashSupport.stableHashMap(values);
    }

}
