package com.wind.funds.spec.transaction;

import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeSpecTests {

    /**
     * 场景：按费率计算不同精度币种的手续费。
     * 预期：计算结果始终以币种最小单位表达，不得被再次按主货币单位放大。
     */
    @ParameterizedTest
    @CsvSource({
            "CNY, 10000, 100",
            "KWD, 10000, 100",
            "JPY, 10000, 100"
    })
    void testCalculateRateFeeInMinorUnits(CurrencyIsoCode currency,
                                          long transactionAmount,
                                          long expectedFeeAmount) {
        FeeSpec feeSpec = FeeSpec.builder()
                .feeType("RATE_FEE")
                .feeRate(new BigDecimal("0.01"))
                .build();

        Money fee = feeSpec.calculateFee(Money.immutable(transactionAmount, currency));

        assertThat(fee).isEqualTo(Money.immutable(expectedFeeAmount, currency));
    }

    /**
     * 场景：费率手续费计算结果包含半个最小货币单位。
     * 预期：手续费按 HALF_UP 舍入到整数最小货币单位。
     */
    @Test
    void testRoundRateFeeHalfUpToMinorUnit() {
        FeeSpec feeSpec = FeeSpec.builder()
                .feeType("RATE_FEE")
                .feeRate(new BigDecimal("0.015"))
                .build();

        Money fee = feeSpec.calculateFee(Money.immutable(100L, CurrencyIsoCode.CNY));

        assertThat(fee).isEqualTo(Money.immutable(2L, CurrencyIsoCode.CNY));
    }

    /**
     * 场景：固定手续费配置为负数。
     * 预期：手续费规范拒绝非法金额，不把负数传入后续资金路径。
     */
    @Test
    void testCalculateFeeShouldRejectNegativeFixedFee() {
        FeeSpec feeSpec = FeeSpec.builder()
                .fixedFee(-1)
                .build();

        assertThatThrownBy(() -> feeSpec.calculateFee(Money.immutable(100L, CurrencyIsoCode.CNY)))
                .hasMessageContaining("固定手续费不能小于 0");
    }

    /**
     * 场景：手续费费率配置为负数。
     * 预期：手续费规范拒绝会产生反向手续费的非法费率。
     */
    @Test
    void testCalculateFeeShouldRejectNegativeRate() {
        FeeSpec feeSpec = FeeSpec.builder()
                .feeRate(new BigDecimal("-0.01"))
                .build();

        assertThatThrownBy(() -> feeSpec.calculateFee(Money.immutable(100L, CurrencyIsoCode.CNY)))
                .hasMessageContaining("手续费费率不能小于 0");
    }

    /**
     * 场景：费率手续费上下限包含负数或最低值大于最高值。
     * 预期：手续费规范在计算前拒绝不成立的费率边界。
     */
    @Test
    void testCalculateFeeShouldRejectInvalidRateBounds() {
        Money transactionAmount = Money.immutable(100L, CurrencyIsoCode.CNY);

        assertThatThrownBy(() -> FeeSpec.builder()
                .minAmountWithRate(-1)
                .build()
                .calculateFee(transactionAmount))
                .hasMessageContaining("最低费率手续费不能小于 0");
        assertThatThrownBy(() -> FeeSpec.builder()
                .maxAmountWithRate(-1)
                .build()
                .calculateFee(transactionAmount))
                .hasMessageContaining("最高费率手续费不能小于 0");
        assertThatThrownBy(() -> FeeSpec.builder()
                .minAmountWithRate(2)
                .maxAmountWithRate(1)
                .build()
                .calculateFee(transactionAmount))
                .hasMessageContaining("最低费率手续费不能大于最高费率手续费");
    }
}
