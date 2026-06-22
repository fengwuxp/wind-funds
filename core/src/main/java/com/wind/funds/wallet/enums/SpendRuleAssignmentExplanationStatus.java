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
public enum SpendRuleAssignmentExplanationStatus implements DescriptiveEnum {

    EFFECTIVE("当前有效"),
    NOT_YET_EFFECTIVE("尚未生效"),
    EXPIRED("已过期"),
    DISABLED("已停用");

    private final String desc;
}
