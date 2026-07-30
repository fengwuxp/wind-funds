package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SettlementMode implements DescriptiveEnum {

    INTERMEDIARY_ACCOUNT("中间户模式"),
    FROZEN("冻结模式"),
    BILL("账单模式");

    private final String desc;
}
