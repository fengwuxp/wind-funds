package com.wind.funds.wallet.services.impl;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.common.exception.BaseException;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountBalanceView;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountOwner;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.ImmutableFundsAccount;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 真实资金账户服务层流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundingAccountServiceImplTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FundingAccountServiceImplTests extends AbstractFundsServiceTest {

    private static final String ACCOUNT_SN = "funding_account_service_basic";

    private static final String PLATFORM_ACCOUNT_WITHOUT_ROLE_SN = "platform_account_without_role";

    private static final String NON_PLATFORM_ACCOUNT_WITH_ROLE_SN = "fund_account_with_role";

    private static final String OWNER_ID = "owner_fas_basic";

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    private static final Map<LedgerSubjectCode, Boolean> EXPECTED_ALLOW_NEGATIVE_RULES = Map.of(
            LedgerSubjectCode.AVAILABLE, Boolean.TRUE,
            LedgerSubjectCode.FROZEN, Boolean.FALSE,
            LedgerSubjectCode.AUTHORIZATION, Boolean.FALSE
    );

    private static final String UNQUOTED_SENSITIVE_CONTEXT_VARIABLES =
            "{processorPayload:{secretKey:\"secret-value\"";

    private static final String UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES =
            "{externalAccount:{bankAccountNo:\"123456789012\"";

    private static final String UNQUOTED_CORE_BENEFIT_CONTEXT_VARIABLES =
            "{benefitPayload:{currentMarketingRule:\"RULE-WALLET-001\"";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private FundsAccountQueryService fundsAccountQueryService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateFundingAccountShouldInitializeRequiredLedgers() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long accountId = fundingAccountService.createFundingAccount(createFundingAccountRequest());

        FundingAccountDTO account = fundingAccountService.getFundingAccountById(accountId);
        List<LedgerDTO> ledgers = loadLedgers();

        assertThat(account.getSn()).isEqualTo(ACCOUNT_SN);
        assertThat(account.getState()).isEqualTo(FundsAccountState.ACTIVE);
        assertThat(account.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC);
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).extracting(LedgerDTO::getLedgerSubjectCode)
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION);
        assertThat(ledgers).allSatisfy(this::assertFundingBasicLedger);
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
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

    /**
     * 场景：运营创建真实资金账户时把外部账户号或通道密钥放入扩展上下文。
     * 输入：contextVariables 含嵌套 bankAccountNo 字段，或嵌套 secretKey 字段。
     * 输出：创建被拒绝，不留下资金账户、账本或账务事实。
     * 红线：资金账户管理对象不得成为外部账户号、PAN、CVV 或 token secret 的旁路存储。
     */
    @Test
    void testCreateFundingAccountShouldRejectSensitiveContextVariablesWithoutAccountOrLedger() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setContextVariables("{\"externalAccount\":{\"bankAccountNo\":\"123456789012\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setContextVariables(UNQUOTED_SENSITIVE_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");

        assertThat(countFundingAccounts()).isZero();
        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建真实资金账户时把权益实时规则或资金责任放入扩展上下文。
     * 输入：contextVariables 含 fundingNature 或当前营销规则，包含标准 JSON 和坏 JSON 两类输入。
     * 输出：创建被拒绝，不留下资金账户、账本或账务事实。
     * 红线：资金账户管理对象不得成为权益核心事实的旁路存储。
     */
    @Test
    void testCreateFundingAccountShouldRejectCoreBenefitContextVariablesWithoutAccountOrLedger() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setContextVariables("{\"benefitPayload\":{\"fundingNature\":\"COUPON\"}}")))
                .hasMessageContaining("wallet.contextVariables must not contain core benefit field: fundingNature");
        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()
                .setContextVariables(UNQUOTED_CORE_BENEFIT_CONTEXT_VARIABLES)))
                .hasMessageContaining(
                        "wallet.contextVariables must not contain core benefit field: currentMarketingRule");

        assertThat(countFundingAccounts()).isZero();
        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testInitializeRequiredLedgersShouldReuseExistingLedgers() {
        ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(2)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.TRUE)
                .setCurrency(CurrencyIsoCode.USD)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()))
                .isInstanceOf(BaseException.class);

        List<LedgerDTO> ledgers = loadLedgers();
        assertThat(countFundingAccounts()).isZero();
        assertThat(ledgers).singleElement().satisfies(ledger -> {
            assertThat(ledger.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(ledger.getLedgerProfileVersion()).isEqualTo(2);
        });
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);

        assertLedgerControlledInitializationOwner(FundingAccountServiceImpl.class);
        assertLedgerControlledInitializationOwner(CreditAccountServiceImpl.class);
    }

    @Test
    void testCreateFundingAccountShouldRollbackRequiredLedgerGroupAfterLaterDrift() {
        createFundingLedger(LedgerSubjectCode.AVAILABLE, 1, true);
        createFundingLedger(LedgerSubjectCode.FROZEN, 2, false);
        List<LedgerDTO> beforeLedgers = loadLedgers();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> fundingAccountService.createFundingAccount(createFundingAccountRequest()))
                .isInstanceOf(BaseException.class);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(countFundingAccounts()).isZero();
        softly.assertThat(loadLedgers()).containsExactlyInAnyOrderElementsOf(beforeLedgers);
        softly.assertThat(ledgerFactSnapshot(jdbcTemplate)).isEqualTo(before);
        softly.assertAll();
    }

    private void assertLedgerControlledInitializationOwner(Class<?> serviceType) {
        List<Class<?>> dependencies = Arrays.stream(serviceType.getDeclaredFields())
                .map(Field::getType)
                .toList();
        assertThat(dependencies).contains(LedgerService.class);
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
     * 输出：账户元数据查询不受多周期账本影响，余额视图只返回默认 LIFETIME 周期。
     * 红线：跨周期余额必须走显式周期查询。
     */
    @Test
    void testFundingAccountDefaultBalanceShouldKeepLifetimeLedgersWhenMonthlyBucketCoexists() {
        Long accountId = fundingAccountService.createFundingAccount(createFundingAccountRequest());
        createMonthlyAvailableLedger();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        FundingAccountDTO account = fundingAccountService.getFundingAccountById(accountId);
        FundsAccount accountView = fundsAccountQueryService.getAccount(TENANT_ID,
                FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT));
        FundsAccountBalanceView balanceView = fundsAccountQueryService.getBalance(TENANT_ID,
                FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT));

        assertThat(account.getSn()).isEqualTo(ACCOUNT_SN);
        assertThat(account.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC);
        assertThat(accountView.getCapabilities())
                .containsExactlyInAnyOrder(FundsAccountCapability.RECEIVE,
                        FundsAccountCapability.PAY,
                        FundsAccountCapability.WITHDRAW);
        assertThat(accountView.canReceive()).isTrue();
        assertThat(accountView.canPay()).isTrue();
        assertThat(accountView.canWithdraw()).isTrue();
        assertThat(balanceView.getBalanceBuckets()).containsOnlyKeys(LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FROZEN, LedgerSubjectCode.AUTHORIZATION);
        assertThat(balanceView.getBalanceBuckets().values())
                .extracting(LedgerBalanceBucket::periodType)
                .containsOnly(AccountBalancePeriodType.LIFETIME);
        assertThat(balanceView.getBalanceBuckets().values())
                .extracting(LedgerBalanceBucket::periodId)
                .containsOnly(AccountBalancePeriodType.LIFETIME.name());
        assertThat(loadLedgers()).hasSize(4);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：资金账户快照没有显式声明账户能力。
     * 输入：ACTIVE 账户但 capabilities 为空。
     * 输出：不具备付款、收款或提现能力。
     * 红线：资金能力必须显式配置，不能把缺失能力字段默认解释成全部能力。
     */
    @Test
    void testFundsAccountWithoutExplicitCapabilitiesShouldDenyMoneyActions() {
        FundsAccount account = ImmutableFundsAccount.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .accountId(FundsAccountId.immutable(ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT))
                .owner(FundsAccountOwner.of(OWNER_ID, FundsAccountOwnerType.USER))
                .state(FundsAccountState.ACTIVE)
                .currency(CurrencyIsoCode.USD)
                .version(1)
                .build();

        assertThat(account.getCapabilities()).isEmpty();
        assertThat(account.canPay()).isFalse();
        assertThat(account.canReceive()).isFalse();
        assertThat(account.canWithdraw()).isFalse();
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

    private Long createFundingLedger(LedgerSubjectCode subjectCode, int profileVersion, boolean allowNegative) {
        return ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(profileVersion)
                .setLedgerSubjectCode(subjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(allowNegative)
                .setCurrency(CurrencyIsoCode.USD)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
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
        assertThat(ledger.getAllowNegative())
                .isEqualTo(EXPECTED_ALLOW_NEGATIVE_RULES.get(ledger.getLedgerSubjectCode()));
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
            LedgerProfileCatalog.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            FundingAccountServiceImpl.class
    })
    static class Config {
    }
}
