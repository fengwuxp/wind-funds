package com.capte.funds.transaction.services.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.funds.transaction.dal.entities.FundingAccount;
import com.capte.funds.transaction.dal.mapper.FundingAccountMapper;
import com.capte.funds.transaction.enums.PlatformFundingAccountRole;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformFundingAccountServiceImplTests {

    @AfterEach
    void after() {
        ThreadContextTenantIdHolder.remove();
    }

    @Test
    void requireAccountIdShouldReturnConfiguredPlatformFundingAccount() {
        FundingAccount account = fundingAccount("cash_mapping_usd", DefaultFundsAccountType.CASH_MAPPING.name());
        AtomicInteger queryCount = new AtomicInteger();
        FundingAccountMapper mapper = FundsAccountServiceTestSupport.mapper(
                FundingAccountMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> {
                    queryCount.incrementAndGet();
                    return account;
                }
        );
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        FundsAccountId result = service.requireAccountId(1L, CurrencyIsoCode.USD,
                PlatformFundingAccountRole.CASH_MAPPING);

        assertThat(result).isEqualTo(FundsAccountId.immutable("cash_mapping_usd", "CASH_MAPPING"));
        assertThat(queryCount).hasValue(1);
    }

    @Test
    void platformFundingAccountRolesShouldUseTargetProductSemantics() {
        assertThat(PlatformFundingAccountRole.values())
                .extracting(PlatformFundingAccountRole::name)
                .containsExactly(
                        "CASH_MAPPING",
                        "PREPAYMENT",
                        "CLEARING",
                        "SETTLEMENT",
                        "FEE",
                        "ADJUSTMENT"
                );
    }

    @Test
    void requireAccountIdShouldUseThreadTenantWhenTenantIdNotProvided() {
        ThreadContextTenantIdHolder.setTenantId(99L);
        FundingAccount account = fundingAccount("prepayment_usd", DefaultFundsAccountType.PLATFORM_LIABILITY.name());
        AtomicInteger queryCount = new AtomicInteger();
        FundingAccountMapper mapper = FundsAccountServiceTestSupport.mapper(
                FundingAccountMapper.class,
                entity -> {
                    throw new UnsupportedOperationException("insertSelective");
                },
                query -> {
                    queryCount.incrementAndGet();
                    return account;
                }
        );
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        FundsAccountId result = service.requireAccountId(CurrencyIsoCode.USD, PlatformFundingAccountRole.PREPAYMENT);

        assertThat(result.id()).isEqualTo("prepayment_usd");
        assertThat(result.type()).isEqualTo(DefaultFundsAccountType.PLATFORM_LIABILITY.name());
        assertThat(queryCount).hasValue(1);
    }

    private static FundingAccount fundingAccount(String sn, String accountType) {
        FundingAccount result = new FundingAccount();
        result.setSn(sn);
        result.setAccountType(accountType);
        return result;
    }
}
