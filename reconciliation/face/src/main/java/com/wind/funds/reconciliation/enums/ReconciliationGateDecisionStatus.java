package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账准入决策状态。
 *
 * <p>职责：表达清算、结算或出款消费方是否可以继续推进。</p>
 *
 * <p>边界：状态只代表准入判断结果，不表达清算批次、结算单、出款单或账务事实生命周期。</p>
 */
@Getter
@AllArgsConstructor
public enum ReconciliationGateDecisionStatus implements DescriptiveEnum {

    /**
     * 未发现相关阻断差错，准入通过。
     */
    PASSED("通过"),

    /**
     * 存在未闭环或未对平差错，必须阻断。
     */
    BLOCKED("阻断");

    private final String desc;
}
