package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleBindingDTO;
import com.wind.funds.wallet.model.dto.SpendRuleBindingExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleBindingExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleBindingQuery;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.ResumeSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.RetireSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.SuspendSpendRuleBindingRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Spend Rule 挂载服务。
 *
 * <p>职责：封装规则挂载写入、读取、查询和只读解释能力。</p>
 *
 * <p>边界：本服务不计算规则、不记录决策记录、不调整控制额度，也不创建交易或账务事实。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleBindingService {

    /**
     * 创建 Spend Rule 挂载。
     *
     * @param request 挂载请求
     * @return 规则挂载
     */
    @NonNull SpendRuleBindingDTO createSpendRuleBinding(@NonNull CreateSpendRuleBindingRequest request);

    /**
     * 暂停 Spend Rule 挂载。
     *
     * <p>仅允许 ACTIVE -> SUSPENDED。重复暂停或退役后暂停均拒绝。</p>
     *
     * @param request 暂停请求
     */
    void suspendSpendRuleBinding(@NonNull SuspendSpendRuleBindingRequest request);

    /**
     * 恢复 Spend Rule 挂载。
     *
     * <p>仅允许 SUSPENDED -> ACTIVE。有效状态重复恢复或退役后恢复均拒绝。</p>
     *
     * @param request 恢复请求
     */
    void resumeSpendRuleBinding(@NonNull ResumeSpendRuleBindingRequest request);

    /**
     * 退役 Spend Rule 挂载。
     *
     * <p>仅允许 ACTIVE/SUSPENDED -> RETIRED。RETIRED 为终态，重复退役拒绝。</p>
     *
     * @param request 退役请求
     */
    void retireSpendRuleBinding(@NonNull RetireSpendRuleBindingRequest request);

    /**
     * 按租户和挂载流水查找 Spend Rule 挂载。
     *
     * @param tenantId 租户 ID
     * @param sn 规则挂载流水号
     * @return 规则挂载，未找到时返回 null
     */
    @Nullable SpendRuleBindingDTO findSpendRuleBinding(@NonNull Long tenantId, @NonNull String sn);

    /**
     * 查询 Spend Rule 挂载。
     *
     * @param query 挂载查询条件
     * @param options 查询选项
     * @return 规则挂载分页结果
     */
    @NonNull WindPagination<SpendRuleBindingDTO> querySpendRuleBindings(
            @NonNull SpendRuleBindingQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 查询 Spend Rule 挂载。
     *
     * @param query 挂载查询条件
     * @return 挂载列表
     */
    @NonNull List<SpendRuleBindingDTO> querySpendRuleBindings(@NonNull SpendRuleBindingQuery query);

    /**
     * 解释指定挂载在某个时间点的可用性。
     *
     * @param query 挂载解释查询条件
     * @return 挂载解释结果
     */
    @NonNull SpendRuleBindingExplanationDTO explainSpendRuleBinding(@NonNull SpendRuleBindingExplainQuery query);

    /**
     * 获取有效状态的 Spend Rule 挂载。
     *
     * @param tenantId 租户 ID
     * @param sn 规则挂载流水号
     * @return 有效状态规则挂载
     */
    @NonNull SpendRuleBindingDTO getActiveSpendRuleBinding(@NonNull Long tenantId, @NonNull String sn);
}
