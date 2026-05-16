package com.capte.funds.route;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.transaction.FundsAccountTransactionFeeProvider;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;

import java.util.Map;

/**
 * Route / Converter 测试支撑。
 */
public final class FundsRouteTestSupport {

    public static final Long TENANT_ID = 1L;

    public static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private FundsRouteTestSupport() {
    }

    public static void bindTenant() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
    }

    public static void clearTenant() {
        ThreadContextTenantIdHolder.remove();
    }

    public static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    public static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    public static FundsAccountId budgetGroup(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.BUDGET_GROUP);
    }

    public static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    public static TransactionAmount transactionAmount(long value) {
        return TransactionAmount.sameCurrency(amount(value));
    }

    public static PlatformFundingAccountService platformFundingAccountService() {
        return new PlatformFundingAccountService() {
            @Override
            public FundsAccountId requireAccountId(CurrencyIsoCode currency, PlatformFundingAccountRole role) {
                return requireAccountId(TENANT_ID, currency, role);
            }

            @Override
            public FundsAccountId requireAccountId(Long tenantId, CurrencyIsoCode currency,
                                                   PlatformFundingAccountRole role) {
                return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                        FundsSubjectType.FUNDING_ACCOUNT);
            }
        };
    }

    public static FundsAccountQueryService accountQueryService() {
        return accountQueryService(CURRENCY);
    }

    public static FundsAccountQueryService accountQueryService(CurrencyIsoCode currency) {
        return new FundsAccountQueryService() {
            @Override
            public FundsAccount getAccount(FundsAccountId accountId) {
                return new TestFundsAccount(accountId, currency);
            }

            @Override
            public FundsAccountBalanceView getBalance(FundsAccountId accountId) {
                throw new UnsupportedOperationException("balance is not required by route tests");
            }

            @Override
            public boolean supports(FundsAccountId accountId) {
                return true;
            }
        };
    }

    public static FundsAccountTransactionFeeProvider noFeeProvider() {
        return new FundsAccountTransactionFeeProvider() {
            @Override
            public FeeSpec apply(FundsAccountId accountId, String businessScene) {
                return null;
            }

            @Override
            public boolean supports(FundsAccountId accountId) {
                return false;
            }
        };
    }

    public static FundsAccountTransactionFeeProvider fixedFeeProvider(long feeAmount) {
        return new FundsAccountTransactionFeeProvider() {
            @Override
            public FeeSpec apply(FundsAccountId accountId, String businessScene) {
                return FeeSpec.builder()
                        .feeType(DefaultFeeType.FEE)
                        .fixedFee(Math.toIntExact(feeAmount))
                        .build();
            }

            @Override
            public boolean supports(FundsAccountId accountId) {
                return true;
            }
        };
    }

    public static FundsDirectTransactionInstructionConverter transactionInstructionConverter() {
        return new FundsDirectTransactionInstructionConverter(platformFundingAccountService(), accountQueryService());
    }

    public static FundsBalanceControlInstructionConverter balanceControlInstructionConverter() {
        return new FundsBalanceControlInstructionConverter(accountQueryService());
    }

    public static FundsAuthorizationInstructionConverter authorizationInstructionConverter() {
        return new FundsAuthorizationInstructionConverter(accountQueryService());
    }

    public static TransferFundsInstructionRouteResolver transferRouteResolver(FundsAccountTransactionFeeProvider feeProvider) {
        return new TransferFundsInstructionRouteResolver(new RouteParticipantFactory(), new RouteSubjectSupport(),
                new PlatformAccountRouteSupport(platformFundingAccountService()), feeProvider);
    }

    public static BalanceControlFundsInstructionRouteResolver balanceControlRouteResolver() {
        return new BalanceControlFundsInstructionRouteResolver(new RouteParticipantFactory(), new RouteSubjectSupport(),
                new PlatformAccountRouteSupport(platformFundingAccountService()));
    }

    public static AuthorizationFundsInstructionRouteResolver authorizationRouteResolver() {
        return new AuthorizationFundsInstructionRouteResolver(new RouteParticipantFactory(), new RouteSubjectSupport(),
                new PlatformAccountRouteSupport(platformFundingAccountService()));
    }

    private record TestFundsAccount(FundsAccountId accountId, CurrencyIsoCode currency) implements FundsAccount {

        @Override
        public Long getId() {
            return 1L;
        }

        @Override
        public FundsAccountId getAccountId() {
            return accountId;
        }

        @Override
        public FundsAccountOwner getOwner() {
            return FundsAccountOwner.of("owner_001", FundsAccountOwnerType.USER);
        }

        @Override
        public FundsAccountStatus getStatus() {
            return FundsAccountStatus.ACTIVE;
        }

        @Override
        public Map<LedgerSubjectCode, Long> getAccountLedgerIds() {
            return Map.of();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public CurrencyIsoCode getCurrency() {
            return currency;
        }

        @Override
        public Integer getVersion() {
            return 0;
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }
    }
}
