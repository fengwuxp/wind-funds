package com.wind.funds.fx;

import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 汇率请求参数
 *
 * @author wuxp
 * @date 2026-04-16 16:09
 **/
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class FxRequest {

    /**
     * 源金额
     */
    @NonNull
    private Money sourceAmount;

    /**
     * 目标币种（例如 USD）
     */
    @NonNull
    private CurrencyIsoCode targetCurrency;

    /**
     * 指定汇率（可选）不传则走汇率服务
     */
    private BigDecimal rate;

    /**
     * 是否使用买入价 / 卖出价
     */
    private ExchangeRateType rateType;

    /**
     * 舍入模式
     */
    private RoundingMode roundingMode = RoundingMode.HALF_UP;

}
