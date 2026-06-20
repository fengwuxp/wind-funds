package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 支出控制活动应用服务。
 *
 * <p>职责：面向支付工具、Spend Rule 和预算控制场景，记录可审计、可幂等、
 * 可回放的控制活动，并从控制活动派生只读预算控制投影。</p>
 *
 * <p>边界：本服务不计算 Spend Rule，不创建资金交易、route snapshot、账本交易、
 * 账目分录、余额投影或交易投影；资金事实仍由交易和账本模块负责。</p>
 *
 * @author Codex
 * @date 2026-06-20
 */
public interface SpendControlActivityApplicationService {

    /**
     * 记录支出控制活动。
     *
     * @param request 支出控制活动记录请求
     * @return 支出控制活动记录
     */
    @NonNull SpendControlActivityDTO recordActivity(@NonNull RecordSpendControlActivityRequest request);

    /**
     * 查询支出控制活动。
     *
     * @param query 支出控制活动查询条件
     * @return 支出控制活动列表
     */
    @NonNull List<SpendControlActivityDTO> queryActivities(@NonNull SpendControlActivityQuery query);

    /**
     * 获取预算控制投影。
     *
     * @param query 预算控制投影查询条件
     * @return 预算控制投影
     */
    @NonNull BudgetControlProjectionDTO getBudgetControlProjection(@NonNull BudgetControlProjectionQuery query);
}
