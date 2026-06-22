package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Spend Rule 挂载冲突策略。
 *
 * @author Codex
 * @date 2026-06-22
 */
@AllArgsConstructor
@Getter
public enum SpendRuleConflictPolicy implements DescriptiveEnum {

    DENY_OVERRIDES("拒绝优先"),
    MOST_RESTRICTIVE("最严格策略优先"),
    FIRST_MATCH("首个命中优先");

    private final String desc;
}
