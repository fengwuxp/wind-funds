package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
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
public interface SpendRuleAssignmentService {

    /**
     * 创建 Spend Rule 挂载。
     *
     * @param request 挂载请求
     * @return 规则挂载主键
     */
    @NonNull Long createAssignment(@NonNull AssignSpendRuleVersionRequest request);

    /**
     * 根据主键查询 Spend Rule 挂载。
     *
     * @param id 主键
     * @return 规则挂载
     */
    @NonNull SpendRuleAssignmentDTO getAssignmentById(@NonNull Long id);

    /**
     * 按租户和挂载流水查找 Spend Rule 挂载。
     *
     * @param tenantId 租户 ID
     * @param assignmentSn 规则挂载流水号
     * @return 规则挂载，未找到时返回 null
     */
    @Nullable SpendRuleAssignmentDTO findAssignment(@NonNull Long tenantId, @NonNull String assignmentSn);

    /**
     * 查询 Spend Rule 挂载。
     *
     * @param query 挂载查询条件
     * @param options 查询选项
     * @return 规则挂载分页结果
     */
    @NonNull WindPagination<SpendRuleAssignmentDTO> queryAssignments(
            @NonNull SpendRuleAssignmentQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 查询 Spend Rule 挂载。
     *
     * @param query 挂载查询条件
     * @return 挂载列表
     */
    @NonNull List<SpendRuleAssignmentDTO> queryAssignments(@NonNull SpendRuleAssignmentQuery query);

    /**
     * 解释指定挂载在某个时间点的可用性。
     *
     * @param query 挂载解释查询条件
     * @return 挂载解释结果
     */
    @NonNull SpendRuleAssignmentExplanationDTO explainAssignment(@NonNull SpendRuleAssignmentExplainQuery query);

    /**
     * 获取有效状态的 Spend Rule 挂载。
     *
     * @param tenantId 租户 ID
     * @param assignmentSn 规则挂载流水号
     * @return 有效状态规则挂载
     */
    @NonNull SpendRuleAssignmentDTO getActiveAssignment(@NonNull Long tenantId, @NonNull String assignmentSn);
}
