package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SettlementDestination implements DescriptiveEnum {

    INTERNAL_ACCOUNT("内部账户"),

    EXTERNAL_ENDPOINT("外部收款端点");

    private final String desc;
}
