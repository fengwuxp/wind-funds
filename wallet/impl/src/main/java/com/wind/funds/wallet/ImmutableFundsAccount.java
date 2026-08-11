package com.wind.funds.wallet;

import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
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

    private final FundsAccountState state;

    private final CurrencyIsoCode currency;

    private final Set<FundsAccountCapability> capabilities;

    private final String capabilitySource;

    private final Integer version;

    @Override
    public Set<FundsAccountCapability> getCapabilities() {
        return capabilities == null ? Set.of() : Collections.unmodifiableSet(capabilities);
    }

    @Override
    public boolean isAvailable() {
        return getState() == FundsAccountState.ACTIVE;
    }
}
