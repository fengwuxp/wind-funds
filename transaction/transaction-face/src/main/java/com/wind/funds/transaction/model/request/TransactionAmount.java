package com.wind.funds.transaction.model.request;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.fx.FxAmountConversionResult;
import com.wind.funds.fx.FxAppliedRate;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 交易金额。
 *
 * <p>用于承载业务原币金额到本次交易主金额的已确认金额事实。该对象不获取汇率、不决定是否换汇，
 * 也不表达下层账本实现细节。</p>
 *
 * @author wuxp
 * @date 2026-05-16 12:47
 */
@Getter
public class TransactionAmount {

    @Schema(description = "本次交易主金额")
    @NonNull
    private final Money amount;

    @Schema(description = "业务原币金额")
    @NonNull
    private final Money originalAmount;

    @Schema(description = "originalAmount -> amount 的汇率")
    @NonNull
    private final BigDecimal exchangeRate;

    private TransactionAmount(@NonNull Money amount,
                              @NonNull Money originalAmount,
                              @NonNull BigDecimal exchangeRate) {
        AssertUtils.notNull(amount, "transactionAmount.amount must not be null");
        AssertUtils.notNull(originalAmount, "transactionAmount.originalAmount must not be null");
        AssertUtils.notNull(exchangeRate, "transactionAmount.exchangeRate must not be null");
        AssertUtils.isTrue(amount.getAmount() > 0, "transactionAmount.amount must be greater than 0");
        AssertUtils.isTrue(originalAmount.getAmount() > 0, "transactionAmount.originalAmount must be greater than 0");
        AssertUtils.isTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0, "transactionAmount.exchangeRate must be greater than 0");
        FxAppliedRate.validateSupportedPrecision(exchangeRate);
        if (amount.getCurrency() == originalAmount.getCurrency()) {
            AssertUtils.isTrue(amount.getAmount() == originalAmount.getAmount(),
                    "transactionAmount.originalAmount must equal amount for same currency");
            AssertUtils.isTrue(exchangeRate.compareTo(BigDecimal.ONE) == 0,
                    "transactionAmount.exchangeRate must be 1 for same currency");
        }
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.exchangeRate = exchangeRate;
    }

    /**
     * 构造同币种交易金额。
     *
     * @param amount 本次交易主金额
     * @return 交易金额
     */
    public static TransactionAmount sameCurrency(@NonNull Money amount) {
        return new TransactionAmount(amount, amount, BigDecimal.ONE);
    }

    /**
     * 构造跨币种交易金额，并根据已确认的主金额和原币金额补齐隐含汇率。
     *
     * <p>仅用于外部授权或通道回调已经同时给出两套金额，但未单独返回汇率的场景；
     * 不得把该方法当作交易层自动换汇能力。</p>
     *
     * @param amount         本次交易主金额
     * @param originalAmount 业务原币金额
     * @return 交易金额
     */
    public static TransactionAmount converted(@NonNull Money amount,
                                              @NonNull Money originalAmount) {
        return converted(amount, originalAmount, calculateExchangeRate(amount, originalAmount));
    }

    /**
     * 构造跨币种交易金额。
     *
     * @param amount         本次交易主金额
     * @param originalAmount 业务原币金额
     * @param exchangeRate   originalAmount -> amount 的汇率
     * @return 交易金额
     */
    public static TransactionAmount converted(@NonNull Money amount,
                                              @NonNull Money originalAmount,
                                              @NonNull BigDecimal exchangeRate) {
        return new TransactionAmount(amount, originalAmount, exchangeRate);
    }

    /**
     * 根据外汇金额换算结果构造交易金额。
     *
     * @param result 外汇金额换算结果
     * @return 交易金额
     */
    public static TransactionAmount converted(@NonNull FxAmountConversionResult result) {
        AssertUtils.notNull(result, "fxAmountConversionResult must not be null");
        FxAppliedRate appliedRate = result.appliedRate();
        return converted(result.targetAmount(), result.sourceAmount(), appliedRate.rate());
    }

    private static BigDecimal calculateExchangeRate(@NonNull Money amount,
                                                    @NonNull Money originalAmount) {
        AssertUtils.notNull(amount, "transactionAmount.amount must not be null");
        AssertUtils.notNull(originalAmount, "transactionAmount.originalAmount must not be null");
        AssertUtils.isTrue(amount.getAmount() > 0, "transactionAmount.amount must be greater than 0");
        AssertUtils.isTrue(originalAmount.getAmount() > 0, "transactionAmount.originalAmount must be greater than 0");
        return amount.fen2Yuan()
                .divide(originalAmount.fen2Yuan(), 8, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }
}
