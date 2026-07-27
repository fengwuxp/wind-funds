package com.wind.funds.reconciliation.application.difference.impl;

import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatchLineage;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifferenceAction;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchLineageMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceActionMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationMatchResultMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.sequence.WindSequenceType;
import com.wind.sequence.time.TemporalSequenceFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 对账差错应用服务实现。
 */
@Slf4j
@Service
@AllArgsConstructor
public class ReconciliationDifferenceApplicationServiceImpl implements ReconciliationDifferenceApplicationService {

    private static final WindSequenceType DIFFERENCE_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_DIFFERENCE", "RDF", 6);

    private static final WindSequenceType DIFFERENCE_ACTION_SEQUENCE_TYPE =
            WindSequenceType.immutable("RECONCILIATION_DIFFERENCE_ACTION", "RDA", 6);

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    private final ReconciliationDifferenceActionMapper reconciliationDifferenceActionMapper;

    private final ReconciliationBatchMapper reconciliationBatchMapper;

    private final ReconciliationBatchLineageMapper reconciliationBatchLineageMapper;

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    private final ReconciliationMatchResultMapper reconciliationMatchResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceDTO createDifference(CreateReconciliationDifferenceRequest request,
                                                        WindOperator operator) {
        validateCreateRequest(request, operator);
        ReconciliationMatchResult matchResult = reconciliationMatchResultMapper.selectBySn(
                request.getTenantId(), request.getReconciliationMatchResultSn());
        AssertUtils.notNull(matchResult, "对账匹配结果不存在，reconciliationMatchResultSn = {}",
                request.getReconciliationMatchResultSn());
        ReconciliationRunResult runResult = reconciliationRunResultMapper.selectBySn(
                request.getTenantId(), matchResult.getReconciliationRunResultSn());
        AssertUtils.notNull(runResult, "对账匹配结果关联运行结果不存在，reconciliationMatchResultSn = {}",
                matchResult.getSn());
        ReconciliationBatchLineage lineage = lockBatchLineage(request.getTenantId(), matchResult, runResult);
        if (!Objects.equals(lineage.getCurrentBatchSn(), matchResult.getReconciliationBatchSn())) {
            ReconciliationDifference existing = reconciliationDifferenceMapper.selectByMatchResultSnForUpdate(
                    request.getTenantId(), matchResult.getSn());
            AssertUtils.notNull(existing,
                    "对账差错来源批次不是当前批次血缘头，不允许追加差错，batchSn = {}, currentBatchSn = {}",
                    matchResult.getReconciliationBatchSn(), lineage.getCurrentBatchSn());
            assertSameCreateRequest(existing, request);
            logDifferenceCreated(existing, true);
            return toDTO(existing);
        }
        ReconciliationBatch batch = reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), matchResult.getReconciliationBatchSn());
        assertGateDifferenceSource(matchResult, runResult, batch);
        ReconciliationDifference existing = reconciliationDifferenceMapper.selectByMatchResultSnForUpdate(
                request.getTenantId(), matchResult.getSn());
        if (existing != null) {
            assertSameCreateRequest(existing, request);
            logDifferenceCreated(existing, true);
            return toDTO(existing);
        }
        ReconciliationDifference entity = toEntity(request, operator, matchResult, runResult, batch);
        reconciliationDifferenceMapper.insertSelective(entity);
        AssertUtils.notNull(entity.getId(), "创建对账差错失败");
        logDifferenceCreated(entity, false);
        return toDTO(entity);
    }

    private ReconciliationBatchLineage lockBatchLineage(Long tenantId,
                                                         ReconciliationMatchResult matchResult,
                                                         ReconciliationRunResult runResult) {
        AssertUtils.notNull(runResult.getGateObjectType(),
                "纯对账运行结果不能物化 Gate 差错，reconciliationMatchResultSn = {}", matchResult.getSn());
        AssertUtils.hasText(runResult.getGateObjectSn(),
                "Gate 对账运行结果缺少准入对象流水号，reconciliationMatchResultSn = {}", matchResult.getSn());
        ReconciliationBatchLineage result = reconciliationBatchLineageMapper.selectForUpdate(
                tenantId, runResult.getGateObjectType().name(), runResult.getGateObjectSn());
        AssertUtils.notNull(result,
                "Gate 对账批次血缘不存在，reconciliationMatchResultSn = {}", matchResult.getSn());
        AssertUtils.isTrue(Objects.equals(result.getReconciliationScopeRef(), runResult.getReconciliationScopeRef()),
                "Gate 对账批次血缘范围与运行结果不一致，reconciliationMatchResultSn = {}", matchResult.getSn());
        return result;
    }

    private void assertGateDifferenceSource(ReconciliationMatchResult matchResult,
                                            ReconciliationRunResult runResult,
                                            ReconciliationBatch batch) {
        AssertUtils.notNull(batch, "对账匹配结果关联批次不存在，reconciliationMatchResultSn = {}",
                matchResult.getSn());
        AssertUtils.isTrue(Objects.equals(matchResult.getReconciliationRunResultSn(), runResult.getSn())
                        && Objects.equals(matchResult.getReconciliationBatchSn(), runResult.getReconciliationBatchSn()),
                "对账匹配结果与运行结果绑定不一致，reconciliationMatchResultSn = {}", matchResult.getSn());
        AssertUtils.isTrue(batch.getStatus() == ReconciliationBatchStatus.COMPLETED
                        && Objects.equals(batch.getRunResultSn(), runResult.getSn()),
                "对账匹配结果关联批次尚未完成或运行结果不一致，reconciliationMatchResultSn = {}",
                matchResult.getSn());
        AssertUtils.isTrue(Objects.equals(batch.getReconciliationScopeRef(), runResult.getReconciliationScopeRef())
                        && Objects.equals(batch.getRuleVersion(), runResult.getRuleVersion())
                        && batch.getGateObjectType() == runResult.getGateObjectType()
                        && Objects.equals(batch.getGateObjectSn(), runResult.getGateObjectSn()),
                "对账批次与运行结果事实不一致，reconciliationMatchResultSn = {}", matchResult.getSn());
        AssertUtils.isTrue(runResult.getStatus() == ReconciliationRunResultStatus.DIFFERENCE_FOUND,
                "只有发现差异的运行结果可以物化 Gate 差错，reconciliationMatchResultSn = {}", matchResult.getSn());
        AssertUtils.isTrue(!isAutomaticMatch(matchResult.getMatchStrength())
                        && matchResult.getDifferenceType() != null
                        && matchResult.getSeverity() != null,
                "自动对平项或缺少差异事实的匹配结果不能物化 Gate 差错，reconciliationMatchResultSn = {}",
                matchResult.getSn());
        AssertUtils.notNull(batch.getGateObjectType(),
                "纯对账批次不物化 Gate 差错，由上层运营流程承接，reconciliationMatchResultSn = {}",
                matchResult.getSn());
        AssertUtils.hasText(batch.getGateObjectSn(),
                "对账批次准入对象流水号不能为空，reconciliationMatchResultSn = {}", matchResult.getSn());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceDTO linkAdjustmentResult(LinkReconciliationDifferenceAdjustmentRequest request,
                                                            WindOperator operator) {
        validateAdjustmentRequest(request);
        AssertUtils.notNull(operator, "对账差错处理回链操作人不能为空");
        ReconciliationDifference snapshot = reconciliationDifferenceMapper.selectByDifferenceSn(
                request.getTenantId(), request.getDifferenceSn());
        AssertUtils.notNull(snapshot, "对账差错不存在，differenceSn = {}", request.getDifferenceSn());
        if (Objects.equals(snapshot.getAdjustmentSn(), request.getAdjustmentSn())) {
            ReconciliationDifference entity = selectRequiredDifferenceForUpdate(request.getTenantId(),
                    request.getDifferenceSn());
            ReconciliationDifferenceAction existing = reconciliationDifferenceActionMapper
                    .selectByAdjustmentSnForUpdate(request.getTenantId(), request.getAdjustmentSn());
            AssertUtils.notNull(existing, "对账差错当前处理动作事实不存在，adjustmentSn = {}", request.getAdjustmentSn());
            assertSameAdjustmentRequest(existing, request);
            logActionLinked(entity, existing, true);
            return toDTO(entity);
        }
        assertDifferenceEvidenceValid(snapshot);

        String currentBatchSn = currentDifferenceBatchSn(snapshot);
        AssertUtils.notNull(snapshot.getBlockingObjectType(),
                "对账差错缺少阻断对象类型，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.hasText(snapshot.getBlockingObjectSn(),
                "对账差错缺少阻断对象流水号，differenceSn = {}", request.getDifferenceSn());
        ReconciliationBatchLineage lineage = reconciliationBatchLineageMapper.selectForUpdate(
                request.getTenantId(), snapshot.getBlockingObjectType().name(), snapshot.getBlockingObjectSn());
        AssertUtils.notNull(lineage, "对账差错批次血缘不存在，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(Objects.equals(lineage.getCurrentBatchSn(), currentBatchSn),
                "处理动作必须在重跑批次创建前回链，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.notNull(reconciliationBatchMapper.selectBySnForUpdate(request.getTenantId(), currentBatchSn),
                "对账差错当前批次不存在，reconciliationBatchSn = {}", currentBatchSn);
        ReconciliationDifference entity = selectRequiredDifferenceForUpdate(request.getTenantId(),
                request.getDifferenceSn());
        AssertUtils.isTrue(Objects.equals(currentDifferenceBatchSn(entity), currentBatchSn),
                "对账差错当前批次已变化，请重新读取后重试，differenceSn = {}", request.getDifferenceSn());
        assertDifferenceEvidenceValid(entity);
        ReconciliationDifferenceAction existing = reconciliationDifferenceActionMapper.selectByAdjustmentSnForUpdate(
                request.getTenantId(), request.getAdjustmentSn());
        if (existing != null) {
            assertSameAdjustmentRequest(existing, request);
            logActionLinked(entity, existing, true);
            return toDTO(entity);
        }
        AssertUtils.isTrue(reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                        request.getTenantId(), currentBatchSn) == null,
                "处理动作必须在重跑批次创建前回链，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(reconciliationDifferenceActionMapper.selectByIdempotencyKeyForUpdate(
                        request.getTenantId(), request.getIdempotencyKey()) == null,
                "对账差错处理动作幂等键已被其他动作使用，idempotencyKey = {}", request.getIdempotencyKey());
        AssertUtils.isTrue(entity.getStatus() != ReconciliationDifferenceStatus.RESOLVED,
                "对账差错已关闭，不允许追加新的处理动作，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(entity.getStatus() != ReconciliationDifferenceStatus.ADJUSTING,
                "上一处理动作尚未完成重新对账，不允许追加新的处理动作，differenceSn = {}", request.getDifferenceSn());
        ReconciliationDifferenceAction action = toDifferenceAction(request, operator);
        reconciliationDifferenceActionMapper.insertSelective(action);
        AssertUtils.notNull(action.getId(), "记录对账差错处理动作失败");
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
        logActionLinked(entity, action, false);
        return toDTO(entity);
    }

    private String currentDifferenceBatchSn(ReconciliationDifference difference) {
        return StringUtils.hasText(difference.getLastRerunBatchSn())
                ? difference.getLastRerunBatchSn()
                : difference.getReconciliationBatchSn();
    }

    private ReconciliationDifferenceAction toDifferenceAction(LinkReconciliationDifferenceAdjustmentRequest request,
                                                               WindOperator operator) {
        ReconciliationDifferenceAction result = new ReconciliationDifferenceAction();
        result.setSn(TemporalSequenceFactory.hourNext(DIFFERENCE_ACTION_SEQUENCE_TYPE));
        result.setTenantId(request.getTenantId());
        result.setDifferenceSn(request.getDifferenceSn());
        result.setActionType(request.getActionType());
        result.setAdjustmentSn(request.getAdjustmentSn());
        result.setIdempotencyKey(request.getIdempotencyKey());
        result.setOriginalFactRef(request.getOriginalFactRef());
        result.setAdjustmentTransactionSn(request.getAdjustmentTransactionSn());
        result.setApprovalRef(request.getApprovalRef());
        result.setEvidenceRef(request.getEvidenceRef());
        result.setReason(request.getReason());
        result.setCreatedBy(operatorId(operator));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceDTO recordRerunResult(RecordReconciliationDifferenceRerunRequest request,
                                                         WindOperator operator) {
        validateRerunRequest(request);
        AssertUtils.notNull(operator, "对账差错重跑操作人不能为空");
        ReconciliationRunResult runResult = selectRequiredRunResult(request);
        ReconciliationBatch batch = selectRequiredCompletedBatch(request.getTenantId(), runResult);
        ReconciliationDifference entity = selectRequiredDifferenceForUpdate(request.getTenantId(),
                request.getDifferenceSn());
        if (request.getReconciliationRunResultSn().equals(entity.getLastRerunSn())) {
            assertSameRerunResult(entity, runResult);
            logRerunLinked(entity, runResult, true);
            return toDTO(entity);
        }
        assertDifferenceEvidenceValid(entity);
        AssertUtils.isTrue(entity.getStatus() != ReconciliationDifferenceStatus.RESOLVED,
                "对账差错已关闭，不允许追加新的重跑结果，differenceSn = {}", request.getDifferenceSn());
        int rerunIncrement = assertCurrentRerunLineage(entity, batch);
        assertSameBlockingObject(entity, runResult);
        boolean balanced = runResult.getStatus() == ReconciliationRunResultStatus.BALANCED;
        if (balanced) {
            AssertUtils.hasText(entity.getAdjustmentSn(), "差错关闭必须先关联处理动作或调账结果");
        } else {
            assertSameDifferenceIdentity(entity, runResult);
        }
        entity.setLastRerunSn(runResult.getSn());
        entity.setLastRerunBatchSn(runResult.getReconciliationBatchSn());
        entity.setLastRerunRuleVersion(runResult.getRuleVersion());
        entity.setLastRerunBalanced(balanced);
        entity.setLastRerunEvidenceRef(runResult.getSn());
        entity.setLastRerunResultDigest(runResult.getResultDigest());
        entity.setRerunCount(entity.getRerunCount() + rerunIncrement);
        if (balanced) {
            entity.setStatus(ReconciliationDifferenceStatus.RESOLVED);
            entity.setResolvedBy(operatorId(operator));
            entity.setResolvedTime(LocalDateTime.now());
        } else {
            entity.setStatus(ReconciliationDifferenceStatus.RECONCILING);
        }
        AssertUtils.isTrue(reconciliationDifferenceMapper.update(entity) == 1,
                "更新对账差错重跑结果失败，differenceSn = {}", request.getDifferenceSn());
        logRerunLinked(entity, runResult, false);
        return toDTO(entity);
    }

    private void assertDifferenceEvidenceValid(ReconciliationDifference difference) {
        AssertUtils.isTrue(difference.getStatus() != ReconciliationDifferenceStatus.INVALIDATED,
                "对账差错依赖证据已失效，不允许继续处置，differenceSn = {}",
                difference.getDifferenceSn());
    }

    private void assertSameDifferenceIdentity(ReconciliationDifference difference,
                                              ReconciliationRunResult runResult) {
        ReconciliationMatchResult original = reconciliationMatchResultMapper.selectBySn(
                difference.getTenantId(), difference.getReconciliationMatchResultSn());
        AssertUtils.notNull(original, "对账差错关联匹配结果不存在，differenceSn = {}", difference.getDifferenceSn());
        ReconciliationMatchResult current = reconciliationMatchResultMapper.selectByRunResultAndIdentity(
                difference.getTenantId(), runResult.getSn(), original.getMatchIdentityDigest());
        AssertUtils.isTrue(current != null && current.getDifferenceType() != null,
                "未对平重跑未包含同一差错身份，不允许回链旧差错，differenceSn = {}", difference.getDifferenceSn());
    }

    private void logDifferenceCreated(ReconciliationDifference difference, boolean reused) {
        log.info("对账差错物化完成，tenantId = {}, differenceSn = {}, batchSn = {}, gateObjectType = {}, gateObjectSn = {}, status = {}, reused = {}",
                difference.getTenantId(), difference.getDifferenceSn(), difference.getReconciliationBatchSn(),
                difference.getBlockingObjectType(), difference.getBlockingObjectSn(), difference.getStatus(), reused);
    }

    private void logActionLinked(ReconciliationDifference difference,
                                 ReconciliationDifferenceAction action,
                                 boolean reused) {
        log.info("对账差错处理动作回链完成，tenantId = {}, differenceSn = {}, actionSn = {}, actionType = {}, adjustmentSn = {}, status = {}, reused = {}",
                difference.getTenantId(), difference.getDifferenceSn(), action.getSn(), action.getActionType(),
                action.getAdjustmentSn(), difference.getStatus(), reused);
    }

    private void logRerunLinked(ReconciliationDifference difference,
                                ReconciliationRunResult runResult,
                                boolean reused) {
        log.info("对账差错重跑结果回链完成，tenantId = {}, differenceSn = {}, runResultSn = {}, status = {}, balanced = {}, reused = {}",
                difference.getTenantId(), difference.getDifferenceSn(), runResult.getSn(), difference.getStatus(),
                runResult.getStatus() == ReconciliationRunResultStatus.BALANCED, reused);
    }

    private ReconciliationDifference selectRequiredDifferenceForUpdate(Long tenantId, String differenceSn) {
        ReconciliationDifference result = reconciliationDifferenceMapper.selectByDifferenceSnForUpdate(tenantId,
                differenceSn);
        AssertUtils.notNull(result, "对账差错不存在，differenceSn = {}", differenceSn);
        return result;
    }

    private ReconciliationDifference toEntity(CreateReconciliationDifferenceRequest request,
                                              WindOperator operator,
                                              ReconciliationMatchResult matchResult,
                                              ReconciliationRunResult runResult,
                                              ReconciliationBatch batch) {
        ReconciliationDifference result = new ReconciliationDifference();
        result.setTenantId(request.getTenantId());
        result.setDifferenceSn(TemporalSequenceFactory.hourNext(DIFFERENCE_SEQUENCE_TYPE));
        result.setReconciliationBatchSn(matchResult.getReconciliationBatchSn());
        result.setReconciliationMatchResultSn(matchResult.getSn());
        result.setSourceQuality(matchResult.getSourceQuality());
        result.setMatchStrength(matchResult.getMatchStrength());
        result.setDifferenceType(matchResult.getDifferenceType());
        result.setSeverity(matchResult.getSeverity());
        result.setStatus(ReconciliationDifferenceStatus.BLOCKED);
        result.setCurrency(matchResult.getCurrency());
        result.setDifferenceAmount(matchResult.getDifferenceAmount());
        result.setResponsiblePartyRef(request.getResponsiblePartyRef().trim());
        result.setBlockingObjectType(batch.getGateObjectType());
        result.setBlockingObjectSn(batch.getGateObjectSn());
        result.setRuleVersion(runResult.getRuleVersion());
        result.setEvidenceRef(matchResult.getEvidenceRef());
        result.setRerunCount(0);
        result.setCreatedBy(operatorId(operator));
        result.setDescription(normalizedOptionalText(request.getDescription()));
        result.setVersion(0);
        return result;
    }

    private ReconciliationDifferenceDTO toDTO(ReconciliationDifference entity) {
        return new ReconciliationDifferenceDTO()
                .setId(entity.getId())
                .setTenantId(entity.getTenantId())
                .setDifferenceSn(entity.getDifferenceSn())
                .setReconciliationBatchSn(entity.getReconciliationBatchSn())
                .setReconciliationMatchResultSn(entity.getReconciliationMatchResultSn())
                .setSourceQuality(entity.getSourceQuality())
                .setMatchStrength(entity.getMatchStrength())
                .setDifferenceType(entity.getDifferenceType())
                .setSeverity(entity.getSeverity())
                .setStatus(entity.getStatus())
                .setCurrency(entity.getCurrency())
                .setDifferenceAmount(entity.getDifferenceAmount())
                .setResponsiblePartyRef(entity.getResponsiblePartyRef())
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

    private void validateCreateRequest(CreateReconciliationDifferenceRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "创建对账差错请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "创建对账差错租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "创建对账差错 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getReconciliationMatchResultSn(), "创建对账差错匹配结果流水号不能为空");
        AssertUtils.hasText(request.getResponsiblePartyRef(), "创建对账差错责任方不能为空");
        assertMaxLength(request.getReconciliationMatchResultSn(),
                CreateReconciliationDifferenceRequest.MAX_MATCH_RESULT_SN_LENGTH,
                "创建对账差错匹配结果流水号");
        assertMaxLength(request.getResponsiblePartyRef().trim(),
                CreateReconciliationDifferenceRequest.MAX_RESPONSIBLE_PARTY_REF_LENGTH,
                "创建对账差错责任方引用");
        assertMaxLength(normalizedOptionalText(request.getDescription()),
                CreateReconciliationDifferenceRequest.MAX_DESCRIPTION_LENGTH,
                "创建对账差错说明");
        AssertUtils.notNull(operator, "创建对账差错操作人不能为空");
    }

    private void validateAdjustmentRequest(LinkReconciliationDifferenceAdjustmentRequest request) {
        AssertUtils.notNull(request, "对账差错处理回链请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账差错处理回链租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账差错处理回链 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getDifferenceSn(), "对账差错处理回链差错流水号不能为空");
        AssertUtils.notNull(request.getActionType(), "对账差错处理动作类型不能为空");
        AssertUtils.hasText(request.getAdjustmentSn(), "对账差错处理动作号不能为空");
        AssertUtils.hasText(request.getIdempotencyKey(), "对账差错处理幂等键不能为空");
        AssertUtils.hasText(request.getOriginalFactRef(), "对账差错处理原始事实引用不能为空");
        AssertUtils.hasText(request.getApprovalRef(), "对账差错处理审批引用不能为空");
        AssertUtils.hasText(request.getEvidenceRef(), "对账差错处理证据引用不能为空");
        AssertUtils.hasText(request.getReason(), "对账差错处理原因不能为空");
        assertMaxLength(request.getDifferenceSn(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_DIFFERENCE_SN_LENGTH,
                "对账差错处理回链差错流水号");
        assertMaxLength(request.getAdjustmentSn(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_ADJUSTMENT_SN_LENGTH,
                "对账差错处理动作号");
        assertMaxLength(request.getIdempotencyKey(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_IDEMPOTENCY_KEY_LENGTH,
                "对账差错处理幂等键");
        assertMaxLength(request.getOriginalFactRef(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_ORIGINAL_FACT_REF_LENGTH,
                "对账差错处理原始事实引用");
        assertMaxLength(request.getAdjustmentTransactionSn(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_TRANSACTION_SN_LENGTH,
                "对账差错处理资金交易流水号");
        assertMaxLength(request.getApprovalRef(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_APPROVAL_REF_LENGTH,
                "对账差错处理审批引用");
        assertMaxLength(request.getEvidenceRef(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_EVIDENCE_REF_LENGTH,
                "对账差错处理证据引用");
        assertMaxLength(request.getReason(),
                LinkReconciliationDifferenceAdjustmentRequest.MAX_REASON_LENGTH,
                "对账差错处理原因");
    }

    private void assertMaxLength(@Nullable String value, int maxLength, String fieldName) {
        AssertUtils.isTrue(value == null || value.length() <= maxLength,
                "{}长度不能超过 {}", fieldName, maxLength);
    }

    private void validateRerunRequest(RecordReconciliationDifferenceRerunRequest request) {
        AssertUtils.notNull(request, "对账差错重跑请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账差错重跑租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账差错重跑 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getDifferenceSn(), "对账差错重跑差错流水号不能为空");
        AssertUtils.hasText(request.getReconciliationRunResultSn(), "对账差错重跑运行结果流水号不能为空");
    }

    private void assertSameCreateRequest(ReconciliationDifference existing,
                                         CreateReconciliationDifferenceRequest request) {
        AssertUtils.isTrue(existing.getResponsiblePartyRef().equals(request.getResponsiblePartyRef().trim()),
                "对账差错幂等请求责任方不一致，reconciliationMatchResultSn = {}",
                request.getReconciliationMatchResultSn());
        AssertUtils.isTrue(sameNullable(existing.getDescription(), normalizedOptionalText(request.getDescription())),
                "对账差错幂等请求说明不一致，reconciliationMatchResultSn = {}",
                request.getReconciliationMatchResultSn());
    }

    private void assertSameAdjustmentRequest(ReconciliationDifferenceAction existing,
                                             LinkReconciliationDifferenceAdjustmentRequest request) {
        AssertUtils.isTrue(existing.getDifferenceSn().equals(request.getDifferenceSn()),
                "对账差错处理动作已关联其他差错，adjustmentSn = {}", request.getAdjustmentSn());
        AssertUtils.isTrue(existing.getActionType() == request.getActionType(),
                "对账差错处理幂等请求动作类型不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getAdjustmentSn().equals(request.getAdjustmentSn()),
                "对账差错已关联其他处理动作，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getIdempotencyKey().equals(request.getIdempotencyKey()),
                "对账差错处理幂等请求幂等键不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getOriginalFactRef().equals(request.getOriginalFactRef()),
                "对账差错处理幂等请求原始事实引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(sameNullable(existing.getAdjustmentTransactionSn(), request.getAdjustmentTransactionSn()),
                "对账差错已关联其他资金交易，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getApprovalRef().equals(request.getApprovalRef()),
                "对账差错处理幂等请求审批引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getEvidenceRef().equals(request.getEvidenceRef()),
                "对账差错处理幂等请求证据引用不一致，differenceSn = {}", request.getDifferenceSn());
        AssertUtils.isTrue(existing.getReason().equals(request.getReason()),
                "对账差错处理幂等请求原因不一致，differenceSn = {}", request.getDifferenceSn());
    }

    private ReconciliationRunResult selectRequiredRunResult(RecordReconciliationDifferenceRerunRequest request) {
        ReconciliationRunResult result = reconciliationRunResultMapper.selectBySn(
                request.getTenantId(), request.getReconciliationRunResultSn());
        AssertUtils.notNull(result, "对账运行结果不存在，reconciliationRunResultSn = {}",
                request.getReconciliationRunResultSn());
        return result;
    }

    private ReconciliationBatch selectRequiredCompletedBatch(Long tenantId, ReconciliationRunResult runResult) {
        ReconciliationBatch result = reconciliationBatchMapper.selectBySnForUpdate(
                tenantId, runResult.getReconciliationBatchSn());
        AssertUtils.notNull(result, "对账运行结果关联批次不存在，reconciliationBatchSn = {}",
                runResult.getReconciliationBatchSn());
        AssertUtils.isTrue(result.getStatus() == ReconciliationBatchStatus.COMPLETED
                        && Objects.equals(result.getRunResultSn(), runResult.getSn()),
                "对账运行结果与完成批次绑定不一致，reconciliationBatchSn = {}", result.getSn());
        AssertUtils.isTrue(Objects.equals(result.getRuleVersion(), runResult.getRuleVersion()),
                "对账运行结果与批次规则版本不一致，reconciliationBatchSn = {}", result.getSn());
        AssertUtils.isTrue(Objects.equals(result.getReconciliationScopeRef(), runResult.getReconciliationScopeRef()),
                "对账运行结果与批次范围不一致，reconciliationBatchSn = {}", result.getSn());
        AssertUtils.isTrue(result.getGateObjectType() == runResult.getGateObjectType()
                        && Objects.equals(result.getGateObjectSn(), runResult.getGateObjectSn()),
                "对账运行结果与批次准入对象不一致，reconciliationBatchSn = {}", result.getSn());
        return result;
    }

    private int assertCurrentRerunLineage(ReconciliationDifference difference, ReconciliationBatch batch) {
        String currentBatchSn = currentDifferenceBatchSn(difference);
        AssertUtils.isTrue(!Objects.equals(batch.getSn(), currentBatchSn),
                "对账差错重跑结果必须来自当前批次的后继血缘，differenceSn = {}, currentBatchSn = {}",
                difference.getDifferenceSn(), currentBatchSn);
        ReconciliationBatch current = batch;
        Set<String> visitedBatchSns = new HashSet<>();
        int rerunCount = 1;
        while (!Objects.equals(current.getPreviousBatchSn(), currentBatchSn)) {
            AssertUtils.isTrue(visitedBatchSns.add(current.getSn()),
                    "对账批次重跑血缘存在循环，reconciliationBatchSn = {}", batch.getSn());
            AssertUtils.hasText(current.getPreviousBatchSn(),
                    "对账差错重跑批次不在当前批次血缘，differenceSn = {}, currentBatchSn = {}",
                    difference.getDifferenceSn(), currentBatchSn);
            current = reconciliationBatchMapper.selectBySn(
                    difference.getTenantId(), current.getPreviousBatchSn());
            AssertUtils.notNull(current,
                    "对账差错重跑批次血缘引用不存在，differenceSn = {}, reconciliationBatchSn = {}",
                    difference.getDifferenceSn(), batch.getSn());
            rerunCount++;
        }
        AssertUtils.isTrue(reconciliationBatchMapper.selectByPreviousBatchSnForUpdate(
                        difference.getTenantId(), batch.getSn()) == null,
                "对账差错重跑批次已被后续批次替代，differenceSn = {}, rerunBatchSn = {}",
                difference.getDifferenceSn(), batch.getSn());
        return rerunCount;
    }

    private void assertSameBlockingObject(ReconciliationDifference difference, ReconciliationRunResult runResult) {
        AssertUtils.isTrue(difference.getBlockingObjectType() == runResult.getGateObjectType()
                        && Objects.equals(difference.getBlockingObjectSn(), runResult.getGateObjectSn()),
                "对账差错与重跑运行结果阻断对象不一致，differenceSn = {}", difference.getDifferenceSn());
    }

    private void assertSameRerunResult(ReconciliationDifference existing, ReconciliationRunResult runResult) {
        boolean balanced = runResult.getStatus() == ReconciliationRunResultStatus.BALANCED;
        AssertUtils.isTrue(Objects.equals(existing.getLastRerunBatchSn(), runResult.getReconciliationBatchSn())
                        && Objects.equals(existing.getLastRerunRuleVersion(), runResult.getRuleVersion())
                        && Objects.equals(existing.getLastRerunBalanced(), balanced)
                        && Objects.equals(existing.getLastRerunEvidenceRef(), runResult.getSn())
                        && Objects.equals(existing.getLastRerunResultDigest(), runResult.getResultDigest()),
                "对账差错重跑快照与运行结果不一致，differenceSn = {}", existing.getDifferenceSn());
    }

    private boolean sameNullable(@Nullable String left, @Nullable String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
            return true;
        }
        return left != null && left.equals(right);
    }

    private boolean isAutomaticMatch(ReconciliationMatchStrength matchStrength) {
        return matchStrength == ReconciliationMatchStrength.EXACT_MATCH
                || matchStrength == ReconciliationMatchStrength.RULE_MATCH;
    }

    @Nullable
    private String normalizedOptionalText(@Nullable String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String operatorId(WindOperator operator) {
        return operator.getOperatorAsText();
    }
}
