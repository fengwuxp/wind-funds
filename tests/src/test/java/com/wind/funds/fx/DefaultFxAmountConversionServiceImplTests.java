package com.wind.funds.fx;

import com.wind.funds.fx.services.impl.DefaultFxAmountConversionServiceImpl;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFxAmountConversionServiceImplTests {

    private static final Instant OBSERVED_AT = Instant.parse("2026-07-15T08:00:00Z");

    private final AtomicInteger rateProviderCalls = new AtomicInteger();

    private final FxRateProvider rateProvider = (sourceCurrency, targetCurrency) -> {
        rateProviderCalls.incrementAndGet();
        return snapshot(sourceCurrency, targetCurrency, "7.10", "7.08", "7.12");
    };

    private final FxAmountConversionService service = new DefaultFxAmountConversionServiceImpl(rateProvider);

    @Test
    void testShouldCalculateWithExplicitAppliedRate() {
        FxAppliedRate appliedRate = appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "7.12");
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.CNY)
                .setAppliedRate(appliedRate);

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.sourceAmount()).isSameAs(request.getSourceAmount());
        assertThat(result.targetAmount().getAmount()).isEqualTo(71_200L);
        assertThat(result.appliedRate()).isSameAs(appliedRate);
        assertThat(result.appliedRate().rateId()).isNull();
        assertThat(rateProviderCalls).hasValue(0);
    }

    @ParameterizedTest
    @CsvSource({
            "MID, 7.10, 71000",
            "BID, 7.08, 70800",
            "ASK, 7.12, 71200"
    })
    void testShouldFetchSnapshotAndUseExplicitPriceType(FxPriceType priceType,
                                                     String expectedRate,
                                                     long expectedAmount) {
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.CNY)
                .setPriceType(priceType);

        FxAmountConversionResult result = service.calculate(request);
        TransactionAmount transactionAmount = TransactionAmount.converted(result);

        assertThat(result.targetAmount().getAmount()).isEqualTo(expectedAmount);
        assertThat(result.appliedRate().rate()).isEqualByComparingTo(expectedRate);
        assertThat(result.appliedRate().rateId()).isEqualTo("SNAPSHOT-001");
        assertThat(transactionAmount.getAmount()).isEqualTo(result.targetAmount());
        assertThat(transactionAmount.getOriginalAmount()).isEqualTo(result.sourceAmount());
        assertThat(transactionAmount.getExchangeRate()).isEqualByComparingTo(expectedRate);
        assertThat(rateProviderCalls).hasValue(1);
    }

    @Test
    void testShouldUseHalfUpRoundingByDefault() {
        FxAmountConversionRequest request = request(100L, CurrencyIsoCode.CNY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "1.005"));

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount().getAmount()).isEqualTo(101L);
    }

    @Test
    void testShouldUseRequestedRoundingMode() {
        FxAmountConversionRequest request = request(100L, CurrencyIsoCode.CNY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "1.005"))
                .setRoundingMode(RoundingMode.DOWN);

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount().getAmount()).isEqualTo(100L);
    }

    @Test
    void testShouldRejectTargetAmountOverflow() {
        FxAmountConversionRequest request = request(Long.MAX_VALUE, CurrencyIsoCode.CNY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "2"));

        assertThatThrownBy(() -> service.calculate(request))
                .hasMessageContaining("外汇金额换算目标金额超出系统金额上限");
    }

    @Test
    void testShouldRejectTargetAmountRoundedToZero() {
        FxAmountConversionRequest request = request(1L, CurrencyIsoCode.JPY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.JPY, "0.4"));

        assertThatThrownBy(() -> service.calculate(request))
                .hasMessageContaining("外汇金额换算目标金额必须大于 0");
    }

    @Test
    void testShouldConvertFromThreeDecimalCurrency() {
        FxAmountConversionRequest request = request(1_000L, CurrencyIsoCode.KWD, CurrencyIsoCode.USD)
                .setAppliedRate(appliedRate(CurrencyIsoCode.KWD, CurrencyIsoCode.USD, "3.25"));

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount()).isEqualTo(Money.immutable(325L, CurrencyIsoCode.USD));
    }

    @Test
    void testShouldRoundTargetThreeDecimalCurrency() {
        FxAmountConversionRequest request = request(100L, CurrencyIsoCode.KWD)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.KWD, "0.3075"));

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount()).isEqualTo(Money.immutable(308L, CurrencyIsoCode.KWD));
    }

    @Test
    void testShouldRoundTargetZeroDecimalCurrencyToWholeMinorUnit() {
        FxAmountConversionRequest request = request(100L, CurrencyIsoCode.JPY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.JPY, "150.5"));

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount()).isEqualTo(Money.immutable(151L, CurrencyIsoCode.JPY));
    }

    @Test
    void testShouldRejectUnknownSourceCurrency() {
        FxAmountConversionRequest request = request(100L, CurrencyIsoCode.UNKNOWN, CurrencyIsoCode.USD);

        assertThatThrownBy(() -> service.calculate(request))
                .hasMessageContaining("外汇金额换算源币种不能为 UNKNOWN");
    }

    @Test
    void testShouldRejectUnknownTargetCurrency() {
        assertThatThrownBy(() -> service.calculate(request(100L, CurrencyIsoCode.UNKNOWN)))
                .hasMessageContaining("外汇金额换算目标币种不能为 UNKNOWN");
    }

    @Test
    void testShouldRejectMismatchedAppliedRateCurrencyPair() {
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.CNY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.EUR, "0.92"));

        assertThatThrownBy(() -> service.calculate(request))
                .hasMessageContaining("应用汇率币种对必须与换算请求一致");
    }

    @Test
    void testShouldRequireAppliedRateOrPriceTypeForCrossCurrency() {
        assertThatThrownBy(() -> service.calculate(request(10_000L, CurrencyIsoCode.CNY)))
                .hasMessageContaining("跨币种换算必须提供应用汇率或来源价格类型");
        assertThat(rateProviderCalls).hasValue(0);
    }

    @Test
    void testShouldRejectAppliedRateAndPriceTypeTogether() {
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.CNY)
                .setAppliedRate(appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "7.12"))
                .setPriceType(FxPriceType.ASK);

        assertThatThrownBy(() -> service.calculate(request))
                .hasMessageContaining("应用汇率和来源价格类型不能同时提供");
        assertThat(rateProviderCalls).hasValue(0);
    }

    @Test
    void testShouldRejectPriceTypeForSameCurrency() {
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.USD)
                .setPriceType(FxPriceType.MID);

        assertThatThrownBy(() -> service.calculate(request))
                .hasMessageContaining("同币种换算不能指定来源价格类型");
        assertThat(rateProviderCalls).hasValue(0);
    }

    @Test
    void testShouldRejectMismatchedProviderSnapshotCurrencyPair() {
        FxRateProvider mismatchedProvider = (sourceCurrency, targetCurrency) ->
                snapshot(CurrencyIsoCode.USD, CurrencyIsoCode.EUR, "0.90", "0.89", "0.91");
        FxAmountConversionService mismatchedProviderService =
                new DefaultFxAmountConversionServiceImpl(mismatchedProvider);
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.CNY)
                .setPriceType(FxPriceType.MID);

        assertThatThrownBy(() -> mismatchedProviderService.calculate(request))
                .hasMessageContaining("汇率快照币种对必须与换算请求一致");
    }

    @Test
    void testShouldPropagateRateProviderFailureWithoutFallback() {
        IllegalStateException providerFailure = new IllegalStateException("FX provider unavailable");
        FxRateProvider unavailableProvider = (sourceCurrency, targetCurrency) -> {
            throw providerFailure;
        };
        FxAmountConversionService unavailableProviderService =
                new DefaultFxAmountConversionServiceImpl(unavailableProvider);
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.CNY)
                .setPriceType(FxPriceType.MID);

        assertThatThrownBy(() -> unavailableProviderService.calculate(request))
                .isSameAs(providerFailure);
    }

    @Test
    void testShouldUseIdentityRateForSameCurrency() {
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.USD);

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount()).isSameAs(request.getSourceAmount());
        assertThat(result.appliedRate().sourceCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(result.appliedRate().targetCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(result.appliedRate().rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(result.appliedRate().rateId()).isNull();
    }

    @Test
    void testShouldAllowExplicitIdentityRateForSameCurrency() {
        FxAppliedRate appliedRate = FxAppliedRate.builder()
                .rateId("QUOTE-001")
                .sourceCurrency(CurrencyIsoCode.USD)
                .targetCurrency(CurrencyIsoCode.USD)
                .rate(BigDecimal.ONE)
                .build();
        FxAmountConversionRequest request = request(10_000L, CurrencyIsoCode.USD)
                .setAppliedRate(appliedRate);

        FxAmountConversionResult result = service.calculate(request);

        assertThat(result.targetAmount()).isSameAs(request.getSourceAmount());
        assertThat(result.appliedRate()).isSameAs(appliedRate);
    }

    @Test
    void testShouldRejectNonPositiveAppliedRateAtConstruction() {
        assertThatThrownBy(() -> appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "0"))
                .hasMessageContaining("应用汇率必须大于 0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.123456789", "10000000000"})
    void testShouldRejectRateOutsideLedgerPrecision(String rate) {
        assertThatThrownBy(() -> appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, rate))
                .hasMessageContaining("应用汇率最多支持 10 位整数和 8 位小数");
        assertThatThrownBy(() -> TransactionAmount.converted(
                Money.immutable(100L, CurrencyIsoCode.CNY),
                Money.immutable(100L, CurrencyIsoCode.USD),
                new BigDecimal(rate)))
                .hasMessageContaining("应用汇率最多支持 10 位整数和 8 位小数");
    }

    @Test
    void testShouldRejectUnknownAppliedRateCurrencyAtConstruction() {
        assertThatThrownBy(() -> appliedRate(CurrencyIsoCode.UNKNOWN, CurrencyIsoCode.CNY, "1"))
                .hasMessageContaining("应用汇率源币种不能为 UNKNOWN");
    }

    @Test
    void testShouldRejectNonIdentityRateForSameCurrencyAtConstruction() {
        assertThatThrownBy(() -> appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.USD, "1.01"))
                .hasMessageContaining("同币种应用汇率必须等于 1");
    }

    @Test
    void testShouldRejectNonPositiveRateSnapshotAtConstruction() {
        assertThatThrownBy(() -> snapshot(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, "7.10", "0", "7.12"))
                .hasMessageContaining("汇率快照价格必须大于 0");
    }

    @ParameterizedTest
    @CsvSource({
            "7.10, 7.11, 7.12",
            "7.10, 7.08, 7.09"
    })
    void testShouldRejectCrossedRateSnapshotAtConstruction(String mid, String bid, String ask) {
        assertThatThrownBy(() -> snapshot(CurrencyIsoCode.USD, CurrencyIsoCode.CNY, mid, bid, ask))
                .hasMessageContaining("汇率快照价格必须满足 bid <= mid <= ask");
    }

    @Test
    void testShouldRejectMismatchedAppliedRateAtConversionResultConstruction() {
        Money sourceAmount = Money.immutable(10_000L, CurrencyIsoCode.USD);
        Money targetAmount = Money.immutable(9_200L, CurrencyIsoCode.EUR);
        FxAppliedRate mismatchedRate = appliedRate(CurrencyIsoCode.GBP, CurrencyIsoCode.CNY, "9.20");

        assertThatThrownBy(() -> new FxAmountConversionResult(sourceAmount, targetAmount, mismatchedRate))
                .hasMessageContaining("换算结果应用汇率币种对必须与源金额和目标金额一致");
    }

    @Test
    void testShouldRejectNonPositiveConversionResultAmounts() {
        FxAppliedRate rate = appliedRate(CurrencyIsoCode.USD, CurrencyIsoCode.EUR, "0.92");

        assertThatThrownBy(() -> new FxAmountConversionResult(
                Money.immutable(0L, CurrencyIsoCode.USD),
                Money.immutable(100L, CurrencyIsoCode.EUR),
                rate))
                .hasMessageContaining("换算结果源金额必须大于 0");
        assertThatThrownBy(() -> new FxAmountConversionResult(
                Money.immutable(100L, CurrencyIsoCode.USD),
                Money.immutable(0L, CurrencyIsoCode.EUR),
                rate))
                .hasMessageContaining("换算结果目标金额必须大于 0");
    }

    @Test
    void testShouldRejectSameCurrencyRateSnapshotAtConstruction() {
        assertThatThrownBy(() -> snapshot(CurrencyIsoCode.USD, CurrencyIsoCode.USD, "1", "1", "1"))
                .hasMessageContaining("汇率快照源币种与目标币种不能相同");
    }

    @Test
    void testShouldRejectUnknownRateSnapshotCurrencyAtConstruction() {
        assertThatThrownBy(() -> snapshot(CurrencyIsoCode.USD, CurrencyIsoCode.UNKNOWN, "1", "1", "1"))
                .hasMessageContaining("汇率快照目标币种不能为 UNKNOWN");
    }

    @Test
    void testShouldRequireRateSnapshotObservationTime() {
        assertThatThrownBy(() -> FxRateSnapshot.builder()
                .snapshotId("SNAPSHOT-001")
                .sourceCurrency(CurrencyIsoCode.USD)
                .targetCurrency(CurrencyIsoCode.CNY)
                .mid(new BigDecimal("7.10"))
                .bid(new BigDecimal("7.08"))
                .ask(new BigDecimal("7.12"))
                .build())
                .hasMessageContaining("汇率快照观测时间不能为空");
    }

    private static FxAmountConversionRequest request(long amount, CurrencyIsoCode targetCurrency) {
        return request(amount, CurrencyIsoCode.USD, targetCurrency);
    }

    private static FxAmountConversionRequest request(long amount,
                                                     CurrencyIsoCode sourceCurrency,
                                                     CurrencyIsoCode targetCurrency) {
        return new FxAmountConversionRequest()
                .setSourceAmount(Money.immutable(amount, sourceCurrency))
                .setTargetCurrency(targetCurrency);
    }

    private static FxAppliedRate appliedRate(CurrencyIsoCode sourceCurrency,
                                             CurrencyIsoCode targetCurrency,
                                             String rate) {
        return FxAppliedRate.builder()
                .sourceCurrency(sourceCurrency)
                .targetCurrency(targetCurrency)
                .rate(new BigDecimal(rate))
                .build();
    }

    private static FxRateSnapshot snapshot(CurrencyIsoCode sourceCurrency,
                                           CurrencyIsoCode targetCurrency,
                                           String mid,
                                           String bid,
                                           String ask) {
        return FxRateSnapshot.builder()
                .snapshotId("SNAPSHOT-001")
                .sourceCurrency(sourceCurrency)
                .targetCurrency(targetCurrency)
                .mid(new BigDecimal(mid))
                .bid(new BigDecimal(bid))
                .ask(new BigDecimal(ask))
                .observedAt(OBSERVED_AT)
                .build();
    }
}
