package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.impl.LedgerServiceImpl;
import com.capte.funds.ledger.query.LedgerQuery;
import com.capte.funds.ledger.service.LedgerService;
import com.capte.funds.wallet.model.dto.BudgetGroupDTO;
import com.capte.funds.wallet.model.dto.CreditAccountDTO;
import com.capte.funds.wallet.model.request.CreateBudgetGroupRequest;
import com.capte.funds.wallet.model.request.CreateCreditAccountRequest;
import com.capte.funds.wallet.model.request.InitializeSubjectLedgerRequest;
import com.capte.funds.wallet.service.BudgetGroupService;
import com.capte.funds.wallet.service.CreditAccountService;
import com.capte.funds.wallet.service.SubjectLedgerInitializer;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.CreditFundsAccountType;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 信用账户和预算组控制账本初始化服务层测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ControlAccountLedgerInitializationTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ControlAccountLedgerInitializationTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "credit_control_basic";

    private static final String NON_LIFETIME_CREDIT_ACCOUNT_SN = "credit_control_monthly";

    private static final String BUDGET_GROUP_SN = "budget_control_basic";

    private static final String CUSTOM_BUDGET_GROUP_SN = "budget_control_custom";

    private static final String CUSTOM_PERIOD_ID = "CONTRACT-2026-H1";

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    private static final String CUSTOM_PERIOD_POLICY = "CONTRACT_H1_RULE_V1";

    private static final String OWNER_ID = "owner_control_basic";

    private static final Map<LedgerSubjectCode, EntrySide> EXPECTED_NORMAL_SIDES = Map.of(
            LedgerSubjectCode.LIMIT, EntrySide.DEBIT,
            LedgerSubjectCode.AVAILABLE, EntrySide.CREDIT,
            LedgerSubjectCode.AUTHORIZATION, EntrySide.CREDIT
    );

    private static final Map<LedgerSubjectCode, Boolean> EXPECTED_NEGATIVE_RULES = Map.of(
            LedgerSubjectCode.LIMIT, Boolean.FALSE,
            LedgerSubjectCode.AVAILABLE, Boolean.TRUE,
            LedgerSubjectCode.AUTHORIZATION, Boolean.FALSE
    );

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private BudgetGroupService budgetGroupService;

    @Autowired
    private SubjectLedgerInitializer subjectLedgerInitializer;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testCreateCreditAccountShouldInitializeLifetimeControlLedgers() {
        Long accountId = creditAccountService.createCreditAccount(createCreditAccountRequest());

        CreditAccountDTO account = creditAccountService.getCreditAccountById(accountId);
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.CREDIT_ACCOUNT, CREDIT_ACCOUNT_SN);

        assertThat(account.getSn()).isEqualTo(CREDIT_ACCOUNT_SN);
        assertThat(account.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(account.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(account.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.CREDIT_BASIC);
        assertThat(account.getLedgerIds()).containsOnlyKeys(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.CREDIT_ACCOUNT,
                CREDIT_ACCOUNT_SN,
                LedgerProfileCode.CREDIT_BASIC,
                AccountBalancePeriodType.LIFETIME,
                AccountBalancePeriodType.LIFETIME.name()));
    }

    @Test
    void testCreateCreditAccountShouldRejectNonLifetimeWithoutPeriodId() {
        assertThatThrownBy(() -> creditAccountService.createCreditAccount(createCreditAccountRequest()
                .setSn(NON_LIFETIME_CREDIT_ACCOUNT_SN)
                .setPeriodType(AccountBalancePeriodType.MONTHLY)))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(countRows("t_credit_account", "sn", NON_LIFETIME_CREDIT_ACCOUNT_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", NON_LIFETIME_CREDIT_ACCOUNT_SN)).isZero();
    }

    @Test
    void testCreateBudgetGroupShouldInitializeLifetimeControlLedgersByDefault() {
        Long budgetGroupId = budgetGroupService.createBudgetGroup(createBudgetGroupRequest());

        BudgetGroupDTO budgetGroup = budgetGroupService.getBudgetGroupById(budgetGroupId);
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.BUDGET_GROUP, BUDGET_GROUP_SN);

        assertThat(budgetGroup.getSn()).isEqualTo(BUDGET_GROUP_SN);
        assertThat(budgetGroup.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(budgetGroup.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(budgetGroup.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.BUDGET_BASIC);
        assertThat(budgetGroup.getLedgerIds()).containsOnlyKeys(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.BUDGET_GROUP,
                BUDGET_GROUP_SN,
                LedgerProfileCode.BUDGET_BASIC,
                AccountBalancePeriodType.LIFETIME,
                AccountBalancePeriodType.LIFETIME.name()));
    }

    @Test
    void testCreateBudgetGroupShouldInitializeMonthlyControlLedgersWhenSpecified() {
        Long budgetGroupId = budgetGroupService.createBudgetGroup(createBudgetGroupRequest()
                .setPeriodType(AccountBalancePeriodType.MONTHLY)
                .setPeriodId(MONTHLY_PERIOD_ID));

        BudgetGroupDTO budgetGroup = budgetGroupService.getBudgetGroupById(budgetGroupId);
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.BUDGET_GROUP, BUDGET_GROUP_SN);

        assertThat(budgetGroup.getSn()).isEqualTo(BUDGET_GROUP_SN);
        assertThat(budgetGroup.getStatus()).isEqualTo(FundsAccountStatus.ACTIVE);
        assertThat(budgetGroup.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(budgetGroup.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.BUDGET_BASIC);
        assertThat(budgetGroup.getLedgerIds()).containsOnlyKeys(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.BUDGET_GROUP,
                BUDGET_GROUP_SN,
                LedgerProfileCode.BUDGET_BASIC,
                AccountBalancePeriodType.MONTHLY,
                MONTHLY_PERIOD_ID));
    }

    @Test
    void testCreateBudgetGroupShouldRejectCustomCycleWithoutPeriodId() {
        assertThatThrownBy(() -> budgetGroupService.createBudgetGroup(customCycleBudgetGroupRequest()))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(countRows("t_budget_group", "sn", CUSTOM_BUDGET_GROUP_SN)).isZero();
        assertThat(countRows("t_ledger", "subject_id", CUSTOM_BUDGET_GROUP_SN)).isZero();
    }

    @Test
    void testCreateBudgetGroupShouldInitializeCustomCycleControlLedgers() {
        Long budgetGroupId = budgetGroupService.createBudgetGroup(
                customCycleBudgetGroupRequest().setPeriodId(CUSTOM_PERIOD_ID));

        BudgetGroupDTO budgetGroup = budgetGroupService.getBudgetGroupById(budgetGroupId);
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.BUDGET_GROUP, CUSTOM_BUDGET_GROUP_SN);

        assertThat(budgetGroup.getSn()).isEqualTo(CUSTOM_BUDGET_GROUP_SN);
        assertThat(budgetGroup.getPeriodType()).isEqualTo(AccountBalancePeriodType.CUSTOM_CYCLE);
        assertThat(budgetGroup.getPeriodPolicy()).isEqualTo(CUSTOM_PERIOD_POLICY);
        assertThat(budgetGroup.getLedgerIds()).containsOnlyKeys(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.BUDGET_GROUP,
                CUSTOM_BUDGET_GROUP_SN,
                LedgerProfileCode.BUDGET_BASIC,
                AccountBalancePeriodType.CUSTOM_CYCLE,
                CUSTOM_PERIOD_ID));
    }

    @Test
    void testInitializeCustomCycleLedgersShouldRequirePeriodId() {
        assertThatThrownBy(() -> subjectLedgerInitializer.initializeRequiredLedgers(customBudgetLedgerRequest()))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(countRows("t_ledger", "subject_id", CUSTOM_BUDGET_GROUP_SN)).isZero();
    }

    @Test
    void testInitializeCustomCycleLedgersShouldUseExplicitPeriodId() {
        Map<LedgerSubjectCode, Long> ledgerIds = subjectLedgerInitializer.initializeRequiredLedgers(
                customBudgetLedgerRequest().setPeriodId(CUSTOM_PERIOD_ID));
        List<LedgerDTO> ledgers = loadLedgers(FundsSubjectType.BUDGET_GROUP, CUSTOM_BUDGET_GROUP_SN);

        assertThat(ledgerIds).containsOnlyKeys(EXPECTED_NORMAL_SIDES.keySet());
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).allSatisfy(ledger -> assertControlLedger(
                ledger,
                FundsSubjectType.BUDGET_GROUP,
                CUSTOM_BUDGET_GROUP_SN,
                LedgerProfileCode.BUDGET_BASIC,
                AccountBalancePeriodType.CUSTOM_CYCLE,
                CUSTOM_PERIOD_ID));
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
                BUDGET_GROUP_SN,
                CUSTOM_BUDGET_GROUP_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn IN (?, ?)",
                CREDIT_ACCOUNT_SN,
                NON_LIFETIME_CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_budget_group WHERE sn IN (?, ?)",
                BUDGET_GROUP_SN,
                CUSTOM_BUDGET_GROUP_SN);
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

    private CreateBudgetGroupRequest createBudgetGroupRequest() {
        return new CreateBudgetGroupRequest()
                .setSn(BUDGET_GROUP_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setBudgetType(DefaultFundsAccountType.BUDGET_GROUP.name())
                .setCurrency(CurrencyIsoCode.USD);
    }

    private CreateBudgetGroupRequest customCycleBudgetGroupRequest() {
        return createBudgetGroupRequest()
                .setSn(CUSTOM_BUDGET_GROUP_SN)
                .setPeriodType(AccountBalancePeriodType.CUSTOM_CYCLE)
                .setPeriodId(null)
                .setPeriodPolicy(CUSTOM_PERIOD_POLICY);
    }

    private InitializeSubjectLedgerRequest customBudgetLedgerRequest() {
        return new InitializeSubjectLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(CUSTOM_BUDGET_GROUP_SN)
                .setSubjectType(FundsSubjectType.BUDGET_GROUP)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.BUDGET_BASIC)
                .setPeriodType(AccountBalancePeriodType.CUSTOM_CYCLE);
    }

    private List<LedgerDTO> loadLedgers(FundsSubjectType subjectType, String subjectId) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(subjectId)
                        .setSubjectType(subjectType.name())
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    private long countRows(String tableName, String columnName, Object value) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class, value);
        return result;
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
        assertThat(ledger.getPeriodType()).isEqualTo(periodType);
        assertThat(ledger.getPeriodId()).isEqualTo(periodId);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            CreditAccountServiceImpl.class,
            BudgetGroupServiceImpl.class
    })
    static class Config {
    }
}
