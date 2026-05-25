package com.wind.integration.funds.model.route;

import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.Nullable;

/**
 * 不可变平台账户快照实现。
 */
@Builder
@FieldNameConstants
public record ImmutablePlatformAccountsSnapshotSpec(@Nullable SubjectRef cashFundingAccount,
                                                    @Nullable SubjectRef prepaymentFundingAccount,
                                                    @Nullable SubjectRef clearingFundingAccount,
                                                    @Nullable SubjectRef settlementFundingAccount,
                                                    @Nullable SubjectRef feeFundingAccount,
                                                    @Nullable SubjectRef adjustmentFundingAccount)
        implements PlatformAccountsSnapshotSpec {

    @Override
    public @Nullable SubjectRef getCashFundingAccount() {
        return cashFundingAccount;
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

    @Override
    public @Nullable SubjectRef getAdjustmentFundingAccount() {
        return adjustmentFundingAccount;
    }
}
