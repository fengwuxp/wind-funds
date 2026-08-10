package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付工具绑定生命周期状态。
 *
 * @author Codex
 * @date 2026-07-15
 */
@Getter
@AllArgsConstructor
public enum PaymentInstrumentBindingStatus implements DescriptiveEnum {

    ACTIVE("有效"),

    SUSPENDED("已暂停");

    private final String desc;
}
