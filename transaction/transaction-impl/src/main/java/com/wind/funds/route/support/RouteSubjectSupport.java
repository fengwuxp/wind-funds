package com.wind.funds.route.support;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.funds.model.route.ImmutableSubjectRef;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 普通主体路由辅助。
 */
@Component
public class RouteSubjectSupport {

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = "SPEND_CONTROL_SCOPE";

    public @NonNull SubjectRef createSubjectRef(@NonNull FundsAccountId accountId) {
        if (isExternalAccountType(accountId.type())) {
            throw new IllegalArgumentException("外部账户不能创建账务主体引用，accountId = " + accountId);
        }
        if (isSpendControlScope(accountId)) {
            throw new IllegalArgumentException("支出控制范围不是核心资金账务主体，不能创建账务主体引用，accountId = "
                    + accountId);
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
        if (isSpendControlScope(accountId)) {
            throw new IllegalArgumentException("支出控制范围不是核心资金账务主体，不能解析账务主体类型，accountId = "
                    + accountId);
        }
        return FundsSubjectType.FUNDING_ACCOUNT;
    }

    public @NonNull RouteParticipantRole resolveParticipantRole(@NonNull FundsAccountId accountId,
                                                                boolean sourceSide) {
        if (Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name())) {
            return RouteParticipantRole.AUTH_HOLDER;
        }
        if (isSpendControlScope(accountId)) {
            throw new IllegalArgumentException("支出控制范围不是核心资金账务主体，不能解析路由参与方，accountId = "
                    + accountId);
        }
        return sourceSide ? RouteParticipantRole.PAYER : RouteParticipantRole.PAYEE;
    }

    public @NonNull LedgerProfileCode resolveLedgerProfileCode(@NonNull FundsAccountId accountId) {
        if (Objects.equals(accountId.type(), FundsSubjectType.CREDIT_ACCOUNT.name())) {
            return LedgerProfileCode.CREDIT_BASIC;
        }
        if (isSpendControlScope(accountId)) {
            throw new IllegalArgumentException("支出控制范围不是核心资金账务主体，不能解析账本 Profile，accountId = "
                    + accountId);
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

    public boolean isSpendControlScope(@NonNull FundsAccountId accountId) {
        return Objects.equals(accountId.type(), SPEND_CONTROL_SCOPE_ACCOUNT_TYPE);
    }

    private boolean isExternalAccountType(String accountType) {
        return DefaultFundsAccountType.isExternalAccount(accountType);
    }

    private boolean isMerchantFundingAccountType(String accountType) {
        return Objects.equals(accountType, DefaultFundsAccountType.PLATFORM_MERCHANT.name());
    }
}
