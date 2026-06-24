package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import org.jspecify.annotations.NonNull;

/**
 * Spend Rule 决策记录领域写服务。
 *
 * <p>职责：在记录决策事实前校验规则版本、挂载状态、控制范围、支付工具引用和幂等摘要，
 * 确保决策证据可以被交易投影、客服审计和对账解释稳定消费。</p>
 *
 * <p>边界：本服务只固化规则决策事实，不执行规则脚本，不调整额度，不创建资金交易、
 * route snapshot、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleDecisionLogDomainService {

    /**
     * 记录 Spend Rule 决策记录。
     *
     * @param request 决策记录请求
     * @return 决策记录
     */
    @NonNull SpendRuleDecisionLogDTO recordDecision(@NonNull RecordSpendRuleDecisionLogRequest request);
}
