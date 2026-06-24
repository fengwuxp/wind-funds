package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 控制额度变动流水服务。
 *
 * <p>职责：封装控制额度变动流水写入、读取、查询和预算控制投影能力。</p>
 *
 * <p>命名：产品语义为 SpendControlMovement；当前接口名保留 Activity 是兼容既有代码和表结构。</p>
 *
 * <p>边界：本服务不执行 Spend Rule，不创建资金交易、route、
 * 账本交易、账目分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendControlActivityService {

    /**
     * 记录控制额度变动流水。
     *
     * @param request 控制额度变动流水记录请求
     * @return 控制额度变动流水
     */
    @NonNull SpendControlActivityDTO recordActivity(@NonNull RecordSpendControlActivityRequest request);

    /**
     * 根据主键查询控制额度变动流水。
     *
     * @param id 主键
     * @return 控制活动记录
     */
    @NonNull SpendControlActivityDTO getSpendControlActivityById(@NonNull Long id);

    /**
     * 根据活动流水查找控制额度变动流水。
     *
     * @param tenantId 租户 ID
     * @param activitySn 控制活动流水号
     * @return 控制活动记录，不存在时返回 null
     */
    @Nullable SpendControlActivityDTO findSpendControlActivity(@NonNull Long tenantId,
                                                               @NonNull String activitySn);

    /**
     * 分页查询控制额度变动流水。
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 控制活动分页结果
     */
    @NonNull
    WindPagination<SpendControlActivityDTO> querySpendControlActivities(
            @NonNull SpendControlActivityQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

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
