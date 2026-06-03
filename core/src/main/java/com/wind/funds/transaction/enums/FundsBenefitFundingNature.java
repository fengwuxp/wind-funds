package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益资金性质。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitFundingNature implements DescriptiveEnum {

    NO_FUNDS_TRANSFER("无资金转移"),

    MERCHANT_BORNE("商户承担"),

    PLATFORM_OWN_FUNDS("平台自有资金"),

    PREPAID_LIABILITY("预付或储值负债"),

    PARTNER_FUNDED("合作方出资"),

    USER_BENEFIT_BALANCE("用户权益余额"),

    UNKNOWN_PENDING_CONFIRMATION("待专业口径确认");

    private final String desc;
}
