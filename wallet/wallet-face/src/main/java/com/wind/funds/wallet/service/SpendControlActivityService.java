package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendControlActivityDTO;
import com.wind.funds.wallet.model.query.SpendControlActivityQuery;
import com.wind.funds.wallet.model.request.RecordSpendControlActivityRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 控制额度变动流水基础服务。
 *
 * <p>职责：封装控制额度变动流水的基础持久化、按主键读取、按幂等流水读取和分页查询能力。</p>
 *
 * <p>命名：产品语义为 SpendControlMovement；当前接口名保留 Activity 是兼容既有代码和表结构。</p>
 *
 * <p>边界：本服务不校验控制活动业务不变量，不计算预算控制投影，不创建资金交易、route、
 * 账本交易、账目分录或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendControlActivityService {

    /**
     * 创建控制额度变动流水。
     *
     * @param request 控制活动记录请求
     * @return 控制活动主键
     */
    @NonNull Long createSpendControlActivity(@NonNull RecordSpendControlActivityRequest request);

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
}
