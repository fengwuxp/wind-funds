package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益金额组件类型。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitComponentType implements DescriptiveEnum {

    MERCHANT_DISCOUNT("商户让利"),

    PLATFORM_SUBSIDY("平台补贴"),

    PLATFORM_DISPLAY_DISCOUNT("平台展示优惠"),

    VOUCHER_REDEEM("代金券核销"),

    PREPAID_REDEEM("储值或预付权益核销"),

    PARTNER_SUBSIDY("合作方补贴"),

    BENEFIT_REFUND("权益退款"),

    SUBSIDY_REVERSAL("补贴冲回"),

    VOUCHER_RESTORE("代金券恢复"),

    NON_REFUNDABLE_BENEFIT("不可退权益");

    private final String desc;
}
