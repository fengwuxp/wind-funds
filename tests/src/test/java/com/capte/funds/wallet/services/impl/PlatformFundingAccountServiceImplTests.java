package com.capte.funds.wallet.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformFundingAccountServiceImplTests {

    @AfterEach
    void after() {
        ThreadContextTenantIdHolder.remove();
    }

    /**
     * 场景：交易路径按平台账户角色解析已配置的 FundingAccount。
     * 输入：tenantId=1、USD、CASH_MAPPING，数据库存在 cash_mapping_usd。
     * 输出：返回资金账户 ID 与资金账户类型。
     * 预期：只查询一次，不创建或改写平台账户。
     * 红线：平台角色解析不得退化为使用角色枚举本身作为入账主体。
     */
    @Test
    void testRequireAccountIdShouldReturnConfiguredPlatformFundingAccount() {
        FundingAccount account = fundingAccount("cash_mapping_usd", DefaultFundsAccountType.CASH_MAPPING.name());
        AtomicInteger queryCount = new AtomicInteger();
        AtomicInteger insertCount = new AtomicInteger();
        FundingAccountMapper mapper = mapper(queryCount, insertCount, account);
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        FundsAccountId result = service.requireAccountId(1L, CurrencyIsoCode.USD,
                PlatformFundingAccountRole.CASH_MAPPING);

        assertThat(result).isEqualTo(FundsAccountId.immutable("cash_mapping_usd", "CASH_MAPPING"));
        assertThat(queryCount).hasValue(1);
        assertThat(insertCount).hasValue(0);
    }

    /**
     * 场景：交易路径解析平台账户角色时，目标租户、币种、角色未配置平台 FundingAccount。
     * 输入：tenantId=1、USD、ADJUSTMENT，数据库查询返回空。
     * 输出：抛出平台资金账户不存在异常。
     * 预期：错误信息包含租户、币种和角色上下文，且不触发自动创建。
     * 红线：平台账户角色不得在交易路径中被隐式创建或被当作资金主体直接入账。
     */
    @Test
    void testRequireAccountIdShouldRejectMissingPlatformFundingAccountRole() {
        AtomicInteger queryCount = new AtomicInteger();
        AtomicInteger insertCount = new AtomicInteger();
        FundingAccountMapper mapper = mapper(queryCount, insertCount);
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        assertThatThrownBy(() -> service.requireAccountId(1L, CurrencyIsoCode.USD,
                PlatformFundingAccountRole.ADJUSTMENT))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("平台资金账户不存在")
                .hasMessageContaining("tenantId = 1")
                .hasMessageContaining("currency = USD")
                .hasMessageContaining("role = ADJUSTMENT");

        assertThat(queryCount).hasValue(1);
        assertThat(insertCount).hasValue(0);
    }

    /**
     * 场景：平台账户配置存在，但账户已停用或未标记为平台账户。
     * 输入：tenantId=1、USD、CLEARING，数据库返回非平台或 SUSPENDED 的 FundingAccount。
     * 输出：拒绝返回该资金账户。
     * 预期：错误信息暴露账户状态/平台标记问题，不触发自动创建。
     * 红线：route 不得把停用账户或普通 FundingAccount 当作平台资金账户入账。
     */
    @Test
    void testRequireAccountIdShouldRejectInactiveOrNonPlatformFundingAccount() {
        FundingAccount account = fundingAccount("clearing_usd", DefaultFundsAccountType.CLEARING.name());
        account.setPlatform(false);
        account.setStatus(FundsAccountStatus.SUSPENDED);
        AtomicInteger queryCount = new AtomicInteger();
        AtomicInteger insertCount = new AtomicInteger();
        FundingAccountMapper mapper = mapper(queryCount, insertCount, account);
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        assertThatThrownBy(() -> service.requireAccountId(1L, CurrencyIsoCode.USD,
                PlatformFundingAccountRole.CLEARING))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("平台资金账户状态不可用");

        assertThat(queryCount).hasValue(1);
        assertThat(insertCount).hasValue(0);
    }

    /**
     * 场景：平台账户查询条件期望 USD，但底层配置返回了 EUR 账户。
     * 输入：tenantId=1、USD、SETTLEMENT，数据库异常返回 settlement_eur。
     * 输出：拒绝返回该资金账户。
     * 预期：错误信息暴露 expected/actual currency，不触发自动创建。
     * 红线：route snapshot 不能把错币种平台账户固化为本次交易平台账户。
     */
    @Test
    void testRequireAccountIdShouldRejectMismatchedPlatformFundingAccountCurrency() {
        FundingAccount account = fundingAccount("settlement_eur", DefaultFundsAccountType.PLATFORM_LIABILITY.name());
        account.setCurrency(CurrencyIsoCode.EUR);
        AtomicInteger queryCount = new AtomicInteger();
        AtomicInteger insertCount = new AtomicInteger();
        FundingAccountMapper mapper = mapper(queryCount, insertCount, account);
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        assertThatThrownBy(() -> service.requireAccountId(1L, CurrencyIsoCode.USD,
                PlatformFundingAccountRole.SETTLEMENT))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("平台资金账户币种不匹配")
                .hasMessageContaining("expectedCurrency = USD")
                .hasMessageContaining("actualCurrency = EUR")
                .hasMessageContaining("role = SETTLEMENT");

        assertThat(queryCount).hasValue(1);
        assertThat(insertCount).hasValue(0);
    }

    /**
     * 场景：同一租户、币种、角色配置了多个平台 FundingAccount。
     * 输入：tenantId=1、USD、FEE，数据库返回两个 ACTIVE 平台 FundingAccount。
     * 输出：拒绝解析平台资金账户。
     * 预期：错误信息暴露配置不唯一，不触发自动创建。
     * 红线：route 不得在多账户歧义下随机选择一个平台资金账户入账。
     */
    @Test
    void testRequireAccountIdShouldRejectDuplicatedPlatformFundingAccountRole() {
        FundingAccount first = fundingAccount("fee_usd_001", DefaultFundsAccountType.PLATFORM_REVENUE.name());
        FundingAccount second = fundingAccount("fee_usd_002", DefaultFundsAccountType.PLATFORM_REVENUE.name());
        AtomicInteger queryCount = new AtomicInteger();
        AtomicInteger insertCount = new AtomicInteger();
        FundingAccountMapper mapper = mapper(queryCount, insertCount, first, second);
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        assertThatThrownBy(() -> service.requireAccountId(1L, CurrencyIsoCode.USD,
                PlatformFundingAccountRole.FEE))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("平台资金账户配置不唯一")
                .hasMessageContaining("role = FEE");

        assertThat(queryCount).hasValue(1);
        assertThat(insertCount).hasValue(0);
    }

    /**
     * 场景：调用方未显式传入 tenantId 时解析平台账户角色。
     * 输入：线程上下文 tenantId=99、USD、PREPAYMENT，数据库存在 prepayment_usd。
     * 输出：返回上下文租户下的平台资金账户。
     * 预期：只查询一次，不创建或改写平台账户。
     * 红线：平台账户解析不得绕过租户上下文或落到默认租户。
     */
    @Test
    void testRequireAccountIdShouldUseThreadTenantWhenTenantIdNotProvided() {
        ThreadContextTenantIdHolder.setTenantId(99L);
        FundingAccount account = fundingAccount("prepayment_usd", DefaultFundsAccountType.PLATFORM_LIABILITY.name());
        AtomicInteger queryCount = new AtomicInteger();
        AtomicInteger insertCount = new AtomicInteger();
        FundingAccountMapper mapper = mapper(queryCount, insertCount, account);
        PlatformFundingAccountServiceImpl service = new PlatformFundingAccountServiceImpl(mapper);

        FundsAccountId result = service.requireAccountId(CurrencyIsoCode.USD, PlatformFundingAccountRole.PREPAYMENT);

        assertThat(result.id()).isEqualTo("prepayment_usd");
        assertThat(result.type()).isEqualTo(DefaultFundsAccountType.PLATFORM_LIABILITY.name());
        assertThat(queryCount).hasValue(1);
        assertThat(insertCount).hasValue(0);
    }

    private static FundingAccount fundingAccount(String sn, String accountType) {
        FundingAccount result = new FundingAccount();
        result.setSn(sn);
        result.setAccountType(accountType);
        result.setPlatform(true);
        result.setStatus(FundsAccountStatus.ACTIVE);
        result.setCurrency(CurrencyIsoCode.USD);
        return result;
    }

    private static FundingAccountMapper mapper(AtomicInteger queryCount,
                                               AtomicInteger insertCount,
                                               FundingAccount... accounts) {
        return FundsAccountServiceTestSupport.mapper(
                FundingAccountMapper.class,
                entity -> {
                    insertCount.incrementAndGet();
                },
                query -> {
                    queryCount.incrementAndGet();
                    return List.of(accounts).stream().findFirst().orElse(null);
                },
                query -> {
                    queryCount.incrementAndGet();
                    return List.of(accounts);
                },
                entity -> {
                    throw new UnsupportedOperationException("update");
                }
        );
    }
}
