package com.wind.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权益金额闭合角色。
 */
@AllArgsConstructor
@Getter
public enum FundsBenefitAmountClosureRole implements DescriptiveEnum {

    ORDER_DISCOUNT_CLOSURE("参与订单正向抵扣闭合"),

    MERCHANT_RECEIVABLE_EFFECT("影响商户应收解释"),

    REFUND_DISPOSITION_EFFECT("参与退款或逆向处置解释"),

    VIEW_RECONCILIATION_ONLY("仅用于展示或对账解释");

    private final String desc;
}
