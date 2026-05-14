package com.capte.funds.fx;

import com.wind.integration.funds.fx.ExchangeRateType;
import com.wind.integration.funds.fx.FxRate;
import com.wind.integration.funds.fx.FxRateProvider;
import com.wind.integration.funds.fx.FxRequest;
import com.wind.integration.funds.fx.FxResult;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFxServiceImplTests {

    @Test
    void testConvertShouldUseTargetPerSourceRateDirection() {
        DefaultFxServiceImpl fxService = new DefaultFxServiceImpl(unsupportedProvider());

        FxResult result = fxService.convert(new FxRequest()
                .setSourceAmount(Money.immutable(10_000L, CurrencyIsoCode.USD))
                .setTargetCurrency(CurrencyIsoCode.CNY)
                .setRate(new BigDecimal("7.200000"))
                .setRateType(ExchangeRateType.ASK)
                .setRoundingMode(RoundingMode.HALF_UP));

        assertThat(result.getTargetAmount()).isEqualTo(Money.immutable(72_000L, CurrencyIsoCode.CNY));
        assertThat(result.getRate()).isEqualByComparingTo("7.200000");
        assertThat(result.getCurrencyPair()).isEqualTo("CNY/USD");
        assertThat(result.getRawResult()).isEqualByComparingTo("720.00000000");
    }

    @Test
    void testConvertShouldUseProviderRateAndCarryRateId() {
        AtomicReference<CurrencyIsoCode> sourceCurrency = new AtomicReference<>();
        AtomicReference<CurrencyIsoCode> targetCurrency = new AtomicReference<>();
        AtomicReference<ExchangeRateType> rateType = new AtomicReference<>();
        DefaultFxServiceImpl fxService = new DefaultFxServiceImpl((source, target, type) -> {
            sourceCurrency.set(source);
            targetCurrency.set(target);
            rateType.set(type);
            return FxRate.builder()
                    .rateId("FXR_0001")
                    .ask(new BigDecimal("0.920000"))
                    .build();
        });

        FxResult result = fxService.convert(new FxRequest()
                .setSourceAmount(Money.immutable(10_000L, CurrencyIsoCode.USD))
                .setTargetCurrency(CurrencyIsoCode.EUR)
                .setRateType(ExchangeRateType.ASK)
                .setRoundingMode(RoundingMode.HALF_UP));

        assertThat(sourceCurrency.get()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(targetCurrency.get()).isEqualTo(CurrencyIsoCode.EUR);
        assertThat(rateType.get()).isEqualTo(ExchangeRateType.ASK);
        assertThat(result.getTargetAmount()).isEqualTo(Money.immutable(9_200L, CurrencyIsoCode.EUR));
        assertThat(result.getRateId()).isEqualTo("FXR_0001");
    }

    @Test
    void testConvertShouldReturnSameAmountForSameCurrency() {
        Money sourceAmount = Money.immutable(12_345L, CurrencyIsoCode.USD);
        DefaultFxServiceImpl fxService = new DefaultFxServiceImpl(unsupportedProvider());

        FxResult result = fxService.convert(new FxRequest()
                .setSourceAmount(sourceAmount)
                .setTargetCurrency(CurrencyIsoCode.USD)
                .setRateType(ExchangeRateType.ASK));

        assertThat(result.getTargetAmount()).isEqualTo(sourceAmount);
        assertThat(result.getRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.getCurrencyPair()).isEqualTo("USD/USD");
        assertThat(result.getRawResult()).isEqualByComparingTo("123.45");
    }

    private static FxRateProvider unsupportedProvider() {
        return (sourceCurrency, targetCurrency, rateType) -> {
            throw new UnsupportedOperationException("provider should not be called");
        };
    }
}
