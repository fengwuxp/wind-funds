package com.capte.funds.route.support;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.integration.funds.model.route.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.capte.funds.transaction.enums.LedgerProfileCode;
import com.capte.funds.transaction.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.services.PlatformFundingAccountService;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 平台账户路由辅助。
 */
@Component
@AllArgsConstructor
public class PlatformAccountRouteSupport {

    private final PlatformFundingAccountService platformFundingAccountService;

    public @NonNull FundsAccountId requireAccount(@NonNull CurrencyIsoCode currency,
                                                  @NonNull PlatformFundingAccountRole role) {
        return platformFundingAccountService.requireAccountId(currency, role);
    }

    public @NonNull SubjectRef createSubjectRef(@NonNull FundsAccountId accountId) {
        return ImmutableSubjectRef.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .subjectId(accountId.id())
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .build();
    }

    public @NonNull LedgerProfileCode resolveLedgerProfileCode(@NonNull PlatformFundingAccountRole role) {
        return role.getLedgerProfileCode();
    }

    public @NonNull PlatformAccountsSnapshotSpec createExternalFundMovementSnapshot(
            @NonNull FundsAccountId reserveAccount,
            @NonNull FundsAccountId prepaymentAccount,
            @Nullable FundsAccountId feeAccount) {
        return createSnapshot(reserveAccount, prepaymentAccount, null, null, feeAccount);
    }

    public @NonNull PlatformAccountsSnapshotSpec createSettlementSnapshot(@NonNull FundsAccountId settlementAccount) {
        return createSnapshot(null, null, null, settlementAccount, null);
    }

    public @Nullable PlatformAccountsSnapshotSpec createFeeSnapshot(@Nullable FundsAccountId feeAccount) {
        return feeAccount == null ? null : createSnapshot(null, null, null, null, feeAccount);
    }

    public @NonNull PlatformAccountsSnapshotSpec createPrepaymentSnapshot(@NonNull FundsAccountId prepaymentAccount) {
        return createSnapshot(null, prepaymentAccount, null, null, null);
    }

    private @NonNull PlatformAccountsSnapshotSpec createSnapshot(@Nullable FundsAccountId reserveAccount,
                                                                 @Nullable FundsAccountId prepaymentAccount,
                                                                 @Nullable FundsAccountId clearingAccount,
                                                                 @Nullable FundsAccountId settlementAccount,
                                                                 @Nullable FundsAccountId feeAccount) {
        if (reserveAccount == null
                && prepaymentAccount == null
                && clearingAccount == null
                && settlementAccount == null
                && feeAccount == null) {
            throw new IllegalArgumentException("platform account snapshot must contain at least one account");
        }
        return ImmutablePlatformAccountsSnapshotSpec.builder()
                .reserveFundingAccount(reserveAccount == null ? null : createSubjectRef(reserveAccount))
                .prepaymentFundingAccount(prepaymentAccount == null ? null : createSubjectRef(prepaymentAccount))
                .clearingFundingAccount(clearingAccount == null ? null : createSubjectRef(clearingAccount))
                .settlementFundingAccount(settlementAccount == null ? null : createSubjectRef(settlementAccount))
                .feeFundingAccount(feeAccount == null ? null : createSubjectRef(feeAccount))
                .build();
    }
}
