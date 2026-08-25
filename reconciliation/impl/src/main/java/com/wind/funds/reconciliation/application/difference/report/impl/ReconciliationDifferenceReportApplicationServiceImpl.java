package com.wind.funds.reconciliation.application.difference.report.impl;

import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.reconciliation.application.difference.report.ReconciliationDifferenceReportApplicationService;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifferenceAction;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceActionMapper;
import com.wind.funds.reconciliation.dal.mapper.ReconciliationDifferenceMapper;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceReportCompleteness;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceState;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceReportDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceActionDTO;
import com.wind.funds.reconciliation.model.request.GetReconciliationDifferenceReportRequest;
import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.integration.core.context.TenantContextHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@NullMarked
@Service
@AllArgsConstructor
public class ReconciliationDifferenceReportApplicationServiceImpl
        implements ReconciliationDifferenceReportApplicationService {

    private static final String SECURITY_WARNING = "报告仅返回安全引用，不返回外部账户、卡号、通道敏感字段或上下文原文";

    private final ReconciliationDifferenceMapper reconciliationDifferenceMapper;

    private final ReconciliationDifferenceActionMapper reconciliationDifferenceActionMapper;

    @Override
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public ReconciliationDifferenceReportDTO getReport(GetReconciliationDifferenceReportRequest request,
                                                       WindOperator operator) {
        validateRequest(request, operator);
        ReconciliationDifference difference = reconciliationDifferenceMapper.selectByDifferenceSn(
                request.getTenantId(), request.getDifferenceSn());
        AssertUtils.notNull(difference, "对账差异报告对应差错不存在，differenceSn = {}", request.getDifferenceSn());

        List<ReconciliationDifferenceAction> actionHistory = reconciliationDifferenceActionMapper.selectByDifferenceSn(
                request.getTenantId(), request.getDifferenceSn());
        ReconciliationDifferenceReportDTO report = toReportDTO(request, difference, actionHistory, operator);
        log.info("对账差异报告查询完成，tenantId={}, differenceSn={}, batchSn={}, state={}, completeness={}, "
                        + "actionCount={}",
                request.getTenantId(), request.getDifferenceSn(), report.getReconciliationBatchSn(),
                report.getState(), report.getCompleteness(), actionHistory.size());
        return report;
    }

    private void validateRequest(GetReconciliationDifferenceReportRequest request, WindOperator operator) {
        AssertUtils.notNull(request, "对账差异报告查询请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "对账差异报告查询租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "对账差异报告查询 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getDifferenceSn(), "对账差异报告查询差错流水号不能为空");
        AssertUtils.notNull(operator, "对账差异报告查询操作人不能为空");
    }

    private ReconciliationDifferenceReportDTO toReportDTO(GetReconciliationDifferenceReportRequest request,
                                                          ReconciliationDifference difference,
                                                          List<ReconciliationDifferenceAction> actionHistory,
                                                          WindOperator operator) {
        ReconciliationDifferenceReportDTO result = new ReconciliationDifferenceReportDTO()
                .setTenantId(difference.getTenantId())
                .setDifferenceSn(difference.getDifferenceSn())
                .setReconciliationBatchSn(difference.getReconciliationBatchSn())
                .setReconciliationMatchResultSn(difference.getReconciliationMatchResultSn())
                .setScopeIdentity(new StableIdentity()
                        .setOwnerNamespace(difference.getScopeOwnerNamespace())
                        .setValue(difference.getScopeIdentityValue()))
                .setPairIdentity(new StableIdentity()
                        .setOwnerNamespace(difference.getPairOwnerNamespace())
                        .setValue(difference.getPairIdentityValue()))
                .setDifferenceType(difference.getDifferenceType())
                .setSeverity(difference.getSeverity())
                .setState(difference.getState())
                .setCurrency(difference.getCurrency())
                .setDifferenceAmount(difference.getDifferenceAmount())
                .setResponsiblePartyRef(difference.getResponsiblePartyRef())
                .setComparisonRuleRef(new ComparisonRuleRef()
                        .setNamespace(difference.getRuleNamespace())
                        .setIdentity(difference.getRuleIdentity())
                        .setVersion(difference.getRuleVersion()))
                .setCurrentLineageRef(difference.getCurrentLineageRef())
                .setEvidenceRef(difference.getEvidenceRef())
                .setActionType(difference.getActionType())
                .setAdjustmentSn(difference.getAdjustmentSn())
                .setAdjustmentIdempotencyKey(difference.getAdjustmentIdempotencyKey())
                .setOriginalFactRef(difference.getOriginalFactRef())
                .setAdjustmentTransactionSn(difference.getAdjustmentTransactionSn())
                .setAdjustmentApprovalRef(difference.getAdjustmentApprovalRef())
                .setAdjustmentEvidenceRef(difference.getAdjustmentEvidenceRef())
                .setAdjustmentReason(difference.getAdjustmentReason())
                .setActionHistory(actionHistory.stream().map(this::toActionDTO).toList())
                .setLastRerunSn(difference.getLastRerunSn())
                .setLastRerunBatchSn(difference.getLastRerunBatchSn())
                .setLastRerunRuleVersion(difference.getLastRerunRuleVersion())
                .setLastRerunBalanced(difference.getLastRerunBalanced())
                .setLastRerunEvidenceRef(difference.getLastRerunEvidenceRef())
                .setLastRerunResultDigest(difference.getLastRerunResultDigest())
                .setRerunCount(difference.getRerunCount())
                .setCompleteness(resolveCompleteness(difference))
                .setSecurityWarnings(List.of(SECURITY_WARNING))
                .setExplanation(resolveExplanation(difference))
                .setCheckedAt(LocalDateTime.now())
                .setCheckedBy(operatorId(operator));
        if (includeEvidenceRefs(request)) {
            result.setEvidenceRefs(evidenceRefs(difference));
        } else {
            result.setEvidenceRefs(List.of());
        }
        return result;
    }

    private ReconciliationDifferenceActionDTO toActionDTO(ReconciliationDifferenceAction source) {
        return new ReconciliationDifferenceActionDTO()
                .setSn(source.getSn())
                .setDifferenceSn(source.getDifferenceSn())
                .setActionType(source.getActionType())
                .setAdjustmentSn(source.getAdjustmentSn())
                .setIdempotencyKey(source.getIdempotencyKey())
                .setOriginalFactRef(source.getOriginalFactRef())
                .setAdjustmentTransactionSn(source.getAdjustmentTransactionSn())
                .setApprovalRef(source.getApprovalRef())
                .setEvidenceRef(source.getEvidenceRef())
                .setReason(source.getReason())
                .setCreatedBy(source.getCreatedBy())
                .setCreatedTime(source.getGmtCreate());
    }

    private ReconciliationDifferenceReportCompleteness resolveCompleteness(ReconciliationDifference difference) {
        if (hasIncompleteActionEvidence(difference)) {
            return ReconciliationDifferenceReportCompleteness.INCOMPLETE_ACTION_EVIDENCE;
        }
        if (needsRerunResult(difference) && !StringUtils.hasText(difference.getLastRerunSn())) {
            return ReconciliationDifferenceReportCompleteness.MISSING_RERUN_RESULT;
        }
        return ReconciliationDifferenceReportCompleteness.COMPLETE;
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
        return difference.getState() == ReconciliationDifferenceState.RECONCILING
                || difference.getState() == ReconciliationDifferenceState.RESOLVED;
    }

    private List<String> evidenceRefs(ReconciliationDifference difference) {
        Set<String> result = new LinkedHashSet<>();
        addText(result, difference.getEvidenceRef());
        addText(result, difference.getAdjustmentEvidenceRef());
        addText(result, difference.getLastRerunEvidenceRef());
        return List.copyOf(result);
    }

    private void addText(Set<String> result, @Nullable String value) {
        if (StringUtils.hasText(value)) {
            result.add(value);
        }
    }

    private String resolveExplanation(ReconciliationDifference difference) {
        if (difference.getState() == ReconciliationDifferenceState.RESOLVED) {
            return "对账差错已处理并经当前批次重新对账通过，当前准入按普通通过处理";
        }
        if (difference.getState() == ReconciliationDifferenceState.INVALIDATED) {
            return "对账差错依赖的来源、解析或匹配证据已被替代批次确认无效，不再参与准入或后续处置";
        }
        return "对账差错处于 " + difference.getState().getDesc() + " 状态，需要人工继续处理或复核";
    }

    private boolean includeEvidenceRefs(GetReconciliationDifferenceReportRequest request) {
        return !Boolean.FALSE.equals(request.getIncludeEvidenceRefs());
    }

    private String operatorId(WindOperator operator) {
        return operator.getOperatorAsText();
    }
}
