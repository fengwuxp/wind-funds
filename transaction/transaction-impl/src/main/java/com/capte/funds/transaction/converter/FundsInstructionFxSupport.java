package com.capte.funds.transaction.converter;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.fx.FxRequest;
import com.wind.integration.funds.fx.FxResult;
import com.wind.integration.funds.fx.FxService;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;

/**
 * 交易请求金额到账本金金额的换汇支撑。
 */
final class FundsInstructionFxSupport {

    private final FundsAccountQueryService fundsAccountQueryService;

    private final FxService fxService;

    FundsInstructionFxSupport(@NonNull FundsAccountQueryService fundsAccountQueryService,
                               @NonNull FxService fxService) {
        this.fundsAccountQueryService = fundsAccountQueryService;
        this.fxService = fxService;
    }

    @NonNull ConvertedAmount convert(@NonNull Money sourceAmount,
                                     @NonNull FundsAccountId targetAccountId) {
        CurrencyIsoCode targetCurrency = fundsAccountQueryService.getAccount(targetAccountId).getCurrency();
        FxResult result = fxService.convert(new FxRequest()
                .setSourceAmount(sourceAmount)
                .setTargetCurrency(targetCurrency));
        AssertUtils.notNull(result.getTargetAmount(), "fx result targetAmount must not be null");
        AssertUtils.notNull(result.getRate(), "fx result rate must not be null");
        return new ConvertedAmount(result.getTargetAmount(), sourceAmount, result.getRate());
    }

    record ConvertedAmount(@NonNull Money amount,
                           @NonNull Money originalAmount,
                           @NonNull BigDecimal exchangeRate) {
    }
}
