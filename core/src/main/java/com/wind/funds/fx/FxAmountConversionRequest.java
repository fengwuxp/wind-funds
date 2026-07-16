package com.wind.funds.fx;

import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;

import java.math.RoundingMode;

/**
 * 外汇金额换算请求。
 *
 * @author wuxp
 * @date 2026-07-15
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class FxAmountConversionRequest {

    /**
     * 源金额。
     */
    @NotNull
    private Money sourceAmount;

    /**
     * 目标币种。
     */
    @NotNull
    private CurrencyIsoCode targetCurrency;

    /**
     * 上层业务已确认的应用汇率；与来源价格类型互斥。
     */
    private @Nullable FxAppliedRate appliedRate;

    /**
     * 来源价格类型；跨币种且未提供应用汇率时必填。
     */
    private @Nullable FxPriceType priceType;

    /**
     * 目标金额舍入模式。
     */
    @NotNull
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
}
