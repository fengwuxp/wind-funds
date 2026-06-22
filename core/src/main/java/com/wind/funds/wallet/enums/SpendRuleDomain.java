package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 规则域。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleDomain implements DescriptiveEnum {

    PAYMENT_INSTRUMENT("支付工具控制"),
    BUDGET_CONTROL("预算控制"),
    ACCOUNT("账户控制"),
    BUSINESS_SCENE("业务场景控制");

    private final String desc;
}
