package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.request.CreateLedgerRequest;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.wallet.model.dto.FundingAccountDTO;
import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.capte.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.capte.funds.wallet.model.request.CreateFundingAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.wallet.service.FundingAccountService;
import com.capte.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.capte.funds.wallet.service.SubjectLedgerInitializer;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.enums.FundingAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.transaction.core.enums.CurrencyIsoCode;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 真实资金账户服务层流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundingAccountServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundingAccountServiceImplTests extends AbstractFundsServiceTest {

    private static final String ACCOUNT_SN = "funding_account_service_basic";

    private static final String PLATFORM_ACCOUNT_WITHOUT_ROLE_SN = "platform_account_without_role";

    private static final String NON_PLATFORM_ACCOUNT_WITH_ROLE_SN = "fund_account_with_role";

    private static final String OWNER_ID = "owner_fas_basic";

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private FundsAccountQueryService fundsAccountQueryService;

    @Autowired
    private SubjectLedgerInitializer subjectLedgerInitializer;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateFundingAccountShouldInitializeRequiredLedgers() {
        Long accountId = fundingAccountService.createFundingAccount(createFundingAccountRequest());

        FundingAccountDTO account = fundingAccountService.getFundingAccountById(accountId);
        List<LedgerDTO> ledgers = loadLedgers();

        assertThat(account.getSn()).isEqualTo(ACCOUNT_SN);
        assertThat(account.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(account.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC);
        assertThat(account.getLedgerIds())
                .containsOnlyKeys(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION);
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).extracting(LedgerDTO::getLedgerSubjectCode)
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION);
        assertThat(ledgers).allSatisfy(this::assertFundingBasicLedger);
    }

    /**
     * 场景：运营配置平台资金账户时漏填平台账户角色。
     * 输入：is_platform = true，但 account_role_code 为空。
     * 输出：创建失败并提示平台资金账户必须指定角色。
     * 红线：平台账户缺角色时不得写入资金账户、初始化账本或产生账务事实。
     */
    @Test
    void testCreateFundingAccountShouldRejectPlatformAccountWithoutRoleAndKeepFactsUnchanged() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(platformAccountWithoutRoleRequest()))
                .hasMessageContaining("平台资金账户必须指定平台账户角色");

        assertThat(countFundingAccounts()).isZero();
        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：普通资金账户请求伪造平台账户角色。
     * 输入：is_platform = false，但 account_role_code = FEE。
     * 输出：创建失败并提示非平台资金账户不得指定平台角色。
     * 红线：普通账户不得占用平台角色唯一键，也不得被初始化为平台账本 profile。
     */
    @Test
    void testCreateFundingAccountShouldRejectNonPlatformAccountWithRoleAndKeepFactsUnchanged() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(nonPlatformAccountWithRoleRequest()))
                .hasMessageContaining("非平台资金账户不得指定平台账户角色");

        assertThat(countFundingAccounts()).isZero();
        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testInitializeRequiredLedgersShouldReuseExistingLedgers() {
        Long accountId = fundingAccountService.createFundingAccount(createFundingAccountRequest());
        FundingAccountDTO account = fundingAccountService.getFundingAccountById(accountId);

        Map<LedgerSubjectCode, Long> reusedLedgerIds =
                subjectLedgerInitializer.initializeRequiredLedgers(initializeSubjectLedgerRequest());

        assertThat(reusedLedgerIds).containsExactlyInAnyOrderEntriesOf(account.getLedgerIds());
        assertThat(loadLedgers()).hasSize(3);
        Integer ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_ledger WHERE subject_id = ?",
                Integer.class,
                ACCOUNT_SN);
        assertThat(ledgerCount).isEqualTo(3);
    }

    /**
     * 场景：真实资金账户已初始化基础账本后，查询当前余额。
     * 输入：FUNDING_BASIC 资金账户，默认 LIFETIME 周期。
     * 输出：返回 AVAILABLE、FROZEN、AUTHORIZATION 三个余额桶，周期均为 LIFETIME。
     * 红线：余额查询只读账本投影，不新增 ledger transaction、posting plan 或 entry。
     */
    @Test
    void testQueryFundingAccountBalanceShouldReadLifetimeLedgerProjectionWithoutLedgerFactsMutation() {
        fundingAccountService.createFundingAccount(createFundingAccountRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundsSubjectBalanceDTO balance = balanceQueryService.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT)))
                .setCurrency(CurrencyIsoCode.USD));

        assertThat(balance.isInitialized()).isTrue();
        assertThat(balance.getSubjectRef())
                .isEqualTo(FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT));
        assertThat(balance.getBalanceBuckets())
                .containsOnlyKeys(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION);
        assertThat(balance.getBalanceBuckets().values())
                .extracting(LedgerBalanceBucket::periodType)
                .containsOnly(AccountBalancePeriodType.LIFETIME);
        assertThat(balance.getBalanceBuckets().values())
                .extracting(LedgerBalanceBucket::periodId)
                .containsOnly(AccountBalancePeriodType.LIFETIME.name());
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一真实资金账户同一账目同时存在 LIFETIME 与 MONTHLY 两个余额 bucket。
     * 输入：资金账户默认 LIFETIME 基础账本，额外存在 AVAILABLE / MONTHLY / 2026-05。
     * 输出：账户基础查询和账户余额视图仍返回默认 LIFETIME 周期账本，不因同账目多周期重复 key 失败。
     * 红线：账户默认视图不得把多周期账本随机折叠；跨周期余额必须走显式周期查询。
     */
    @Test
    void testFundingAccountDefaultViewsShouldKeepLifetimeLedgersWhenMonthlyBucketCoexists() {
        Long accountId = fundingAccountService.createFundingAccount(createFundingAccountRequest());
        FundingAccountDTO lifetimeAccount = fundingAccountService.getFundingAccountById(accountId);
        Long monthlyAvailableLedgerId = createMonthlyAvailableLedger();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundingAccountDTO account = fundingAccountService.getFundingAccountById(accountId);
        FundsAccount accountView = fundsAccountQueryService.getAccount(
                FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT));
        FundsAccountBalanceView balanceView = fundsAccountQueryService.getBalance(
                FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT));

        assertThat(account.getLedgerIds()).containsExactlyInAnyOrderEntriesOf(lifetimeAccount.getLedgerIds());
        assertThat(accountView.getAccountLedgerIds()).containsExactlyInAnyOrderEntriesOf(lifetimeAccount.getLedgerIds());
        assertThat(balanceView.getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FROZEN, LedgerSubjectCode.AUTHORIZATION);
        assertThat(balanceView.getBalanceBuckets().values())
                .extracting(LedgerBalanceBucket::periodType)
                .containsOnly(AccountBalancePeriodType.LIFETIME);
        assertThat(balanceView.getBalanceBuckets().values())
                .extracting(LedgerBalanceBucket::periodId)
                .containsOnly(AccountBalancePeriodType.LIFETIME.name());
        assertThat(account.getLedgerIds()).doesNotContainValue(monthlyAvailableLedgerId);
        assertThat(accountView.getAccountLedgerIds()).doesNotContainValue(monthlyAvailableLedgerId);
        assertThat(loadLedgers()).hasSize(4);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpFundingAccountServiceTestData() {
        cleanupFundingAccountServiceTestData();
    }

    @AfterEach
    void tearDownFundingAccountServiceTestData() {
        cleanupFundingAccountServiceTestData();
    }

    private void cleanupFundingAccountServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                ACCOUNT_SN,
                PLATFORM_ACCOUNT_WITHOUT_ROLE_SN,
                NON_PLATFORM_ACCOUNT_WITH_ROLE_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                ACCOUNT_SN,
                PLATFORM_ACCOUNT_WITHOUT_ROLE_SN,
                NON_PLATFORM_ACCOUNT_WITH_ROLE_SN);
    }

    private CreateFundingAccountRequest createFundingAccountRequest() {
        return new CreateFundingAccountRequest()
                .setSn(ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
    }

    private CreateFundingAccountRequest platformAccountWithoutRoleRequest() {
        return new CreateFundingAccountRequest()
                .setSn(PLATFORM_ACCOUNT_WITHOUT_ROLE_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId("platform")
                .setOwnerType(FundsAccountOwnerType.PLATFORM)
                .setAccountType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setPlatform(Boolean.TRUE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_PLATFORM);
    }

    private CreateFundingAccountRequest nonPlatformAccountWithRoleRequest() {
        return new CreateFundingAccountRequest()
                .setSn(NON_PLATFORM_ACCOUNT_WITH_ROLE_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setAccountRoleCode(PlatformFundingAccountRole.FEE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
    }

    private InitializeSubjectLedgerRequest initializeSubjectLedgerRequest() {
        return new InitializeSubjectLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
    }

    private List<LedgerDTO> loadLedgers() {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(ACCOUNT_SN)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private long countFundingAccounts() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_funding_account WHERE sn IN (?, ?, ?)",
                Long.class,
                ACCOUNT_SN,
                PLATFORM_ACCOUNT_WITHOUT_ROLE_SN,
                NON_PLATFORM_ACCOUNT_WITH_ROLE_SN);
        return result;
    }

    private long countLedgers() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                Long.class,
                ACCOUNT_SN,
                PLATFORM_ACCOUNT_WITHOUT_ROLE_SN,
                NON_PLATFORM_ACCOUNT_WITH_ROLE_SN);
        return result;
    }

    private Long createMonthlyAvailableLedger() {
        return ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(MONTHLY_PERIOD_ID));
    }

    private void assertFundingBasicLedger(LedgerDTO ledger) {
        assertThat(ledger.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(ledger.getSubjectId()).isEqualTo(ACCOUNT_SN);
        assertThat(ledger.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(ledger.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(ledger.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC.name());
        assertThat(ledger.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(ledger.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
        assertThat(ledger.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
        assertThat(ledger.getDebitAmount()).isZero();
        assertThat(ledger.getCreditAmount()).isZero();
        assertThat(ledger.getNormalBalance()).isZero();
        assertThat(ledger.getSettlementPolicy()).isEqualTo("RT");
        assertThat(ledger.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(ledger.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(ledger.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            DefaultFundsAccountQueryServiceImpl.class,
            FundingAccountServiceImpl.class
    })
    static class Config {
    }
}
