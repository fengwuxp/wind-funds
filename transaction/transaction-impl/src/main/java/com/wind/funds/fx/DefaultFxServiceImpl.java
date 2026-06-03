package com.wind.funds.fx;

import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author wuxp
 * @date 2026-04-16 16:14
 **/
@Slf4j
@Component
@AllArgsConstructor
public class DefaultFxServiceImpl implements FxService {

    private FxRateProvider provider;

    @Override
    public @NonNull FxResult convert(@NonNull FxRequest request) {
        if (request.getSourceAmount().getCurrency() == request.getTargetCurrency()) {
            BigDecimal sourceAmount = request.getSourceAmount().fen2Yuan();
            return FxResult.builder()
                    .sourceAmount(request.getSourceAmount())
                    .targetAmount(request.getSourceAmount())
                    .rate(BigDecimal.ONE)
                    .currencyPair(request.getTargetCurrency() + "/" + request.getSourceAmount().getCurrency())
                    .rateType(request.getRateType())
                    .rawResult(sourceAmount)
                    .build();
        }
        BigDecimal rate = request.getRate();
        String rateId = null;
        // 1. 获取汇率
        if (rate == null) {
            FxRate fxRate = provider.getRate(request.getSourceAmount().getCurrency(), request.getTargetCurrency(), request.getRateType());
            rate = fxRate.getAsk();
            rateId = fxRate.getRateId();
        }
        // 2. 转换金额（分 → 元）
        BigDecimal sourceAmount = request.getSourceAmount().fen2Yuan();
        // 3. 汇率方向：rate = target/source，例如 CNY/USD
        BigDecimal rawTarget = sourceAmount.multiply(rate).setScale(8, RoundingMode.HALF_UP);
        // 4. 舍入（目标币种 2 位）
        BigDecimal targetRounded = rawTarget.setScale(request.getTargetCurrency().getPrecision(), request.getRoundingMode());
        return FxResult.builder()
                .sourceAmount(request.getSourceAmount())
                .targetAmount(Money.immutable(targetRounded, request.getTargetCurrency()))
                .rate(rate)
                .currencyPair(request.getTargetCurrency() + "/" + request.getSourceAmount().getCurrency())
                .rateType(request.getRateType())
                .rateId(rateId)
                .rawResult(rawTarget)
                .build();
    }
}
