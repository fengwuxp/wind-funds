package com.wind.funds.fx;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 汇率提供者
 *
 * @author wuxp
 * @date 2026-04-16 16:26
 **/
public interface FxRateProvider {

    /**
     * 获取汇率
     */
    @NonNull
    FxRate getRate(@NonNull CurrencyIsoCode sourceCurrency, @NonNull CurrencyIsoCode targetCurrency, @Nullable ExchangeRateType type);
}
