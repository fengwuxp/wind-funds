package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益组件账务效果。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitLedgerEffect implements DescriptiveEnum {

    NO_LEDGER("不生成账务分录"),

    POSTING_REQUIRED("需要生成账务分录"),

    HOLD_ONLY("仅占用"),

    RELEASE_ONLY("仅释放占用"),

    REVERSAL_REQUIRED("需要冲回"),

    PROJECTION_ONLY("仅投影展示");

    private final String desc;
}
