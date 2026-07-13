package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 挂载状态。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleBindingStatus implements DescriptiveEnum {

    ACTIVE("生效"),
    SUSPENDED("暂停"),
    RETIRED("退役");

    private final String desc;
}
