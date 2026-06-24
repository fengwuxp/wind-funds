package com.wind.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionLogQuery;
import com.wind.funds.wallet.service.SpendRuleDecisionLogDomainQueryService;
import com.wind.funds.wallet.service.SpendRuleDecisionLogService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Spend Rule 决策记录领域读服务实现。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Service
@AllArgsConstructor
public class SpendRuleDecisionLogDomainQueryServiceImpl implements SpendRuleDecisionLogDomainQueryService {

    private static final int DECISION_LOG_QUERY_PAGE_SIZE = 100;

    private final SpendRuleDecisionLogService spendRuleDecisionLogService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<SpendRuleDecisionLogDTO> queryDecisions(
            @NonNull SpendRuleDecisionLogQuery query) {
        return spendRuleDecisionLogService.queryDecisionLogs(
                query,
                DefaultPageQueryOptions.defaults(DECISION_LOG_QUERY_PAGE_SIZE)).getRecords();
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull SpendRuleDecisionExplanationDTO explainDecision(
            @NonNull SpendRuleDecisionExplainQuery query) {
        validateDecisionExplainQuery(query);
        SpendRuleDecisionLogDTO decision =
                spendRuleDecisionLogService.findDecisionLog(query.getTenantId(), query.getDecisionSn());
        AssertUtils.notNull(decision, "Spend Rule 决策记录不存在，decisionSn = {}", query.getDecisionSn());
        return new SpendRuleDecisionExplanationDTO()
                .setDecision(decision)
                .setAdmitted(decision.getDecisionResult() == SpendControlDecisionResult.PASSED)
                .setExplanationMessage(toDecisionExplanationMessage(decision))
                .setEvidenceRefs(toDecisionEvidenceRefs(decision));
    }

    private void validateDecisionExplainQuery(SpendRuleDecisionExplainQuery query) {
        AssertUtils.notNull(query.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(query.getDecisionSn(), "Spend Rule 决策流水号不能为空");
    }

    private String toDecisionExplanationMessage(SpendRuleDecisionLogDTO decision) {
        if (decision.getDecisionResult() == SpendControlDecisionResult.REJECTED) {
            return decision.getDecisionResult().getDesc() + "：" + decision.getRejectReason();
        }
        return decision.getDecisionResult().getDesc();
    }

    private List<String> toDecisionEvidenceRefs(SpendRuleDecisionLogDTO decision) {
        List<String> refs = new ArrayList<>();
        refs.add("spendRule:" + decision.getRuleId());
        refs.add("spendRuleVersion:" + decision.getRuleId() + "@" + decision.getRuleVersion());
        if (StringUtils.hasText(decision.getAssignmentSn())) {
            refs.add("spendRuleAssignment:" + decision.getAssignmentSn());
        }
        refs.add("spendRuleScope:" + decision.getScopeType() + ":" + decision.getScopeId());
        refs.add("spendRuleDecision:" + decision.getDecisionSn());
        if (decision.getId() != null) {
            refs.add("spendRuleDecisionLog:" + decision.getId());
        }
        if (StringUtils.hasText(decision.getInstrumentSn())) {
            refs.add("paymentInstrument:" + decision.getInstrumentSn());
        }
        refs.add("spendRuleBusiness:" + decision.getBusinessScene() + ":" + decision.getBusinessSn());
        return List.copyOf(refs);
    }
}
