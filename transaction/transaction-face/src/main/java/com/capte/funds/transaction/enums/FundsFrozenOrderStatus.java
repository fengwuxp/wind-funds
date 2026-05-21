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

    CREATED("已创建"),
    FROZEN("已冻结"),
    PARTIALLY_RELEASED("部分释放"),
    RELEASED("已释放"),
    EXPIRED("已过期"),
    CLOSED("已关闭");

    private final String desc;
}
