package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益部分退款分摊策略。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitPartialRefundStrategy implements DescriptiveEnum {

    ORIGINAL_SNAPSHOT("按原权益快照"),

    ITEM_LINE_BASED("按商品行"),

    PROPORTIONAL("按比例"),

    CASH_FIRST("现金优先"),

    BENEFIT_FIRST("权益优先"),

    NON_REFUNDABLE_BENEFIT_FIRST("不可退权益优先"),

    MANUAL_REVIEW("人工审核");

    private final String desc;
}
