package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益生命周期动作。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitLifecycleAction implements DescriptiveEnum {

    DECIDED("已决策"),

    HOLD("占用"),

    WRITE_OFF("核销"),

    RELEASE("释放"),

    REISSUE("返还"),

    VOID("作废"),

    REVERSAL("冲回");

    private final String desc;
}
