package com.wind.funds.reconciliation.application.gate.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.clearing.ClearingBatchApplicationService;
import com.wind.funds.reconciliation.application.clearing.ClearingSplittableDetailApplicationService;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingBatchApplicationServiceImpl;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingSplittableDetailApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.enums.ClearingBatchState;
import com.wind.funds.reconciliation.enums.ClearingCandidateState;
import com.wind.funds.reconciliation.enums.ClearingSplittableAdmissionResult;
import com.wind.funds.reconciliation.enums.ReconciliationGateBlockerCode;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.model.dto.ClearingBatchDTO;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.ConfirmClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateClearingBatchRequest;
import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationGateRequirementRequest;
import com.wind.funds.reconciliation.model.request.SubmitClearingBatchRequest;
import com.wind.funds.reconciliation.model.value.GateRequirementRef;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.RequiredPairRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.route.ClearingFundsInstructionRouteResolver;
import com.wind.funds.route.CompositeRouteResolver;
import com.wind.funds.route.DefaultRouteSnapshotFactory;
import com.wind.funds.route.RefundRouteAdmission;
import com.wind.funds.route.RouteAccountHierarchySnapshotAppender;
import com.wind.funds.route.RouteFeeChargeAppender;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsClearingTransactionService;
import com.wind.funds.transaction.application.impl.FundsClearingTransactionServiceImpl;
import com.wind.funds.transaction.converter.FundsClearingInstructionConverter;
import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.services.impl.AccountHierarchyRelationServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.jackson.WindJson;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.wind.funds.reconciliation.ReconciliationTestFixture.identity;
import static com.wind.funds.reconciliation.ReconciliationTestFixture.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 门禁要求与必需项消费行为契约测试。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationGateApplicationServiceTests.Config.class,
        ReconciliationGateRequirementBehaviorTests.StageConfig.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationGateRequirementBehaviorTests extends AbstractFundsServiceTest {

    private static final String RULE_VERSION = "v1";

    private static final String STAGE_FUNDS_TRANSACTION_SN = "gate-stage-funds-tx";

    private static final String STAGE_FUNDS_TRANSACTION_DETAIL_SN = "gate-stage-funds-detail";

    private static final String STAGE_PAYER_DETAIL_SN = "gate-stage-payer-detail";

    private static final String STAGE_LEDGER_TRANSACTION_SN = "gate-stage-ledger-tx";

    private static final String STAGE_POSTING_PLAN_SN = "gate-stage-posting-plan";

    private static final String STAGE_LEDGER_ENTRY_SN = "gate-stage-ledger-entry";

    private static final String STAGE_ACCOUNT_SN = "gate-stage-subject";

    private static final String STAGE_ROUTE_SNAPSHOT = stageRouteSnapshot();

    private static final long STAGE_AMOUNT = 9800L;

    private static final long STAGE_CLEARING_AMOUNT = 100L;

    private static final LocalDateTime STAGE_LEDGER_TRANSACTION_TIME = LocalDateTime.of(2026, 8, 19, 10, 0);

    private static final String LEDGER_TRANSACTION_DIGEST_DOMAIN = "ledger.persisted-transaction.v1";

    private static final String LEDGER_POSTING_PLAN_DIGEST_DOMAIN = "ledger.persisted-plan.v1";

    private static final String LEDGER_ENTRY_DIGEST_DOMAIN = "ledger.persisted-entry.v1";

    @Autowired
    private ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Autowired
    private ClearingSplittableDetailApplicationService clearingSplittableDetailApplicationService;

    @Autowired
    private ClearingBatchApplicationService clearingBatchApplicationService;

    @Autowired
    private FundsClearingTransactionService fundsClearingTransactionService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanFacts() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_stage_gate_evidence");
        jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement_pair");
        jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement_head");
        jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference_action");
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM t_clearing_splittable_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch_detail");
        jdbcTemplate.update("DELETE FROM t_clearing_batch");
        jdbcTemplate.update("DELETE FROM t_clearing_candidate");
        jdbcTemplate.update("DELETE FROM t_ledger_entry");
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan");
        jdbcTemplate.update("DELETE FROM t_ledger_transaction");
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail");
        jdbcTemplate.update("DELETE FROM t_funds_transaction");
        jdbcTemplate.update("DELETE FROM t_ledger");
        jdbcTemplate.update("DELETE FROM t_funding_account");
    }

    @Test
    void testGtB01ShouldRequireAndConsumeAllThreeMandatoryPairs() {
        assertBehavior("MIG07-GATE-001", () -> {
            GateSetup setup = prepareGate("gt-b01", 3, 3);
            GateRequirementRef requirement = record(setup.request());
            ReconciliationGateDecisionDTO decision = check(setup.stageRef());

            assertThat(decision.isPassed()).isTrue();
            assertThat(decision.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
            assertThat(decision.getRequirementRef()).isEqualTo(requirement);
            assertThat(decision.getPairDecisions()).hasSize(3);
            assertThat(requirementPairCount()).isEqualTo(3);
            assertThat(consumedEvidenceCount()).isOne();
            assertThat(jdbcTemplate.queryForObject("SELECT consumed_pair_evidence "
                    + "FROM t_reconciliation_stage_gate_evidence", String.class)).contains("resultDigest");
        });
    }

    @Test
    void testGtB02ShouldReplayDualDigestsAndAdvanceHeadWithCas() {
        assertBehavior("MIG07-GATE-002", () -> {
            GateSetup setup = prepareGate("gt-b02", 1, 1);
            GateRequirementRef first = record(setup.request());
            assertThat(record(setup.request())).isEqualTo(first);

            RecordReconciliationGateRequirementRequest evidenceConflict = copy(setup.request())
                    .setEvidenceRefs(List.of("evidence:gt-b02:changed"));
            assertThatThrownBy(() -> record(evidenceConflict)).hasMessageContaining("冲突");

            RecordReconciliationGateRequirementRequest next = copy(setup.request())
                    .setRequirementVersion("v2")
                    .setExpectedCurrentRequirementRef(first);
            GateRequirementRef second = record(next);
            assertThat(second.getRequirementVersion()).isEqualTo("v2");
            assertThat(requirementCount()).isEqualTo(2);

            assertConcurrentFirstPublication("gt-b02-concurrent");
            assertConcurrentConflictingFirstPublication("gt-b02-conflicting-concurrent");
        });
    }

    @Test
    void testGtB03ShouldBlockWholeStageWhenAnyMandatoryPairIsInvalid() {
        assertBehavior("MIG07-GATE-003", () -> {
            GateSetup missingSetup = prepareGate("gt-b03-missing", 3, 2);
            record(missingSetup.request());
            ReconciliationGateDecisionDTO missing = check(missingSetup.stageRef());

            assertThat(missing.isPassed()).isFalse();
            assertThat(missing.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
            assertThat(missing.getPairDecisions()).hasSize(3)
                    .anySatisfy(pair -> assertThat(pair.getBlockerCodes())
                            .contains(ReconciliationGateBlockerCode.REQUIRED_PAIR_RUN_NOT_FOUND));
            assertThat(consumedEvidenceCount()).isZero();

            GateSetup wrongScopeSetup = prepareGate("gt-b03-scope", 1, 1);
            RequiredPairRef seededPair = wrongScopeSetup.request().getRequiredPairs().getFirst();
            record(copy(wrongScopeSetup.request()).setRequiredPairs(List.of(new RequiredPairRef()
                    .setScopeIdentity(identity("test.scope", "WRONG_SCOPE"))
                    .setPairIdentity(seededPair.getPairIdentity())
                    .setComparisonRuleRef(seededPair.getComparisonRuleRef()))));
            assertThat(check(wrongScopeSetup.stageRef()).isPassed()).isFalse();

            GateSetup nonCurrentSetup = prepareGate("gt-b03-current", 1, 1);
            record(nonCurrentSetup.request());
            jdbcTemplate.update("UPDATE t_reconciliation_batch_lineage SET current_batch_sn = ? "
                            + "WHERE scope_identity_value = ?",
                    "not-current-batch", pair("gt-b03-current", 0).getScopeIdentity().getValue());
            assertThat(check(nonCurrentSetup.stageRef()).isPassed()).isFalse();

            GateSetup coverageSetup = prepareGate("gt-b03-coverage", 1, 1);
            record(coverageSetup.request());
            jdbcTemplate.update("UPDATE t_reconciliation_source_snapshot SET coverage_complete = FALSE "
                    + "WHERE reconciliation_batch_sn = ?", batchSn("gt-b03-coverage", 0));
            assertThat(check(coverageSetup.stageRef()).isPassed()).isFalse();

            GateSetup ruleSetup = prepareGate("gt-b03-rule", 1, 1);
            record(ruleSetup.request());
            jdbcTemplate.update("UPDATE t_reconciliation_run_result SET rule_version = 'stale-rule' "
                    + "WHERE reconciliation_batch_sn = ?", batchSn("gt-b03-rule", 0));
            assertThat(check(ruleSetup.stageRef()).isPassed()).isFalse();

            GateSetup headSetup = prepareGate("gt-b03-head", 1, 1);
            record(headSetup.request());
            jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement_head "
                    + "WHERE stage_identity_value = ?", headSetup.stageRef().getStageIdentity().getValue());
            assertThat(check(headSetup.stageRef()).isPassed()).isFalse();
            assertThat(consumedEvidenceCount()).isZero();
        });
    }

    @Test
    void testGtB04ShouldHaveNoOptionalThresholdOrConditionalPassSurface() {
        assertBehavior("MIG07-GATE-004", () -> {
            Set<String> fields = Arrays.stream(RecordReconciliationGateRequirementRequest.class.getDeclaredFields())
                    .filter(field -> !field.isSynthetic())
                    .map(Field::getName)
                    .filter(name -> !"serialVersionUID".equals(name))
                    .collect(java.util.stream.Collectors.toSet());
            assertThat(fields).containsExactlyInAnyOrder("tenantId", "stageRef", "requirementVersion",
                    "requiredPairs", "expectedCurrentRequirementRef", "evidenceRefs");
            assertThat(fields).noneMatch(name -> name.contains("optional") || name.contains("threshold")
                    || name.contains("conditional"));

            GateSetup setup = prepareGate("gt-b04", 1, 1);
            record(setup.request());
            assertThat(check(setup.stageRef()).isPassed()).isTrue();
        });
    }

    @Test
    void testGtB05ShouldTreatInspectAsExplanationAndRecheckCurrentHead() {
        assertBehavior("MIG07-GATE-005", () -> {
            GateSetup setup = prepareGate("gt-b05", 1, 1);
            record(setup.request());
            ReconciliationGateDecisionDTO inspected = reconciliationGateApplicationService.inspectGate(
                    checkRequest(setup.stageRef()), WindOperatorFactory.system());

            assertThat(inspected.isPassed()).isTrue();
            assertThat(consumedEvidenceCount()).isZero();

            jdbcTemplate.update("UPDATE t_reconciliation_batch_lineage SET current_batch_sn = ?",
                    "new-lineage-head");
            ReconciliationGateDecisionDTO checked = check(setup.stageRef());
            assertThat(checked.isPassed()).isFalse();
            assertThat(consumedEvidenceCount()).isZero();
        });
    }

    @Test
    void testGtB06ShouldCommitStageConsumptionEvidenceInTheCallerTransaction() {
        assertBehavior("MIG07-GATE-006", () -> {
            prepareStageSourceFacts();
            GateSetup setup = prepareGate("gt-b06", 1, 1, stageSourceRef());
            record(setup.request());

            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).execute(status -> {
                ClearingSplittableDetailDTO detail = clearingSplittableDetailApplicationService.identifySplittableDetail(
                        stageRequest(), WindOperatorFactory.system());
                assertThat(detail.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.SPLIT_READY);
                throw new ExpectedRollback();
            })).isInstanceOf(ExpectedRollback.class);
            assertThat(stageDetailCount()).isZero();
            assertThat(consumedEvidenceCount()).isZero();

            ClearingSplittableDetailDTO committed = clearingSplittableDetailApplicationService.identifySplittableDetail(
                    stageRequest(), WindOperatorFactory.system());
            assertThat(committed.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.SPLIT_READY);
            assertThat(stageDetailCount()).isOne();
            assertThat(consumedEvidenceCount()).isOne();
            assertThat(committed.getGateEvidenceRef()).isEqualTo(consumedEvidenceSn());
            assertThat(jdbcTemplate.queryForObject("SELECT decision_digest "
                    + "FROM t_reconciliation_stage_gate_evidence", String.class)).hasSize(64);
        });
    }

    @Test
    void testGtB07ShouldSeparateRollbackProvenZeroAndUnknownFailureEvidence() {
        assertBehavior("MIG07-GATE-007A/B/C", () -> {
            prepareStageSourceFacts();
            GateSetup blockedSetup = prepareGate("gt-b07-blocked", 2, 1, stageSourceRef());
            record(blockedSetup.request());
            int upstreamRunFacts = runResultCount();
            int sourceFundsFacts = fundsFactCount(STAGE_FUNDS_TRANSACTION_SN);
            int sourceLedgerFacts = ledgerFactCount(STAGE_FUNDS_TRANSACTION_SN);

            ClearingSplittableDetailDTO blocked = clearingSplittableDetailApplicationService.identifySplittableDetail(
                    stageRequest(), WindOperatorFactory.system());
            assertThat(blocked.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.EXCLUDED);
            assertThat(stageDetailCount()).isZero();
            assertThat(consumedEvidenceCount()).isZero();
            assertThat(runResultCount()).isEqualTo(upstreamRunFacts);
            assertThat(fundsFactCount(STAGE_FUNDS_TRANSACTION_SN)).isEqualTo(sourceFundsFacts);
            assertThat(ledgerFactCount(STAGE_FUNDS_TRANSACTION_SN)).isEqualTo(sourceLedgerFacts);

            StageBatch provenZeroStage = prepareReviewingStageBatch("zero", 0L);
            GateSetup provenZeroSetup = prepareGate("gt-b07-zero", 1, 1, provenZeroStage.stageRef());
            record(provenZeroSetup.request());
            assertDeterministicStageRejection(provenZeroStage);

            StageBatch unknownStage = prepareReviewingStageBatch("unknown", STAGE_CLEARING_AMOUNT);
            String existingFundsTransactionSn = createConflictingExistingFundsFact(unknownStage);
            GateSetup unknownSetup = prepareGate("gt-b07-unknown", 1, 1, unknownStage.stageRef());
            record(unknownSetup.request());
            assertUnknownStageFailsClosed(unknownStage, existingFundsTransactionSn);
        });
    }

    @Test
    void testGtB08ShouldRecoverSameStageIdentityWithoutSecondAction() {
        assertBehavior("MIG07-GATE-008", () -> {
            GateSetup setup = prepareGate("gt-b08", 1, 1);
            GateRequirementRef requirement = record(setup.request());
            ReconciliationGateDecisionDTO first = check(setup.stageRef());
            ReconciliationGateDecisionDTO recovered = check(setup.stageRef());

            assertThat(first.getDecisionDigest()).isEqualTo(recovered.getDecisionDigest());
            assertThat(recovered.getRequirementRef()).isEqualTo(requirement);
            assertThat(consumedEvidenceCount()).isOne();

            RecordReconciliationGateRequirementRequest conflict = copy(setup.request())
                    .setRequiredPairs(List.of(new RequiredPairRef()
                            .setScopeIdentity(identity("test.scope", "conflicting-scope"))
                            .setPairIdentity(identity("test.pair", "conflicting-pair"))
                            .setComparisonRuleRef(rule(RULE_VERSION))));
            assertThatThrownBy(() -> record(conflict)).hasMessageContaining("冲突");
            assertThat(consumedEvidenceCount()).isOne();
        });
    }

    @Test
    void testGtB09ShouldResolveBlockersOnlyThroughCurrentPairLineage() {
        assertBehavior("MIG07-GATE-009", () -> {
            GateSetup setup = prepareGate("gt-b09", 1, 1);
            record(setup.request());
            RequiredPairRef requiredPair = setup.request().getRequiredPairs().getFirst();
            String currentBatchSn = batchSn("gt-b09", 0);
            seedDifference("gt-b09-required", currentBatchSn,
                    requiredPair.getScopeIdentity(), requiredPair.getPairIdentity(), "BLOCKED");
            seedDifference("gt-b09-other-scope", "other-scope-batch",
                    identity("test.scope", "OTHER_SCOPE"), requiredPair.getPairIdentity(), "RESOLVED");
            List<Map<String, Object>> blockedDifferences = differenceSnapshot();

            assertThat(check(setup.stageRef()).isPassed()).isFalse();
            assertThat(differenceSnapshot()).isEqualTo(blockedDifferences);

            String successorBatchSn = "gate-gt-b09-successor";
            seedBalancedPair(successorBatchSn, requiredPair.getScopeIdentity(), requiredPair.getPairIdentity(),
                    currentBatchSn);
            resolveDifference("gt-b09-required", successorBatchSn);
            List<Map<String, Object>> resolvedDifferences = differenceSnapshot();
            assertThat(check(setup.stageRef()).isPassed()).isTrue();
            assertThat(differenceSnapshot()).isEqualTo(resolvedDifferences);
        });
    }

    @Test
    void testGtB10ShouldUseOnlyTheRequirementBasedGateSurface() {
        assertBehavior("MIG07-GATE-010", () -> {
            assertClassMissing("com.wind.funds.reconciliation.model.request.CheckClearingSettlementGateRequest");
            assertClassMissing("com.wind.funds.reconciliation.model.dto.ClearingSettlementGateResultDTO");
            assertThat(Arrays.stream(ReconciliationGateApplicationService.class.getMethods())
                    .map(java.lang.reflect.Method::getName))
                    .containsExactlyInAnyOrder("recordGateRequirement", "checkGate", "inspectGate");

            GateSetup setup = prepareGate("gt-b10", 1, 1);
            record(setup.request());
            assertThat(check(setup.stageRef()).isPassed()).isTrue();
        });
    }

    private GateSetup prepareGate(String caseId, int requiredPairCount, int seededPairCount) {
        return prepareGate(caseId, requiredPairCount, seededPairCount,
                ReconciliationTestFixture.stage("SETTLEMENT_LOCK", "stage-" + caseId));
    }

    private GateSetup prepareGate(String caseId,
                                  int requiredPairCount,
                                  int seededPairCount,
                                  GateStageRef stageRef) {
        List<RequiredPairRef> requiredPairs = java.util.stream.IntStream.range(0, requiredPairCount)
                .mapToObj(index -> pair(caseId, index))
                .toList();
        java.util.stream.IntStream.range(0, seededPairCount).forEach(index -> seedBalancedPair(caseId, index));
        return new GateSetup(stageRef, new RecordReconciliationGateRequirementRequest()
                .setTenantId(TENANT_ID)
                .setStageRef(stageRef)
                .setRequirementVersion("v1")
                .setRequiredPairs(requiredPairs)
                .setEvidenceRefs(List.of("evidence:" + caseId)));
    }

    private RequiredPairRef pair(String caseId, int index) {
        String batchSn = batchSn(caseId, index);
        return new RequiredPairRef()
                .setScopeIdentity(identity("test.scope", "REQUIRED_PAIR:" + caseId + "-" + index))
                .setPairIdentity(identity("test.pair", "test-pair:" + batchSn))
                .setComparisonRuleRef(rule(RULE_VERSION));
    }

    private void seedBalancedPair(String caseId, int index) {
        String batchSn = batchSn(caseId, index);
        RequiredPairRef requiredPair = pair(caseId, index);
        seedBalancedPair(batchSn, requiredPair.getScopeIdentity(), requiredPair.getPairIdentity(), null);
    }

    private void seedBalancedPair(String batchSn,
                                  StableIdentity scopeIdentity,
                                  StableIdentity pairIdentity,
                                  String previousBatchSn) {
        String evidenceRef = "evidence:" + batchSn;
        ReconciliationTestFixture.prepareReadyBatch(jdbcTemplate, TENANT_ID, batchSn,
                null, null, RULE_VERSION, evidenceRef,
                "reference:" + batchSn, "comparison:" + batchSn, previousBatchSn);
        jdbcTemplate.update("UPDATE t_reconciliation_batch SET scope_owner_namespace = ?, "
                        + "scope_identity_value = ?, pair_owner_namespace = ?, pair_identity_value = ? WHERE sn = ?",
                scopeIdentity.getOwnerNamespace(), scopeIdentity.getValue(), pairIdentity.getOwnerNamespace(),
                pairIdentity.getValue(), batchSn);
        int updated = jdbcTemplate.update("UPDATE t_reconciliation_batch_lineage SET current_batch_sn = ? "
                        + "WHERE tenant_id = ? AND scope_owner_namespace = ? AND scope_identity_value = ? "
                        + "AND pair_owner_namespace = ? AND pair_identity_value = ?",
                batchSn, TENANT_ID, scopeIdentity.getOwnerNamespace(), scopeIdentity.getValue(),
                pairIdentity.getOwnerNamespace(), pairIdentity.getValue());
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO t_reconciliation_batch_lineage "
                            + "(tenant_id, scope_owner_namespace, scope_identity_value, pair_owner_namespace, "
                            + "pair_identity_value, current_batch_sn) VALUES (?, ?, ?, ?, ?, ?)",
                    TENANT_ID, scopeIdentity.getOwnerNamespace(), scopeIdentity.getValue(),
                    pairIdentity.getOwnerNamespace(), pairIdentity.getValue(), batchSn);
        }

        String referenceSnapshotSn = sourceSnapshotSn(batchSn, "REFERENCE");
        String comparisonSnapshotSn = sourceSnapshotSn(batchSn, "COMPARISON");
        String referenceDigest = sourceDigest(referenceSnapshotSn);
        String comparisonDigest = sourceDigest(comparisonSnapshotSn);
        String runSn = batchSn + ":run";
        String sourceDigest = FundsStableHashSupport.sha256(referenceDigest + ":" + comparisonDigest);
        String resultDigest = FundsStableHashSupport.sha256("balanced:" + batchSn);
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_run_result (
                    sn, tenant_id, reconciliation_batch_sn,
                    scope_owner_namespace, scope_identity_value,
                    pair_owner_namespace, pair_identity_value, currency, status,
                    rule_namespace, rule_identity, rule_version,
                    reference_snapshot_sn, comparison_snapshot_sn,
                    reference_source_digest, comparison_source_digest, source_digest, result_digest,
                    total_count, matched_count, difference_count, evidence_refs, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'USD', 'BALANCED',
                    'test.rule', 'strict-exact', ?, ?, ?, ?, ?, ?, ?, 1, 1, 0, ?, 'SYSTEM')
                """, runSn, TENANT_ID, batchSn, scopeIdentity.getOwnerNamespace(), scopeIdentity.getValue(),
                pairIdentity.getOwnerNamespace(), pairIdentity.getValue(), RULE_VERSION,
                referenceSnapshotSn, comparisonSnapshotSn,
                referenceDigest, comparisonDigest, sourceDigest, resultDigest, "[\"" + evidenceRef + "\"]");
        jdbcTemplate.update("UPDATE t_reconciliation_batch SET status = 'COMPLETED', run_result_sn = ? "
                + "WHERE tenant_id = ? AND sn = ?", runSn, TENANT_ID, batchSn);
    }

    private GateStageRef stageSourceRef() {
        return new GateStageRef()
                .setStageKind("CLEARING_SPLITTABLE_IDENTIFY")
                .setStageIdentity(identity("funds", STAGE_FUNDS_TRANSACTION_SN + ":primary:0"));
    }

    private GateStageRef clearingConfirmStageRef(String suffix) {
        return new GateStageRef()
                .setStageKind("CLEARING_CONFIRM_ITEM")
                .setStageIdentity(identity("clearing-candidate", "gate-behavior-" + suffix + "-candidate"));
    }

    private IdentifyClearingSplittableDetailRequest stageRequest() {
        return new IdentifyClearingSplittableDetailRequest()
                .setTenantId(TENANT_ID)
                .setSourceActionFactRef(identity("funds", STAGE_FUNDS_TRANSACTION_SN + ":primary:0"))
                .setBusinessLine("ACQUIRING")
                .setSplitPeriod("2026-08-19")
                .setSplitRuleCode("GATE_BEHAVIOR")
                .setSplitRuleVersion("v1");
    }

    private void prepareStageSourceFacts() {
        jdbcTemplate.update("""
                INSERT INTO t_funds_transaction (
                    sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                    status, amount, currency, completed_amount, refunded_amount, declined_amount,
                    route_snapshot, version
                ) VALUES (?, ?, 'DIRECT', 'PAY', 'GATE_BEHAVIOR', 'gate-stage-source',
                    'CLOSED', ?, 'USD', ?, 0, 0,
                    ?, 0)
                """, STAGE_FUNDS_TRANSACTION_SN, TENANT_ID, STAGE_AMOUNT, STAGE_AMOUNT, STAGE_ROUTE_SNAPSHOT);
        jdbcTemplate.update("""
                INSERT INTO t_funds_transaction_detail (
                    sn, tenant_id, transaction_sn, business_scene, business_sn, transaction_type,
                    event_type, subject_id, subject_type, participant_role, request_hash,
                    funds_effect_type, ledger_transaction_sn, amount, currency, status
                ) VALUES (?, ?, ?, 'GATE_BEHAVIOR', 'gate-stage-source', 'PAY', 'PAY',
                    'gate-stage-subject', 'FUNDING_ACCOUNT', 'PAYEE', 'gate-stage-request',
                    'DIRECT', ?, ?, 'USD', 'SUCCEEDED')
                """, STAGE_FUNDS_TRANSACTION_DETAIL_SN, TENANT_ID, STAGE_FUNDS_TRANSACTION_SN,
                STAGE_LEDGER_TRANSACTION_SN, STAGE_AMOUNT);
        jdbcTemplate.update("""
                INSERT INTO t_funds_transaction_detail (
                    sn, tenant_id, transaction_sn, business_scene, business_sn, transaction_type,
                    event_type, subject_id, subject_type, participant_role, request_hash,
                    funds_effect_type, ledger_transaction_sn, amount, currency, status
                ) VALUES (?, ?, ?, 'GATE_BEHAVIOR', 'gate-stage-source', 'PAY', 'PAY',
                    'gate-stage-payer', 'FUNDING_ACCOUNT', 'PAYER', 'gate-stage-payer-request',
                    'DIRECT', ?, ?, 'USD', 'SUCCEEDED')
                """, STAGE_PAYER_DETAIL_SN, TENANT_ID, STAGE_FUNDS_TRANSACTION_SN,
                STAGE_LEDGER_TRANSACTION_SN, STAGE_AMOUNT);
        insertLedgerFacts(STAGE_FUNDS_TRANSACTION_SN, STAGE_LEDGER_TRANSACTION_SN,
                STAGE_POSTING_PLAN_SN, STAGE_LEDGER_ENTRY_SN, STAGE_AMOUNT);
    }

    private StageBatch prepareReviewingStageBatch(String suffix, long clearingBalance) {
        prepareStageAccount(clearingBalance);
        String candidateSn = insertStageCandidate(suffix);
        ClearingBatchDTO batch = clearingBatchApplicationService.createBatch(
                new CreateClearingBatchRequest().setTenantId(TENANT_ID).setCandidateSns(List.of(candidateSn)),
                WindOperatorFactory.system());
        batch = clearingBatchApplicationService.submitBatch(new SubmitClearingBatchRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSn(batch.getSn()), WindOperatorFactory.system());
        assertThat(batch.getState()).isEqualTo(ClearingBatchState.REVIEWING);
        assertThat(candidateState(candidateSn)).isEqualTo(ClearingCandidateState.LOCKED.name());
        return new StageBatch(batch.getSn(), candidateSn, clearingConfirmStageRef(suffix));
    }

    private String insertStageCandidate(String suffix) {
        String candidateSn = "gate-behavior-" + suffix + "-candidate";
        jdbcTemplate.update("""
                INSERT INTO t_clearing_candidate (
                    sn, tenant_id, split_result_sn, split_batch_sn, splittable_detail_sn,
                    subject_type, subject_id, currency, business_line, clearing_period, amount,
                    funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn,
                    posting_plan_sn, ledger_entry_sn, route_snapshot_digest, clearing_available_time,
                    clearing_rule_code, clearing_rule_version, gate_evidence_ref,
                    reconciliation_evidence_refs, source_digest, candidate_digest,
                    active_splittable_detail_sn, status, created_by
                ) VALUES (?, ?, ?, ?, ?, 'FUNDING_ACCOUNT', 'gate-stage-subject', 'USD',
                    'GATE_BEHAVIOR', '2026-08-19', 100, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
                    'GATE_BEHAVIOR', 'v1', ?, '["source-gate-evidence"]', ?, ?, ?, ?, 'SYSTEM')
                """, candidateSn, TENANT_ID, "gate-behavior-" + suffix + "-split-result",
                "gate-behavior-" + suffix + "-split-batch", "gate-behavior-" + suffix + "-detail",
                STAGE_FUNDS_TRANSACTION_SN, STAGE_FUNDS_TRANSACTION_DETAIL_SN, STAGE_LEDGER_TRANSACTION_SN,
                STAGE_POSTING_PLAN_SN, STAGE_LEDGER_ENTRY_SN,
                FundsStableHashSupport.sha256Json(Map.of("routeSnapshot", STAGE_ROUTE_SNAPSHOT)),
                "source-gate-evidence:" + suffix,
                FundsStableHashSupport.sha256("source:" + suffix),
                FundsStableHashSupport.sha256("candidate:" + suffix),
                "gate-behavior-" + suffix + "-detail", ClearingCandidateState.READY.name());
        return candidateSn;
    }

    private void prepareStageAccount(long clearingBalance) {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_funding_account WHERE sn = ?",
                Integer.class, STAGE_ACCOUNT_SN) == 0) {
            jdbcTemplate.update("""
                    INSERT INTO t_funding_account (
                        sn, tenant_id, owner_id, owner_type, account_type, is_platform,
                        currency, ledger_profile_code, ledger_profile_version, status, version
                    ) VALUES (?, ?, 'gate-stage-owner', 'USER', 'FUNDING_ACCOUNT', 0,
                        'USD', 'FUNDING_MERCHANT', 1, 'ACTIVE', 0)
                    """, STAGE_ACCOUNT_SN, TENANT_ID);
        }
        ledgerService.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(STAGE_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_MERCHANT)
                .setLedgerProfileVersion(1));
        jdbcTemplate.update("UPDATE t_ledger SET debit_amount = 0, credit_amount = ?, version = 0 "
                        + "WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = 'CLEARING'",
                clearingBalance, TENANT_ID, STAGE_ACCOUNT_SN);
        jdbcTemplate.update("UPDATE t_ledger SET debit_amount = 0, credit_amount = 0, version = 0 "
                        + "WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = 'AVAILABLE'",
                TENANT_ID, STAGE_ACCOUNT_SN);
    }

    private void assertDeterministicStageRejection(StageBatch stage) {
        List<Map<String, Object>> balanceBefore = rows("SELECT * FROM t_ledger WHERE subject_id = ? "
                + "ORDER BY ledger_subject_code", STAGE_ACCOUNT_SN);

        assertThatThrownBy(() -> confirmStage(stage.batchSn()))
                .isInstanceOf(LedgerPostingRejectedException.class)
                .hasMessageContaining("账本余额不足");

        ClearingBatchDTO failed = clearingBatchApplicationService.getBatch(TENANT_ID, stage.batchSn());
        assertThat(failed.getState()).isEqualTo(ClearingBatchState.FAILED);
        assertThat(candidateState(stage.candidateSn())).isEqualTo(ClearingCandidateState.BLOCKED.name());
        assertThat(failed.getFundsTransactionSn()).isNotBlank();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM t_funds_transaction WHERE sn = ?",
                String.class, failed.getFundsTransactionSn())).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT error_code FROM t_funds_transaction_detail "
                        + "WHERE transaction_sn = ?", String.class, failed.getFundsTransactionSn()))
                .isEqualTo("LEDGER_POSTING_REJECTED");
        assertThat(ledgerFactCount(failed.getFundsTransactionSn())).isZero();
        assertThat(rows("SELECT * FROM t_ledger WHERE subject_id = ? ORDER BY ledger_subject_code", STAGE_ACCOUNT_SN))
                .isEqualTo(balanceBefore);
        assertThat(consumedEvidenceCount()).isZero();
    }

    private String createConflictingExistingFundsFact(StageBatch stage) {
        String fundsTransactionSn = fundsClearingTransactionService.confirm(new FundsClearingConfirmRequest()
                .setAccountId(FundsAccountId.immutable(STAGE_ACCOUNT_SN, "FUNDING_ACCOUNT"))
                .setAmount(Money.immutable(STAGE_CLEARING_AMOUNT, CURRENCY))
                .setClearingBatchSn(stage.batchSn())
                .setSourceTransactionSns(List.of(STAGE_FUNDS_TRANSACTION_SN))
                .setDescription("gate behavior existing clearing fact"), WindOperatorFactory.system());
        jdbcTemplate.update("UPDATE t_funds_transaction_detail SET status = 'PROCESSING', request_hash = ? "
                        + "WHERE transaction_sn = ?",
                FundsStableHashSupport.sha256("gate-behavior-conflicting-detail"), fundsTransactionSn);
        return fundsTransactionSn;
    }

    private void assertUnknownStageFailsClosed(StageBatch stage, String fundsTransactionSn) {
        List<Map<String, Object>> stageBefore = rows("SELECT * FROM t_clearing_batch WHERE sn = ?", stage.batchSn());
        List<Map<String, Object>> candidateBefore = rows("SELECT * FROM t_clearing_candidate WHERE sn = ?",
                stage.candidateSn());
        List<Map<String, Object>> fundsBefore = rows("SELECT * FROM t_funds_transaction WHERE sn = ?",
                fundsTransactionSn);
        List<Map<String, Object>> detailsBefore = rows("SELECT * FROM t_funds_transaction_detail "
                + "WHERE transaction_sn = ?", fundsTransactionSn);
        List<Map<String, Object>> ledgerBefore = rows("SELECT * FROM t_ledger_transaction "
                + "WHERE funds_transaction_sn = ?", fundsTransactionSn);
        List<Map<String, Object>> postingBefore = rows("SELECT * FROM t_ledger_posting_plan "
                + "WHERE funds_transaction_sn = ?", fundsTransactionSn);
        List<Map<String, Object>> entriesBefore = rows("SELECT * FROM t_ledger_entry "
                + "WHERE funds_transaction_sn = ?", fundsTransactionSn);
        List<Map<String, Object>> balanceBefore = rows("SELECT * FROM t_ledger WHERE subject_id = ? "
                + "ORDER BY ledger_subject_code", STAGE_ACCOUNT_SN);
        assertThat(fundsBefore).hasSize(1);
        assertThat(detailsBefore).hasSize(1);
        assertThat(ledgerBefore).hasSize(1);
        assertThat(postingBefore).hasSize(1);
        assertThat(entriesBefore).hasSize(2);
        assertThat(jdbcTemplate.queryForObject("SELECT credit_amount - debit_amount FROM t_ledger "
                        + "WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = 'CLEARING'",
                Long.class, TENANT_ID, STAGE_ACCOUNT_SN)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT credit_amount - debit_amount FROM t_ledger "
                        + "WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = 'AVAILABLE'",
                Long.class, TENANT_ID, STAGE_ACCOUNT_SN)).isEqualTo(STAGE_CLEARING_AMOUNT);

        assertThatThrownBy(() -> confirmStage(stage.batchSn())).hasMessageContaining("请求参数不一致");

        assertThat(rows("SELECT * FROM t_clearing_batch WHERE sn = ?", stage.batchSn())).isEqualTo(stageBefore);
        assertThat(rows("SELECT * FROM t_clearing_candidate WHERE sn = ?", stage.candidateSn()))
                .isEqualTo(candidateBefore);
        assertThat(rows("SELECT * FROM t_funds_transaction WHERE sn = ?", fundsTransactionSn))
                .isEqualTo(fundsBefore);
        assertThat(rows("SELECT * FROM t_funds_transaction_detail WHERE transaction_sn = ?", fundsTransactionSn))
                .isEqualTo(detailsBefore);
        assertThat(rows("SELECT * FROM t_ledger_transaction WHERE funds_transaction_sn = ?", fundsTransactionSn))
                .isEqualTo(ledgerBefore);
        assertThat(rows("SELECT * FROM t_ledger_posting_plan WHERE funds_transaction_sn = ?", fundsTransactionSn))
                .isEqualTo(postingBefore);
        assertThat(rows("SELECT * FROM t_ledger_entry WHERE funds_transaction_sn = ?", fundsTransactionSn))
                .isEqualTo(entriesBefore);
        assertThat(rows("SELECT * FROM t_ledger WHERE subject_id = ? ORDER BY ledger_subject_code", STAGE_ACCOUNT_SN))
                .isEqualTo(balanceBefore);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_funds_transaction "
                        + "WHERE business_scene = 'CLEARING_CONFIRM' AND business_sn = ?",
                Integer.class, stage.batchSn())).isOne();
        assertThat(consumedEvidenceCount()).isZero();
    }

    private void confirmStage(String clearingBatchSn) {
        clearingBatchApplicationService.confirmBatch(new ConfirmClearingBatchRequest()
                .setTenantId(TENANT_ID)
                .setClearingBatchSn(clearingBatchSn), WindOperatorFactory.system());
    }

    private String candidateState(String candidateSn) {
        return jdbcTemplate.queryForObject("SELECT status FROM t_clearing_candidate WHERE sn = ?",
                String.class, candidateSn);
    }

    private void insertLedgerFacts(String fundsTransactionSn,
                                   String ledgerTransactionSn,
                                   String postingPlanSn,
                                   String ledgerEntrySn,
                                   long amount) {
        Map<String, Object> payeeEntryFacts = stageLedgerEntryDigestFacts(
                fundsTransactionSn, ledgerTransactionSn, postingPlanSn, ledgerEntrySn, 1L,
                "gate-stage-subject", "CLEARING", "CLEARING", "CREDIT", "INCREASE", amount);
        Map<String, Object> payerEntryFacts = stageLedgerEntryDigestFacts(
                fundsTransactionSn, ledgerTransactionSn, postingPlanSn, ledgerEntrySn + "-payer", 2L,
                "gate-stage-payer", "AVAILABLE", "ASSET", "DEBIT", "DECREASE", amount);
        Map<String, Object> postingPlanFacts = stagePostingPlanDigestFacts(
                fundsTransactionSn, ledgerTransactionSn, postingPlanSn, amount);
        Map<String, Object> planAggregate = new TreeMap<>();
        planAggregate.put("plan", postingPlanFacts);
        planAggregate.put("entries", List.of(payeeEntryFacts, payerEntryFacts));
        String transactionDigest = stageLedgerTransactionDigest(
                fundsTransactionSn, ledgerTransactionSn, amount, List.of(planAggregate));
        jdbcTemplate.update("""
                INSERT INTO t_ledger_transaction (
                    sn, tenant_id, funds_transaction_sn, instruction_type, event_type, transaction_type,
                    business_scene, business_sn, amount, currency, original_amount, original_currency,
                    exchange_rate, debit_amount, credit_amount, transaction_time, sha256
                ) VALUES (?, ?, ?, 'DIRECT_TRANSACTION', 'PAY', 'PAY', 'GATE_BEHAVIOR', ?,
                    ?, 'USD', ?, 'USD', 1, ?, ?, ?, ?)
                """, ledgerTransactionSn, TENANT_ID, fundsTransactionSn, fundsTransactionSn,
                amount, amount, amount, amount, STAGE_LEDGER_TRANSACTION_TIME, transactionDigest);
        jdbcTemplate.update("""
                INSERT INTO t_ledger_posting_plan (
                    sn, tenant_id, ledger_transaction_sn, funds_transaction_sn, route_leg_id, intent,
                    posting_scope, balance_effect_type, phase_code, amount, currency, debit_amount,
                    credit_amount, sha256
                ) VALUES (?, ?, ?, ?, 'gate-stage-leg', 'TRANSFER', 'BETWEEN_SUBJECTS', 'INCREASE',
                    'TRANSFER', ?, 'USD', ?, ?, ?)
                """, postingPlanSn, TENANT_ID, ledgerTransactionSn, fundsTransactionSn,
                amount, amount, amount,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_POSTING_PLAN_DIGEST_DOMAIN, postingPlanFacts));
        jdbcTemplate.update("""
                INSERT INTO t_ledger_entry (
                    sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn, ledger_id,
                    subject_id, subject_type, ledger_subject_code, ledger_subject_category, entry_side,
                    posting_role, balance_constraint_type, intent, posting_scope, balance_effect_type,
                    phase_code, business_scene, business_sn, amount, currency, original_amount,
                    original_currency, exchange_rate, transaction_time, sha256
                ) VALUES (?, ?, ?, ?, ?, 1, 'gate-stage-subject', 'FUNDING_ACCOUNT', 'CLEARING',
                    'CLEARING', 'CREDIT', 'DETAIL', 'MUST_NOT_BE_NEGATIVE', 'TRANSFER',
                    'BETWEEN_SUBJECTS', 'INCREASE', 'TRANSFER', 'GATE_BEHAVIOR', ?, ?, 'USD', ?, 'USD', 1, ?, ?)
                """, ledgerEntrySn, TENANT_ID, ledgerTransactionSn, postingPlanSn, fundsTransactionSn,
                fundsTransactionSn, amount, amount, STAGE_LEDGER_TRANSACTION_TIME,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, payeeEntryFacts));
        jdbcTemplate.update("""
                INSERT INTO t_ledger_entry (
                    sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn, ledger_id,
                    subject_id, subject_type, ledger_subject_code, ledger_subject_category, entry_side,
                    posting_role, balance_constraint_type, intent, posting_scope, balance_effect_type,
                    phase_code, business_scene, business_sn, amount, currency, original_amount,
                    original_currency, exchange_rate, transaction_time, sha256
                ) VALUES (?, ?, ?, ?, ?, 2, 'gate-stage-payer', 'FUNDING_ACCOUNT', 'AVAILABLE',
                    'ASSET', 'DEBIT', 'DETAIL', 'MUST_NOT_BE_NEGATIVE', 'TRANSFER',
                    'BETWEEN_SUBJECTS', 'DECREASE', 'TRANSFER', 'GATE_BEHAVIOR', ?, ?, 'USD', ?, 'USD', 1, ?, ?)
                """, ledgerEntrySn + "-payer", TENANT_ID, ledgerTransactionSn, postingPlanSn,
                fundsTransactionSn, fundsTransactionSn, amount, amount,
                STAGE_LEDGER_TRANSACTION_TIME,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, payerEntryFacts));
    }

    private String stageLedgerTransactionDigest(String fundsTransactionSn,
                                                String ledgerTransactionSn,
                                                long amount,
                                                List<Map<String, Object>> postingPlans) {
        Map<String, Object> transaction = new TreeMap<>();
        transaction.put("sn", ledgerTransactionSn);
        transaction.put("tenantId", TENANT_ID);
        transaction.put("instructionType", "DIRECT_TRANSACTION");
        transaction.put("eventType", "PAY");
        transaction.put("fundsTransactionSn", fundsTransactionSn);
        transaction.put("transactionType", "PAY");
        transaction.put("businessScene", "GATE_BEHAVIOR");
        transaction.put("businessSn", fundsTransactionSn);
        transaction.put("amount", amount);
        transaction.put("currency", "USD");
        transaction.put("originalAmount", amount);
        transaction.put("originalCurrency", "USD");
        transaction.put("exchangeRate", BigDecimal.ONE);
        transaction.put("debitAmount", amount);
        transaction.put("creditAmount", amount);
        transaction.put("transactionTime", STAGE_LEDGER_TRANSACTION_TIME);
        transaction.put("referenceLedgerTransactionSn", null);
        Map<String, Object> aggregate = new TreeMap<>();
        aggregate.put("transaction", transaction);
        aggregate.put("postingPlans", postingPlans);
        return FundsStableHashSupport.sha256CanonicalJson(LEDGER_TRANSACTION_DIGEST_DOMAIN, aggregate);
    }

    private Map<String, Object> stagePostingPlanDigestFacts(String fundsTransactionSn,
                                                            String ledgerTransactionSn,
                                                            String postingPlanSn,
                                                            long amount) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", postingPlanSn);
        facts.put("tenantId", TENANT_ID);
        facts.put("ledgerTransactionSn", ledgerTransactionSn);
        facts.put("fundsTransactionSn", fundsTransactionSn);
        facts.put("routeLegId", "gate-stage-leg");
        facts.put("intent", "TRANSFER");
        facts.put("postingScope", "BETWEEN_SUBJECTS");
        facts.put("balanceEffectType", "INCREASE");
        facts.put("phaseCode", "TRANSFER");
        facts.put("amount", amount);
        facts.put("currency", "USD");
        facts.put("debitAmount", amount);
        facts.put("creditAmount", amount);
        return facts;
    }

    private Map<String, Object> stageLedgerEntryDigestFacts(String fundsTransactionSn,
                                                            String ledgerTransactionSn,
                                                            String postingPlanSn,
                                                            String ledgerEntrySn,
                                                            long ledgerId,
                                                            String subjectId,
                                                            String subjectCode,
                                                            String subjectCategory,
                                                            String entrySide,
                                                            String balanceEffectType,
                                                            long amount) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", ledgerEntrySn);
        facts.put("tenantId", TENANT_ID);
        facts.put("ledgerTransactionSn", ledgerTransactionSn);
        facts.put("postingPlanSn", postingPlanSn);
        facts.put("fundsTransactionSn", fundsTransactionSn);
        facts.put("ledgerId", ledgerId);
        facts.put("periodType", "LIFETIME");
        facts.put("periodId", "LIFETIME");
        facts.put("subjectId", subjectId);
        facts.put("subjectType", "FUNDING_ACCOUNT");
        facts.put("ledgerSubjectCode", subjectCode);
        facts.put("ledgerSubjectCategory", subjectCategory);
        facts.put("entrySide", entrySide);
        facts.put("postingRole", "DETAIL");
        facts.put("balanceConstraintType", "MUST_NOT_BE_NEGATIVE");
        facts.put("intent", "TRANSFER");
        facts.put("postingScope", "BETWEEN_SUBJECTS");
        facts.put("balanceEffectType", balanceEffectType);
        facts.put("phaseCode", "TRANSFER");
        facts.put("businessScene", "GATE_BEHAVIOR");
        facts.put("businessSn", fundsTransactionSn);
        facts.put("amount", amount);
        facts.put("currency", "USD");
        facts.put("originalAmount", amount);
        facts.put("originalCurrency", "USD");
        facts.put("exchangeRate", BigDecimal.ONE);
        facts.put("transactionTime", STAGE_LEDGER_TRANSACTION_TIME);
        return facts;
    }

    private static String stageRouteSnapshot() {
        Map<String, Object> payer = subject("gate-stage-payer");
        Map<String, Object> payee = subject(STAGE_ACCOUNT_SN);
        Map<String, Object> values = new java.util.TreeMap<>();
        values.put("tenantId", TENANT_ID);
        values.put("snapshotId", "gate-stage-route");
        values.put("snapshotSchemaVersion", "1.0");
        values.put("routeCode", "DIRECT_PAY_STANDARD");
        values.put("routeVersion", "1.0");
        values.put("businessScene", "GATE_BEHAVIOR");
        values.put("businessSn", "gate-stage-source");
        values.put("instructionType", "DIRECT_TRANSACTION");
        values.put("eventType", "PAY");
        values.put("transactionType", "PAY");
        values.put("participants", List.of(
                participant("PAYER", payer), participant("PAYEE", payee)));
        values.put("legs", List.of(Map.ofEntries(
                Map.entry("legId", "PAY"),
                Map.entry("sequence", 0),
                Map.entry("legType", "INTERNAL_TRANSFER"),
                Map.entry("sourceNode", node("SOURCE", payer)),
                Map.entry("targetNode", node("TARGET", payee)),
                Map.entry("amount", money(STAGE_AMOUNT)),
                Map.entry("originalAmount", money(STAGE_AMOUNT)),
                Map.entry("exchangeRate", 1),
                Map.entry("replayPolicy", "FULL_ONLY"),
                Map.entry("contextVariables", Map.of()))));
        return WindJson.toJsonString(values);
    }

    private static Map<String, Object> participant(String role, Map<String, Object> subject) {
        return Map.of(
                "participantRole", role,
                "subjectRef", subject,
                "currency", "USD",
                "amount", money(STAGE_AMOUNT),
                "contextVariables", Map.of());
    }

    private static Map<String, Object> node(String role, Map<String, Object> subject) {
        return Map.of("nodeType", "SUBJECT", "subjectRef", subject, "nodeRole", role);
    }

    private static Map<String, Object> subject(String subjectId) {
        return Map.of(
                "tenantId", TENANT_ID,
                "subjectId", subjectId,
                "subjectType", "FUNDING_ACCOUNT",
                "currency", "USD");
    }

    private static Map<String, Object> money(long amount) {
        return Map.of("amount", amount, "currency", "USD");
    }

    private void seedDifference(String suffix,
                                String batchSn,
                                StableIdentity scopeIdentity,
                                StableIdentity pairIdentity,
                                String status) {
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_difference (
                    difference_sn, tenant_id, reconciliation_batch_sn, reconciliation_match_result_sn,
                    scope_owner_namespace, scope_identity_value, pair_owner_namespace, pair_identity_value,
                    difference_type, severity, status, responsible_party_ref,
                    rule_namespace, rule_identity, rule_version, current_lineage_ref, evidence_ref, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'STATUS_MISMATCH', 'S1_MAJOR', ?, 'owner:test',
                    'test.rule', 'strict-exact', ?, ?, ?, 'SYSTEM')
                """, suffix, TENANT_ID, batchSn, suffix + ":match",
                scopeIdentity.getOwnerNamespace(), scopeIdentity.getValue(),
                pairIdentity.getOwnerNamespace(), pairIdentity.getValue(), status, RULE_VERSION,
                batchSn, suffix + ":evidence");
    }

    private void resolveDifference(String differenceSn, String successorBatchSn) {
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_difference_action (
                    sn, tenant_id, difference_sn, action_type, adjustment_sn, idempotency_key,
                    original_fact_ref, approval_ref, evidence_ref, reason, created_by
                ) VALUES (?, ?, ?, 'ADJUST', ?, ?, ?, ?, ?, 'controlled adjustment', 'SYSTEM')
                """, differenceSn + ":action", TENANT_ID, differenceSn, differenceSn + ":adjustment",
                differenceSn + ":idempotency", differenceSn + ":original",
                differenceSn + ":approval", differenceSn + ":action-evidence");
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'RESOLVED', action_type = 'ADJUST', adjustment_sn = ?,
                    adjustment_idempotency_key = ?, original_fact_ref = ?,
                    adjustment_approval_ref = ?, adjustment_evidence_ref = ?,
                    last_rerun_sn = ?, last_rerun_batch_sn = ?, last_rerun_rule_version = ?,
                    last_rerun_balanced = TRUE, last_rerun_evidence_ref = ?,
                    last_rerun_result_digest = ?, current_lineage_ref = ?, rerun_count = 1,
                    resolved_by = 'SYSTEM', resolved_time = CURRENT_TIMESTAMP
                WHERE tenant_id = ? AND difference_sn = ?
                """, differenceSn + ":adjustment", differenceSn + ":idempotency",
                differenceSn + ":original", differenceSn + ":approval", differenceSn + ":action-evidence",
                successorBatchSn + ":run", successorBatchSn, RULE_VERSION,
                successorBatchSn + ":evidence", FundsStableHashSupport.sha256("balanced:" + successorBatchSn),
                successorBatchSn, TENANT_ID, differenceSn);
    }

    private List<Map<String, Object>> differenceSnapshot() {
        return jdbcTemplate.queryForList("SELECT * FROM t_reconciliation_difference ORDER BY difference_sn");
    }

    private List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbcTemplate.queryForList(sql, args);
    }

    private void assertConcurrentFirstPublication(String caseId) throws Exception {
        GateSetup setup = prepareGate(caseId, 1, 1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<GateRequirementRef> first = executor.submit(() -> concurrentRecord(start, setup.request()));
            Future<GateRequirementRef> second = executor.submit(() -> concurrentRecord(start, setup.request()));
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsOnly(first.get());
            assertThat(requirementCount(caseId)).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertConcurrentConflictingFirstPublication(String caseId) throws Exception {
        GateSetup setup = prepareGate(caseId, 1, 1);
        RecordReconciliationGateRequirementRequest conflicting = copy(setup.request())
                .setRequirementVersion("v2")
                .setEvidenceRefs(List.of("evidence:" + caseId + ":v2"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<GateRequirementRef>> futures = List.of(
                    executor.submit(() -> concurrentRecord(start, setup.request())),
                    executor.submit(() -> concurrentRecord(start, conflicting)));
            start.countDown();
            List<GateRequirementRef> successes = new ArrayList<>();
            List<Throwable> failures = new ArrayList<>();
            for (Future<GateRequirementRef> future : futures) {
                try {
                    successes.add(future.get());
                } catch (ExecutionException exception) {
                    failures.add(exception.getCause());
                }
            }
            assertThat(successes).hasSize(1);
            assertThat(failures).singleElement().satisfies(failure ->
                    assertThat(failure).hasMessageContaining("冲突"));
            assertThat(requirementCount(caseId)).isOne();
            assertThat(requirementPairCount(caseId)).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private GateRequirementRef concurrentRecord(CountDownLatch start,
                                                 RecordReconciliationGateRequirementRequest request) throws Exception {
        TenantContextHolder.setTenantId(TENANT_ID);
        try {
            start.await();
            return record(request);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private GateRequirementRef record(RecordReconciliationGateRequirementRequest request) {
        return reconciliationGateApplicationService.recordGateRequirement(request, WindOperatorFactory.system());
    }

    private ReconciliationGateDecisionDTO check(GateStageRef stageRef) {
        return new TransactionTemplate(transactionManager).execute(status -> reconciliationGateApplicationService.checkGate(
                checkRequest(stageRef), WindOperatorFactory.system()));
    }

    private CheckReconciliationGateRequest checkRequest(GateStageRef stageRef) {
        return new CheckReconciliationGateRequest().setTenantId(TENANT_ID).setStageRef(stageRef);
    }

    private RecordReconciliationGateRequirementRequest copy(RecordReconciliationGateRequirementRequest source) {
        return new RecordReconciliationGateRequirementRequest()
                .setTenantId(source.getTenantId())
                .setStageRef(source.getStageRef())
                .setRequirementVersion(source.getRequirementVersion())
                .setRequiredPairs(source.getRequiredPairs())
                .setExpectedCurrentRequirementRef(source.getExpectedCurrentRequirementRef())
                .setEvidenceRefs(source.getEvidenceRefs());
    }

    private String sourceSnapshotSn(String batchSn, String role) {
        return jdbcTemplate.queryForObject("SELECT sn FROM t_reconciliation_source_snapshot "
                + "WHERE tenant_id = ? AND reconciliation_batch_sn = ? AND source_role = ?",
                String.class, TENANT_ID, batchSn, role);
    }

    private String sourceDigest(String snapshotSn) {
        return jdbcTemplate.queryForObject("SELECT source_digest FROM t_reconciliation_source_snapshot "
                + "WHERE tenant_id = ? AND sn = ?", String.class, TENANT_ID, snapshotSn);
    }

    private String batchSn(String caseId, int index) {
        return "gate-" + caseId + "-" + index;
    }

    private int requirementCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_gate_requirement", Integer.class);
    }

    private int requirementCount(String caseId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_gate_requirement "
                + "WHERE stage_identity_value = ?", Integer.class, "stage-" + caseId);
    }

    private int requirementPairCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_gate_requirement_pair", Integer.class);
    }

    private int requirementPairCount(String caseId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_reconciliation_gate_requirement_pair pair_item
                JOIN t_reconciliation_gate_requirement requirement
                  ON requirement.tenant_id = pair_item.tenant_id
                 AND requirement.requirement_identity_owner_namespace = pair_item.requirement_identity_owner_namespace
                 AND requirement.requirement_identity_value = pair_item.requirement_identity_value
                WHERE requirement.stage_identity_value = ?
                """, Integer.class, "stage-" + caseId);
    }

    private int consumedEvidenceCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_stage_gate_evidence", Integer.class);
    }

    private String consumedEvidenceSn() {
        return jdbcTemplate.queryForObject("SELECT sn FROM t_reconciliation_stage_gate_evidence", String.class);
    }

    private int stageDetailCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_splittable_detail", Integer.class);
    }

    private int fundsFactCount(String fundsTransactionSn) {
        int transactions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_funds_transaction WHERE sn = ?",
                Integer.class, fundsTransactionSn);
        int details = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_funds_transaction_detail "
                + "WHERE transaction_sn = ?", Integer.class, fundsTransactionSn);
        return transactions + details;
    }

    private int runResultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_run_result", Integer.class);
    }

    private int differenceCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_difference", Integer.class);
    }

    private int ledgerFactCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger_transaction", Integer.class);
    }

    private int ledgerFactCount(String fundsTransactionSn) {
        int transactions = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger_transaction "
                + "WHERE funds_transaction_sn = ?", Integer.class, fundsTransactionSn);
        int plans = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger_posting_plan "
                + "WHERE funds_transaction_sn = ?", Integer.class, fundsTransactionSn);
        int entries = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ledger_entry "
                + "WHERE funds_transaction_sn = ?", Integer.class, fundsTransactionSn);
        return transactions + plans + entries;
    }

    private void assertClassMissing(String className) {
        assertThatThrownBy(() -> Class.forName(className)).isInstanceOf(ClassNotFoundException.class);
    }

    private void assertBehavior(String contractId, ThrowingRunnable assertions) {
        assertThatCode(assertions::run)
                .as(contractId + " accepted Gate behavior")
                .doesNotThrowAnyException();
    }

    private record GateSetup(GateStageRef stageRef, RecordReconciliationGateRequirementRequest request) {
    }

    private record StageBatch(String batchSn, String candidateSn, GateStageRef stageRef) {
    }

    @Configuration
    @Import({
            FundsClearingInstructionConverter.class,
            RouteParticipantFactory.class,
            RouteSubjectSupport.class,
            PlatformAccountRouteSupport.class,
            RefundRouteAdmission.class,
            RouteFeeChargeAppender.class,
            RouteAccountHierarchySnapshotAppender.class,
            ClearingFundsInstructionRouteResolver.class,
            CompositeRouteResolver.class,
            DefaultRouteSnapshotFactory.class,
            DefaultLedgerPostingAssembler.class,
            DefaultRoutedFundsInstructionOrchestrator.class,
            FundsClearingTransactionServiceImpl.class,
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultFundsInstructionLifecycleSaver.class,
            DelegatingFundsInstructionLifecycleRecorder.class,
            DefaultFundsTransactionQueryService.class,
            LedgerServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class,
            AccountHierarchyRelationServiceImpl.class,
            ClearingBatchApplicationServiceImpl.class,
            ClearingSplittableDetailApplicationServiceImpl.class
    })
    static class StageConfig {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {

        void run() throws Exception;
    }

    private static final class ExpectedRollback extends RuntimeException {
    }
}
