package com.wind.funds.route.support;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.funds.model.route.ImmutablePlatformAccountsSnapshotSpec;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.service.PlatformFundingAccountService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
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
                .tenantId(TenantContextHolder.requireTenantId())
                .subjectId(accountId.id())
                .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .build();
    }

    public @NonNull LedgerProfileCode resolveLedgerProfileCode(@NonNull PlatformFundingAccountRole role) {
        return role.getLedgerProfileCode();
    }

    public @NonNull LedgerSubjectCode resolveLedgerSubjectCode(@NonNull PlatformFundingAccountRole role) {
        return role.getLedgerSubjectCode();
    }

    public @NonNull PlatformAccountsSnapshotSpec createExternalFundMovementSnapshot(
            @NonNull FundsAccountId cashMappingAccount,
            @NonNull FundsAccountId prepaymentAccount,
            @Nullable FundsAccountId feeAccount) {
        return createSnapshot(cashMappingAccount, prepaymentAccount, null, null, feeAccount, null);
    }

    public @NonNull PlatformAccountsSnapshotSpec createSettlementSnapshot(@NonNull FundsAccountId settlementAccount) {
        return createSnapshot(null, null, null, settlementAccount, null, null);
    }

    public @Nullable PlatformAccountsSnapshotSpec createFeeSnapshot(@Nullable FundsAccountId feeAccount) {
        return feeAccount == null ? null : createSnapshot(null, null, null, null, feeAccount, null);
    }

    public @NonNull PlatformAccountsSnapshotSpec createPrepaymentSnapshot(@NonNull FundsAccountId prepaymentAccount) {
        return createSnapshot(null, prepaymentAccount, null, null, null, null);
    }

    public @NonNull PlatformAccountsSnapshotSpec createAdjustmentSnapshot(@NonNull FundsAccountId adjustmentAccount) {
        return createSnapshot(null, null, null, null, null, adjustmentAccount);
    }

    private @NonNull PlatformAccountsSnapshotSpec createSnapshot(@Nullable FundsAccountId cashMappingAccount,
                                                                 @Nullable FundsAccountId prepaymentAccount,
                                                                 @Nullable FundsAccountId clearingAccount,
                                                                 @Nullable FundsAccountId settlementAccount,
                                                                 @Nullable FundsAccountId feeAccount,
                                                                 @Nullable FundsAccountId adjustmentAccount) {
        if (cashMappingAccount == null
                && prepaymentAccount == null
                && clearingAccount == null
                && settlementAccount == null
                && feeAccount == null
                && adjustmentAccount == null) {
            throw new IllegalArgumentException("platform account snapshot must contain at least one account");
        }
        return ImmutablePlatformAccountsSnapshotSpec.builder()
                .cashFundingAccount(cashMappingAccount == null ? null : createSubjectRef(cashMappingAccount))
                .prepaymentFundingAccount(prepaymentAccount == null ? null : createSubjectRef(prepaymentAccount))
                .clearingFundingAccount(clearingAccount == null ? null : createSubjectRef(clearingAccount))
                .settlementFundingAccount(settlementAccount == null ? null : createSubjectRef(settlementAccount))
                .feeFundingAccount(feeAccount == null ? null : createSubjectRef(feeAccount))
                .adjustmentFundingAccount(adjustmentAccount == null ? null : createSubjectRef(adjustmentAccount))
                .build();
    }
}
