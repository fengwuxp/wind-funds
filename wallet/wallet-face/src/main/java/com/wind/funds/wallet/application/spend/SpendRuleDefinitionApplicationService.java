package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import org.jspecify.annotations.NonNull;

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
     * 记录规则决策日志。
     *
     * @param request 规则决策日志请求
     * @return 规则决策日志
     */
    @NonNull SpendRuleDecisionLogDTO recordDecision(@NonNull RecordSpendRuleDecisionLogRequest request);
}
