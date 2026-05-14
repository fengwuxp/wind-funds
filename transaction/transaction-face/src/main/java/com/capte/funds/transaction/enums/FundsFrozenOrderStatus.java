package com.capte.funds.transaction.enums;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资金冻结单状态。
 *
 * @author Codex
 * @date 2026-05-07
 */
@AllArgsConstructor
@Getter
public enum FundsFrozenOrderStatus implements DescriptiveEnum {

    FROZEN("已冻结"),
    PARTIALLY_RELEASED("部分释放"),
    RELEASED("已释放"),
    PARTIALLY_CONSUMED("部分消耗"),
    CONSUMED("已消耗"),
    EXPIRED("已过期"),
    CLOSED("已关闭");

    private final String desc;
}
