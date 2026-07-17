package com.wind.funds.spec.transaction;

import com.wind.common.exception.AssertUtils;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 资金交易手续费规范
 *
 * @author wuxp
 * @date 2026-04-17 11:22
 **/
@Getter
@Builder
public final class FeeSpec {

    /**
     * 手续费类型
     */
    private final String feeType;

    /**
     * 固定手续费，单位：币种最小货币单位
     */
    private final Integer fixedFee;

    /**
     * 手续费费率
     * 例如：0.01 表示 1%
     */
    private final BigDecimal feeRate;

    /**
     * 最高手续费金额 (通过费率计算的场景)，单位：币种最小货币单位
     */
    private final Integer maxAmountWithRate;

    /**
     * 最低手续费金额 (通过费率计算的场景)，单位：币种最小货币单位
     */
    private final Integer minAmountWithRate;

    /**
     * 计算手续费
     *
     * @param transactionAmount 交易金额
     * @return 按 HALF_UP 舍入到币种最小货币单位的手续费
     */
    @NonNull
    public Money calculateFee(Money transactionAmount) {
        AssertUtils.isTrue(fixedFee == null || fixedFee >= 0, "固定手续费不能小于 0");
        AssertUtils.isTrue(feeRate == null || feeRate.signum() >= 0, "手续费费率不能小于 0");
        AssertUtils.isTrue(minAmountWithRate == null || minAmountWithRate >= 0,
                "最低费率手续费不能小于 0");
        AssertUtils.isTrue(maxAmountWithRate == null || maxAmountWithRate >= 0,
                "最高费率手续费不能小于 0");
        AssertUtils.isTrue(minAmountWithRate == null || maxAmountWithRate == null
                        || minAmountWithRate <= maxAmountWithRate,
                "最低费率手续费不能大于最高费率手续费");
        Money result = Money.immutable(fixedFee == null ? 0 : fixedFee, transactionAmount.getCurrency());
        if (feeRate != null) {
            BigDecimal fee = BigDecimal.valueOf(transactionAmount.getAmount()).multiply(feeRate);
            long feeAmount = fee.setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (minAmountWithRate != null) {
                feeAmount = Math.max(feeAmount, minAmountWithRate);
            }
            if (maxAmountWithRate != null) {
                feeAmount = Math.min(feeAmount, maxAmountWithRate);
            }
            return result.add(Money.immutable(feeAmount, transactionAmount.getCurrency()));
        }
        return result;
    }
}
