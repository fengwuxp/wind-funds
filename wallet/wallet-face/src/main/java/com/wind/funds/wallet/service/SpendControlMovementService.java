package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
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
 * <p>边界：本服务不执行 Spend Rule，不创建资金交易、route、
 * 账本交易、账目分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendControlMovementService {

    /**
     * 记录控制额度变动流水。
     *
     * @param request 控制额度变动流水记录请求
     * @return 控制额度变动流水
     */
    @NonNull SpendControlMovementDTO recordMovement(@NonNull RecordSpendControlMovementRequest request);

    /**
     * 根据主键查询控制额度变动流水。
     *
     * @param id 主键
     * @return 控制额度变动记录
     */
    @NonNull SpendControlMovementDTO getSpendControlMovementById(@NonNull Long id);

    /**
     * 根据变动流水查找控制额度变动流水。
     *
     * @param tenantId 租户 ID
     * @param movementSn 控制额度变动流水号
     * @return 控制额度变动记录，不存在时返回 null
     */
    @Nullable SpendControlMovementDTO findSpendControlMovement(@NonNull Long tenantId,
                                                               @NonNull String movementSn);

    /**
     * 分页查询控制额度变动流水。
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 控制额度变动分页结果
     */
    @NonNull
    WindPagination<SpendControlMovementDTO> querySpendControlMovements(
            @NonNull SpendControlMovementQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 查询控制额度变动流水。
     *
     * @param query 控制额度变动流水查询条件
     * @return 控制额度变动流水列表
     */
    @NonNull List<SpendControlMovementDTO> queryMovements(@NonNull SpendControlMovementQuery query);

    /**
     * 获取预算控制投影。
     *
     * @param query 预算控制投影查询条件
     * @return 预算控制投影
     */
    @NonNull BudgetControlProjectionDTO getBudgetControlProjection(@NonNull BudgetControlProjectionQuery query);
}
