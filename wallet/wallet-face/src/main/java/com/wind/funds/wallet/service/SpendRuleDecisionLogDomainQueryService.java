package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionLogQuery;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Spend Rule 决策记录领域读服务。
 *
 * <p>职责：按业务范围查询已固化决策记录，并基于决策事实生成只读解释结果。</p>
 *
 * <p>边界：本服务不重新执行规则、不记录新决策、不调整控制额度、不创建交易或账务事实。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleDecisionLogDomainQueryService {

    /**
     * 查询 Spend Rule 决策记录。
     *
     * @param query 查询条件
     * @return 决策记录列表
     */
    @NonNull List<SpendRuleDecisionLogDTO> queryDecisions(@NonNull SpendRuleDecisionLogQuery query);

    /**
     * 解释 Spend Rule 决策事实。
     *
     * @param query 决策解释查询条件
     * @return 决策解释结果
     */
    @NonNull SpendRuleDecisionExplanationDTO explainDecision(@NonNull SpendRuleDecisionExplainQuery query);
}
