package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.wallet.model.request.CreateFundingAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FundingAccountServiceImplTests {

    /**
     * 场景：创建平台资金账户。
     * 输入：租户平台自有 CASH_MAPPING 资金账户，角色为 CASH_MAPPING，币种为 USD。
     * 输出：写入平台 funding account，并初始化 FUNDING_PLATFORM profile 下的主体账本。
     * 预期：账户为 ACTIVE、platform=true，账本初始化请求使用 FUNDING_ACCOUNT 主体与 FUNDING_PLATFORM profile。
     * 红线：平台资金账户不得落到商户资金 profile，也不得绕过主体账本初始化。
     */
    @Test
    void testCreateFundingAccountShouldUseRoleProfileAndInitializeLedgers() {
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

    /**
     * 场景：创建普通商户资金账户。
     * 输入：商户 owner、PLATFORM_MERCHANT 账户类型，未声明平台资金角色。
     * 输出：写入商户 funding account，并初始化 FUNDING_MERCHANT profile 下的主体账本。
     * 预期：账户为 ACTIVE、platform=false，账本初始化请求保持 FUNDING_ACCOUNT 主体。
     * 红线：商户资金账户不得被误归为平台自有资金账户。
     */
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
