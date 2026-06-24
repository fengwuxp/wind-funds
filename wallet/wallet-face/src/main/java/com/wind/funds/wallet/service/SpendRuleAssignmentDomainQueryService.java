package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Spend Rule 挂载领域读服务。
 *
 * <p>职责：提供面向准入、投影解释、客服审计和运营排查的挂载只读查询与解释能力。</p>
 *
 * <p>边界：本服务只读取已固化挂载事实，不重新执行规则、不记录决策记录、不调整控制额度，
 * 也不创建资金交易、route snapshot、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleAssignmentDomainQueryService {

    /**
     * 查询 Spend Rule 挂载。
     *
     * @param query 挂载查询条件
     * @return 挂载列表
     */
    @NonNull List<SpendRuleAssignmentDTO> queryAssignments(@NonNull SpendRuleAssignmentQuery query);

    /**
     * 解释指定挂载在某个时间点的可用性。
     *
     * @param query 挂载解释查询条件
     * @return 挂载解释结果
     */
    @NonNull SpendRuleAssignmentExplanationDTO explainAssignment(@NonNull SpendRuleAssignmentExplainQuery query);
}
