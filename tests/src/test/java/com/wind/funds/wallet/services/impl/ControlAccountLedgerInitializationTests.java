package com.wind.funds.wallet.services.impl;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.model.dto.SpendControlScopeDTO;
import com.wind.funds.wallet.model.dto.CreditAccountDTO;
import com.wind.funds.wallet.model.request.CreateSpendControlScopeRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.wind.funds.wallet.service.SpendControlScopeService;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.SubjectLedgerInitializer;
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
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 信用账户账本初始化、支出控制范围元数据创建服务层测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ControlAccountLedgerInitializationTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ControlAccountLedgerInitializationTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "credit_control_basic";

    private static final String NON_LIFETIME_CREDIT_ACCOUNT_SN = "credit_control_monthly";

    private static final String SPEND_CONTROL_SCOPE_SN = "budget_control_basic";

    private static final String CUSTOM_SPEND_CONTROL_SCOPE_SN = "budget_control_custom";

    private static final String CUSTOM_PERIOD_ID = "CONTRACT-2026-H1";

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    private static final String NEXT_MONTHLY_PERIOD_ID = "2026-06";

    private static final String CUSTOM_PERIOD_POLICY = "CONTRACT_H1_RULE_V1";

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = "SPEND_CONTROL_SCOPE";

    private static final String OWNER_ID = "owner_control_basic";

    private static final String UNQUOTED_PAYMENT_CONTEXT_VARIABLES =
            "{processorPayload:{secretKey:\"secret-value\"";

    private static final String UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES =
            "{externalAccount:{bankAccountNo:\"123456789012\"";

    private static final Map<LedgerSubjectCode, EntrySide> EXPECTED_NORMAL_SIDES = Map.of(
            LedgerSubjectCode.LIMIT, EntrySide.DEBIT,
            LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
            LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT,
            LedgerSubjectCode.OUTSTANDING, EntrySide.CREDIT
    );

    private static final Map<LedgerSubjectCode, Boolean> EXPECTED_NEGATIVE_RULES = Map.of(
            LedgerSubjectCode.LIMIT, Boolean.FALSE,
            LedgerSubjectCode.AVAILABLE, Boolean.TRUE,
            LedgerSubjectCode.AUTHORIZATION, Boolean.FALSE,
            LedgerSubjectCode.OUTSTANDING, Boolean.FALSE
    );

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private SpendControlScopeService spendControlScopeService;

    @Autowired
    private FundsAccountQueryService fundsAccountQueryService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private SubjectLedgerInitializer subjectLedgerInitializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateCreditAccountShouldInitializeLifetimeControlLedgers() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long accountId = creditAccountService.createCreditAccount(createCreditAccountRequest());

        CreditAccountDTO account = creditAccountService.getCreditAccountById(accountId);
        FundsAccount accountView = fundsAccountQueryService.getAccount(
                FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.CREDIT_ACCOUNT, CREDIT_ACCOUNT_SN);

        assertThat(account.getSn()).isEqualTo(CREDIT_ACCOUNT_SN);
        assertThat(account.getState()).isEqualTo(FundsAccountState.ACTIVE);
        assertThat(account.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(account.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        assertThat(account.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC);
        assertThat(accountView.getCapabilities()).containsExactly(FundsAccountCapability.PAY);
        assertThat(accountView.canPay()).isTrue();
        assertThat(accountView.canReceive()).isFalse();
        assertThat(accountView.canWithdraw()).isFalse();
        assertThat(ledgers).hasSize(4);
        assertThat(ledgers).extracting(LedgerDTO::getLedgerSubjectCode)
                .containsExactlyInAnyOrderElementsOf(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.CREDIT_ACCOUNT,
                CREDIT_ACCOUNT_SN,
                LedgerProfileCode.CREDIT_BASIC,
                AccountBalancePeriodType.LIFETIME,
                AccountBalancePeriodType.LIFETIME.name()));
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateCreditAccountShouldRejectNonLifetimeWithoutPeriodId() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setSn(NON_LIFETIME_CREDIT_ACCOUNT_SN)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(countRows("t_credit_account", "sn", NON_LIFETIME_CREDIT_ACCOUNT_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", NON_LIFETIME_CREDIT_ACCOUNT_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateCreditAccountShouldInitializeMonthlyControlLedgersWhenSpecified() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long accountId = creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setSn(NON_LIFETIME_CREDIT_ACCOUNT_SN)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(MONTHLY_PERIOD_ID));

        CreditAccountDTO account = creditAccountService.getCreditAccountById(accountId);
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.CREDIT_ACCOUNT, NON_LIFETIME_CREDIT_ACCOUNT_SN);

        assertThat(account.getSn()).isEqualTo(NON_LIFETIME_CREDIT_ACCOUNT_SN);
        assertThat(account.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(account.getPeriodId()).isEqualTo(MONTHLY_PERIOD_ID);
        assertThat(ledgers).hasSize(4);
        assertThat(ledgers).extracting(LedgerDTO::getLedgerSubjectCode)
                .containsExactlyInAnyOrderElementsOf(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.CREDIT_ACCOUNT,
                NON_LIFETIME_CREDIT_ACCOUNT_SN,
                LedgerProfileCode.CREDIT_BASIC,
                AccountBalancePeriodType.MONTHLY,
                MONTHLY_PERIOD_ID));
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testSubjectLedgerInitializerShouldKeepOldPeriodLedgersWhenCreatingNextPeriod() {
        creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setSn(NON_LIFETIME_CREDIT_ACCOUNT_SN)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(MONTHLY_PERIOD_ID));
        List<LedgerDTO> oldPeriodLedgers = loadLedgers(
                FundsSubjectType.CREDIT_ACCOUNT,
                NON_LIFETIME_CREDIT_ACCOUNT_SN,
                AccountBalancePeriodType.MONTHLY,
                MONTHLY_PERIOD_ID);

        Map<LedgerSubjectCode, Long> nextPeriodLedgerIds = subjectLedgerInitializer.initializeRequiredLedgers(
                initializeCreditSubjectLedgerRequest(NEXT_MONTHLY_PERIOD_ID));
        Map<LedgerSubjectCode, Long> reusedNextPeriodLedgerIds = subjectLedgerInitializer.initializeRequiredLedgers(
                initializeCreditSubjectLedgerRequest(NEXT_MONTHLY_PERIOD_ID));

        List<LedgerDTO> nextPeriodLedgers = loadLedgers(
                FundsSubjectType.CREDIT_ACCOUNT,
                NON_LIFETIME_CREDIT_ACCOUNT_SN,
                AccountBalancePeriodType.MONTHLY,
                NEXT_MONTHLY_PERIOD_ID);
        List<LedgerDTO> allLedgers = loadLedgers(FundsSubjectType.CREDIT_ACCOUNT, NON_LIFETIME_CREDIT_ACCOUNT_SN);

        assertThat(nextPeriodLedgerIds).isEqualTo(reusedNextPeriodLedgerIds);
        assertThat(nextPeriodLedgerIds).containsOnlyKeys(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(oldPeriodLedgers).hasSize(4);
        assertThat(nextPeriodLedgers).hasSize(4);
        assertThat(allLedgers).hasSize(8);
        assertThat(oldPeriodLedgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.CREDIT_ACCOUNT,
                NON_LIFETIME_CREDIT_ACCOUNT_SN,
                LedgerProfileCode.CREDIT_BASIC,
                AccountBalancePeriodType.MONTHLY,
                MONTHLY_PERIOD_ID));
        assertThat(nextPeriodLedgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.CREDIT_ACCOUNT,
                NON_LIFETIME_CREDIT_ACCOUNT_SN,
                LedgerProfileCode.CREDIT_BASIC,
                AccountBalancePeriodType.MONTHLY,
                NEXT_MONTHLY_PERIOD_ID));
    }

    @Test
    void testCreateSpendControlScopeShouldCreateLifetimeMetadataWithoutLedgersByDefault() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long spendControlScopeId = spendControlScopeService.createSpendControlScope(createSpendControlScopeRequest());

        SpendControlScopeDTO spendControlScope = spendControlScopeService.getSpendControlScopeById(spendControlScopeId);
        SpendControlScopeDTO controlScope = spendControlScopeService.getSpendControlScope(
                TENANT_ID,
                SPEND_CONTROL_SCOPE_SN,
                SpendRuleScopeType.SPEND_CONTROL_SCOPE.name());
        List<LedgerDTO> ledgers = loadLedgers(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE, SPEND_CONTROL_SCOPE_SN);

        assertThat(spendControlScope.getSn()).isEqualTo(SPEND_CONTROL_SCOPE_SN);
        assertThat(controlScope.getId()).isEqualTo(spendControlScopeId);
        assertThat(controlScope.getSn()).isEqualTo(SPEND_CONTROL_SCOPE_SN);
        assertThat(spendControlScope.getState()).isEqualTo(FundsAccountState.ACTIVE);
        assertThat(spendControlScope.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(spendControlScope.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        assertThat(ledgers).isEmpty();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendControlScopeShouldCreateMonthlyMetadataWithoutLedgersWhenSpecified() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long spendControlScopeId = spendControlScopeService.createSpendControlScope(createSpendControlScopeRequest()
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(MONTHLY_PERIOD_ID));

        SpendControlScopeDTO spendControlScope = spendControlScopeService.getSpendControlScopeById(spendControlScopeId);
        List<LedgerDTO> ledgers = loadLedgers(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE, SPEND_CONTROL_SCOPE_SN);

        assertThat(spendControlScope.getSn()).isEqualTo(SPEND_CONTROL_SCOPE_SN);
        assertThat(spendControlScope.getState()).isEqualTo(FundsAccountState.ACTIVE);
        assertThat(spendControlScope.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(spendControlScope.getPeriodId()).isEqualTo(MONTHLY_PERIOD_ID);
        assertThat(ledgers).isEmpty();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendControlScopeShouldRejectCustomCycleWithoutPeriodId() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlScopeService.createSpendControlScope(customCycleSpendControlScopeRequest()))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(countRows("t_spend_control_scope", "sn", CUSTOM_SPEND_CONTROL_SCOPE_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", CUSTOM_SPEND_CONTROL_SCOPE_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：企业自定义周期预算缺少周期策略。
     * 输入：periodType = CUSTOM_CYCLE，periodId 已指定，但 periodPolicy 为空。
     * 输出：创建被拒绝，不留下支出控制范围、控制流水或预算投影。
     * 红线：自定义周期缺少规则版本时不得生成预算控制流水或预算投影，避免后续额度跨周期误用。
     */
    @Test
    void testCreateSpendControlScopeShouldRejectCustomCycleWithoutPeriodPolicy() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlScopeService.createSpendControlScope(customCycleSpendControlScopeRequest()
                .setPeriodId(CUSTOM_PERIOD_ID)
                .setPeriodPolicy(null)))
                .hasMessageContaining("自定义周期支出控制范围 periodPolicy 不能为空");

        assertThat(countRows("t_spend_control_scope", "sn", CUSTOM_SPEND_CONTROL_SCOPE_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", CUSTOM_SPEND_CONTROL_SCOPE_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营创建信用账户或支出控制范围时把外部账户号、PAN 或通道密钥放入扩展上下文。
     * 输入：contextVariables 含嵌套敏感值、敏感字段名，或坏 JSON 未加引号敏感字段名。
     * 输出：创建被拒绝，不留下控制账户、支出控制范围、账本或账务事实。
     * 红线：控制类钱包对象不得通过扩展上下文保存敏感支付工具或外部账户原文。
     */
    @Test
    void testCreateControlWalletSubjectsShouldRejectSensitiveContextVariablesWithoutLedger() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setContextVariables("{\"processorPayload\":{\"networkReference\":\"GB82WEST12345698765432\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> spendControlScopeService.createSpendControlScope(createSpendControlScopeRequest()
                .setContextVariables("{\"processorPayload\":{\"secretKey\":\"secret-value\"}}")))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setContextVariables(UNQUOTED_PAYMENT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> spendControlScopeService.createSpendControlScope(createSpendControlScopeRequest()
                .setContextVariables(UNQUOTED_PAYMENT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");
        assertThatThrownBy(() -> spendControlScopeService.createSpendControlScope(createSpendControlScopeRequest()
                .setContextVariables(UNQUOTED_EXTERNAL_ACCOUNT_CONTEXT_VARIABLES)))
                .hasMessageContaining("contextVariables must not contain sensitive wallet fields");

        assertThat(countRows("t_credit_account", "sn", CREDIT_ACCOUNT_SN)).isZero();
        assertThat(countRows("t_spend_control_scope", "sn", SPEND_CONTROL_SCOPE_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", CREDIT_ACCOUNT_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", SPEND_CONTROL_SCOPE_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateSpendControlScopeShouldCreateCustomCycleMetadataWithoutLedgers() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long spendControlScopeId = spendControlScopeService.createSpendControlScope(
                customCycleSpendControlScopeRequest().setPeriodId(CUSTOM_PERIOD_ID));

        SpendControlScopeDTO spendControlScope = spendControlScopeService.getSpendControlScopeById(spendControlScopeId);
        List<LedgerDTO> ledgers = loadLedgers(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE, CUSTOM_SPEND_CONTROL_SCOPE_SN);

        assertThat(spendControlScope.getSn()).isEqualTo(CUSTOM_SPEND_CONTROL_SCOPE_SN);
        assertThat(spendControlScope.getPeriodType()).isEqualTo(AccountBalancePeriodType.CUSTOM_CYCLE);
        assertThat(spendControlScope.getPeriodId()).isEqualTo(CUSTOM_PERIOD_ID);
        assertThat(spendControlScope.getPeriodPolicy()).isEqualTo(CUSTOM_PERIOD_POLICY);
        assertThat(ledgers).isEmpty();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testBudgetBasicProfileShouldNotBeActiveLedgerInitializationProfile() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> defaultLedgerProfileService().getProfile(LedgerProfileCode.BUDGET_BASIC))
                .hasMessageContaining("LedgerProfile 不存在");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpControlAccountLedgerTestData() {
        cleanupControlAccountLedgerTestData();
    }

    @AfterEach
    void tearDownControlAccountLedgerTestData() {
        cleanupControlAccountLedgerTestData();
    }

    private void cleanupControlAccountLedgerTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?, ?)",
                CREDIT_ACCOUNT_SN,
                NON_LIFETIME_CREDIT_ACCOUNT_SN,
                SPEND_CONTROL_SCOPE_SN,
                CUSTOM_SPEND_CONTROL_SCOPE_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn IN (?, ?)",
                CREDIT_ACCOUNT_SN,
                NON_LIFETIME_CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_spend_control_scope WHERE sn IN (?, ?)",
                SPEND_CONTROL_SCOPE_SN,
                CUSTOM_SPEND_CONTROL_SCOPE_SN);
    }

    private CreateCreditAccountRequest createCreditAccountRequest() {
        return new CreateCreditAccountRequest()
                .setSn(CREDIT_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD);
    }

    private CreateSpendControlScopeRequest createSpendControlScopeRequest() {
        return new CreateSpendControlScopeRequest()
                .setSn(SPEND_CONTROL_SCOPE_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setScopeType(SpendRuleScopeType.SPEND_CONTROL_SCOPE.name())
                .setCurrency(CurrencyIsoCode.USD);
    }

    private CreateSpendControlScopeRequest customCycleSpendControlScopeRequest() {
        return createSpendControlScopeRequest()
                .setSn(CUSTOM_SPEND_CONTROL_SCOPE_SN)
                .setPeriodType(AccountBalancePeriodType.CUSTOM_CYCLE)
                .setPeriodId(null)
                .setPeriodPolicy(CUSTOM_PERIOD_POLICY);
    }

    private DefaultLedgerProfileServiceImpl defaultLedgerProfileService() {
        return new DefaultLedgerProfileServiceImpl();
    }

    private List<LedgerDTO> loadLedgers(FundsSubjectType subjectType, String subjectId) {
        return loadLedgers(subjectType.name(), subjectId);
    }

    private List<LedgerDTO> loadLedgers(FundsSubjectType subjectType,
                                        String subjectId,
                                        AccountBalancePeriodType periodType,
                                        String periodId) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(subjectId)
                        .setSubjectType(subjectType.name())
                        .setCurrency(CurrencyIsoCode.USD)
                        .setPeriodType(periodType)
                        .setPeriodId(periodId),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private List<LedgerDTO> loadLedgers(String subjectType, String subjectId) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(subjectId)
                        .setSubjectType(subjectType)
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private long countRows(String tableName, String columnName, Object value) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class, value);
        return result;
    }

    private InitializeSubjectLedgerRequest initializeCreditSubjectLedgerRequest(String periodId) {
        return new InitializeSubjectLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(NON_LIFETIME_CREDIT_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(periodId);
    }

    private void assertControlLedger(LedgerDTO ledger,
                                     FundsSubjectType subjectType,
                                     String subjectId,
                                     LedgerProfileCode profileCode,
                                     AccountBalancePeriodType periodType,
                                     String periodId) {
        assertThat(ledger.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(ledger.getSubjectId()).isEqualTo(subjectId);
        assertThat(ledger.getSubjectType()).isEqualTo(subjectType.name());
        assertThat(ledger.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(ledger.getLedgerProfileCode()).isEqualTo(profileCode.name());
        assertThat(ledger.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(ledger.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.CONTROL);
        assertThat(ledger.getNormalBalanceSide())
                .isEqualTo(EXPECTED_NORMAL_SIDES.get(ledger.getLedgerSubjectCode()));
        assertThat(ledger.getAllowNegative())
                .isEqualTo(EXPECTED_NEGATIVE_RULES.get(ledger.getLedgerSubjectCode()));
        assertThat(ledger.getDebitAmount()).isZero();
        assertThat(ledger.getCreditAmount()).isZero();
        assertThat(ledger.getNormalBalance()).isZero();
        assertThat(ledger.getSettlementPolicy()).isEqualTo("RT");
        assertThat(ledger.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(ledger.getPeriodType()).isEqualTo(periodType);
        assertThat(ledger.getPeriodId()).isEqualTo(periodId);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendControlScopeServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}
