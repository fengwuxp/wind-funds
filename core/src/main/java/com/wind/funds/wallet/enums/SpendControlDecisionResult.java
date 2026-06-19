package com.wind.funds.wallet.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支出控制决策结果。
 *
 * @author Codex
 * @date 2026-06-19
 */
@AllArgsConstructor
@Getter
public enum SpendControlDecisionResult implements DescriptiveEnum {

    PASSED("通过"),
    REJECTED("拒绝");

    private final String desc;
}
