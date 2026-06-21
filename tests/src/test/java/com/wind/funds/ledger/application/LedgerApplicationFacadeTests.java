package com.wind.funds.ledger.application;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.application.impl.DefaultLedgerBalanceProjectionApplicationService;
import com.wind.funds.ledger.application.impl.DefaultLedgerFactQueryApplicationService;
import com.wind.funds.ledger.application.impl.DefaultLedgerPostingApplicationService;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.query.LedgerTransactionQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.spec.ledger.LedgerEntrySpec;
import com.wind.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.transaction.core.Money;
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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ledger application facade 生产入口契约测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        LedgerApplicationFacadeTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerApplicationFacadeTests extends AbstractFundsServiceTest {

    private static final String LEDGER_TRANSACTION_SN = "LT-APP-FACADE-001";

    private static final String POSTING_PLAN_ID = "PLAN-APP-FACADE-001";

    private static final String SOURCE_SUBJECT_ID = "ledger_app_facade_source";

    private static final String TARGET_SUBJECT_ID = "ledger_app_facade_target";

    private static final String SUBJECT_TYPE = FundsSubjectType.FUNDING_ACCOUNT.name();

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final Money TRANSACTION_AMOUNT = Money.immutable(100L, CURRENCY);

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 6, 21, 10, 0);

    @Autowired
    private LedgerPostingApplicationService postingApplicationService;

    @Autowired
    private LedgerFactQueryApplicationService factQueryApplicationService;

    @Autowired
    private LedgerBalanceProjectionApplicationService balanceProjectionApplicationService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpLedgerApplicationFacadeTestData() {
        cleanupLedgerApplicationFacadeTestData();
    }

    @AfterEach
    void tearDownLedgerApplicationFacadeTestData() {
        cleanupLedgerApplicationFacadeTestData();
    }

    /**
     * 场景：跨模块调用方只依赖 ledger application facade。
     * 输入：ledger application 包下的公开接口。
     * 输出：接口不暴露 update/delete 资源突变能力，也不泄漏 DAL/Mapper/Entity 类型。
     * 红线：生产调用入口不能绕过账本交易、分录和投影链路直接更新或删除账务事实。
     */
    @Test
    void testLedgerApplicationFacadesShouldExposeOnlySafeCapabilities() {
        List<Class<?>> facades = List.of(
                LedgerPostingApplicationService.class,
                LedgerFactQueryApplicationService.class,
                LedgerBalanceProjectionApplicationService.class);

        facades.forEach(facade -> {
            assertThat(facade.getPackageName()).isEqualTo("com.wind.funds.ledger.application");
            assertThat(facade.getDeclaredMethods())
                    .extracting(Method::getName)
                    .noneMatch(name -> name.startsWith("update") || name.startsWith("delete"));
            for (Method method : facade.getDeclaredMethods()) {
                assertThat(method.getReturnType().getName()).doesNotContain(".dal.");
                for (Class<?> parameterType : method.getParameterTypes()) {
                    assertThat(parameterType.getName()).doesNotContain(".dal.");
                }
            }
        });
    }

    /**
     * 场景：跨模块调用方通过 application facade 执行一次已完成的账本交易入账。
     * 输入：source / target 两个资金账户、各自 AVAILABLE 账本和一笔借贷平衡的账本交易。
     * 输出：入账通过标准 posting service 生成事实，查询 facade 可读到账本交易和分录，余额投影 facade 可读到余额变化。
     * 红线：application facade 只能编排标准入账与只读查询，不能暴露账本余额直接更新或事实删除入口。
     */
    @Test
    void testPostAndQueryThroughLedgerApplicationFacades() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);

        postingApplicationService.postLedgerTransaction(transaction(sourceLedgerId, targetLedgerId));

        LedgerTransactionDTO transaction = factQueryApplicationService.queryLedgerTransactions(
                        new LedgerTransactionQuery().setSn(LEDGER_TRANSACTION_SN),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords()
                .getFirst();
        List<LedgerEntryDTO> entries = factQueryApplicationService.queryLedgerEntries(
                        new LedgerEntryQuery().setLedgerTransactionSn(LEDGER_TRANSACTION_SN),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords();
        LedgerDTO sourceLedger = balanceProjectionApplicationService.getLedgerById(sourceLedgerId);
        LedgerDTO targetLedger = balanceProjectionApplicationService.getLedgerById(targetLedgerId);
        List<LedgerDTO> sourceLedgers = balanceProjectionApplicationService.queryLedgerBalances(
                        new LedgerQuery()
                                .setSubjectId(SOURCE_SUBJECT_ID)
                                .setSubjectType(SUBJECT_TYPE)
                                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords();

        assertThat(transaction.getStatus()).isEqualTo(LedgerTransactionStatus.POSTED);
        assertThat(transaction.getSn()).isEqualTo(LEDGER_TRANSACTION_SN);
        assertThat(entries).hasSize(2);
        assertThat(sourceLedger.getNormalBalance()).isEqualTo(100L);
        assertThat(targetLedger.getNormalBalance()).isEqualTo(100L);
        assertThat(sourceLedgers)
                .extracting(LedgerDTO::getId)
                .containsExactly(sourceLedgerId);
    }

    private LedgerTransactionSpec transaction(Long sourceLedgerId, Long targetLedgerId) {
        return new TestLedgerTransactionSpec(List.of(new TestLedgerPostingPlanSpec(List.of(
                new TestLedgerPostingPhaseSpec(LedgerPhaseCode.TRANSFER, List.of(
                        creditEntry(SOURCE_SUBJECT_ID, sourceLedgerId),
                        debitEntry(TARGET_SUBJECT_ID, targetLedgerId)))))));
    }

    private LedgerEntrySpec debitEntry(String subjectId, Long ledgerId) {
        return ledgerEntry(subjectId, ledgerId, EntrySide.DEBIT);
    }

    private LedgerEntrySpec creditEntry(String subjectId, Long ledgerId) {
        return ledgerEntry(subjectId, ledgerId, EntrySide.CREDIT);
    }

    private LedgerEntrySpec ledgerEntry(String subjectId, Long ledgerId, EntrySide entrySide) {
        return new TestLedgerEntrySpec(subjectId, ledgerId, entrySide);
    }

    private Long createAvailableLedger(String subjectId, long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(subjectId)
                .setSubjectType(SUBJECT_TYPE)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.ASSET)
                .setNormalBalanceSide(EntrySide.DEBIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        if (initialBalance != 0L) {
            ledgerService.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                    .setId(ledgerId)
                    .setDebitAmountDelta(initialBalance)
                    .setCreditAmountDelta(0L));
        }
        return ledgerId;
    }

    private void seedFundingAccount(String accountSn) {
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountSn);
        account.setOwnerId("owner_" + accountSn);
        account.setOwnerType(FundsAccountOwnerType.USER);
        account.setAccountType(FundingAccountType.USER_WALLET.getAccountType().name());
        account.setPlatform(Boolean.FALSE);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("ledger application facade funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void cleanupLedgerApplicationFacadeTestData() {
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE ledger_transaction_sn = ?", LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE ledger_transaction_sn = ?",
                LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                SOURCE_SUBJECT_ID,
                TARGET_SUBJECT_ID);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                SOURCE_SUBJECT_ID,
                TARGET_SUBJECT_ID);
    }

    private record TestLedgerTransactionSpec(List<LedgerPostingPlanSpec> postingPlans)
            implements LedgerTransactionSpec {

        private TestLedgerTransactionSpec {
            postingPlans = List.copyOf(postingPlans);
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }

        @Override
        public String getSn() {
            return LEDGER_TRANSACTION_SN;
        }

        @Override
        public FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TRANSFER;
        }

        @Override
        public LedgerTransactionStatus getStatus() {
            return LedgerTransactionStatus.POSTED;
        }

        @Override
        public Money getAmount() {
            return TRANSACTION_AMOUNT;
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-APP-FACADE-001";
        }

        @Override
        public DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public String getBusinessScene() {
            return "LEDGER_APPLICATION_FACADE";
        }

        @Override
        public String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
        }

        @Override
        public String getDescription() {
            return "ledger application facade contract";
        }

        @Override
        public List<LedgerPostingPlanSpec> getPostingPlans() {
            return postingPlans;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("traceId", "TRACE-APP-FACADE-001");
        }
    }

    private record TestLedgerPostingPlanSpec(List<LedgerPostingPhaseSpec> postingPhases)
            implements LedgerPostingPlanSpec {

        private TestLedgerPostingPlanSpec {
            postingPhases = List.copyOf(postingPhases);
        }

        @Override
        public String getPlanId() {
            return POSTING_PLAN_ID;
        }

        @Override
        public String getLedgerTransactionSn() {
            return LEDGER_TRANSACTION_SN;
        }

        @Override
        public LedgerPostingIntentType getIntent() {
            return LedgerPostingIntentType.TRANSFER;
        }

        @Override
        public List<LedgerPostingPhaseSpec> getPostingPhases() {
            return postingPhases;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("routeTraceId", "ROUTE-APP-FACADE-001");
        }
    }

    private record TestLedgerPostingPhaseSpec(LedgerPhaseCode phaseCode,
                                              List<LedgerEntrySpec> entries)
            implements LedgerPostingPhaseSpec {

        private TestLedgerPostingPhaseSpec {
            entries = List.copyOf(entries);
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return phaseCode;
        }

        @Override
        public List<LedgerEntrySpec> getEntries() {
            return entries;
        }
    }

    private record TestLedgerEntrySpec(String subjectId,
                                       Long ledgerId,
                                       EntrySide entryType)
            implements LedgerEntrySpec {

        @Override
        public String getSubjectId() {
            return subjectId;
        }

        @Override
        public String getSubjectType() {
            return SUBJECT_TYPE;
        }

        @Override
        public LedgerSubjectCode getLedgerSubjectCode() {
            return LedgerSubjectCode.AVAILABLE;
        }

        @Override
        public LedgerSubjectCategory getLedgerSubjectCategory() {
            return LedgerSubjectCategory.ASSET;
        }

        @Override
        public Long getLedgerId() {
            return ledgerId;
        }

        @Override
        public String getLedgerTransactionSn() {
            return LEDGER_TRANSACTION_SN;
        }

        @Override
        public EntrySide getEntryType() {
            return entryType;
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.TRANSFER;
        }

        @Override
        public LedgerPostingRole getPostingRole() {
            return LedgerPostingRole.DETAIL;
        }

        @Override
        public LedgerBalanceConstraintType getBalanceConstraintType() {
            return null;
        }

        @Override
        public String getBusinessScene() {
            return "LEDGER_APPLICATION_FACADE";
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-APP-FACADE-001";
        }

        @Override
        public Money getAmount() {
            return TRANSACTION_AMOUNT;
        }

        @Override
        public Money getOriginalAmount() {
            return TRANSACTION_AMOUNT;
        }

        @Override
        public BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
        }

        @Override
        public String getDescription() {
            return "ledger application facade contract";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("ledgerEntrySn", "LE-APP-FACADE-001");
        }
    }

    @Configuration
    @Import({
            DefaultLedgerPostingApplicationService.class,
            DefaultLedgerFactQueryApplicationService.class,
            DefaultLedgerBalanceProjectionApplicationService.class,
            DefaultLedgerTransactionPostingServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class
    })
    static class Config {
    }
}
