package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 挂载解释状态。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleBindingExplanationStatus implements DescriptiveEnum {

    EFFECTIVE("当前有效"),
    NOT_YET_EFFECTIVE("尚未生效"),
    EXPIRED("已过期"),
    SUSPENDED("已暂停"),
    RETIRED("已退役");

    private final String desc;
}
