package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.wallet.enums.SpendRuleAssignmentExplanationStatus;
import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import com.wind.funds.wallet.service.SpendRuleAssignmentDomainQueryService;
import com.wind.funds.wallet.service.SpendRuleAssignmentService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spend Rule 挂载领域读服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleAssignmentDomainQueryServiceImpl implements SpendRuleAssignmentDomainQueryService {

    private static final int ASSIGNMENT_QUERY_PAGE_SIZE = 100;

    private final SpendRuleAssignmentService spendRuleAssignmentService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendRuleAssignmentDTO> queryAssignments(
            @NonNull SpendRuleAssignmentQuery query) {
        return spendRuleAssignmentService.queryAssignments(
                query,
                DefaultPageQueryOptions.defaults(ASSIGNMENT_QUERY_PAGE_SIZE)).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleAssignmentExplanationDTO explainAssignment(
            @NonNull SpendRuleAssignmentExplainQuery query) {
        validateAssignmentExplainQuery(query);
        SpendRuleAssignmentDTO assignment =
                spendRuleAssignmentService.findAssignment(query.getTenantId(), query.getAssignmentSn());
        AssertUtils.notNull(assignment, "Spend Rule 挂载不存在，assignmentSn = {}", query.getAssignmentSn());
        LocalDateTime evaluatedAt = resolveEvaluationTime(query.getExplainAt());
        SpendRuleAssignmentExplanationStatus status = resolveExplanationStatus(assignment, evaluatedAt);
        return new SpendRuleAssignmentExplanationDTO()
                .setAssignment(assignment)
                .setEvaluatedAt(evaluatedAt)
                .setEffective(status == SpendRuleAssignmentExplanationStatus.EFFECTIVE)
                .setExplanationStatus(status)
                .setExplanationMessage(status.getDesc())
                .setEvidenceRefs(toAssignmentEvidenceRefs(assignment));
    }

    private void validateAssignmentExplainQuery(SpendRuleAssignmentExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getAssignmentSn(), "Spend Rule 挂载流水号不能为空");
    }

    private LocalDateTime resolveEvaluationTime(LocalDateTime evaluationTime) {
        if (evaluationTime == null) {
            return LocalDateTime.now();
        }
        return evaluationTime;
    }

    private SpendRuleAssignmentExplanationStatus resolveExplanationStatus(SpendRuleAssignmentDTO assignment,
                                                                          LocalDateTime evaluatedAt) {
        if (assignment.getStatus() != SpendRuleAssignmentStatus.ACTIVE) {
            return SpendRuleAssignmentExplanationStatus.DISABLED;
        }
        if (evaluatedAt.isBefore(assignment.getEffectiveFrom())) {
            return SpendRuleAssignmentExplanationStatus.NOT_YET_EFFECTIVE;
        }
        if (!evaluatedAt.isBefore(assignment.getEffectiveTo())) {
            return SpendRuleAssignmentExplanationStatus.EXPIRED;
        }
        return SpendRuleAssignmentExplanationStatus.EFFECTIVE;
    }

    private List<String> toAssignmentEvidenceRefs(SpendRuleAssignmentDTO assignment) {
        return List.of(
                "spendRule:" + assignment.getRuleId(),
                "spendRuleVersion:" + assignment.getRuleId() + "@" + assignment.getRuleVersion(),
                "spendRuleAssignment:" + assignment.getAssignmentSn(),
                "spendRuleScope:" + assignment.getScopeType() + ":" + assignment.getScopeId());
    }
}
