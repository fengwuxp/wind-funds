package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundingAccount;
import com.capte.funds.transaction.dal.mapper.FundingAccountMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.model.request.CreateFundingAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FundingAccountServiceImplTests {

    @Test
    void createFundingAccountShouldUseRoleProfileAndInitializeLedgers() {
        AtomicReference<FundingAccount> inserted = new AtomicReference<>();
        FundingAccountMapper fundingAccountMapper = FundsAccountServiceTestSupport.mapper(
                FundingAccountMapper.class,
                entityObject -> {
                    FundingAccount entity = (FundingAccount) entityObject;
                    entity.setId(101L);
                    inserted.set(entity);
                },
                query -> null
        );
        FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer initializer =
                new FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer();
        FundingAccountServiceImpl service = new FundingAccountServiceImpl(
                fundingAccountMapper,
                initializer,
                FundsAccountServiceTestSupport.unsupportedLedgerService()
        );

        Long id = service.createFundingAccount(new CreateFundingAccountRequest()
                .setSn("funding_001")
                .setTenantId(1L)
                .setOwnerId("tenant_001")
                .setOwnerType(FundsAccountOwnerType.TENANT)
                .setAccountType(DefaultFundsAccountType.CASH_MAPPING.name())
                .setPlatform(true)
                .setAccountRoleCode(PlatformFundingAccountRole.CASH_MAPPING)
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(101L);
        FundingAccount entity = inserted.get();
        assertThat(entity).isNotNull();
        assertThat(entity.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(entity.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(entity.getPlatform()).isTrue();

        InitializeSubjectLedgerRequest initRequest = initializer.getRequest();
        assertThat(initRequest.getSubjectId()).isEqualTo("funding_001");
        assertThat(initRequest.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(initRequest.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_PLATFORM);
        assertThat(initRequest.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
    }

    @Test
    void testCreateMerchantFundingAccountShouldUseMerchantProfileByDefault() {
        AtomicReference<FundingAccount> inserted = new AtomicReference<>();
        FundingAccountMapper fundingAccountMapper = FundsAccountServiceTestSupport.mapper(
                FundingAccountMapper.class,
                entityObject -> {
                    FundingAccount entity = (FundingAccount) entityObject;
                    entity.setId(102L);
                    inserted.set(entity);
                },
                query -> null
        );
        FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer initializer =
                new FundsAccountServiceTestSupport.RecordingSubjectLedgerInitializer();
        FundingAccountServiceImpl service = new FundingAccountServiceImpl(
                fundingAccountMapper,
                initializer,
                FundsAccountServiceTestSupport.unsupportedLedgerService()
        );

        Long id = service.createFundingAccount(new CreateFundingAccountRequest()
                .setSn("merchant_funding_001")
                .setTenantId(1L)
                .setOwnerId("merchant_001")
                .setOwnerType(FundsAccountOwnerType.MERCHANT)
                .setAccountType(DefaultFundsAccountType.PLATFORM_MERCHANT.name())
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(id).isEqualTo(102L);
        FundingAccount entity = inserted.get();
        assertThat(entity).isNotNull();
        assertThat(entity.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_MERCHANT);
        assertThat(entity.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(entity.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(entity.getPlatform()).isFalse();

        InitializeSubjectLedgerRequest initRequest = initializer.getRequest();
        assertThat(initRequest.getSubjectId()).isEqualTo("merchant_funding_001");
        assertThat(initRequest.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
        assertThat(initRequest.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_MERCHANT);
        assertThat(initRequest.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
    }
}
