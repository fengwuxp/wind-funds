package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 挂载范围类型。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleScopeType implements DescriptiveEnum {

    PAYMENT_INSTRUMENT("支付工具"),
    SPEND_CONTROL_SCOPE("支出控制范围"),
    FUNDING_ACCOUNT("资金账户"),
    CREDIT_ACCOUNT("信用账户"),
    BUSINESS_SCENE("业务场景"),
    ACCOUNT_HIERARCHY("账户层级或使用主体控制范围");

    private final String desc;
}
