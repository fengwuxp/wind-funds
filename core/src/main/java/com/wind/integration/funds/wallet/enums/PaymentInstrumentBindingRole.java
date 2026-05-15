package com.wind.integration.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付工具绑定角色。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum PaymentInstrumentBindingRole implements DescriptiveEnum {

    FUNDING_SUBJECT("真实资金主体"),
    CREDIT_SUBJECT("信用控制主体"),
    BUDGET_SUBJECT("预算控制主体"),
    RECEIVE_SUBJECT("收款主体"),
    PAYMENT_SUBJECT("付款主体");

    private final String desc;
}
