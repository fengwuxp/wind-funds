package com.wind.funds.reconciliation.application.difference.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 对账差错应用服务实现。
 */
@Service
@AllArgsConstructor
public class ReconciliationDifferenceApplicationServiceImpl implements ReconciliationDifferenceApplicationService {

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceDTO createDifference(CreateReconciliationDifferenceRequest request,
                                                        WindOperator operator) {
        validateCreateRequest(request);
        ReconciliationDifference existing = reconciliationDifferenceMapper.selectByDifferenceSnForUpdate(
                request.getTenantId(), request.getDifferenceSn());
        if (existing != null) {
            assertSameCreateRequest(existing, request);
            return toDTO(existing);
        }
        ReconciliationDifference entity = toEntity(request, operator);
        reconciliationDifferenceMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建对账差错失败");
        return toDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceDTO linkAdjustmentResult(LinkReconciliationDifferenceAdjustmentRequest request,
                                                            WindOperator operator) {
        validateAdjustmentRequest(request);
        ReconciliationDifference entity = selectRequiredDifferenceForUpdate(request.getTenantId(),
                request.getDifferenceSn());
        if (StringUtils.hasText(entity.getAdjustmentSn())) {
            assertSameAdjustmentRequest(entity, request);
            return toDTO(entity);
        }
        entity.setActionType(request.getActionType());
        entity.setAdjustmentSn(request.getAdjustmentSn());
        entity.setAdjustmentIdempotencyKey(request.getIdempotencyKey());
        entity.setOriginalFactRef(request.getOriginalFactRef());
        entity.setAdjustmentTransactionSn(request.getAdjustmentTransactionSn());
        entity.setAdjustmentApprovalRef(request.getApprovalRef());
        entity.setAdjustmentEvidenceRef(request.getEvidenceRef());
        entity.setAdjustmentReason(request.getReason());
        entity.setAdjustedBy(operatorId(operator));
        entity.setAdjustedTime(LocalDateTime.now());
        entity.setStatus(ReconciliationDifferenceStatus.ADJUSTING);
        AssertUtils.isTrue(reconciliationDifferenceMapper.update(entity) == 1,
                "更新对账差错处理回链失败，differenceSn = {}", request.getDifferenceSn());
        return toDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceDTO recordRerunResult(RecordReconciliationDifferenceRerunRequest request,
                                                         WindOperator operator) {
        validateRerunRequest(request);
        ReconciliationDifference entity = selectRequiredDifferenceForUpdate(request.getTenantId(),
                request.getDifferenceSn());
        if (request.getRerunSn().equals(entity.getLastRerunSn())) {
            assertSameRerunRequest(entity, request);
            return toDTO(entity);
        }
        AssertUtils.isTrue(entity.getStatus() != ReconciliationDifferenceStatus.RESOLVED,
                "对账差错已关闭，不允许追加新的重跑结果，differenceSn = {}", request.getDifferenceSn());
        if (Boolean.TRUE.equals(request.getBalanced())) {
            AssertUtils.hasText(entity.getAdjustmentSn(), "差错关闭必须先关联处理动作或调账结果");
        }
        entity.setLastRerunSn(request.getRerunSn());
        entity.setLastRerunBatchSn(request.getRerunBatchSn());
        entity.setLastRerunRuleVersion(request.getRuleVersion());
        entity.setLastRerunBalanced(request.getBalanced());
        entity.setLastRerunEvidenceRef(request.getEvidenceRef());
        entity.setLastRerunResultDigest(request.getResultDigest());
        entity.setRerunCount(entity.getRerunCount() + 1);
        if (Boolean.TRUE.equals(request.getBalanced())) {
            entity.setStatus(ReconciliationDifferenceStatus.RESOLVED);
            entity.setResolvedBy(operatorId(operator));
            entity.setResolvedTime(LocalDateTime.now());
        } else {
            entity.setStatus(ReconciliationDifferenceStatus.RECONCILING);
        }
        AssertUtils.isTrue(reconciliationDifferenceMapper.update(entity) == 1,
                "更新对账差错重跑结果失败，differenceSn = {}", request.getDifferenceSn());
        return toDTO(entity);
    }

    private ReconciliationDifference selectRequiredDifferenceForUpdate(Long tenantId, String differenceSn) {
        ReconciliationDifference result = reconciliationDifferenceMapper.selectByDifferenceSnForUpdate(tenantId,
                differenceSn);
        AssertUtils.notNull(result, "对账差错不存在，differenceSn = {}", differenceSn);
        return result;
    }

    private ReconciliationDifference toEntity(CreateReconciliationDifferenceRequest request, WindOperator operator) {
        ReconciliationDifference result = new ReconciliationDifference();
        result.setTenantId(request.getTenantId());
        result.setDifferenceSn(request.getDifferenceSn());
        result.setReconciliationBatchSn(request.getReconciliationBatchSn());
        result.setSourceRecordSn(request.getSourceRecordSn());
        result.setSourceQuality(request.getSourceQuality());
        result.setMatchStrength(request.getMatchStrength());
        result.setDifferenceType(request.getDifferenceType());
        result.setSeverity(request.getSeverity());
        result.setStatus(ReconciliationDifferenceStatus.BLOCKED);
        result.setCurrency(request.getCurrency());
        result.setDifferenceAmount(request.getDifferenceAmount());
        result.setResponsiblePartyRef(request.getResponsiblePartyRef());
        result.setBlockingScope(request.getBlockingScope());
        result.setBlockingObjectType(request.getBlockingObjectType());
        result.setBlockingObjectSn(request.getBlockingObjectSn());
        result.setRuleVersion(request.getRuleVersion());
        result.setEvidenceRef(request.getEvidenceRef());
        result.setRerunCount(0);
        result.setCreatedBy(operatorId(operator));
        result.setDescription(request.getDescription());
        result.setVersion(0);
        return result;
    }

    private ReconciliationDifferenceDTO toDTO(ReconciliationDifference entity) {
        return new ReconciliationDifferenceDTO()
                .setId(entity.getId())
                .setTenantId(entity.getTenantId())
                .setDifferenceSn(entity.getDifferenceSn())
                .setReconciliationBatchSn(entity.getReconciliationBatchSn())
                .setSourceRecordSn(entity.getSourceRecordSn())
                .setSourceQuality(entity.getSourceQuality())
                .setMatchStrength(entity.getMatchStrength())
                .setDifferenceType(entity.getDifferenceType())
                .setSeverity(entity.getSeverity())
                .setStatus(entity.getStatus())
                .setCurrency(entity.getCurrency())
                .setDifferenceAmount(entity.getDifferenceAmount())
                .setResponsiblePartyRef(entity.getResponsiblePartyRef())
                .setBlockingScope(entity.getBlockingScope())
                .setBlockingObjectType(entity.getBlockingObjectType())
                .setBlockingObjectSn(entity.getBlockingObjectSn())
                .setRuleVersion(entity.getRuleVersion())
                .setEvidenceRef(entity.getEvidenceRef())
                .setActionType(entity.getActionType())
                .setAdjustmentSn(entity.getAdjustmentSn())
                .setAdjustmentIdempotencyKey(entity.getAdjustmentIdempotencyKey())
                .setOriginalFactRef(entity.getOriginalFactRef())
                .setAdjustmentTransactionSn(entity.getAdjustmentTransactionSn())
                .setLastRerunSn(entity.getLastRerunSn())
                .setLastRerunBatchSn(entity.getLastRerunBatchSn())
                .setRerunCount(entity.getRerunCount())
                .setCreatedBy(entity.getCreatedBy())
                .setAdjustedBy(entity.getAdjustedBy())
                .setResolvedBy(entity.getResolvedBy())
                .setCreatedTime(entity.getGmtCreate())
                .setAdjustedTime(entity.getAdjustedTime())
                .setResolvedTime(entity.getResolvedTime())
                .setDescription(entity.getDescription());
    }

    private void validateCreateRequest(CreateReconciliationDifferenceRequest request) {
        AssertUtils.notNull(request.getTenantId(), "创建对账差错租户 ID 不能为空");
        AssertUtils.hasText(request.getDifferenceSn(), "创建对账差错流水号不能为空");
        AssertUtils.hasText(request.getReconciliationBatchSn(), "创建对账差错批次号不能为空");
        AssertUtils.hasText(request.getSourceRecordSn(), "创建对账差错来源记录不能为空");
        AssertUtils.notNull(request.getSourceQuality(), "创建对账差错来源质量不能为空");
        AssertUtils.notNull(request.getMatchStrength(), "创建对账差错匹配强度不能为空");
        AssertUtils.notNull(request.getDifferenceType(), "创建对账差错类型不能为空");
        AssertUtils.notNull(request.getSeverity(), "创建对账差错严重等级不能为空");
        AssertUtils.notNull(request.getCurrency(), "创建对账差错币种不能为空");
        AssertUtils.notNull(request.getDifferenceAmount(), "创建对账差错金额不能为空");
        AssertUtils.isTrue(request.getDifferenceAmount() > 0, "创建对账差错金额必须大于 0");
        AssertUtils.hasText(request.getResponsiblePartyRef(), "创建对账差错责任方不能为空");
        AssertUtils.hasText(request.getBlockingScope(), "创建对账差错阻断范围不能为空");
        AssertUtils.isTrue(objectScopeComplete(request), "创建对账差错阻断对象类型和流水号必须同时填写或同时为空");
        AssertUtils.hasText(request.getRuleVersion(), "创建对账差错规则版本不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "创建对账差错证据引用不能为空");
    }

    private void validateAdjustmentRequest(LinkReconciliationDifferenceAdjustmentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "对账差错处理回链租户 ID 不能为空");
        AssertUtils.hasText(request.getDifferenceSn(), "对账差错处理回链差错流水号不能为空");
        AssertUtils.notNull(request.getActionType(), "对账差错处理动作类型不能为空");
        AssertUtils.hasText(request.getAdjustmentSn(), "对账差错处理动作号不能为空");
        AssertUtils.hasText(request.getIdempotencyKey(), "对账差错处理幂等键不能为空");
        AssertUtils.hasText(request.getOriginalFactRef(), "对账差错处理原始事实引用不能为空");
        AssertUtils.hasText(request.getApprovalRef(), "对账差错处理审批引用不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "对账差错处理证据引用不能为空");
        AssertUtils.hasText(request.getReason(), "对账差错处理原因不能为空");
    }

    private void validateRerunRequest(RecordReconciliationDifferenceRerunRequest request) {
        AssertUtils.notNull(request.getTenantId(), "对账差错重跑租户 ID 不能为空");
        AssertUtils.hasText(request.getDifferenceSn(), "对账差错重跑差错流水号不能为空");
        AssertUtils.hasText(request.getRerunSn(), "对账差错重跑流水号不能为空");
        AssertUtils.hasText(request.getRerunBatchSn(), "对账差错重跑批次号不能为空");
        AssertUtils.hasText(request.getRuleVersion(), "对账差错重跑规则版本不能为空");
        AssertUtils.notNull(request.getBalanced(), "对账差错重跑结果不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "对账差错重跑证据引用不能为空");
        AssertUtils.hasText(request.getResultDigest(), "对账差错重跑结果摘要不能为空");
    }

    private void assertSameCreateRequest(ReconciliationDifference existing,
                                         CreateReconciliationDifferenceRequest request) {
        AssertUtils.isTrue(existing.getReconciliationBatchSn().equals(request.getReconciliationBatchSn()),
                "对账差错幂等请求批次号不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getSourceRecordSn().equals(request.getSourceRecordSn()),
                "对账差错幂等请求来源记录不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getDifferenceType() == request.getDifferenceType(),
                "对账差错幂等请求差错类型不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getDifferenceAmount().equals(request.getDifferenceAmount()),
                "对账差错幂等请求差异金额不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getCurrency() == request.getCurrency(),
                "对账差错幂等请求币种不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getSourceQuality() == request.getSourceQuality(),
                "对账差错幂等请求来源质量不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getMatchStrength() == request.getMatchStrength(),
                "对账差错幂等请求匹配强度不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getSeverity() == request.getSeverity(),
                "对账差错幂等请求严重等级不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getResponsiblePartyRef().equals(request.getResponsiblePartyRef()),
                "对账差错幂等请求责任方不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getBlockingScope().equals(request.getBlockingScope()),
                "对账差错幂等请求阻断范围不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getBlockingObjectType() == request.getBlockingObjectType(),
                "对账差错幂等请求阻断对象类型不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(sameNullable(existing.getBlockingObjectSn(), request.getBlockingObjectSn()),
                "对账差错幂等请求阻断对象流水号不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getRuleVersion().equals(request.getRuleVersion()),
                "对账差错幂等请求规则版本不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getEvidenceRef().equals(request.getEvidenceRef()),
                "对账差错幂等请求证据引用不一致，differenceSn = {}", request.getDifferenceSn());
    }

    private void assertSameAdjustmentRequest(ReconciliationDifference existing,
                                             LinkReconciliationDifferenceAdjustmentRequest request) {
        AssertUtils.isTrue(existing.getActionType() == request.getActionType(),
                "对账差错处理幂等请求动作类型不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getAdjustmentSn().equals(request.getAdjustmentSn()),
                "对账差错已关联其他处理动作，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getAdjustmentIdempotencyKey().equals(request.getIdempotencyKey()),
                "对账差错处理幂等请求幂等键不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getOriginalFactRef().equals(request.getOriginalFactRef()),
                "对账差错处理幂等请求原始事实引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(sameNullable(existing.getAdjustmentTransactionSn(), request.getAdjustmentTransactionSn()),
                "对账差错已关联其他资金交易，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getAdjustmentApprovalRef().equals(request.getApprovalRef()),
                "对账差错处理幂等请求审批引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getAdjustmentEvidenceRef().equals(request.getEvidenceRef()),
                "对账差错处理幂等请求证据引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getAdjustmentReason().equals(request.getReason()),
                "对账差错处理幂等请求原因不一致，differenceSn = {}", request.getDifferenceSn());
    }

    private void assertSameRerunRequest(ReconciliationDifference existing,
                                        RecordReconciliationDifferenceRerunRequest request) {
        AssertUtils.isTrue(existing.getLastRerunBatchSn().equals(request.getRerunBatchSn()),
                "对账差错重跑幂等请求批次号不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getLastRerunRuleVersion().equals(request.getRuleVersion()),
                "对账差错重跑幂等请求规则版本不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getLastRerunBalanced().equals(request.getBalanced()),
                "对账差错重跑幂等请求对平结果不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getLastRerunEvidenceRef().equals(request.getEvidenceRef()),
                "对账差错重跑幂等请求证据引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getLastRerunResultDigest().equals(request.getResultDigest()),
                "对账差错重跑幂等请求结果摘要不一致，differenceSn = {}", request.getDifferenceSn());
    }

    private boolean sameNullable(@Nullable String left, @Nullable String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
            return true;
        }
        return left != null && left.equals(right);
    }

    private boolean objectScopeComplete(CreateReconciliationDifferenceRequest request) {
        return (request.getBlockingObjectType() == null && !StringUtils.hasText(request.getBlockingObjectSn()))
                || (request.getBlockingObjectType() != null && StringUtils.hasText(request.getBlockingObjectSn()));
    }

    private String operatorId(WindOperator operator) {
        return String.valueOf(operator.getOperatorId());
    }
}
