package com.wind.integration.funds.model.route;

import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * 不可变平台账户快照实现。
 */
@Builder
public record ImmutablePlatformAccountsSnapshotSpec(@Nullable SubjectRef reserveFundingAccount,
                                                    @Nullable SubjectRef prepaymentFundingAccount,
                                                    @Nullable SubjectRef clearingFundingAccount,
                                                    @Nullable SubjectRef settlementFundingAccount,
                                                    @Nullable SubjectRef feeFundingAccount)
        implements PlatformAccountsSnapshotSpec {

    @Override
    public @Nullable SubjectRef getReserveFundingAccount() {
        return reserveFundingAccount;
    }

    @Override
    public @Nullable SubjectRef getPrepaymentFundingAccount() {
        return prepaymentFundingAccount;
    }

    @Override
    public @Nullable SubjectRef getClearingFundingAccount() {
        return clearingFundingAccount;
    }

    @Override
    public @Nullable SubjectRef getSettlementFundingAccount() {
        return settlementFundingAccount;
    }

    @Override
    public @Nullable SubjectRef getFeeFundingAccount() {
        return feeFundingAccount;
    }
}
