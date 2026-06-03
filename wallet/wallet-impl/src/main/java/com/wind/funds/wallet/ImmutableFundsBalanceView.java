package com.wind.funds.wallet;

import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

import java.util.Map;

/**
 * 不可变资金账户余额视图。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Getter
@Builder
public class ImmutableFundsBalanceView implements FundsAccountBalanceView {

    private final Long id;

    private final Long tenantId;

    private final FundsAccountId accountId;

    private final CurrencyIsoCode currency;

    private final Map<LedgerSubjectCode, LedgerBalanceBucket> balanceBuckets;

    @Override
    public Money getAvailableBalance() {
        return getBalance(LedgerSubjectCode.AVAILABLE);
    }

    @Override
    public @NonNull Money getFrozenBalance() {
        return getBalance(LedgerSubjectCode.FROZEN);
    }

    @Override
    public @NonNull Money getPendingBalance() {
        return getBalance(LedgerSubjectCode.AUTHORIZATION);
    }
}
