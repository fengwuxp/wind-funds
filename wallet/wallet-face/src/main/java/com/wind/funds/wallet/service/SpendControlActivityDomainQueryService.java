package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 控制额度变动流水领域读服务。
 *
 * <p>职责：查询控制活动事实，并从控制活动事实派生只读预算控制投影。</p>
 *
 * <p>边界：本服务不写入控制活动，不重新执行 Spend Rule，不创建资金交易或账务事实。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendControlActivityDomainQueryService {

    /**
     * 查询控制额度变动流水。
     *
     * @param query 控制额度变动流水查询条件
     * @return 控制额度变动流水列表
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
