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
    /**
     * @deprecated 冻结单只表达同主体 AVAILABLE 与 FROZEN 的控制事实，不再用冻结状态表达消费或扣划。
     */
    @Deprecated(since = "1.0.1", forRemoval = false)
    PARTIALLY_CONSUMED("部分消耗"),
    /**
     * @deprecated 冻结单只表达同主体 AVAILABLE 与 FROZEN 的控制事实，不再用冻结状态表达消费或扣划。
     */
    @Deprecated(since = "1.0.1", forRemoval = false)
    CONSUMED("已消耗"),
    EXPIRED("已过期"),
    CLOSED("已关闭");

    private final String desc;
}
