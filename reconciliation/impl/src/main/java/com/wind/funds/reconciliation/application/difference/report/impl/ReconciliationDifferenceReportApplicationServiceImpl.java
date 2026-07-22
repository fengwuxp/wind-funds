package com.wind.funds.reconciliation.application.difference.report.impl;

import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.difference.report.ReconciliationDifferenceReportApplicationService;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceReportCompleteness;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceReportDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.GetReconciliationDifferenceReportRequest;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 对账差异报告应用服务实现。
 */
@NullMarked
@Service
@AllArgsConstructor
public class ReconciliationDifferenceReportApplicationServiceImpl
        implements ReconciliationDifferenceReportApplicationService {

    private static final String SECURITY_WARNING = "报告仅返回安全引用，不返回外部账户、卡号、通道敏感字段或上下文原文";

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationDifferenceReportDTO getReport(GetReconciliationDifferenceReportRequest request,
                                                       WindOperator operator) {
        validateRequest(request, operator);
        ReconciliationDifference difference = reconciliationDifferenceMapper.selectByDifferenceSn(
                request.getTenantId(), request.getDifferenceSn());
        AssertUtils.notNull(difference, "对账差异报告对应差错不存在，differenceSn = {}", request.getDifferenceSn());

        ReconciliationGateDecisionDTO gateDecision = resolveGateDecision(request, difference, operator);
        return toReportDTO(request, difference, gateDecision, operator);
    }

    private void validateRequest(GetReconciliationDifferenceReportRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "对账差异报告查询请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账差异报告查询租户 ID 不能为空");
        AssertUtils.hasText(request.getDifferenceSn(), "对账差异报告查询差错流水号不能为空");
        AssertUtils.notNull(operator, "对账差异报告查询操作人不能为空");
    }

    @Nullable
    private ReconciliationGateDecisionDTO resolveGateDecision(GetReconciliationDifferenceReportRequest request,
                                                              ReconciliationDifference difference,
                                                              WindOperator operator) {
        if (!includeGateDecision(request)) {
            return null;
        }
        if (difference.getBlockingObjectType() == null || !StringUtils.hasText(difference.getBlockingObjectSn())) {
            return null;
        }
        AssertUtils.hasText(request.getReconciliationRunResultSn(),
                "查询对账差异报告 gate 决策时，对账运行结果流水号不能为空");
        return reconciliationGateApplicationService.checkGate(new CheckReconciliationGateRequest()
                .setTenantId(request.getTenantId())
                .setGateObjectType(difference.getBlockingObjectType())
                .setGateObjectSn(difference.getBlockingObjectSn())
                .setReconciliationRunResultSn(request.getReconciliationRunResultSn()), operator);
    }

    private ReconciliationDifferenceReportDTO toReportDTO(GetReconciliationDifferenceReportRequest request,
                                                          ReconciliationDifference difference,
                                                          @Nullable ReconciliationGateDecisionDTO gateDecision,
                                                          WindOperator operator) {
        ReconciliationDifferenceReportDTO result = new ReconciliationDifferenceReportDTO()
                .setTenantId(difference.getTenantId())
                .setDifferenceSn(difference.getDifferenceSn())
                .setReconciliationBatchSn(difference.getReconciliationBatchSn())
                .setSourceRecordSn(difference.getSourceRecordSn())
                .setSourceQuality(difference.getSourceQuality())
                .setMatchStrength(difference.getMatchStrength())
                .setDifferenceType(difference.getDifferenceType())
                .setSeverity(difference.getSeverity())
                .setStatus(difference.getStatus())
                .setCurrency(difference.getCurrency())
                .setDifferenceAmount(difference.getDifferenceAmount())
                .setResponsiblePartyRef(difference.getResponsiblePartyRef())
                .setBlockingScope(difference.getBlockingScope())
                .setBlockingObjectType(difference.getBlockingObjectType())
                .setBlockingObjectSn(difference.getBlockingObjectSn())
                .setRuleVersion(difference.getRuleVersion())
                .setEvidenceRef(difference.getEvidenceRef())
                .setActionType(difference.getActionType())
                .setAdjustmentSn(difference.getAdjustmentSn())
                .setAdjustmentIdempotencyKey(difference.getAdjustmentIdempotencyKey())
                .setOriginalFactRef(difference.getOriginalFactRef())
                .setAdjustmentTransactionSn(difference.getAdjustmentTransactionSn())
                .setAdjustmentApprovalRef(difference.getAdjustmentApprovalRef())
                .setAdjustmentEvidenceRef(difference.getAdjustmentEvidenceRef())
                .setAdjustmentReason(difference.getAdjustmentReason())
                .setLastRerunSn(difference.getLastRerunSn())
                .setLastRerunBatchSn(difference.getLastRerunBatchSn())
                .setLastRerunRuleVersion(difference.getLastRerunRuleVersion())
                .setLastRerunBalanced(difference.getLastRerunBalanced())
                .setLastRerunEvidenceRef(difference.getLastRerunEvidenceRef())
                .setLastRerunResultDigest(difference.getLastRerunResultDigest())
                .setRerunCount(difference.getRerunCount())
                .setGateDecisionStatus(resolveGateDecisionStatus(gateDecision))
                .setGateExplanation(resolveGateExplanation(gateDecision))
                .setCompleteness(resolveCompleteness(request, difference, gateDecision))
                .setSecurityWarnings(List.of(SECURITY_WARNING))
                .setExplanation(resolveExplanation(difference, gateDecision))
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operatorId(operator));
        if (includeEvidenceRefs(request)) {
            result.setEvidenceRefs(evidenceRefs(difference, gateDecision));
        } else {
            result.setEvidenceRefs(List.of());
        }
        return result;
    }

    @Nullable
    private ReconciliationGateDecisionStatus resolveGateDecisionStatus(@Nullable ReconciliationGateDecisionDTO gateDecision) {
        if (gateDecision == null) {
            return null;
        }
        return gateDecision.getDecisionStatus();
    }

    @Nullable
    private String resolveGateExplanation(@Nullable ReconciliationGateDecisionDTO gateDecision) {
        if (gateDecision == null) {
            return null;
        }
        return gateDecision.getExplanation();
    }

    private ReconciliationDifferenceReportCompleteness resolveCompleteness(GetReconciliationDifferenceReportRequest request,
                                                                           ReconciliationDifference difference,
                                                                           @Nullable ReconciliationGateDecisionDTO gateDecision) {
        if (hasIncompleteActionEvidence(difference)) {
            return ReconciliationDifferenceReportCompleteness.INCOMPLETE_ACTION_EVIDENCE;
        }
        if (needsRerunResult(difference) && !StringUtils.hasText(difference.getLastRerunSn())) {
            return ReconciliationDifferenceReportCompleteness.MISSING_RERUN_RESULT;
        }
        if (includeGateDecision(request) && missingGateDecision(gateDecision)) {
            return ReconciliationDifferenceReportCompleteness.MISSING_GATE_DECISION;
        }
        return ReconciliationDifferenceReportCompleteness.COMPLETE;
    }

    private boolean missingGateDecision(@Nullable ReconciliationGateDecisionDTO gateDecision) {
        return gateDecision == null;
    }

    private boolean hasIncompleteActionEvidence(ReconciliationDifference difference) {
        if (!StringUtils.hasText(difference.getAdjustmentSn())) {
            return false;
        }
        return difference.getActionType() == null
                || !StringUtils.hasText(difference.getAdjustmentIdempotencyKey())
                || !StringUtils.hasText(difference.getOriginalFactRef())
                || !StringUtils.hasText(difference.getAdjustmentEvidenceRef());
    }

    private boolean needsRerunResult(ReconciliationDifference difference) {
        return difference.getStatus() == ReconciliationDifferenceStatus.RECONCILING
                || difference.getStatus() == ReconciliationDifferenceStatus.RESOLVED;
    }

    private List<String> evidenceRefs(ReconciliationDifference difference,
                                      @Nullable ReconciliationGateDecisionDTO gateDecision) {
        Set<String> result = new LinkedHashSet<>();
        addText(result, difference.getEvidenceRef());
        addText(result, difference.getAdjustmentEvidenceRef());
        addText(result, difference.getLastRerunEvidenceRef());
        if (gateDecision != null) {
            result.addAll(gateDecision.getEvidenceRefs());
        }
        return List.copyOf(result);
    }

    private void addText(Set<String> result, @Nullable String value) {
        if (StringUtils.hasText(value)) {
            result.add(value);
        }
    }

    private String resolveExplanation(ReconciliationDifference difference,
                                      @Nullable ReconciliationGateDecisionDTO gateDecision) {
        if (gateDecision != null && gateDecision.getDecisionStatus() == ReconciliationGateDecisionStatus.BLOCKED) {
            return "对账差错仍命中阻断对象，当前准入必须阻断";
        }
        if (gateDecision != null
                && gateDecision.getDecisionStatus() == ReconciliationGateDecisionStatus.CONDITIONALLY_PASSED) {
            return "对账差错已处理并重新对账对平，当前准入可条件放行";
        }
        if (difference.getStatus() == ReconciliationDifferenceStatus.RESOLVED) {
            return "对账差错已重新对账通过并关闭";
        }
        return "对账差错处于 " + difference.getStatus().getDesc() + " 状态，需要人工继续处理或复核";
    }

    private boolean includeGateDecision(GetReconciliationDifferenceReportRequest request) {
        return !Boolean.FALSE.equals(request.getIncludeGateDecision());
    }

    private boolean includeEvidenceRefs(GetReconciliationDifferenceReportRequest request) {
        return !Boolean.FALSE.equals(request.getIncludeEvidenceRefs());
    }

    private String operatorId(WindOperator operator) {
        return operator.getOperatorAsText();
    }
}
