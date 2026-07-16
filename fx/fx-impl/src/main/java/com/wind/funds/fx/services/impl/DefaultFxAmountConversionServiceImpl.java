package com.wind.funds.fx.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.fx.FxAmountConversionRequest;
import com.wind.funds.fx.FxAmountConversionResult;
import com.wind.funds.fx.FxAmountConversionService;
import com.wind.funds.fx.FxAppliedRate;
import com.wind.funds.fx.FxPriceType;
import com.wind.funds.fx.FxRateProvider;
import com.wind.funds.fx.FxRateSnapshot;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 默认外汇金额换算服务实现。
 *
 * @author wuxp
 * @date 2026-07-15
 */
@Service
@RequiredArgsConstructor
public class DefaultFxAmountConversionServiceImpl implements FxAmountConversionService {

    private final FxRateProvider rateProvider;

    @Override
    public @NonNull FxAmountConversionResult calculate(@NonNull FxAmountConversionRequest request) {
        Money sourceAmount = request.getSourceAmount();
        CurrencyIsoCode targetCurrency = request.getTargetCurrency();
        AssertUtils.notNull(sourceAmount, "外汇金额换算源金额不能为空");
        AssertUtils.isTrue(sourceAmount.getAmount() > 0, "外汇金额换算源金额必须大于 0");
        AssertUtils.notNull(targetCurrency, "外汇金额换算目标币种不能为空");
        AssertUtils.notNull(request.getRoundingMode(), "外汇金额换算舍入模式不能为空");

        CurrencyIsoCode sourceCurrency = sourceAmount.getCurrency();
        AssertUtils.isTrue(sourceCurrency != CurrencyIsoCode.UNKNOWN, "外汇金额换算源币种不能为 UNKNOWN");
        AssertUtils.isTrue(targetCurrency != CurrencyIsoCode.UNKNOWN, "外汇金额换算目标币种不能为 UNKNOWN");
        FxAppliedRate appliedRate = resolveAppliedRate(request, sourceCurrency, targetCurrency);
        Money targetAmount = sourceAmount;
        if (sourceCurrency != targetCurrency) {
            BigDecimal targetAmountValue = sourceAmount.fen2Yuan()
                    .multiply(appliedRate.rate())
                    .setScale(targetCurrency.getPrecision(), request.getRoundingMode());
            try {
                long targetAmountInMinorUnits = targetAmountValue
                        .scaleByPowerOfTen(targetCurrency.getPrecision())
                        .longValueExact();
                AssertUtils.isTrue(targetAmountInMinorUnits > 0, "外汇金额换算目标金额必须大于 0");
                targetAmount = Money.immutable(targetAmountInMinorUnits, targetCurrency);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("外汇金额换算目标金额超出系统金额上限", exception);
            }
        }
        return new FxAmountConversionResult(sourceAmount, targetAmount, appliedRate);
    }

    private FxAppliedRate resolveAppliedRate(FxAmountConversionRequest request,
                                            CurrencyIsoCode sourceCurrency,
                                            CurrencyIsoCode targetCurrency) {
        FxAppliedRate appliedRate = request.getAppliedRate();
        FxPriceType priceType = request.getPriceType();
        AssertUtils.isTrue(appliedRate == null || priceType == null,
                "应用汇率和来源价格类型不能同时提供");
        if (appliedRate != null) {
            AssertUtils.isTrue(appliedRate.sourceCurrency() == sourceCurrency
                            && appliedRate.targetCurrency() == targetCurrency,
                    "应用汇率币种对必须与换算请求一致");
            return appliedRate;
        }
        if (sourceCurrency == targetCurrency) {
            AssertUtils.isTrue(priceType == null, "同币种换算不能指定来源价格类型");
            return FxAppliedRate.builder()
                    .sourceCurrency(sourceCurrency)
                    .targetCurrency(targetCurrency)
                    .rate(BigDecimal.ONE)
                    .build();
        }
        AssertUtils.notNull(priceType, "跨币种换算必须提供应用汇率或来源价格类型");
        FxRateSnapshot snapshot = rateProvider.getRateSnapshot(sourceCurrency, targetCurrency);
        AssertUtils.notNull(snapshot, "汇率提供方返回的来源价格快照不能为空");
        AssertUtils.isTrue(snapshot.sourceCurrency() == sourceCurrency
                        && snapshot.targetCurrency() == targetCurrency,
                "汇率快照币种对必须与换算请求一致");
        BigDecimal rate = switch (priceType) {
            case MID -> snapshot.mid();
            case BID -> snapshot.bid();
            case ASK -> snapshot.ask();
        };
        return FxAppliedRate.builder()
                .rateId(snapshot.snapshotId())
                .sourceCurrency(sourceCurrency)
                .targetCurrency(targetCurrency)
                .rate(rate)
                .build();
    }
}
