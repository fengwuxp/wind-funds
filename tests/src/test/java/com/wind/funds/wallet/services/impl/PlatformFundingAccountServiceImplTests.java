package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.service.PlatformFundingAccountService;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 平台资金账户角色解析服务层边界测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PlatformFundingAccountServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlatformFundingAccountServiceImplTests extends AbstractFundsServiceTest {

    private static final String PLATFORM_ACCOUNT_SN = "platform_cash_mapping_service";

    private static final String NON_PLATFORM_ACCOUNT_SN = "non_platform_cash_mapping_service";

    private static final String SUSPENDED_PLATFORM_ACCOUNT_SN = "platform_cash_mapping_suspended";

    @Autowired
    private PlatformFundingAccountService platformFundingAccountService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：充值 route 需要按当前租户、币种和平台账户角色解析现金映射账户。
     * 输入：存在唯一 ACTIVE 平台资金账户。
     * 输出：返回可记账资金账户标识。
     * 红线：平台角色解析只读配置，不自动创建平台账户或账本。
     */
    @Test
    void testRequireAccountIdShouldResolveUniqueActivePlatformAccountWithoutLedgerMutation() {
        insertFundingAccount(PLATFORM_ACCOUNT_SN, true, FundsAccountState.ACTIVE);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundsAccountId accountId =
                platformFundingAccountService.requireAccountId(CURRENCY, PlatformFundingAccountRole.CASH_MAPPING);

        assertThat(accountId.id()).isEqualTo(PLATFORM_ACCOUNT_SN);
        assertThat(accountId.type()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(countLedgers(PLATFORM_ACCOUNT_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：交易 route 需要平台账户角色，但当前租户没有配置。
     * 输入：没有任何匹配的 CASH_MAPPING 平台账户。
     * 输出：解析失败。
     * 红线：缺平台角色时不得自动创建账户、账本或继续给 route 返回抽象平台主体。
     */
    @Test
    void testRequireAccountIdShouldRejectMissingPlatformAccountWithoutLedgerMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> platformFundingAccountService.requireAccountId(TENANT_ID,
                CURRENCY,
                PlatformFundingAccountRole.CASH_MAPPING))
                .hasMessageContaining("平台资金账户不存在");

        assertThat(countFundingAccounts()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：账户带有平台角色字段，但没有标记为平台账户。
     * 输入：is_platform = false 的误配置账户。
     * 输出：解析失败。
     * 红线：业务主体或普通资金账户不得因为带了角色字段就被当作平台可记账主体。
     */
    @Test
    void testRequireAccountIdShouldRejectNonPlatformRoleAccountWithoutLedgerMutation() {
        insertFundingAccount(NON_PLATFORM_ACCOUNT_SN, false, FundsAccountState.ACTIVE);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> platformFundingAccountService.requireAccountId(TENANT_ID,
                CURRENCY,
                PlatformFundingAccountRole.CASH_MAPPING))
                .hasMessageContaining("平台资金账户不存在");

        assertThat(countFundingAccounts()).isOne();
        assertThat(countLedgers(NON_PLATFORM_ACCOUNT_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：平台账户角色存在，但账户已被风控或合规暂停。
     * 输入：唯一平台资金账户状态为 SUSPENDED。
     * 输出：解析失败并给出状态不可用原因。
     * 红线：不可用平台账户不得进入新交易 route，也不得触发自动修复或建账。
     */
    @Test
    void testRequireAccountIdShouldRejectSuspendedPlatformAccountWithoutLedgerMutation() {
        insertFundingAccount(SUSPENDED_PLATFORM_ACCOUNT_SN, true, FundsAccountState.SUSPENDED);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> platformFundingAccountService.requireAccountId(TENANT_ID,
                CURRENCY,
                PlatformFundingAccountRole.CASH_MAPPING))
                .hasMessageContaining("平台资金账户状态不可用");

        assertThat(countFundingAccounts()).isOne();
        assertThat(countLedgers(SUSPENDED_PLATFORM_ACCOUNT_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpPlatformFundingAccountServiceTestData() {
        cleanupPlatformFundingAccountServiceTestData();
    }

    @AfterEach
    void tearDownPlatformFundingAccountServiceTestData() {
        cleanupPlatformFundingAccountServiceTestData();
    }

    private void cleanupPlatformFundingAccountServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                PLATFORM_ACCOUNT_SN,
                NON_PLATFORM_ACCOUNT_SN,
                SUSPENDED_PLATFORM_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                PLATFORM_ACCOUNT_SN,
                NON_PLATFORM_ACCOUNT_SN,
                SUSPENDED_PLATFORM_ACCOUNT_SN);
    }

    private void insertFundingAccount(String accountSn, boolean platform, FundsAccountState state) {
        FundingAccount account = new FundingAccount();
        account.setSn(accountSn);
        account.setTenantId(TENANT_ID);
        account.setOwnerId(platform ? "platform" : "ordinary_owner");
        account.setOwnerType(platform ? FundsAccountOwnerType.PLATFORM : FundsAccountOwnerType.USER);
        account.setAccountType(FundsSubjectType.FUNDING_ACCOUNT.name());
        account.setPlatform(platform);
        account.setAccountRoleCode(PlatformFundingAccountRole.CASH_MAPPING);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(LedgerProfileCode.FUNDING_PLATFORM);
        account.setLedgerProfileVersion(1);
        account.setState(state);
        account.setDescription("platform funding account resolver test");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private long countFundingAccounts() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_funding_account WHERE sn IN (?, ?, ?)",
                Long.class,
                PLATFORM_ACCOUNT_SN,
                NON_PLATFORM_ACCOUNT_SN,
                SUSPENDED_PLATFORM_ACCOUNT_SN);
        return result;
    }

    private long countLedgers(String subjectId) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger WHERE subject_id = ?",
                Long.class,
                subjectId);
        return result;
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {
    }
}
