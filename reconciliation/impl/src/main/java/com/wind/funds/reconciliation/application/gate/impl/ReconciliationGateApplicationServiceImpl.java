package com.wind.funds.reconciliation.application.gate.impl;

import com.alibaba.fastjson2.JSON;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationRunResultMapper;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateBlockingDifferenceDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
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
@Service
@AllArgsConstructor
public class ReconciliationGateApplicationServiceImpl implements ReconciliationGateApplicationService {

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    private final ReconciliationRunResultMapper reconciliationRunResultMapper;

    @Override
    @Transactional(readOnly = true)
    public ReconciliationGateDecisionDTO checkGate(CheckReconciliationGateRequest request, WindOperator operator) {
        validateRequest(request);
        AssertUtils.notNull(operator, "对账差错准入检查操作人不能为空");
        ReconciliationRunResult runResult = reconciliationRunResultMapper.selectBySn(
                request.getTenantId(), request.getReconciliationRunResultSn());
        if (runResult == null) {
            return blockedForRunResult(request, operator, null, List.of(), "对账运行结果不存在，准入必须阻断");
        }
        List<String> runEvidenceRefs = parseEvidenceRefs(runResult.getEvidenceRefs());
        if (!matchesGateObject(runResult, request)) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账运行结果与准入对象不匹配，准入必须阻断");
        }
        if (runResult.getStatus() != ReconciliationRunResultStatus.BALANCED) {
            return blockedForRunResult(request, operator, runResult, runEvidenceRefs,
                    "对账运行结果状态为 " + runResult.getStatus() + "，只有 BALANCED 可以进入后续准入");
        }
        List<ReconciliationDifference> scopedDifferences = reconciliationDifferenceMapper.selectByGateObject(
                request.getTenantId(), request.getGateObjectType().name(), request.getGateObjectType().name(),
                request.getGateObjectSn());
        List<ReconciliationDifference> blockingDifferences = scopedDifferences.stream()
                .filter(difference -> shouldBlock(difference, runResult))
                .toList();
        ReconciliationGateDecisionStatus decisionStatus = resolveDecisionStatus(scopedDifferences,
                blockingDifferences);
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
                .setEvidenceRefs(evidenceRefs(runEvidenceRefs, scopedDifferences))
                .setExplanation(resolveExplanation(decisionStatus, scopedDifferences, blockingDifferences))
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operatorId(operator));
    }

    private void validateRequest(CheckReconciliationGateRequest request) {
        AssertUtils.notNull(request, "对账准入检查请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账差错准入检查租户 ID 不能为空");
        AssertUtils.notNull(request.getGateObjectType(), "对账差错准入消费对象类型不能为空");
        AssertUtils.hasText(request.getGateObjectSn(), "对账差错准入消费对象流水号不能为空");
        AssertUtils.hasText(request.getReconciliationRunResultSn(), "对账运行结果流水号不能为空");
    }

    private boolean matchesGateObject(ReconciliationRunResult runResult, CheckReconciliationGateRequest request) {
        return runResult.getGateObjectType() == request.getGateObjectType()
                && Objects.equals(runResult.getGateObjectSn(), request.getGateObjectSn());
    }

    private boolean shouldBlock(ReconciliationDifference difference, ReconciliationRunResult runResult) {
        if (difference.getStatus() != ReconciliationDifferenceStatus.RESOLVED
                || !Boolean.TRUE.equals(difference.getLastRerunBalanced())) {
            return true;
        }
        return !Objects.equals(difference.getLastRerunBatchSn(), runResult.getReconciliationBatchSn())
                || !Objects.equals(difference.getLastRerunRuleVersion(), runResult.getRuleVersion());
    }

    private ReconciliationGateDecisionStatus resolveDecisionStatus(List<ReconciliationDifference> scopedDifferences,
                                                                   List<ReconciliationDifference> blockingDifferences) {
        if (!blockingDifferences.isEmpty()) {
            return ReconciliationGateDecisionStatus.BLOCKED;
        }
        if (!scopedDifferences.isEmpty()) {
            return ReconciliationGateDecisionStatus.CONDITIONALLY_PASSED;
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
                .setBlockingScope(difference.getBlockingScope())
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
        return StringUtils.hasText(value) ? List.copyOf(JSON.parseArray(value, String.class)) : List.of();
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
                                      List<ReconciliationDifference> scopedDifferences,
                                      List<ReconciliationDifference> blockingDifferences) {
        if (decisionStatus == ReconciliationGateDecisionStatus.BLOCKED) {
            return "存在 " + blockingDifferences.size() + " 个未闭环对账差错，准入必须阻断";
        }
        if (decisionStatus == ReconciliationGateDecisionStatus.CONDITIONALLY_PASSED) {
            return "命中范围内 " + scopedDifferences.size() + " 个差错已处理且重新对账已对平，可条件放行";
        }
        return "未发现命中阻断范围的对账差错，准入通过";
    }

    private String operatorId(WindOperator operator) {
        return operator.getOperatorAsText();
    }
}
