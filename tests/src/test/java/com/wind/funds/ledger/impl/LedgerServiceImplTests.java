package com.wind.funds.ledger.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.common.exception.BaseException;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerState;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.ledger.request.UpdateLedgerStateRequest;
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 账本服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        LedgerServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerServiceImplTests extends AbstractFundsServiceTest {

    private static final String SUBJECT_ID = "ledger_period_contract";

    private static final String DEFAULT_SUBJECT_ID = "ledger_defaults_contract";

    private static final String MONTHLY_PERIOD_ID = "2026-05";

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：调用底层账本服务创建月度账本，但调用方未传周期标识。
     * 输入：periodType=MONTHLY，periodId=null。
     * 输出：创建请求失败，且不落 t_ledger 事实。
     * 红线：底层账本服务不得用当前月份静默补齐非 LIFETIME 的 periodId。
     */
    @Test
    void testCreateNonLifetimeLedgerShouldRejectMissingPeriodId() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> ledgerService.createLedger(createLedgerRequest(
                AccountBalancePeriodType.MONTHLY, null)))
                .hasMessageContaining("非生命周期账本周期 periodId 不能为空");

        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateNonLifetimeLedgerShouldUseExplicitPeriodId() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long ledgerId = ledgerService.createLedger(createLedgerRequest(
                AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID));

        LedgerDTO ledger = ledgerService.getLedgerById(ledgerId);
        assertThat(ledger.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(ledger.getSubjectId()).isEqualTo(SUBJECT_ID);
        assertThat(ledger.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(ledger.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC.name());
        assertThat(ledger.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(ledger.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        assertThat(ledger.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.ASSET);
        assertThat(ledger.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(ledger.getAllowNegative()).isFalse();
        assertThat(ledger.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(ledger.getDebitAmount()).isZero();
        assertThat(ledger.getCreditAmount()).isZero();
        assertThat(ledger.getNormalBalance()).isZero();
        assertThat(ledger.getState()).isEqualTo(LedgerState.ACTIVE);
        assertThat(ledger.getSettlementPolicy()).isEqualTo("RT");
        assertThat(ledger.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(ledger.getPeriodType()).isEqualTo(AccountBalancePeriodType.MONTHLY);
        assertThat(ledger.getPeriodId()).isEqualTo(MONTHLY_PERIOD_ID);
        assertThat(countLedgers()).isOne();
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：上层显式初始化账本时只传入必需主体、账目和币种。
     * 输入：未传 profile、科目分类、正常余额方向、负余额规则、结算策略、切日和周期。
     * 输出：账本服务补齐默认账本事实，并使用 LIFETIME 周期。
     * 红线：底层建账默认值必须稳定可追溯，不得产生账务交易、posting plan 或 entry。
     */
    @Test
    void testCreateLedgerShouldFillDefaultLedgerFactsWithoutLedgerTransactionMutation() throws Exception {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Class<?> requestType = loadControlledInitializationRequest();
        Method command = controlledInitializationCommand(requestType);
        Object request = controlledInitializationRequest(requestType, 1);
        invokeControlledInitialization(command, request);

        List<LedgerDTO> ledgers = queryLedgers(DEFAULT_SUBJECT_ID);
        assertThat(ledgers).hasSize(3);
        assertThat(ledgers).extracting(LedgerDTO::getLedgerSubjectCode)
                .containsExactlyInAnyOrder(
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCode.FROZEN,
                        LedgerSubjectCode.AUTHORIZATION);
        assertThat(ledgers).allSatisfy(ledger -> {
            assertThat(ledger.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(ledger.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
            assertThat(ledger.getLedgerProfileCode()).isEqualTo(LedgerProfileCode.FUNDING_BASIC.name());
            assertThat(ledger.getLedgerProfileVersion()).isEqualTo(1);
            assertThat(ledger.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.LIABILITY);
            assertThat(ledger.getNormalBalanceSide()).isEqualTo(EntrySide.CREDIT);
            assertThat(ledger.getAllowNegative()).isEqualTo(ledger.getLedgerSubjectCode() == LedgerSubjectCode.AVAILABLE);
            assertThat(ledger.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
            assertThat(ledger.getSettlementPolicy()).isEqualTo("RT");
            assertThat(ledger.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
            assertThat(ledger.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
            assertThat(ledger.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        });

        invokeControlledInitialization(command, controlledInitializationRequest(requestType, 1));
        assertThat(queryLedgers(DEFAULT_SUBJECT_ID)).containsExactlyInAnyOrderElementsOf(ledgers);

        jdbcTemplate.update("UPDATE t_ledger SET ledger_profile_version = 2"
                        + " WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = ?",
                TENANT_ID, DEFAULT_SUBJECT_ID, LedgerSubjectCode.AVAILABLE.name());
        assertThatThrownBy(() -> invokeControlledInitialization(
                command, controlledInitializationRequest(requestType, 1)))
                .isInstanceOf(BaseException.class);
        assertThat(countLedgers(DEFAULT_SUBJECT_ID)).isEqualTo(3);
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    private Class<?> loadControlledInitializationRequest() {
        Class<?> requestType;
        try {
            requestType = Class.forName("com.wind.funds.ledger.request.InitializeSubjectLedgerRequest");
        } catch (ClassNotFoundException ignored) {
            requestType = null;
        }
        assertThat(requestType)
                .as("Ledger-owned controlled initialization request must exist")
                .isNotNull();
        return requestType;
    }

    private Method controlledInitializationCommand(Class<?> requestType) {
        Method command;
        try {
            command = LedgerService.class.getMethod("initializeRequiredLedgers", requestType);
        } catch (NoSuchMethodException ignored) {
            command = null;
        }
        assertThat(command)
                .as("LedgerService must own controlled required-ledger initialization")
                .isNotNull();
        return command;
    }

    private Object controlledInitializationRequest(Class<?> requestType, int profileVersion) throws Exception {
        Object request = requestType.getDeclaredConstructor().newInstance();
        requestType.getMethod("setTenantId", Long.class).invoke(request, TENANT_ID);
        requestType.getMethod("setSubjectId", String.class).invoke(request, DEFAULT_SUBJECT_ID);
        requestType.getMethod("setSubjectType", FundsSubjectType.class)
                .invoke(request, FundsSubjectType.FUNDING_ACCOUNT);
        requestType.getMethod("setCurrency", CurrencyIsoCode.class).invoke(request, CurrencyIsoCode.USD);
        requestType.getMethod("setLedgerProfileCode", LedgerProfileCode.class)
                .invoke(request, LedgerProfileCode.FUNDING_BASIC);
        requestType.getMethod("setLedgerProfileVersion", Integer.class).invoke(request, profileVersion);
        requestType.getMethod("setPeriodType", AccountBalancePeriodType.class)
                .invoke(request, AccountBalancePeriodType.LIFETIME);
        requestType.getMethod("setPeriodId", String.class)
                .invoke(request, AccountBalancePeriodType.LIFETIME.name());
        return request;
    }

    private void invokeControlledInitialization(Method command, Object request) {
        try {
            command.invoke(ledgerService, request);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(exception.getCause());
        }
    }

    private List<LedgerDTO> queryLedgers(String subjectId) {
        return ledgerService.queryLedgers(new LedgerQuery()
                        .setTenantId(TENANT_ID)
                        .setSubjectId(subjectId)
                        .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                        .setCurrency(CurrencyIsoCode.USD),
                DefaultPageQueryOptions.defaults(10)).getRecords();
    }

    /**
     * 场景：调用方显式传入固定科目类别不匹配的正常余额方向。
     * 输入：ASSET 账本使用 CREDIT 正常余额方向。
     * 输出：建账入口拒绝请求，账本事实保持不变。
     * 红线：固定方向科目不得创建出会反向投影余额的账本 profile。
     */
    @Test
    void testCreateLedgerShouldRejectFixedCategoryNormalBalanceSideMismatchBeforePersistence() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> ledgerService.createLedger(createLedgerRequest(
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name())
                .setNormalBalanceSide(EntrySide.CREDIT)))
                .hasMessageContaining("账本科目类别与正常余额方向不一致");

        assertThat(countLedgers()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testUpdateLedgerStateShouldAllowReactivatingSuspendedLedger() {
        Long ledgerId = ledgerService.createLedger(createLedgerRequest(
                AccountBalancePeriodType.MONTHLY, MONTHLY_PERIOD_ID));
        ledgerService.updateLedgerState(new UpdateLedgerStateRequest()
                .setId(ledgerId)
                .setState(LedgerState.SUSPENDED));

        ledgerService.updateLedgerState(new UpdateLedgerStateRequest()
                .setId(ledgerId)
                .setState(LedgerState.ACTIVE));

        assertThat(ledgerService.getLedgerById(ledgerId).getState()).isEqualTo(LedgerState.ACTIVE);
    }

    @BeforeEach
    void setUpLedgerServiceTestData() {
        cleanupLedgerServiceTestData();
    }

    @AfterEach
    void tearDownLedgerServiceTestData() {
        cleanupLedgerServiceTestData();
    }

    private void cleanupLedgerServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)", SUBJECT_ID, DEFAULT_SUBJECT_ID);
    }

    private long countLedgers() {
        return countLedgers(SUBJECT_ID);
    }

    private long countLedgers(String subjectId) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger WHERE subject_id = ?",
                Long.class, subjectId);
        return result;
    }

    private CreateLedgerRequest createLedgerRequest(AccountBalancePeriodType periodType, String periodId) {
        return new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(SUBJECT_ID)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.ASSET)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(periodType)
                .setPeriodId(periodId);
    }

    @Configuration
    @Import({LedgerServiceImpl.class, LedgerProfileCatalog.class})
    static class Config {
    }
}
