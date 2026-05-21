package com.wind.integration.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益退款处置。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitRefundDisposition implements DescriptiveEnum {

    REISSUE("返还或重发权益"),

    RELEASE_HOLD("释放权益占用"),

    VOID("作废权益"),

    NO_REFUND("不退权益"),

    REVERSE_SUBSIDY("冲回补贴"),

    RETAIN_SUBSIDY("保留补贴"),

    REDUCE_MERCHANT_RECEIVABLE("减少商户应收"),

    RESTORE_PREPAID_LIABILITY("恢复预付负债"),

    RELEASE_TO_INCOME_OR_BREAKAGE("释放为收入或沉淀收益");

    private final String desc;
}
