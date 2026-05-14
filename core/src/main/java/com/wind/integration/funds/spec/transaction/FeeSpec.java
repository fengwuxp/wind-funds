package com.wind.integration.funds.spec.transaction;

import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;

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
    private final DefaultFeeType feeType;

    /**
     * 固定手续费
     */
    private final Integer fixedFee;

    /**
     * 手续费费率
     * 例如：0.01 表示 1%
     */
    private final BigDecimal feeRate;

    /**
     * 最高手续费金额 (通过费率计算的场景)，单位：分
     */
    private final Integer maxAmountWithRate;

    /**
     * 最低手续费金额 (通过费率计算的场景)，单位：分
     */
    private final Integer minAmountWithRate;

    /**
     * 计算手续费
     *
     * @param transactionAmount 交易金额
     * @return 手续费，若手续费为 0 则表示不需要收取
     */
    @NonNull
    public Money calculateFee(Money transactionAmount) {
        Money result = Money.immutable(fixedFee == null ? 0 : fixedFee, transactionAmount.getCurrency());
        if (feeRate != null) {
            BigDecimal fee = new BigDecimal(transactionAmount.getAmount()).multiply(feeRate);
            long feeAmount = Money.immutable(fee, transactionAmount.getCurrency()).getAmount();
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
