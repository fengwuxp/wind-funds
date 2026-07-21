package com.wind.funds.wallet.application.spend;

import com.wind.funds.wallet.model.dto.SpendRuleEvaluationDecisionDTO;
import com.wind.funds.wallet.model.request.EvaluateSpendRuleRequest;
import org.jspecify.annotations.NonNull;

/**
 * Spend Rule 规则评估应用服务。
 *
 * <p>职责：在现有支出控制准入前，对单条已发布 Spend Rule 做只读评估并返回决策证据候选。
 * 当前最小切片支持单笔金额限额、周期金额可用额度、周期次数限额、滚动窗口次数、MCC、商户国家、
 * 卡数据输入能力、卡交易处理类型、商户标识、PAN 录入方式、POS 类别、CVV 必填、AVS 邮编校验结果、
 * 币种和本地授权时间窗口。每个 ruleSpec 只允许一个可执行控制项；多规则或复合控制裁决由上游合成，
 * 本服务不在 wallet 内部实现规则组合器。</p>
 * <p>金额口径：请求金额必须是调用方已归一后的本次评估金额。卡授权接入方如果同时保留 requested amount
 * 和 authorized amount，应在上游完成取舍后再调用本服务；本服务不从外部原始网络字段、退款、撤销或异步规则结果
 * 反推累计授权金额。</p>
 *
 * <p>边界：本服务不记录决策记录、不写控制额度变动流水、不创建资金交易、route、posting、
 * LedgerEntry 或账本投影；调用方仍需将最终决策证据交给准入服务固化。
 * 本服务不承接信用额度浮动、条件规则、授权 hold 配置、外部入金或街道地址原文规则。
 * 滚动窗口次数评估只读既有控制流水，不提供并发强一致授权拦截；
 * 强一致扣占必须由交易 / 准入编排在同一事务或锁定边界内完成。</p>
 *
 * @author Codex
 * @date 2026-06-30
 */
public interface SpendRuleEvaluationApplicationService {

    /**
     * 评估单条已发布 Spend Rule。
     *
     * @param request 规则评估请求
     * @return 规则评估决策
     */
    @NonNull SpendRuleEvaluationDecisionDTO evaluate(@NonNull EvaluateSpendRuleRequest request);
}
