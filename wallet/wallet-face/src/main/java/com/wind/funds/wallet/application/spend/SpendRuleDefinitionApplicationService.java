package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionLogQuery;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Spend Rule 定义应用服务。
 *
 * <p>职责：维护 Spend Rule 定义、不可变版本、控制范围挂载和决策日志。
 * 它为支付工具准入、预算控制和交易投影提供规则证据，不负责规则计算。</p>
 *
 * <p>边界：本服务不创建资金交易、route snapshot、账本交易、账目分录、余额投影或
 * SpendControlActivity；支付工具、预算组和 Spend Rule 在本服务内都只是控制范围和审计证据。</p>
 *
 * @author Codex
 * @date 2026-06-22
 */
public interface SpendRuleDefinitionApplicationService {

    /**
     * 创建 Spend Rule 定义。
     *
     * @param request 规则定义创建请求
     * @return 规则定义
     */
    @NonNull SpendRuleDefinitionDTO createDefinition(@NonNull CreateSpendRuleDefinitionRequest request);

    /**
     * 发布不可变 Spend Rule 版本。
     *
     * @param request 规则版本发布请求
     * @return 规则版本
     */
    @NonNull SpendRuleVersionDTO publishVersion(@NonNull PublishSpendRuleVersionRequest request);

    /**
     * 将已发布规则版本挂载到控制范围。
     *
     * @param request 规则版本挂载请求
     * @return 规则挂载
     */
    @NonNull SpendRuleAssignmentDTO assignVersion(@NonNull AssignSpendRuleVersionRequest request);

    /**
     * 查询 Spend Rule 挂载。
     *
     * <p>只读能力，用于支付工具准入、预算控制、投影解释和运营排查读取挂载事实。
     * 本方法不重新执行规则，不记录决策日志，也不创建资金交易或账本事实。</p>
     *
     * @param query 挂载查询条件
     * @return 规则挂载列表
     */
    @NonNull List<SpendRuleAssignmentDTO> queryAssignments(@NonNull SpendRuleAssignmentQuery query);

    /**
     * 解释 Spend Rule 挂载在指定时间点的可用性。
     *
     * @param query 挂载解释查询条件
     * @return 挂载解释结果
     */
    @NonNull SpendRuleAssignmentExplanationDTO explainAssignment(@NonNull SpendRuleAssignmentExplainQuery query);

    /**
     * 记录规则决策日志。
     *
     * @param request 规则决策日志请求
     * @return 规则决策日志
     */
    @NonNull SpendRuleDecisionLogDTO recordDecision(@NonNull RecordSpendRuleDecisionLogRequest request);

    /**
     * 查询 Spend Rule 决策日志。
     *
     * <p>只读能力，用于交易投影、客服审计、对账和规则排查读取已固化决策事实。
     * 本方法不重新执行规则，不调整额度，不创建资金交易、账本交易或账目分录。</p>
     *
     * @param query 决策日志查询条件
     * @return 规则决策日志列表
     */
    @NonNull List<SpendRuleDecisionLogDTO> queryDecisions(@NonNull SpendRuleDecisionLogQuery query);

    /**
     * 解释 Spend Rule 决策事实。
     *
     * <p>解释结果只基于已固化决策日志，不重新计算规则，也不改变交易、route、账本或余额事实。</p>
     *
     * @param query 决策解释查询条件
     * @return 规则决策解释结果
     */
    @NonNull SpendRuleDecisionExplanationDTO explainDecision(@NonNull SpendRuleDecisionExplainQuery query);
}
