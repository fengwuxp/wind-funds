package com.wind.funds.ledger.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

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
    void testCreateLedgerShouldFillDefaultLedgerFactsWithoutLedgerTransactionMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(DEFAULT_SUBJECT_ID)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setLedgerSubjectCode(LedgerSubjectCode.SUSPENSE)
                .setCurrency(CurrencyIsoCode.USD));

        LedgerDTO ledger = ledgerService.getLedgerById(ledgerId);
        assertThat(ledger.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(ledger.getSubjectId()).isEqualTo(DEFAULT_SUBJECT_ID);
        assertThat(ledger.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(ledger.getLedgerProfileCode()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(ledger.getLedgerProfileVersion()).isEqualTo(1);
        assertThat(ledger.getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.SUSPENSE);
        assertThat(ledger.getLedgerSubjectCategory()).isEqualTo(LedgerSubjectCategory.MEMO);
        assertThat(ledger.getNormalBalanceSide()).isEqualTo(EntrySide.DEBIT);
        assertThat(ledger.getAllowNegative()).isFalse();
        assertThat(ledger.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(ledger.getDebitAmount()).isZero();
        assertThat(ledger.getCreditAmount()).isZero();
        assertThat(ledger.getNormalBalance()).isZero();
        assertThat(ledger.getSettlementPolicy()).isEqualTo("RT");
        assertThat(ledger.getCutOffTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(ledger.getPeriodType()).isEqualTo(AccountBalancePeriodType.LIFETIME);
        assertThat(ledger.getPeriodId()).isEqualTo(AccountBalancePeriodType.LIFETIME.name());
        assertThat(countLedgers(DEFAULT_SUBJECT_ID)).isOne();
        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
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
    @Import(LedgerServiceImpl.class)
    static class Config {
    }
}
