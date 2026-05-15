package com.capte.funds.route.support;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 普通主体路由辅助。
 */
@Component
public class RouteSubjectSupport {

    public @NonNull SubjectRef createSubjectRef(@NonNull FundsAccountId accountId) {
        if (isExternalAccountType(accountId.type())) {
            throw new IllegalArgumentException("外部账户不能创建账务主体引用，accountId = " + accountId);
        }
        return ImmutableSubjectRef.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .subjectId(accountId.id())
                .subjectType(resolveSubjectType(accountId))
                .build();
    }

    public @NonNull FundsSubjectType resolveSubjectType(@NonNull FundsAccountId accountId) {
        if (Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name())) {
            return FundsSubjectType.CREDIT_ACCOUNT;
        }
        if (Objects.equals(accountId.type(), FundsSubjectType.BUDGET_GROUP.name())) {
            return FundsSubjectType.BUDGET_GROUP;
        }
        return FundsSubjectType.FUNDING_ACCOUNT;
    }

    public @NonNull RouteParticipantRole resolveParticipantRole(@NonNull FundsAccountId accountId,
                                                                boolean sourceSide) {
        if (Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name())) {
            return RouteParticipantRole.AUTH_HOLDER;
        }
        if (Objects.equals(accountId.type(), FundsSubjectType.BUDGET_GROUP.name())) {
            return RouteParticipantRole.BUDGET_CONTROLLER;
        }
        return sourceSide ? RouteParticipantRole.PAYER : RouteParticipantRole.PAYEE;
    }

    public @NonNull LedgerProfileCode resolveLedgerProfileCode(@NonNull FundsAccountId accountId) {
        if (Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name())) {
            return LedgerProfileCode.CREDIT_BASIC;
        }
        if (Objects.equals(accountId.type(), FundsSubjectType.BUDGET_GROUP.name())) {
            return LedgerProfileCode.BUDGET_BASIC;
        }
        if (isMerchantFundingAccountType(accountId.type())) {
            return LedgerProfileCode.FUNDING_MERCHANT;
        }
        return LedgerProfileCode.FUNDING_BASIC;
    }

    public boolean isFundingAccount(@NonNull FundsAccountId accountId) {
        return Objects.equals(accountId.type(), FundsSubjectType.FUNDING_ACCOUNT.name())
                || isMerchantFundingAccountType(accountId.type());
    }

    public boolean isCreditAccount(@NonNull FundsAccountId accountId) {
        return Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name());
    }

    public boolean isBudgetGroup(@NonNull FundsAccountId accountId) {
        return Objects.equals(accountId.type(), FundsSubjectType.BUDGET_GROUP.name());
    }

    private boolean isExternalAccountType(String accountType) {
        return DefaultFundsAccountType.isExternalAccount(accountType);
    }

    private boolean isMerchantFundingAccountType(String accountType) {
        return Objects.equals(accountType, DefaultFundsAccountType.PLATFORM_MERCHANT.name());
    }
}
