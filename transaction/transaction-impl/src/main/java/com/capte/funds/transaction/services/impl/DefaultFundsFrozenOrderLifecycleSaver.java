package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.entities.table.FundsFrozenOrderNameRefs;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.services.FundsFrozenOrderLifecycleSaver;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认冻结单生命周期保存服务。
 *
 * @author Codex
 * @date 2026-05-14
 */
@Service
@AllArgsConstructor
public class DefaultFundsFrozenOrderLifecycleSaver implements FundsFrozenOrderLifecycleSaver {

    private static final WindSequenceType FUNDS_FROZEN_ORDER_SEQUENCE_TYPE = WindSequenceType.immutable(
            "FUNDS_FROZEN_ORDER", "FO", 6);

    private final FundsFrozenOrderMapper fundsFrozenOrderMapper;

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
    public void markSucceeded(@NonNull FundsInstructionLifecycleResult result, @Nullable String ledgerTransactionSn) {
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
    public void markFailed(@NonNull FundsInstructionLifecycleResult result, @NonNull Throwable cause) {
        findFrozenOrderBySn(result.getTransactionSn());
    }

    private FundsFrozenOrder findOrCreateFrozenOrder(FundsInstructionSpec instruction,
                                                     RouteSnapshotSpec routeSnapshot) {
        FundsFrozenOrder result = findFrozenOrderByBusiness(instruction);
        if (result != null) {
            return result;
        }
        if (instruction.getEventType() == FundsTransactionEventType.UNFREEZE) {
            return createUnfreezeRecord(instruction);
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
        entity.setConsumedAmount(0L);
        entity.setCurrency(instruction.getAmount().getCurrency());
        entity.setStatus(FundsFrozenOrderStatus.CREATED);
        entity.setDescription(instruction.getDescription());
        entity.setContextVariables(toLifecycleContext(instruction, Map.of(
                FundsInstructionContextKeys.ROUTE_SNAPSHOT,
                RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot)
        )));
        fundsFrozenOrderMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建资金冻结单生命周期记录失败");
        return entity;
    }

    private FundsFrozenOrder createUnfreezeRecord(FundsInstructionSpec instruction) {
        FundsFrozenOrder originalOrder = findReferencedFrozenOrder(instruction.getReference());
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
        entity.setConsumedAmount(0L);
        entity.setCurrency(instruction.getAmount().getCurrency());
        entity.setStatus(FundsFrozenOrderStatus.CREATED);
        entity.setDescription(instruction.getDescription());
        entity.setContextVariables(toLifecycleContext(instruction,
                Map.of(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, originalOrder.getSn())));
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
        AssertUtils.isTrue(reference != null && hasText(reference.getReferenceSn()),
                "解冻事件必须引用冻结单");
        return findFrozenOrderBySn(reference.getReferenceSn());
    }

    private RouteParticipantSpec resolveParticipant(RouteSnapshotSpec routeSnapshot) {
        AssertUtils.isFalse(routeSnapshot.getParticipants().isEmpty(), "RouteSnapshot participants 不能为空");
        return routeSnapshot.getParticipants().getFirst();
    }

    private boolean isCompleted(FundsFrozenOrder order, FundsTransactionEventType eventType) {
        if (eventType == FundsTransactionEventType.UNFREEZE) {
            return order.getStatus() == FundsFrozenOrderStatus.RELEASED
                    || order.getStatus() == FundsFrozenOrderStatus.CLOSED
                    || order.getStatus() == FundsFrozenOrderStatus.CONSUMED;
        }
        return order.getStatus() == FundsFrozenOrderStatus.FROZEN
                || order.getStatus() == FundsFrozenOrderStatus.RELEASED
                || order.getStatus() == FundsFrozenOrderStatus.CLOSED
                || order.getStatus() == FundsFrozenOrderStatus.CONSUMED;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void markUnfreezeSucceeded(FundsFrozenOrder releaseRecord,
                                       @Nullable String ledgerTransactionSn) {
        releaseRecord.setFreezeLedgerTransactionSn(ledgerTransactionSn);
        releaseRecord.setReleaseTime(LocalDateTime.now());
        releaseRecord.setStatus(FundsFrozenOrderStatus.RELEASED);
        AssertUtils.isTrue(fundsFrozenOrderMapper.update(releaseRecord) == 1,
                "更新资金解冻生命周期状态失败，sn = {}", releaseRecord.getSn());

        FundsFrozenOrder originalOrder = findReferencedFrozenOrder(releaseRecord.getContextVariables());
        Long releasedAmount = originalOrder.getReleasedAmount() + releaseRecord.getAmount();
        AssertUtils.isTrue(releasedAmount <= originalOrder.getAmount(),
                "冻结单剩余可释放金额不足，sn = {}，remainingAmount = {}，amount = {}",
                originalOrder.getSn(), originalOrder.getAmount() - originalOrder.getReleasedAmount(),
                releaseRecord.getAmount());
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

    private FundsTransactionEventType resolveEventType(FundsFrozenOrder order) {
        if (!hasText(order.getContextVariables())) {
            return FundsTransactionEventType.FREEZE;
        }
        JSONObject values = JSON.parseObject(order.getContextVariables());
        String eventType = values.getString(FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE);
        return hasText(eventType) ? FundsTransactionEventType.valueOf(eventType) : FundsTransactionEventType.FREEZE;
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

}
