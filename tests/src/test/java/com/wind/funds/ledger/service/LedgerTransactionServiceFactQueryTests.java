package com.wind.funds.ledger.service;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.LedgerBalanceProjectionService;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.LedgerTransactionPostingService;
import com.wind.funds.ledger.dto.LedgerDTO;
import com.wind.funds.ledger.dto.LedgerEntryDTO;
import com.wind.funds.ledger.dto.LedgerTransactionDTO;
import com.wind.funds.ledger.dal.entities.LedgerEntry;
import com.wind.funds.ledger.dal.entities.LedgerPostingPlan;
import com.wind.funds.ledger.dal.entities.LedgerTransaction;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPhaseSpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.wind.funds.support.LedgerProjectionTestFixture.balanceEntry;

/**
 * 账本交易基础服务事实查询契约测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestCoreInfrastructureConfig.class,
        LedgerTransactionServiceFactQueryTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerTransactionServiceFactQueryTests extends AbstractFundsServiceTest {

    private static final String LEDGER_TRANSACTION_SN = "LT-TRX-SERVICE-FACT-001";

    private static final String POSTING_PLAN_ID = "PLAN-TRX-SERVICE-FACT-001";

    private static final String SOURCE_SUBJECT_ID = "ledger_trx_service_fact_source";

    private static final String TARGET_SUBJECT_ID = "ledger_trx_service_fact_target";

    private static final String SUBJECT_TYPE = FundsSubjectType.FUNDING_ACCOUNT.name();

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private static final Money TRANSACTION_AMOUNT = Money.immutable(100L, CURRENCY);

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 6, 25, 10, 0);

    @Autowired
    private LedgerTransactionPostingService ledgerTransactionPostingService;

    @Autowired
    private LedgerTransactionService ledgerTransactionService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerBalanceProjectionService ledgerBalanceProjectionService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpLedgerTransactionServiceFactQueryTestData() {
        cleanupLedgerTransactionServiceFactQueryTestData();
    }

    @AfterEach
    void tearDownLedgerTransactionServiceFactQueryTestData() {
        cleanupLedgerTransactionServiceFactQueryTestData();
    }

    /**
     * 场景：跨模块调用方按稳定流水读取必然存在的账务事实。
     * 输入：LedgerTransactionService 公开 get-by-sn 方法。
     * 输出：get 方法返回确定 DTO，不返回 Optional，不暴露 DAL 类型。
     * 红线：按 sn 或 id 的 get 查询是必然存在语义，查不到应抛异常。
     */
    @Test
    void testLedgerTransactionServiceShouldExposeStableSnGetQueriesWithoutOptional() throws NoSuchMethodException {
        Method getTransactionBySn = LedgerTransactionService.class.getDeclaredMethod(
                "getLedgerTransactionBySn", Long.class, String.class);
        Method getEntryBySn = LedgerTransactionService.class.getDeclaredMethod(
                "getLedgerEntryBySn", Long.class, String.class);

        assertThat(getTransactionBySn.getReturnType()).isEqualTo(LedgerTransactionDTO.class);
        assertThat(getEntryBySn.getReturnType()).isEqualTo(LedgerEntryDTO.class);
        assertThat(getTransactionBySn.getReturnType().getName()).doesNotContain(".dal.");
        assertThat(getEntryBySn.getReturnType().getName()).doesNotContain(".dal.");
    }

    /**
     * 场景：账本交易作为成功过账后形成的不可变账务事实。
     * 输入：账本交易 DSL、基础服务、持久化实体和 H2 表结构。
     * 输出：不暴露生命周期状态、更新或删除能力，不持久化恒为 true 的平衡标记，保留借贷控制总额。
     * 红线：业务处理中、失败、结算或冲正状态不得反向污染不可变账本交易事实。
     */
    @Test
    void testLedgerTransactionFactShouldExposeNoLifecycleMutationOrRedundantBalancedFlag() throws Exception {
        assertThat(LedgerTransactionSpec.class.getMethods())
                .extracting(Method::getName)
                .doesNotContain("getStatus");
        assertThat(LedgerTransactionService.class.getMethods())
                .extracting(Method::getName)
                .doesNotContain(
                        "postLedgerTransaction",
                        "updateLedgerTransaction",
                        "deleteLedgerTransactionById",
                        "deleteLedgerTransactionByIds");
        assertThat(LedgerTransaction.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("debitAmount", "creditAmount")
                .doesNotContain("status", "balanced");
        assertThat(LedgerPostingPlan.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("debitAmount", "creditAmount")
                .doesNotContain("balanced");
        assertThat(tableColumns("T_LEDGER_TRANSACTION"))
                .contains("DEBIT_AMOUNT", "CREDIT_AMOUNT")
                .doesNotContain("STATUS", "IS_BALANCED");
        assertThat(tableColumns("T_LEDGER_POSTING_PLAN"))
                .contains("DEBIT_AMOUNT", "CREDIT_AMOUNT")
                .doesNotContain("IS_BALANCED");

        Field digestFields = LedgerTransactionServiceImpl.class
                .getDeclaredField("LEDGER_TRANSACTION_SHA256_FIELDS");
        digestFields.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> transactionDigestFields = (List<String>) digestFields.get(null);
        assertThat(transactionDigestFields)
                .contains(LedgerTransaction.Fields.fundsTransactionSn)
                .doesNotHaveDuplicates();

        assertThat(LedgerTransactionDTO.class.getDeclaredField("eventType").getType())
                .isEqualTo(FundsTransactionEventType.class);
        assertThat(LedgerTransactionDTO.class.getDeclaredFields())
                .extracting(Field::getName)
                .contains(
                        "instructionType",
                        "transactionType",
                        "debitAmount",
                        "creditAmount",
                        "sha256");
    }

    /**
     * 场景：对账清算域以稳定分录号引用不可变 LedgerEntry，并独立保存批次、匹配和差错事实。
     * 输入：LedgerEntry 持久化事实、公开 DTO、查询契约和 H2 表结构。
     * 输出：账本分录不承载后发生的清算或对账生命周期字段。
     * 红线：对账或清算结果不得反写 LedgerEntry，也不得由账本层提前生成虚假批次和状态。
     */
    @Test
    void testLedgerEntryFactShouldNotOwnSettlementOrReconciliationLifecycle() {
        List<String> lifecycleFields = List.of(
                "settlementStatus",
                "settlementPeriod",
                "settlementCompletedTime",
                "reconcileStatus",
                "reconcileRemark",
                "reconciliationBatch",
                "reconciliationCompletedTime");

        assertThat(LedgerEntry.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContainAnyElementsOf(lifecycleFields);
        assertThat(LedgerEntryDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContainAnyElementsOf(lifecycleFields);
        assertThat(LedgerEntryQuery.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContainAnyElementsOf(lifecycleFields);
        assertThat(tableColumns("T_LEDGER_ENTRY"))
                .doesNotContain(
                        "SETTLEMENT_STATUS",
                        "SETTLEMENT_PERIOD",
                        "SETTLEMENT_COMPLETED_TIME",
                        "RECONCILE_STATUS",
                        "RECONCILE_REMARK",
                        "RECONCILIATION_BATCH",
                        "RECONCILIATION_COMPLETED_TIME");
    }

    /**
     * 场景：对账、报表和回放按稳定分录识别明细、父级控制和父子划拨口径。
     * 输入：LedgerEntry 持久化事实、公开 DTO、H2 表结构和分录摘要字段集。
     * 输出：postingRole 被固化，摘要覆盖分录身份及其账本、交易和计划引用。
     * 红线：记账角色或父事实引用被改写时，分录摘要不得保持不变。
     */
    @Test
    @SuppressWarnings("unchecked")
    void testLedgerEntryFactShouldPersistPostingRoleAndDigestStableReferences() throws Exception {
        java.lang.reflect.Field digestFields = LedgerTransactionServiceImpl.class
                .getDeclaredField("LEDGER_ENTRY_SHA256_FIELDS");
        digestFields.setAccessible(true);

        assertThat(LedgerEntry.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("postingRole");
        assertThat(LedgerEntryDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("postingRole");
        assertThat(tableColumns("T_LEDGER_ENTRY")).contains("POSTING_ROLE");
        assertThat((List<String>) digestFields.get(null)).contains(
                "sn",
                "ledgerTransactionSn",
                "postingPlanSn",
                "fundsTransactionSn",
                "ledgerId",
                "postingRole",
                "balanceConstraintType");
    }

    /**
     * 场景：交易侧、清结算或对账侧需要读取一笔已落账的账务事实。
     * 输入：source / target 两个资金账户、标准 posting gateway 和一笔借贷平衡的账本交易。
     * 输出：基础服务可按稳定 sn 读到账本交易和分录，异租户 get 查询抛异常；余额读取继续走 LedgerService。
     * 红线：基础查询不负责入账，不能跨租户兜底查询，也不能替代余额投影或资源初始化服务。
     */
    @Test
    void testPostThroughGatewayAndQueryFactsByStableSn() {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);

        ledgerTransactionPostingService.post(transaction(sourceLedgerId, targetLedgerId));

        LedgerTransactionDTO transaction = ledgerTransactionService.getLedgerTransactionBySn(
                TENANT_ID, LEDGER_TRANSACTION_SN);
        List<LedgerEntryDTO> entries = ledgerTransactionService.queryLedgerEntries(
                        new LedgerEntryQuery()
                                .setTenantId(TENANT_ID)
                                .setLedgerTransactionSn(LEDGER_TRANSACTION_SN),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords();
        LedgerEntryDTO firstEntry = ledgerTransactionService.getLedgerEntryBySn(TENANT_ID, entries.getFirst().getSn());
        List<LedgerEntryDTO> foreignTenantEntries = ledgerTransactionService.queryLedgerEntries(
                        new LedgerEntryQuery()
                                .setTenantId(TENANT_ID + 1)
                                .setLedgerTransactionSn(LEDGER_TRANSACTION_SN),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords();
        LedgerDTO sourceLedger = ledgerService.getLedgerById(sourceLedgerId);
        LedgerDTO targetLedger = ledgerService.getLedgerById(targetLedgerId);
        List<LedgerDTO> sourceLedgers = ledgerService.queryLedgers(new LedgerQuery()
                                .setSubjectId(SOURCE_SUBJECT_ID)
                                .setSubjectType(SUBJECT_TYPE)
                                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords();

        assertThat(transaction.getSn()).isEqualTo(LEDGER_TRANSACTION_SN);
        assertThat(transaction.getEventType()).isEqualTo(FundsTransactionEventType.TRANSFER);
        assertThat(transaction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(transaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TRANSFER);
        assertThat(transaction.getDebitAmount()).isEqualTo(TRANSACTION_AMOUNT);
        assertThat(transaction.getCreditAmount()).isEqualTo(TRANSACTION_AMOUNT);
        assertThat(transaction.getSha256()).isNotBlank();
        assertThat(entries).hasSize(2);
        assertThat(firstEntry.getLedgerTransactionSn()).isEqualTo(LEDGER_TRANSACTION_SN);
        assertThat(firstEntry.getPostingRole()).isEqualTo(LedgerPostingRole.DETAIL);
        assertThat(foreignTenantEntries).isEmpty();
        assertThatThrownBy(() -> ledgerTransactionService.getLedgerTransactionBySn(
                TENANT_ID + 1, LEDGER_TRANSACTION_SN))
                .hasMessageContaining("账户账本交易不存在");
        assertThatThrownBy(() -> ledgerTransactionService.getLedgerEntryBySn(
                TENANT_ID + 1, firstEntry.getSn()))
                .hasMessageContaining("账户账本条目不存在");
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
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledgerService.getLedgerById(ledgerId),
                    EntrySide.DEBIT,
                    initialBalance)));
        }
        return ledgerId;
    }

    private void seedFundingAccount(String accountSn) {
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountSn);
        account.setOwnerId(SOURCE_SUBJECT_ID.equals(accountSn) ? "owner_source" : "owner_target");
        account.setOwnerType(FundsAccountOwnerType.USER);
        account.setAccountType(FundingAccountType.USER_WALLET.getAccountType().name());
        account.setPlatform(Boolean.FALSE);
        account.setCurrency(CURRENCY);
        account.setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC);
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("ledger transaction service fact query funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void cleanupLedgerTransactionServiceFactQueryTestData() {
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

    private List<String> tableColumns(String tableName) {
        return jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
                String.class,
                tableName);
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
        public FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TRANSFER;
        }

        @Override
        public Money getAmount() {
            return TRANSACTION_AMOUNT;
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-TRX-SERVICE-FACT-001";
        }

        @Override
        public DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public String getBusinessScene() {
            return "LEDGER_TRANSACTION_SERVICE_FACT";
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
            return "ledger transaction service fact query contract";
        }

        @Override
        public List<LedgerPostingPlanSpec> getPostingPlans() {
            return postingPlans;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("traceId", "TRACE-TRX-SERVICE-FACT-001");
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
            return Map.of("routeTraceId", "ROUTE-TRX-SERVICE-FACT-001");
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
            return "LEDGER_TRANSACTION_SERVICE_FACT";
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-TRX-SERVICE-FACT-001";
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
            return "ledger transaction service fact query contract";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of("ledgerEntryTraceId", "LE-TRX-SERVICE-FACT-001");
        }
    }

    @Configuration
    @Import({
            DefaultLedgerTransactionPostingServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class
    })
    static class Config {
    }
}
