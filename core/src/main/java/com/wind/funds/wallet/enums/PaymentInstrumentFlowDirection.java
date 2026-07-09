package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付工具资金流向。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum PaymentInstrumentFlowDirection implements DescriptiveEnum {

    INBOUND("入向"),
    OUTBOUND("出向"),
    BIDIRECTIONAL("双向");

    private final String desc;
}
