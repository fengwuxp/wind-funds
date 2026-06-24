package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionLogQuery;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
import com.wind.common.query.WindPagination;
import com.wind.common.query.WindQuery;
import com.wind.common.query.supports.QueryOrderField;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Spend Rule 决策记录服务。
 *
 * <p>职责：封装决策记录的写入、读取、查询和只读解释能力。</p>
 *
 * <p>命名：产品语义为 SpendRuleDecisionRecord；当前接口名保留 Log 是兼容既有代码和表结构。</p>
 *
 * <p>边界：本服务只固化规则决策事实，不执行规则脚本，不调整额度，
 * 不创建资金交易、route snapshot、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleDecisionLogService {

    /**
     * 记录 Spend Rule 决策记录。
     *
     * @param request 决策记录请求
     * @return 决策记录
     */
    @NonNull SpendRuleDecisionLogDTO recordDecision(@NonNull RecordSpendRuleDecisionLogRequest request);

    /**
     * 根据主键查询 Spend Rule 决策记录。
     *
     * @param id 主键
     * @return 决策记录
     */
    @NonNull SpendRuleDecisionLogDTO getDecisionLogById(@NonNull Long id);

    /**
     * 按租户和决策流水查找决策记录。
     *
     * @param tenantId 租户 ID
     * @param decisionSn 规则决策流水号
     * @return 决策记录，未找到时返回 null
     */
    @Nullable SpendRuleDecisionLogDTO findDecisionLog(@NonNull Long tenantId, @NonNull String decisionSn);

    /**
     * 查询 Spend Rule 决策记录。
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 决策记录分页结果
     */
    @NonNull WindPagination<SpendRuleDecisionLogDTO> queryDecisionLogs(
            @NonNull SpendRuleDecisionLogQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 查询 Spend Rule 决策记录。
     *
     * @param query 查询条件
     * @return 决策记录列表
     */
    @NonNull List<SpendRuleDecisionLogDTO> queryDecisions(@NonNull SpendRuleDecisionLogQuery query);

    /**
     * 解释 Spend Rule 决策事实。
     *
     * @param query 决策解释查询条件
     * @return 决策解释结果
     */
    @NonNull SpendRuleDecisionExplanationDTO explainDecision(@NonNull SpendRuleDecisionExplainQuery query);
}
