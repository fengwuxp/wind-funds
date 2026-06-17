package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益让利来源类型。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitFundingSourceType implements DescriptiveEnum {

    COUPON("优惠券"),

    VOUCHER("代金券"),

    PAYMENT_DISCOUNT("支付立减"),

    MERCHANT_DISCOUNT("商户让利"),

    PLATFORM_SUBSIDY("平台补贴"),

    PARTNER_SUBSIDY("合作方补贴"),

    STORED_VALUE_BENEFIT("储值或预付权益"),

    EXTERNAL_DECISION("外部权益决策"),

    MANUAL_ADJUSTMENT("人工权益调整");

    private final String desc;
}
