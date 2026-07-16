package com.wind.funds.fx;

import com.wind.common.enums.DescriptiveEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 汇率来源价格类型。
 *
 * @author wuxp
 * @date 2026-07-15
 */
@Getter
@AllArgsConstructor
public enum FxPriceType implements DescriptiveEnum {

    /**
     * 中间价。
     */
    MID("中间价"),

    /**
     * 汇率提供方买入源币价格。
     */
    BID("买入价"),

    /**
     * 汇率提供方卖出源币价格。
     */
    ASK("卖出价");

    private final String desc;
}
