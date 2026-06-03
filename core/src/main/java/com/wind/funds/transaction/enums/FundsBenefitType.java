package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益类型。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitType implements DescriptiveEnum {

    MERCHANT_COUPON("商户优惠券"),

    PLATFORM_COUPON("平台优惠券"),

    VOUCHER("代金券"),

    PREPAID_VOUCHER("储值或预付代金券"),

    GIFT_CARD("礼品卡"),

    PARTNER_SUBSIDY("合作方补贴"),

    MANUAL_BENEFIT("人工权益");

    private final String desc;
}
