package com.wind.funds.reconciliation.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对账来源事实类型。
 */
@Getter
@AllArgsConstructor
public enum ReconciliationSourceType implements DescriptiveEnum {

    TRANSACTION("交易事实"),
    LEDGER("账本事实"),
    BALANCE("余额事实"),
    EXTERNAL_STATEMENT("外部流水"),
    SETTLEMENT_REPORT("清结算报表");

    private final String desc;
}
