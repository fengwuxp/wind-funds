package com.wind.funds.fx;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

/**
 * 由使用方提供的汇率来源端口。
 *
 * <p>wind-funds 只定义来源价格快照契约，不关心具体汇率来源及其接入实现。来源不可用、超时或不支持币种对时，
 * 实现应抛出异常；换算服务原样传播异常，不重试、不降级，也不使用兜底价格。</p>
 *
 * @author wuxp
 * @date 2026-04-16 16:26
 **/
public interface FxRateProvider {

    /**
     * 获取指定币种对当前可用的来源价格快照。
     *
     * @param sourceCurrency 源币种
     * @param targetCurrency 目标币种
     * @return 来源价格快照
     */
    @NonNull
    FxRateSnapshot getRateSnapshot(@NonNull CurrencyIsoCode sourceCurrency,
                                   @NonNull CurrencyIsoCode targetCurrency);
}
