package com.wind.funds.wallet.service;

import com.wind.funds.wallet.model.dto.SpendRuleDecisionRecordDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionRecordQuery;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
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
 * <p>边界：本服务只固化规则决策事实，不执行规则脚本，不调整额度，
 * 不创建资金交易、route snapshot、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-23
 */
public interface SpendRuleDecisionRecordService {

    /**
     * 记录 Spend Rule 决策记录。
     *
     * <p>该写入口只供受信任的规则或业务决策方调用。记录成功只形成可回读证据，
     * 不等于授权放行；wallet 准入仍会独立解析适用挂载并核对交易上下文。</p>
     *
     * @param request 决策记录请求
     * @return 决策记录
     */
    @NonNull SpendRuleDecisionRecordDTO recordDecision(@NonNull RecordSpendRuleDecisionRecordRequest request);

    /**
     * 按租户和决策流水查找决策记录。
     *
     * @param tenantId 租户 ID
     * @param decisionSn 规则决策流水号
     * @return 决策记录，未找到时返回 null
     */
    @Nullable SpendRuleDecisionRecordDTO findDecisionRecord(@NonNull Long tenantId, @NonNull String decisionSn);

    /**
     * 查询 Spend Rule 决策记录。
     *
     * @param query 查询条件
     * @param options 查询选项
     * @return 决策记录分页结果
     */
    @NonNull WindPagination<SpendRuleDecisionRecordDTO> queryDecisionRecords(
            @NonNull SpendRuleDecisionRecordQuery query,
            @NonNull WindQuery<? extends QueryOrderField> options);

    /**
     * 查询 Spend Rule 决策记录。
     *
     * @param query 查询条件
     * @return 决策记录列表
     */
    @NonNull List<SpendRuleDecisionRecordDTO> queryDecisions(@NonNull SpendRuleDecisionRecordQuery query);

    /**
     * 解释 Spend Rule 决策事实。
     *
     * @param query 决策解释查询条件
     * @return 决策解释结果
     */
    @NonNull SpendRuleDecisionExplanationDTO explainDecision(@NonNull SpendRuleDecisionExplainQuery query);
}
