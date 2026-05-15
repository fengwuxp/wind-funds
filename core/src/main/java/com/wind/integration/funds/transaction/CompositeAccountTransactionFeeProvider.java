package com.wind.integration.funds.transaction;

import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账户交易手续费提供者组合实现
 *
 * @author wuxp
 * @date 2026-04-22 09:42
 **/
@AllArgsConstructor
@Slf4j
@Primary
@Component
@NullMarked
public class CompositeAccountTransactionFeeProvider implements FundsAccountTransactionFeeProvider {

    private final List<FundsAccountTransactionFeeProvider> delegates;

    @Override
    public @Nullable FeeSpec apply(FundsAccountId accountId, String businessScene) {
        for (FundsAccountTransactionFeeProvider delegate : delegates) {
            if (delegate.supports(accountId)) {
                return delegate.apply(accountId, businessScene);
            }
        }
        return null;
    }

    @Override
    public boolean supports(FundsAccountId accountId) {
        return true;
    }
}
