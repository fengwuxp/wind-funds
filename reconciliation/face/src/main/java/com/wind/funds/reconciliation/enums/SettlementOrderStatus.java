package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SettlementOrderStatus implements DescriptiveEnum {

    DRAFT("草稿"),
    REVIEWING("复核中"),
    APPROVED("已审批"),
    LOCKED("资金已锁定"),
    FAILED("锁定失败"),
    CANCELLED("已取消");

    private final String desc;
}
