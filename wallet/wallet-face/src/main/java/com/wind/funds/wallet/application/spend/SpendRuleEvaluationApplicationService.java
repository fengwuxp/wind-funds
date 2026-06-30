package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendRuleEvaluationDecisionDTO;
import com.wind.funds.wallet.model.request.EvaluateSpendRuleRequest;
import org.jspecify.annotations.NonNull;

/**
 * Spend Rule 规则评估应用服务。
 *
 * <p>职责：在现有支出控制准入前，对单条已发布 Spend Rule 做只读评估并返回决策证据候选。
 * 当前最小切片支持单笔金额限额、周期金额可用额度、周期次数限额和 MCC 黑白名单。</p>
 *
 * <p>边界：本服务不记录决策记录、不写控制额度变动流水、不创建资金交易、route、posting、
 * LedgerEntry 或账本投影；调用方仍需将最终决策证据交给准入服务固化。</p>
 *
 * @author Codex
 * @date 2026-06-30
 */
public interface SpendRuleEvaluationApplicationService {

    /**
     * 评估单条已发布 Spend Rule。
     *
     * @param request 规则评估请求
     * @return 规则评估决策
     */
    @NonNull SpendRuleEvaluationDecisionDTO evaluate(@NonNull EvaluateSpendRuleRequest request);
}
