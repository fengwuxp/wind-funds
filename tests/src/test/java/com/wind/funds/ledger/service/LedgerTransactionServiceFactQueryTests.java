package com.wind.funds.ledger.service;

import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
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
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.ledger.query.LedgerEntryQuery;
import com.wind.funds.ledger.query.LedgerQuery;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.enums.RouteLegType;
import com.wind.funds.route.enums.RouteNodeRole;
import com.wind.funds.route.enums.RouteNodeType;
import com.wind.funds.route.model.ImmutableResolvedRouteSpec;
import com.wind.funds.route.model.ImmutableRouteLegSpec;
import com.wind.funds.route.model.ImmutableRouteNodeSpec;
import com.wind.funds.route.model.ImmutableSubjectRef;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static com.wind.funds.support.LedgerProjectionTestFixture.balanceEntry;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;

/**
 * 账本交易基础服务事实查询契约测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestCoreInfrastructureConfig.class,
        LedgerTransactionServiceFactQueryTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerTransactionServiceFactQueryTests extends AbstractFundsServiceTest {

    private static final String FUNDS_TRANSACTION_SN = "FT-TRX-SERVICE-FACT-001";

    private static final String BUSINESS_SCENE = "LEDGER_TRANSACTION_SERVICE_FACT";

    private static final String BUSINESS_SN = "BIZ-TRX-SERVICE-FACT-001";

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
    private LedgerBalanceProjectionServiceImpl ledgerBalanceProjectionService;

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

        String ledgerTransactionSn = postTransfer();

        LedgerTransactionDTO transaction = ledgerTransactionService.getLedgerTransactionBySn(
                TENANT_ID, ledgerTransactionSn);
        List<LedgerEntryDTO> entries = ledgerTransactionService.queryLedgerEntries(
                        new LedgerEntryQuery()
                                .setTenantId(TENANT_ID)
                                .setLedgerTransactionSn(ledgerTransactionSn),
                        DefaultPageQueryOptions.defaults(10))
                .getRecords();
        LedgerEntryDTO firstEntry = ledgerTransactionService.getLedgerEntryBySn(TENANT_ID, entries.getFirst().getSn());
        List<LedgerEntryDTO> foreignTenantEntries = ledgerTransactionService.queryLedgerEntries(
                        new LedgerEntryQuery()
                                .setTenantId(TENANT_ID + 1)
                                .setLedgerTransactionSn(ledgerTransactionSn),
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

        assertThat(transaction.getSn()).isEqualTo(ledgerTransactionSn);
        assertThat(transaction.getEventType()).isEqualTo(FundsTransactionEventType.TRANSFER);
        assertThat(transaction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(transaction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TRANSFER);
        assertThat(transaction.getDebitAmount()).isEqualTo(TRANSACTION_AMOUNT);
        assertThat(transaction.getCreditAmount()).isEqualTo(TRANSACTION_AMOUNT);
        assertThat(transaction.getSha256()).isNotBlank();
        assertThat(entries).hasSize(2);
        assertThat(firstEntry.getLedgerTransactionSn()).isEqualTo(ledgerTransactionSn);
        assertThat(firstEntry.getPostingRole()).isEqualTo(LedgerPostingRole.DETAIL);
        assertThat(foreignTenantEntries).isEmpty();
        assertThatThrownBy(() -> ledgerTransactionService.getLedgerTransactionBySn(
                TENANT_ID + 1, ledgerTransactionSn))
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

    /**
     * 场景：持久化 Ledger transaction/plan/entry 任一层摘要被改写后执行稳定流水查询。
     * 输入：真实 posting gateway 形成完整账务事实，再仅篡改目标层 sha256。
     * 输出：Ledger read boundary 抛出包含 layer/SN 的完整性错误，全部账务与余额事实不变。
     * 红线：查询不得返回未验证 DTO，也不得修复摘要或产生资金副作用。
     */
    @ParameterizedTest(name = "tampered persisted {0} digest")
    @ValueSource(strings = {"TRANSACTION", "POSTING_PLAN", "LEDGER_ENTRY"})
    void testStableSnQueriesShouldRejectTamperedPersistedLedgerAggregate(String layer) {
        seedFundingAccount(SOURCE_SUBJECT_ID);
        seedFundingAccount(TARGET_SUBJECT_ID);
        Long sourceLedgerId = createAvailableLedger(SOURCE_SUBJECT_ID, 200L);
        Long targetLedgerId = createAvailableLedger(TARGET_SUBJECT_ID, 0L);
        String ledgerTransactionSn = postTransfer();
        String postingPlanSn = jdbcTemplate.queryForObject("""
                SELECT sn FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn = ?
                """, String.class, ledgerTransactionSn);
        String entrySn = jdbcTemplate.queryForObject("""
                SELECT sn FROM t_ledger_entry
                WHERE ledger_transaction_sn = ?
                ORDER BY sn LIMIT 1
                """, String.class, ledgerTransactionSn);
        Long transactionId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_ledger_transaction WHERE sn = ?", Long.class, ledgerTransactionSn);
        Long entryId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_ledger_entry WHERE sn = ?", Long.class, entrySn);
        String targetSn = tamperPersistedDigest(layer, ledgerTransactionSn, postingPlanSn, entrySn);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<String> guardGaps = exactReadGuardGaps(
                layer, transactionId, entryId, ledgerTransactionSn, postingPlanSn, entrySn, targetSn);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
        assertThat(guardGaps)
                .as("stable-sn ledger read must reject tampered " + stableLayerLabel(layer) + " digest")
                .isEmpty();
    }

    private String tamperPersistedDigest(String layer,
                                         String ledgerTransactionSn,
                                         String postingPlanSn,
                                         String entrySn) {
        return switch (layer) {
            case "TRANSACTION" -> {
                jdbcTemplate.update("UPDATE t_ledger_transaction SET sha256 = ? WHERE sn = ?",
                        "tampered-transaction-digest", ledgerTransactionSn);
                yield ledgerTransactionSn;
            }
            case "POSTING_PLAN" -> {
                jdbcTemplate.update("UPDATE t_ledger_posting_plan SET sha256 = ? WHERE sn = ?",
                        "tampered-posting-plan-digest", postingPlanSn);
                yield postingPlanSn;
            }
            case "LEDGER_ENTRY" -> {
                jdbcTemplate.update("UPDATE t_ledger_entry SET sha256 = ? WHERE sn = ?",
                        "tampered-ledger-entry-digest", entrySn);
                yield entrySn;
            }
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        };
    }

    private List<String> exactReadGuardGaps(String layer,
                                            Long transactionId,
                                            Long entryId,
                                            String ledgerTransactionSn,
                                            String postingPlanSn,
                                            String entrySn,
                                            String targetSn) {
        List<String> result = new ArrayList<>();
        switch (layer) {
            case "TRANSACTION" -> {
                collectGuardGap(result, "getLedgerTransactionById",
                        () -> ledgerTransactionService.getLedgerTransactionById(transactionId), layer, targetSn);
                collectGuardGap(result, "getLedgerTransactionBySn",
                        () -> ledgerTransactionService.getLedgerTransactionBySn(TENANT_ID, ledgerTransactionSn),
                        layer, targetSn);
            }
            case "POSTING_PLAN" -> {
                collectGuardGap(result, "getLedgerTransactionBySn",
                        () -> ledgerTransactionService.getLedgerTransactionBySn(TENANT_ID, ledgerTransactionSn),
                        layer, targetSn);
                collectGuardGap(result, "existsPostingPlan",
                        () -> ledgerTransactionService.existsPostingPlan(
                                TENANT_ID, postingPlanSn, ledgerTransactionSn), layer, targetSn);
            }
            case "LEDGER_ENTRY" -> {
                collectGuardGap(result, "getLedgerEntryById",
                        () -> ledgerTransactionService.getLedgerEntryById(entryId), layer, targetSn);
                collectGuardGap(result, "getLedgerEntryBySn",
                        () -> ledgerTransactionService.getLedgerEntryBySn(TENANT_ID, entrySn), layer, targetSn);
                collectGuardGap(result, "queryLedgerEntries",
                        () -> ledgerTransactionService.queryLedgerEntries(
                                new LedgerEntryQuery()
                                        .setTenantId(TENANT_ID)
                                        .setLedgerTransactionSn(ledgerTransactionSn),
                                DefaultPageQueryOptions.defaults(10)), layer, targetSn);
            }
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        }
        return result;
    }

    private void collectGuardGap(List<String> result,
                                 String entryPoint,
                                 ThrowingCallable read,
                                 String layer,
                                 String targetSn) {
        Throwable failure = catchThrowable(read);
        if (failure == null) {
            result.add(entryPoint + " returned an unverified fact");
            return;
        }
        String message = failure.getMessage();
        if (message == null
                || !message.contains(integrityLayerName(layer))
                || !message.contains(targetSn)) {
            result.add(entryPoint + " returned an incomplete integrity error: " + message);
        }
    }

    private String stableLayerLabel(String layer) {
        return switch (layer) {
            case "TRANSACTION" -> "transaction";
            case "POSTING_PLAN" -> "plan";
            case "LEDGER_ENTRY" -> "entry";
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        };
    }

    private String integrityLayerName(String layer) {
        return switch (layer) {
            case "TRANSACTION" -> "transaction";
            case "POSTING_PLAN" -> "posting plan";
            case "LEDGER_ENTRY" -> "ledger entry";
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        };
    }

    private String postTransfer() {
        return ledgerTransactionPostingService.post(
                transferInstruction(), FUNDS_TRANSACTION_SN, transferRoute());
    }

    private FundsInstructionSpec transferInstruction() {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(TRANSACTION_AMOUNT)
                .originalAmount(TRANSACTION_AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .ledgerPeriodType(AccountBalancePeriodType.LIFETIME)
                .businessScene(BUSINESS_SCENE)
                .businessSn(BUSINESS_SN)
                .eventTime(TRANSACTION_TIME)
                .operator(WindOperatorFactory.system())
                .contextVariables(Map.of())
                .build();
    }

    private ResolvedRouteSpec transferRoute() {
        ImmutableRouteNodeSpec source = routeNode(SOURCE_SUBJECT_ID, RouteNodeRole.SOURCE);
        ImmutableRouteNodeSpec target = routeNode(TARGET_SUBJECT_ID, RouteNodeRole.TARGET);
        ImmutableRouteLegSpec leg = ImmutableRouteLegSpec.builder()
                .legId("LEG-TRX-SERVICE-FACT-001")
                .sequence(1)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(source)
                .targetNode(target)
                .amount(TRANSACTION_AMOUNT)
                .originalAmount(TRANSACTION_AMOUNT)
                .exchangeRate(BigDecimal.ONE)
                .contextVariables(Map.of())
                .build();
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(TENANT_ID)
                .routeCode("LEDGER_TRANSACTION_SERVICE_FACT_ROUTE")
                .routeVersion("v1")
                .businessScene(BUSINESS_SCENE)
                .businessSn(BUSINESS_SN)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .participants(List.of())
                .legs(List.of(leg))
                .resolvedAt(TRANSACTION_TIME)
                .contextVariables(Map.of())
                .build();
    }

    private ImmutableRouteNodeSpec routeNode(String subjectId, RouteNodeRole role) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .nodeRole(role)
                .subjectRef(ImmutableSubjectRef.builder()
                        .tenantId(TENANT_ID)
                        .subjectId(subjectId)
                        .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                        .currency(CURRENCY.name())
                        .ledgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                        .build())
                .build();
    }

    private Long createAvailableLedger(String subjectId, long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(subjectId)
                .setSubjectType(SUBJECT_TYPE)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC.name())
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.TRUE)
                .setCurrency(CURRENCY)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        if (initialBalance != 0L) {
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledgerService.getLedgerById(ledgerId),
                    EntrySide.CREDIT,
                    initialBalance)), LedgerPostingAccessType.NORMAL);
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
        account.setState(FundsAccountState.ACTIVE);
        account.setDescription("ledger transaction service fact query funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void cleanupLedgerTransactionServiceFactQueryTestData() {
        List<String> ledgerTransactionSns = jdbcTemplate.queryForList("""
                SELECT sn FROM t_ledger_transaction
                WHERE tenant_id = ? AND business_scene = ? AND business_sn = ?
                """, String.class, TENANT_ID, BUSINESS_SCENE, BUSINESS_SN);
        for (String ledgerTransactionSn : ledgerTransactionSns) {
            jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE ledger_transaction_sn = ?", ledgerTransactionSn);
            jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE ledger_transaction_sn = ?",
                    ledgerTransactionSn);
            jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", ledgerTransactionSn);
        }
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

    @Configuration
    @Import({
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultLedgerPostingAssembler.class,
            LedgerTransactionServiceImpl.class,
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class
    })
    static class Config {
    }
}
