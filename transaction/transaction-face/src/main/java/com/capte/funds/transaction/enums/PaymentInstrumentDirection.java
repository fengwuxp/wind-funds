package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付工具使用方向。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum PaymentInstrumentDirection implements DescriptiveEnum {

    RECEIVE("收款"),
    PAYMENT("付款"),
    BOTH("收付款");

    private final String desc;
}
