package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 定义状态。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleDefinitionStatus implements DescriptiveEnum {

    ACTIVE("生效"),
    DISABLED("停用");

    private final String desc;
}
