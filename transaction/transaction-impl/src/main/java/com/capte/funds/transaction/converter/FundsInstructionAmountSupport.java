package com.capte.funds.transaction.converter;

import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;

/**
 * 交易指令金额事实校验支撑。
 */
final class FundsInstructionAmountSupport {

    private final FundsAccountQueryService fundsAccountQueryService;

    FundsInstructionAmountSupport(@NonNull FundsAccountQueryService fundsAccountQueryService) {
        this.fundsAccountQueryService = fundsAccountQueryService;
    }

    @NonNull ConvertedAmount fromTransactionAmount(@NonNull TransactionAmount transactionAmount,
                                                   @NonNull FundsAccountId targetAccountId) {
        AssertUtils.notNull(transactionAmount, "transactionAmount must not be null");
        validateCurrency(transactionAmount.getAmount(), targetAccountId, "transactionAmount.amount");
        return new ConvertedAmount(transactionAmount.getAmount(), transactionAmount.getOriginalAmount(),
                transactionAmount.getExchangeRate());
    }

    @NonNull ConvertedAmount sameCurrency(@NonNull Money amount,
                                          @NonNull FundsAccountId targetAccountId) {
        validateCurrency(amount, targetAccountId, "amount");
        return new ConvertedAmount(amount, amount, BigDecimal.ONE);
    }

    private void validateCurrency(@NonNull Money amount,
                                  @NonNull FundsAccountId targetAccountId,
                                  @NonNull String amountName) {
        AssertUtils.notNull(amount, "{} must not be null", amountName);
        CurrencyIsoCode targetCurrency = fundsAccountQueryService.getAccount(targetAccountId).getCurrency();
        AssertUtils.isTrue(amount.getCurrency() == targetCurrency,
                "{} currency must equal account currency, accountId = {}", amountName, targetAccountId);
    }

    record ConvertedAmount(@NonNull Money amount,
                           @NonNull Money originalAmount,
                           @NonNull BigDecimal exchangeRate) {
    }
}
