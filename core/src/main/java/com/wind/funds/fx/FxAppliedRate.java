package com.wind.funds.fx;

import com.wind.common.exception.AssertUtils;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

/**
 * 金额换算实际采用的汇率事实。
 *
 * <p>rate 表示一单位源币可兑换的目标币数量。该对象承载上层业务已确认的最终价格，
 * 但不表达报价有效期、费用、加点依据或换汇执行结果。</p>
 *
 * <p>rateId 是本次应用汇率或报价的可选引用标识；没有独立汇率标识的场景由上层业务事实负责追溯。
 * 同币种换算固定为 1。</p>
 *
 * @param rateId         应用汇率或报价引用标识，可空
 * @param sourceCurrency 源币种
 * @param targetCurrency 目标币种
 * @param rate           一单位源币可兑换的目标币数量
 * @author wuxp
 * @date 2026-07-15
 */
@Builder
public record FxAppliedRate(@Nullable String rateId,
                            @NonNull CurrencyIsoCode sourceCurrency,
                            @NonNull CurrencyIsoCode targetCurrency,
                            @NonNull BigDecimal rate) {

    private static final int MAX_INTEGER_DIGITS = 10;

    private static final int MAX_FRACTION_DIGITS = 8;

    public FxAppliedRate {
        AssertUtils.notNull(sourceCurrency, "应用汇率源币种不能为空");
        AssertUtils.notNull(targetCurrency, "应用汇率目标币种不能为空");
        AssertUtils.isTrue(sourceCurrency != CurrencyIsoCode.UNKNOWN, "应用汇率源币种不能为 UNKNOWN");
        AssertUtils.isTrue(targetCurrency != CurrencyIsoCode.UNKNOWN, "应用汇率目标币种不能为 UNKNOWN");
        AssertUtils.notNull(rate, "应用汇率不能为空");
        AssertUtils.isTrue(rate.compareTo(BigDecimal.ZERO) > 0, "应用汇率必须大于 0");
        validateSupportedPrecision(rate);
        if (sourceCurrency == targetCurrency) {
            AssertUtils.isTrue(rate.compareTo(BigDecimal.ONE) == 0, "同币种应用汇率必须等于 1");
        }
    }

    /**
     * 校验应用汇率可以被账本交易和分录无损保存。
     *
     * @param rate 应用汇率
     */
    public static void validateSupportedPrecision(@NonNull BigDecimal rate) {
        AssertUtils.notNull(rate, "应用汇率不能为空");
        BigDecimal normalizedRate = rate.stripTrailingZeros();
        int integerDigits = Math.max(normalizedRate.precision() - normalizedRate.scale(), 0);
        int fractionDigits = Math.max(normalizedRate.scale(), 0);
        AssertUtils.isTrue(integerDigits <= MAX_INTEGER_DIGITS && fractionDigits <= MAX_FRACTION_DIGITS,
                "应用汇率最多支持 10 位整数和 8 位小数");
    }
}
