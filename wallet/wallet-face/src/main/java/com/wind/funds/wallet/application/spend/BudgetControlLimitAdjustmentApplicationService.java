package com.wind.funds.wallet.application.spend;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.wallet.model.dto.BudgetControlLimitAdjustmentResultDTO;
import com.wind.funds.wallet.model.request.AdjustBudgetControlLimitRequest;
import org.jspecify.annotations.NonNull;

/**
 * 预算控制额度调整应用服务。
 *
 * <p>职责：把支出控制范围和 Spend Rule 的额度调整记录为控制额度变动事实，并返回由控制额度变动
 * 派生的预算控制投影。</p>
 *
 * <p>边界：本服务不创建资金交易、route snapshot、账本交易、账目分录、余额投影或
 * 交易投影；预算额度调整不是资金价值转移，也不把支出控制范围重新打开成账本主体。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
public interface BudgetControlLimitAdjustmentApplicationService {

    /**
     * 调整预算控制额度。
     *
     * @param request 预算控制额度调整请求
     * @param operator 操作者
     * @return 预算控制额度调整结果
     */
    @NonNull BudgetControlLimitAdjustmentResultDTO adjustLimit(@NonNull AdjustBudgetControlLimitRequest request,
                                                               @NonNull WindOperator operator);
}
