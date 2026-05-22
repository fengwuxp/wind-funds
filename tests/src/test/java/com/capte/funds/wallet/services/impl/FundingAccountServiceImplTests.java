package com.capte.funds.wallet.services.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.capte.funds.ledger.dto.LedgerDTO;
import com.capte.funds.ledger.query.LedgerQuery;
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
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.FundingAccountType;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    private static final String OWNER_ID = "owner_fas_basic";

    @Autowired
    private FundingAccountService fundingAccountService;

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

    @BeforeEach
    void setUpFundingAccountServiceTestData() {
        cleanupFundingAccountServiceTestData();
    }

    @AfterEach
    void tearDownFundingAccountServiceTestData() {
        cleanupFundingAccountServiceTestData();
    }

    private void cleanupFundingAccountServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn = ?", ACCOUNT_SN);
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
