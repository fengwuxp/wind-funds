package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付工具绑定变更类型。
 *
 * @author Codex
 * @date 2026-05-20
 */
@AllArgsConstructor
@Getter
public enum PaymentInstrumentBindingChangeType implements DescriptiveEnum {

    CREATE("创建绑定"),

    UPDATE("更新绑定"),

    UNBIND("解除绑定");

    private final String desc;
}
