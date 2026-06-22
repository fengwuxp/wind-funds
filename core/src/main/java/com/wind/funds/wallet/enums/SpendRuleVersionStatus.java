package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 版本状态。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleVersionStatus implements DescriptiveEnum {

    PUBLISHED("已发布"),
    RETIRED("已退役");

    private final String desc;
}
