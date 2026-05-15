package com.capte.funds.wallet;

import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.enums.FundsAccountCapability;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 不可变资金账户快照。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Getter
@AllArgsConstructor
@Builder
public class ImmutableFundsAccount implements FundsAccount {

    private final Long id;

    private final Long tenantId;

    private final FundsAccountId accountId;

    private final FundsAccountOwner owner;

    private final FundsAccountStatus status;

    private final CurrencyIsoCode currency;

    private final Set<FundsAccountCapability> capabilities;

    private final Map<LedgerSubjectCode, Long> accountLedgerIds;

    private final Integer version;

    @Override
    public Set<FundsAccountCapability> getCapabilities() {
        return capabilities == null ? FundsAccount.super.getCapabilities() : capabilities;
    }

    @Override
    public boolean isAvailable() {
        return getStatus() == FundsAccountStatus.ACTIVE;
    }
}
