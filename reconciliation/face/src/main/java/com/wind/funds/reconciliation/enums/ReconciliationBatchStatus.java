package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账批次状态。
 *
 * <p>该状态只描述来源收集和运行结果固化进度，不表达对平结论或准入决策。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationBatchStatus implements DescriptiveEnum {

    CREATED("已创建"),
    DATA_COLLECTING("来源收集中"),
    DATA_READY("来源已冻结"),
    COMPLETED("运行结果已固化"),
    ABORTED("批次证据已终止");

    private final String desc;
}
