package com.wind.integration.funds.fx;

import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 汇率计算结果
 *
 * @author wuxp
 * @date 2026-04-16 16:10
 **/
@Builder
@Getter
public class FxResult {

    /**
     * 源金额（分）
     */
    private final Money sourceAmount;

    /**
     * 目标金额（分）
     */
    private final Money targetAmount;

    /**
     * 使用的汇率
     */
    private final BigDecimal rate;

    /**
     * 汇率对（USD/CNY）
     */
    private final String currencyPair;

    /**
     * 汇率类型
     */
    private final ExchangeRateType rateType;

    /**
     * 汇率表 id
     */
    private final String rateId;

    /**
     * 原始计算值（未舍入）
     */
    private final BigDecimal rawResult;
}
