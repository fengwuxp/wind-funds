package com.wind.funds.wallet;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.LedgerProfileCode;
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

    @lombok.NonNull
    private final LedgerProfileCode ledgerProfileCode;

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
    public @NonNull Money getTotalBalance() {
        AssertUtils.isTrue(ledgerProfileCode == LedgerProfileCode.FUNDING_BASIC,
                "总余额口径尚未定义，accountId = {}, ledgerProfileCode = {}",
                accountId,
                ledgerProfileCode);
        return getAvailableBalance().add(getFrozenBalance()).add(getAuthorizationBalance());
    }

}
