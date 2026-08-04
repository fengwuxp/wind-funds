package com.wind.funds.reconciliation.application.gate.impl;

import com.wind.jackson.WindJson;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatchLineage;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationBatchLineageMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateBlockingDifferenceDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.integration.core.context.TenantContextHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 对账差错准入消费应用服务实现。
 */
@NullMarked
@Slf4j
@Service
@AllArgsConstructor
public class ReconciliationGateApplicationServiceImpl implements ReconciliationGateApplicationService {

    static final int MAX_GATE_LINEAGE_DEPTH = 100;

    static final int MAX_GATE_DIFFERENCE_COUNT = 2000;

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    private final ReconciliationBatchMapper reconciliationBatchMapper;

    private final ReconciliationBatchLineageMapper reconciliationBatchLineageMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public ReconciliationGateDecisionDTO checkGate(CheckReconciliationGateRequest request, WindOperator operator) {
        ReconciliationGateDecisionDTO result = evaluateGate(request, operator, true);
        log.info("对账 Gate 检查完成，tenantId = {}, gateObjectType = {}, gateObjectSn = {}, runResultSn = {}, decisionStatus = {}, blockingDifferenceCount = {}",
                request.getTenantId(), request.getGateObjectType(), request.getGateObjectSn(),
                request.getReconciliationRunResultSn(), result.getDecisionStatus(),
                result.getBlockingDifferences().size());
        return result;
    }

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ReconciliationGateDecisionDTO inspectGate(CheckReconciliationGateRequest request, WindOperator operator) {
        return evaluateGate(request, operator, false);
    }

