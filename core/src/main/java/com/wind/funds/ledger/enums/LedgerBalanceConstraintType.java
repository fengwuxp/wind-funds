package com.wind.funds.ledger.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 本次分录余额约束。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum LedgerBalanceConstraintType implements DescriptiveEnum {

    PROFILE_DEFAULT("使用 LedgerProfile 默认负余额约束"),

    MUST_NOT_BE_NEGATIVE("本次更新后必须非负"),

    ALLOW_NEGATIVE("本次允许 profile 声明的负余额能力");

    private final String desc;
}
