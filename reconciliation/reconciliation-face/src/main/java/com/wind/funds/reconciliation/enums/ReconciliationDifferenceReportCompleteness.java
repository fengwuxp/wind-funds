package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账差异报告完整性。
 *
 * <p>职责：解释报告是否能基于既有差错、处理动作、重跑结果和准入 gate 证据形成完整视图。</p>
 *
 * <p>边界：完整性只服务报告展示和人工判断，不驱动补事实、清结算、出款或资金写入动作。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationDifferenceReportCompleteness implements DescriptiveEnum {

    /**
     * 报告视图完整。
     */
    COMPLETE("完整"),

    /**
     * 差错已有处理动作，但缺少动作、原始事实或处理证据。
     */
    INCOMPLETE_ACTION_EVIDENCE("处理证据不完整"),

    /**
     * 差错处于重跑相关状态，但缺少重跑结果。
     */
    MISSING_RERUN_RESULT("缺少重跑结果"),

    /**
     * 请求包含准入 gate 摘要，但当前差错缺少可查询的 gate 对象或决策结果。
     */
    MISSING_GATE_DECISION("缺少准入决策");

    private final String desc;
}
