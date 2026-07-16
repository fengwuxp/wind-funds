package com.wind.funds.fx;

import com.wind.common.exception.AssertUtils;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;

/**
 * 外汇金额换算结果。
 *
 * @param sourceAmount 源金额
 * @param targetAmount 目标金额
 * @param appliedRate  本次计算实际采用的汇率
 * @author wuxp
 * @date 2026-07-15
 */
public record FxAmountConversionResult(@NonNull Money sourceAmount,
                                       @NonNull Money targetAmount,
                                       @NonNull FxAppliedRate appliedRate) {

    public FxAmountConversionResult {
        AssertUtils.notNull(sourceAmount, "换算结果源金额不能为空");
        AssertUtils.notNull(targetAmount, "换算结果目标金额不能为空");
        AssertUtils.notNull(appliedRate, "换算结果应用汇率不能为空");
        AssertUtils.isTrue(sourceAmount.getAmount() > 0, "换算结果源金额必须大于 0");
        AssertUtils.isTrue(targetAmount.getAmount() > 0, "换算结果目标金额必须大于 0");
        AssertUtils.isTrue(appliedRate.sourceCurrency() == sourceAmount.getCurrency()
                        && appliedRate.targetCurrency() == targetAmount.getCurrency(),
                "换算结果应用汇率币种对必须与源金额和目标金额一致");
    }
}
