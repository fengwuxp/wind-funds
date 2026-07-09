package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付工具动作能力。
 *
 * @author Codex
 * @date 2026-06-16
 */
@AllArgsConstructor
@Getter
public enum PaymentInstrumentAction implements DescriptiveEnum {

    RECEIVE("收款"),
    PAY("付款"),
    AUTHORIZE("授权"),
    REFUND("退款"),
    WITHDRAW("提现/出款");

    private final String desc;
}