    private ReconciliationGateDecisionDTO evaluateGate(CheckReconciliationGateRequest request,
                                                        WindOperator operator,
                                                        boolean lockCurrentHead) {
        validateRequest(request);
        AssertUtils.notNull(operator, "对账差错准入检查操作人不能为空");
        ReconciliationRunResult runResult = reconciliationRunResultMapper.selectBySn(
                request.getTenantId(), request.getReconciliationRunResultSn());
        if (runResult == null) {
            return blockedForRunResult(request, operator, null, List.of(), "对账运行结果不存在，准入必须阻断");
        }
        List<String> runEvidenceRefs = parseEvidenceRefs(runResult.getEvidenceRefs());
        if (runResult.getGateObjectType() == null || !StringUtils.hasText(runResult.getGateObjectSn())) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账运行结果未绑定准入对象，只能用于对账查询和差错处理");
        }
        if (!matchesGateObject(runResult, request)) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账运行结果与准入对象不匹配，准入必须阻断");
        }
        ReconciliationBatchLineage lineage = lockCurrentHead
                ? reconciliationBatchLineageMapper.selectForUpdate(
                request.getTenantId(), request.getGateObjectType().name(), request.getGateObjectSn())
                : reconciliationBatchLineageMapper.selectByGateObject(
                request.getTenantId(), request.getGateObjectType().name(), request.getGateObjectSn());
        if (lineage == null
                || !Objects.equals(lineage.getReconciliationScopeRef(), runResult.getReconciliationScopeRef())
                || !Objects.equals(lineage.getCurrentBatchSn(), runResult.getReconciliationBatchSn())) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账运行结果不是当前批次血缘头，旧结论仅可追溯、不得继续用于准入");
        }
        ReconciliationBatch batch = lockCurrentHead
                ? reconciliationBatchMapper.selectBySnForUpdate(
                request.getTenantId(), runResult.getReconciliationBatchSn())
                : reconciliationBatchMapper.selectBySn(
                request.getTenantId(), runResult.getReconciliationBatchSn());
        if (!isCompletedBatchBoundToRunResult(batch, runResult)) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账批次未完成或与运行结果绑定不一致，准入必须阻断");
        }
        if (runResult.getStatus() != ReconciliationRunResultStatus.BALANCED) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账运行结果状态为 " + runResult.getStatus() + "，只有 BALANCED 可以进入后续准入");
        }
        Set<String> currentLineageBatchSns = currentLineageBatchSns(batch);
        List<String> currentLineage = List.copyOf(currentLineageBatchSns);
        List<ReconciliationDifference> blockingDifferences = reconciliationDifferenceMapper.selectBlockingByGateObject(
                request.getTenantId(), request.getGateObjectType().name(), request.getGateObjectSn(),
                currentLineage, MAX_GATE_DIFFERENCE_COUNT + 1, lockCurrentHead);
        if (blockingDifferences.size() > MAX_GATE_DIFFERENCE_COUNT) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "命中准入对象的对账差错数量超过单次检查容量，必须阻断并拆分处置范围");
        }
        int resolvedDifferenceCount = reconciliationDifferenceMapper.countResolvedByGateObject(
                request.getTenantId(), request.getGateObjectType().name(), request.getGateObjectSn(), currentLineage);
        ReconciliationGateDecisionStatus decisionStatus = resolveDecisionStatus(blockingDifferences);
        return new ReconciliationGateDecisionDTO()
                .setPassed(decisionStatus != ReconciliationGateDecisionStatus.BLOCKED)
                .setDecisionStatus(decisionStatus)
                .setGateObjectType(request.getGateObjectType())
                .setGateObjectSn(request.getGateObjectSn())
                .setReconciliationRunResultSn(runResult.getSn())
                .setReconciliationBatchSn(runResult.getReconciliationBatchSn())
                .setReconciliationRunResultStatus(runResult.getStatus())
                .setReconciliationResultDigest(runResult.getResultDigest())
                .setBlockingDifferences(toBlockingDifferenceDTOs(blockingDifferences))
                .setResolvedDifferenceCount(resolvedDifferenceCount)
                .setEvidenceRefs(evidenceRefs(runEvidenceRefs, blockingDifferences))
                .setExplanation(resolveExplanation(decisionStatus, resolvedDifferenceCount, blockingDifferences))
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operatorId(operator));
    }

    private void validateRequest(CheckReconciliationGateRequest request) {
        AssertUtils.notNull(request, "对账准入检查请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账差错准入检查租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账差错准入检查 tenantId 与当前租户不一致");
        AssertUtils.notNull(request.getGateObjectType(), "对账差错准入消费对象类型不能为空");
        AssertUtils.hasText(request.getGateObjectSn(), "对账差错准入消费对象流水号不能为空");
        AssertUtils.hasText(request.getReconciliationRunResultSn(), "对账运行结果流水号不能为空");
    }

    private boolean matchesGateObject(ReconciliationRunResult runResult, CheckReconciliationGateRequest request) {
        return runResult.getGateObjectType() == request.getGateObjectType()
                && Objects.equals(runResult.getGateObjectSn(), request.getGateObjectSn());
    }

    private boolean isCompletedBatchBoundToRunResult(@Nullable ReconciliationBatch batch,
                                                     ReconciliationRunResult runResult) {
        return batch != null
                && batch.getStatus() == ReconciliationBatchStatus.COMPLETED
                && Objects.equals(batch.getRunResultSn(), runResult.getSn())
                && Objects.equals(batch.getReconciliationScopeRef(), runResult.getReconciliationScopeRef())
                && batch.getGateObjectType() == runResult.getGateObjectType()
                && Objects.equals(batch.getGateObjectSn(), runResult.getGateObjectSn())
                && Objects.equals(batch.getRuleVersion(), runResult.getRuleVersion());
    }

    private Set<String> currentLineageBatchSns(ReconciliationBatch currentBatch) {
        Set<String> result = new LinkedHashSet<>();
        ReconciliationBatch cursor = currentBatch;
        while (cursor != null) {
            AssertUtils.isTrue(result.size() < MAX_GATE_LINEAGE_DEPTH,
                    "Gate 对账批次血缘深度不能超过 {}",
                    MAX_GATE_LINEAGE_DEPTH);
            AssertUtils.isTrue(result.add(cursor.getSn()),
                    "Gate 对账批次血缘存在循环，batchSn = {}", cursor.getSn());
            if (!StringUtils.hasText(cursor.getPreviousBatchSn())) {
                break;
            }
            ReconciliationBatch previous = reconciliationBatchMapper.selectBySn(
                    cursor.getTenantId(), cursor.getPreviousBatchSn());
            AssertUtils.notNull(previous,
                    "Gate 对账批次血缘断裂，batchSn = {}, previousBatchSn = {}",
                    cursor.getSn(), cursor.getPreviousBatchSn());
            AssertUtils.isTrue(Objects.equals(previous.getReconciliationScopeRef(), currentBatch.getReconciliationScopeRef())
                            && previous.getGateObjectType() == currentBatch.getGateObjectType()
                            && Objects.equals(previous.getGateObjectSn(), currentBatch.getGateObjectSn()),
                    "Gate 对账批次血缘身份不一致，batchSn = {}", previous.getSn());
            cursor = previous;
        }
        return result;
    }

    private ReconciliationGateDecisionStatus resolveDecisionStatus(List<ReconciliationDifference> blockingDifferences) {
        if (!blockingDifferences.isEmpty()) {
            return ReconciliationGateDecisionStatus.BLOCKED;
        }
        return ReconciliationGateDecisionStatus.PASSED;
    }

    private List<ReconciliationGateBlockingDifferenceDTO> toBlockingDifferenceDTOs(
            List<ReconciliationDifference> blockingDifferences) {
        return blockingDifferences.stream()
                .map(this::toBlockingDifferenceDTO)
                .toList();
    }

    private ReconciliationGateBlockingDifferenceDTO toBlockingDifferenceDTO(ReconciliationDifference difference) {
        return new ReconciliationGateBlockingDifferenceDTO()
                .setDifferenceSn(difference.getDifferenceSn())
                .setStatus(difference.getStatus())
                .setSeverity(difference.getSeverity())
                .setResponsiblePartyRef(difference.getResponsiblePartyRef())
                .setBlockingObjectType(difference.getBlockingObjectType())
                .setBlockingObjectSn(difference.getBlockingObjectSn())
                .setEvidenceRef(difference.getEvidenceRef())
                .setActionType(difference.getActionType())
                .setAdjustmentSn(difference.getAdjustmentSn())
                .setAdjustmentIdempotencyKey(difference.getAdjustmentIdempotencyKey())
                .setOriginalFactRef(difference.getOriginalFactRef())
                .setAdjustmentTransactionSn(difference.getAdjustmentTransactionSn())
                .setLastRerunSn(difference.getLastRerunSn())
                .setLastRerunBalanced(difference.getLastRerunBalanced())
                .setLastRerunEvidenceRef(difference.getLastRerunEvidenceRef())
                .setBlockingReason(blockingReason(difference));
    }

    private String blockingReason(ReconciliationDifference difference) {
        if (difference.getStatus() == ReconciliationDifferenceStatus.BLOCKED) {
            return "对账差错未闭环，仍命中阻断范围";
        }
        if (!Boolean.TRUE.equals(difference.getLastRerunBalanced())) {
            return "差错处理后重新对账未对平，不能释放准入";
        }
        return "对账差错状态未满足准入放行条件";
    }

    private List<String> evidenceRefs(List<String> runEvidenceRefs,
                                      List<ReconciliationDifference> scopedDifferences) {
        Set<String> result = new LinkedHashSet<>(runEvidenceRefs);
        for (ReconciliationDifference difference : scopedDifferences) {
            addText(result, difference.getEvidenceRef());
            addText(result, difference.getAdjustmentEvidenceRef());
            addText(result, difference.getLastRerunEvidenceRef());
        }
        return List.copyOf(result);
    }

    private List<String> parseEvidenceRefs(String value) {
        return StringUtils.hasText(value) ? List.copyOf(WindJson.parseArray(value, String.class)) : List.of();
    }

    private ReconciliationGateDecisionDTO blockedForRunResult(CheckReconciliationGateRequest request,
                                                               WindOperator operator,
                                                               @Nullable ReconciliationRunResult runResult,
                                                               List<String> evidenceRefs,
                                                               String explanation) {
        return new ReconciliationGateDecisionDTO()
                .setPassed(false)
                .setDecisionStatus(ReconciliationGateDecisionStatus.BLOCKED)
                .setGateObjectType(request.getGateObjectType())
                .setGateObjectSn(request.getGateObjectSn())
                .setReconciliationRunResultSn(request.getReconciliationRunResultSn())
                .setReconciliationBatchSn(runResult == null ? null : runResult.getReconciliationBatchSn())
                .setReconciliationRunResultStatus(runResult == null ? null : runResult.getStatus())
                .setReconciliationResultDigest(runResult == null ? null : runResult.getResultDigest())
                .setBlockingDifferences(List.of())
                .setResolvedDifferenceCount(0)
                .setEvidenceRefs(evidenceRefs)
                .setExplanation(explanation)
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operatorId(operator));
    }

    private void addText(Set<String> result, @Nullable String value) {
        if (StringUtils.hasText(value)) {
            result.add(value);
        }
    }

    private String resolveExplanation(ReconciliationGateDecisionStatus decisionStatus,
                                      int resolvedDifferenceCount,
                                      List<ReconciliationDifference> blockingDifferences) {
        if (decisionStatus == ReconciliationGateDecisionStatus.BLOCKED) {
            return "存在 " + blockingDifferences.size() + " 个未闭环对账差错，准入必须阻断";
        }
        if (resolvedDifferenceCount > 0) {
            return "命中范围内 " + resolvedDifferenceCount + " 个历史差错已处理，且经当前批次重新对账对平，准入通过";
        }
        return "未发现命中阻断范围的对账差错，准入通过";
    }

    private String operatorId(WindOperator operator) {
        return operator.getOperatorAsText();
    }
}
