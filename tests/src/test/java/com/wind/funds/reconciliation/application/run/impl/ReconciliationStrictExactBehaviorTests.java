package com.wind.funds.reconciliation.application.run.impl;

import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.enums.ReconciliationMatchResultKind;
import com.wind.funds.reconciliation.enums.ReconciliationRunOutcome;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.NormalizedComparisonFactInput;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.reconciliation.model.value.SnapshotCoverage;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.wind.funds.reconciliation.ReconciliationTestFixture.identity;
import static com.wind.funds.reconciliation.ReconciliationTestFixture.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 严格精确来源/运行行为契约测试。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationRunResultApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationStrictExactBehaviorTests extends AbstractFundsServiceTest {

    private static final String RULE_VERSION = "strict-exact-v1";

    @Autowired
    private ReconciliationBatchApplicationService reconciliationBatchApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanFacts() {
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    @Test
    void srB01ShouldComputeMatchedBalancedAndReuseExactReplay() {
        assertBehavior("MIG07-SR-001", () -> {
            RecordReconciliationRunResultRequest request = readyRequest("sr-b01",
                    List.of(fact("reference-1", "payment-1", 60, CurrencyIsoCode.CNY,
                            "CONFIRMED", true, "FUNDS", "PRINCIPAL", "CREDIT", RULE_VERSION,
                            List.of("report:reference-1"))),
                    List.of(fact("comparison-1", "payment-1", 60, CurrencyIsoCode.CNY,
                            "CONFIRMED", true, "FUNDS", "PRINCIPAL", "CREDIT", RULE_VERSION,
                            List.of("report:comparison-1"))), true, true);

            ReconciliationRunResultDTO first = execute(request);
            ReconciliationRunResultDTO replay = execute(request);

            assertThat(first.getOutcome()).isEqualTo(ReconciliationRunOutcome.BALANCED);
            assertThat(first.getTotalCount()).isOne();
            assertThat(first.getMatchedCount()).isOne();
            assertThat(first.getDifferenceCount()).isZero();
            assertThat(replay).isEqualTo(first);
            assertThat(runResultCount()).isOne();
            assertThat(matchResults(first)).singleElement()
                    .extracting(ReconciliationMatchResultDTO::getResultKind)
                    .isEqualTo(ReconciliationMatchResultKind.MATCHED);
        });
    }

    @Test
    void srB02ShouldClassifyEveryStrictMismatchWithoutCallerOverride() {
        assertBehavior("MIG07-SR-002", () -> {
            assertMismatch("money", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 80, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS",
                    "PRINCIPAL", "CREDIT", RULE_VERSION, ReconciliationMatchResultKind.MONEY_MISMATCH);
            assertMismatch("currency", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 60, CurrencyIsoCode.USD, "CONFIRMED", "FUNDS",
                    "PRINCIPAL", "CREDIT", RULE_VERSION, ReconciliationMatchResultKind.CURRENCY_MISMATCH);
            assertMismatch("status", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 60, CurrencyIsoCode.CNY, "RETURNED", "FUNDS",
                    "PRINCIPAL", "CREDIT", RULE_VERSION, ReconciliationMatchResultKind.STATUS_MISMATCH);
            assertMismatch("claim", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 60, CurrencyIsoCode.CNY, "CONFIRMED", "FEE",
                    "PRINCIPAL", "CREDIT", RULE_VERSION, ReconciliationMatchResultKind.SEMANTICS_MISMATCH);
            assertMismatch("component", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS",
                    "FEE", "CREDIT", RULE_VERSION, ReconciliationMatchResultKind.SEMANTICS_MISMATCH);
            assertMismatch("direction", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS",
                    "PRINCIPAL", "DEBIT", RULE_VERSION, ReconciliationMatchResultKind.SEMANTICS_MISMATCH);
            assertMismatch("rule", 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS", "PRINCIPAL",
                    "CREDIT", RULE_VERSION, 60, CurrencyIsoCode.CNY, "CONFIRMED", "FUNDS",
                    "PRINCIPAL", "CREDIT", "strict-exact-v2", ReconciliationMatchResultKind.RULE_MISMATCH);
        });
    }

    @Test
    void srB03ShouldNeverBalanceMissingIncompleteOrBothEmptySources() {
        assertBehavior("MIG07-SR-003", () -> {
            RecordReconciliationRunResultRequest missingComparison = readyRequest("sr-b03-missing",
                    List.of(standardFact("reference-1", "payment-1")), List.of(), true, true);
            assertThat(singleMatch(execute(missingComparison)).getResultKind())
                    .isEqualTo(ReconciliationMatchResultKind.COMPARISON_MISSING);

            RecordReconciliationRunResultRequest incomplete = readyRequest("sr-b03-incomplete",
                    List.of(standardFact("reference-1", "payment-1")),
                    List.of(standardFact("comparison-1", "payment-1")), true, false);
            assertThat(execute(incomplete).getOutcome()).isNotEqualTo(ReconciliationRunOutcome.BALANCED);

            ReconciliationBatchDTO emptyBatch = createBatch("sr-b03-empty", CurrencyIsoCode.CNY, RULE_VERSION);
            recordSnapshot(emptyBatch.getSn(), "sr-b03-empty", ReconciliationSourceRole.REFERENCE,
                    List.of(), true);
            assertThatThrownBy(() -> recordSnapshot(emptyBatch.getSn(), "sr-b03-empty",
                    ReconciliationSourceRole.COMPARISON, List.of(), true))
                    .hasMessageContaining("两侧来源不能同时为空");
        });
    }

    @Test
    void srB04ShouldFailClosedOnComparisonOrSourceIdentityConflict() {
        assertBehavior("MIG07-SR-004", () -> {
            RecordReconciliationRunResultRequest request = readyRequest("sr-b04",
                    List.of(
                            standardFact("reference-1", "payment-1"),
                            standardFact("reference-2", "payment-1").setAmount(61L)),
                    List.of(standardFact("comparison-1", "payment-1")), true, true);

            ReconciliationRunResultDTO result = execute(request);
            assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.DIFFERENCE_FOUND);
            assertThat(matchResults(result))
                    .extracting(ReconciliationMatchResultDTO::getResultKind)
                    .contains(ReconciliationMatchResultKind.IDENTITY_CONFLICT);

            int before = runResultCount();
            jdbcTemplate.update("UPDATE t_reconciliation_source_item SET amount = 62 WHERE source_fact_identity_value = ?",
                    "reference-1");
            assertThatThrownBy(() -> execute(request)).hasMessageContaining("冲突");
            assertThat(runResultCount()).isEqualTo(before);
        });
    }

    @Test
    void srB05ShouldReturnNotComparableWhenRuleEvidenceIsUnknown() {
        assertBehavior("MIG07-SR-005", () -> {
            RecordReconciliationRunResultRequest request = readyRequest("sr-b05",
                    List.of(standardFact("reference-1", "payment-1").setComparisonProven(false)),
                    List.of(standardFact("comparison-1", "payment-1")), true, true);

            ReconciliationRunResultDTO result = execute(request);
            assertThat(singleMatch(result).getResultKind()).isEqualTo(ReconciliationMatchResultKind.NOT_COMPARABLE);
            int before = runResultCount();
            jdbcTemplate.update("UPDATE t_reconciliation_source_item SET comparison_proven = TRUE "
                    + "WHERE source_fact_identity_value = ?", "reference-1");
            assertThatThrownBy(() -> execute(request)).hasMessageContaining("冲突");
            assertThat(runResultCount()).isEqualTo(before);
        });
    }

    @Test
    void srB06ShouldTreatCarrierFormsAsEvidenceForOneSemanticFact() {
        assertBehavior("MIG07-SR-006", () -> {
            List<String> evidence = List.of("file:statement.csv#line-1", "api:query-1", "event:event-1",
                    "report:daily-1");
            RecordReconciliationRunResultRequest request = readyRequest("sr-b06",
                    List.of(standardFact("reference-1", "payment-1").setEvidenceRefs(evidence)),
                    List.of(standardFact("comparison-1", "payment-1").setEvidenceRefs(evidence)), true, true);

            ReconciliationRunResultDTO result = execute(request);
            assertThat(result.getTotalCount()).isOne();
            assertThat(matchResults(result)).singleElement()
                    .satisfies(match -> assertThat(match.getEvidenceRefs()).containsAll(evidence));
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_source_item", Integer.class))
                    .isEqualTo(2);
        });
    }

    @Test
    void srB07ShouldRecoverOriginalIdentityAndFailClosedOnDurableTamper() {
        assertBehavior("MIG07-SR-007", () -> {
            RecordReconciliationRunResultRequest request = readyRequest("sr-b07",
                    List.of(standardFact("reference-1", "payment-1")),
                    List.of(standardFact("comparison-1", "payment-1")), true, true);
            ReconciliationRunResultDTO first = execute(request);
            ReconciliationRunResultDTO recovered = execute(request);

            assertThat(recovered).isEqualTo(first);
            assertThat(reconciliationRunResultApplicationService.getRunResult(TENANT_ID, first.getSn()))
                    .isEqualTo(first);
            assertThat(runResultCount()).isOne();

            jdbcTemplate.update("UPDATE t_reconciliation_source_item SET semantic_digest = ? "
                    + "WHERE source_fact_identity_value = ?", "f".repeat(64), "reference-1");
            assertThatThrownBy(() -> execute(request)).hasMessageContaining("摘要");
            assertThat(runResultCount()).isOne();
        });
    }

    @Test
    void srB08ShouldExposeNoOperationalOverrideWriteBypass() {
        assertBehavior("MIG07-SR-008", () -> {
            Set<String> requestFields = Arrays.stream(RecordReconciliationRunResultRequest.class.getDeclaredFields())
                    .filter(field -> !field.isSynthetic())
                    .map(Field::getName)
                    .filter(name -> !"serialVersionUID".equals(name))
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(requestFields).containsExactlyInAnyOrder("tenantId", "reconciliationBatchSn");

            ReconciliationRunResultDTO result = execute(readyRequest("sr-b08",
                    List.of(standardFact("reference-1", "payment-1")),
                    List.of(standardFact("comparison-1", "payment-1")), true, true));
            assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.BALANCED);
        });
    }

    @Test
    void srB09ShouldUseTheSharedSourceRunModelWithoutLegacyProductionSurface() {
        assertBehavior("MIG07-SR-009", () -> {
            assertClassMissing("com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem");
            assertThat(Arrays.stream(ReconciliationRunResultApplicationService.class.getMethods())
                    .map(java.lang.reflect.Method::getName)).doesNotContain("recordRunResult");

            ReconciliationRunResultDTO result = execute(readyRequest("sr-b09",
                    List.of(standardFact("reference-1", "payment-1")),
                    List.of(standardFact("comparison-1", "payment-1")), true, true));
            assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.BALANCED);
        });
    }

    private void assertMismatch(String caseId,
                                long referenceAmount,
                                CurrencyIsoCode referenceCurrency,
                                String referenceStatus,
                                String referenceClaim,
                                String referenceComponent,
                                String referenceDirection,
                                String referenceRule,
                                long comparisonAmount,
                                CurrencyIsoCode comparisonCurrency,
                                String comparisonStatus,
                                String comparisonClaim,
                                String comparisonComponent,
                                String comparisonDirection,
                                String comparisonRule,
                                ReconciliationMatchResultKind expected) {
        RecordReconciliationRunResultRequest request = readyRequest("sr-b02-" + caseId,
                List.of(fact("reference-" + caseId, "payment-" + caseId, referenceAmount,
                        referenceCurrency, referenceStatus, true, referenceClaim, referenceComponent,
                        referenceDirection, referenceRule, List.of("report:reference-" + caseId))),
                List.of(fact("comparison-" + caseId, "payment-" + caseId, comparisonAmount,
                        comparisonCurrency, comparisonStatus, true, comparisonClaim, comparisonComponent,
                        comparisonDirection, comparisonRule, List.of("report:comparison-" + caseId))), true, true);
        assertThat(singleMatch(execute(request)).getResultKind()).isEqualTo(expected);
    }

    private RecordReconciliationRunResultRequest readyRequest(String caseId,
                                                               List<NormalizedComparisonFactInput> referenceFacts,
                                                               List<NormalizedComparisonFactInput> comparisonFacts,
                                                               boolean referenceComplete,
                                                               boolean comparisonComplete) {
        CurrencyIsoCode currency = referenceFacts.isEmpty()
                ? comparisonFacts.getFirst().getCurrency() : referenceFacts.getFirst().getCurrency();
        String ruleVersion = referenceFacts.isEmpty()
                ? comparisonFacts.getFirst().getComparisonRuleRef().getVersion()
                : referenceFacts.getFirst().getComparisonRuleRef().getVersion();
        ReconciliationBatchDTO batch = createBatch(caseId, currency, ruleVersion);
        recordSnapshot(batch.getSn(), caseId, ReconciliationSourceRole.REFERENCE, referenceFacts, referenceComplete);
        recordSnapshot(batch.getSn(), caseId, ReconciliationSourceRole.COMPARISON, comparisonFacts, comparisonComplete);
        return new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());
    }

    private ReconciliationBatchDTO createBatch(String caseId, CurrencyIsoCode currency, String ruleVersion) {
        return reconciliationBatchApplicationService.createBatch(new CreateReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
                .setScopeIdentity(identity("test.scope", caseId))
                .setPairIdentity(identity("test.pair", caseId))
                .setCurrency(currency)
                .setComparisonRuleRef(rule(ruleVersion))
                .setWindowStart(LocalDateTime.of(2026, 8, 18, 0, 0))
                .setWindowEnd(LocalDateTime.of(2026, 8, 19, 0, 0))
                .setTimeSemantics("occurredAt")
                .setTimezoneId("Asia/Shanghai"), WindOperatorFactory.system());
    }

    private void recordSnapshot(String batchSn,
                                String caseId,
                                ReconciliationSourceRole role,
                                List<NormalizedComparisonFactInput> facts,
                                boolean complete) {
        reconciliationBatchApplicationService.recordSourceSnapshot(new RecordReconciliationSourceSnapshotRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn)
                        .setSourceRole(role)
                        .setSourceNamespace(role == ReconciliationSourceRole.REFERENCE ? "transaction" : "settlement")
                        .setSnapshotIdentity(identity("test.snapshot", caseId + ":" + role.name()))
                        .setSnapshotVersion("v1")
                        .setCoverage(new SnapshotCoverage()
                                .setComplete(complete)
                                .setWatermark("watermark:" + caseId)
                                .setMemberCount(facts.size()))
                        .setFacts(facts)
                        .setEvidenceRefs(List.of("evidence:" + caseId + ":" + role.name())),
                WindOperatorFactory.system());
    }

    private NormalizedComparisonFactInput standardFact(String sourceId, String comparisonId) {
        return fact(sourceId, comparisonId, 60, CurrencyIsoCode.CNY, "CONFIRMED", true,
                "FUNDS", "PRINCIPAL", "CREDIT", RULE_VERSION, List.of("evidence:" + sourceId));
    }

    private NormalizedComparisonFactInput fact(String sourceId,
                                                String comparisonId,
                                                long amount,
                                                CurrencyIsoCode currency,
                                                String status,
                                                boolean proven,
                                                String claimKind,
                                                String component,
                                                String direction,
                                                String ruleVersion,
                                                List<String> evidenceRefs) {
        return new NormalizedComparisonFactInput()
                .setSourceFactRef(identity("test.fact", sourceId))
                .setComparisonIdentity(identity("test.compare", comparisonId))
                .setAmount(amount)
                .setCurrency(currency)
                .setComparisonRuleRef(rule(ruleVersion))
                .setComparisonStatusCode(status)
                .setComparisonProven(proven)
                .setClaimKind(claimKind)
                .setEconomicComponent(component)
                .setDirection(direction)
                .setNormalizationVersion("v1")
                .setEvidenceRefs(evidenceRefs);
    }

    private ReconciliationRunResultDTO execute(RecordReconciliationRunResultRequest request) {
        return reconciliationRunResultApplicationService.executeStrictExact(request, WindOperatorFactory.system());
    }

    private ReconciliationMatchResultDTO singleMatch(ReconciliationRunResultDTO result) {
        return matchResults(result).getFirst();
    }

    private List<ReconciliationMatchResultDTO> matchResults(ReconciliationRunResultDTO result) {
        WindPagination<ReconciliationMatchResultDTO> page = reconciliationRunResultApplicationService.queryMatchResults(
                TENANT_ID, result.getSn(), DefaultPageQueryOptions.defaults(1, 100));
        return page.getRecords();
    }

    private int runResultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_run_result", Integer.class);
    }

    private void assertClassMissing(String className) {
        assertThatThrownBy(() -> Class.forName(className)).isInstanceOf(ClassNotFoundException.class);
    }

    private void assertBehavior(String contractId, Runnable assertions) {
        assertThatCode(assertions::run)
                .as(contractId + " accepted strict-exact behavior")
                .doesNotThrowAnyException();
    }
}
