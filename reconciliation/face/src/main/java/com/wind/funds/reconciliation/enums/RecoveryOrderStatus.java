package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RecoveryOrderStatus implements DescriptiveEnum {

    CREATED("待追回"),

    PARTIALLY_RECOVERED("部分追回"),

    RECOVERED("已追回");

    private final String desc;
}
