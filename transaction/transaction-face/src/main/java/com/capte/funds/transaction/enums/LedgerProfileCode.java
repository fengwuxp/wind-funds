package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内置账务 Profile 编码。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum LedgerProfileCode implements DescriptiveEnum {

    /**
     * 普通资金账户。
     */
    FUNDING_BASIC("普通资金账户"),

    /**
     * 平台资金账户。
     */
    FUNDING_PLATFORM("平台资金账户"),

    /**
     * 信用额度账户。
     */
    CREDIT_BASIC("信用额度账户"),

    /**
     * 预算控制组。
     */
    BUDGET_BASIC("预算控制组");

    private final String desc;
}
