package com.wind.funds.reconciliation.application.gate.impl;

import com.wind.integration.operator.WindOperator;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.application.batch.impl.ReconciliationBatchApplicationServiceImpl;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceState;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateBlockerCode;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGatePairDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.ReplaceReconciliationBatchRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 对账差错准入消费应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationGateApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationGateApplicationServiceTests extends AbstractFundsServiceTest {

    private static final int MAX_GATE_DIFFERENCE_COUNT = 100;

    private static final String RECONCILIATION_BATCH_SN = "recon_gate_batch_001";

    private static final String ADJUSTMENT_SN = "balance_adjust_gate_001";

    private static final String RERUN_BATCH_SN = "recon_gate_batch_001_rerun_001";

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private ReconciliationBatchApplicationService reconciliationBatchApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepareReconciliationEvidence() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", RECONCILIATION_BATCH_SN, "processor-gate-file-digest-001");
        clearingRunResultSn = recordBalancedRunResult("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-recon-run-001");
        settlementMatchResultSn = recordDifferenceMatchResultSn("SETTLEMENT_LOCK",
                "settlement-order-001", "recon-settlement-difference-batch-001",
                "processor-settlement-file-digest-001");
        settlementRunResultSn = recordBalancedRunResult("SETTLEMENT_LOCK",
                "settlement-order-001", "recon-settlement-batch-001",
                "recon-settlement-difference-batch-001", "report:settlement-recon-run-001");
    }

    private String clearingRunResultSn;

    private String settlementRunResultSn;

    private String clearingMatchResultSn;

    private String settlementMatchResultSn;

    private ReconciliationGateDecisionDTO checkGate(CheckReconciliationGateRequest request,
                                                     WindOperator operator) {
        return new TransactionTemplate(transactionManager).execute(
                status -> reconciliationGateApplicationService.checkGate(request, operator));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void testCheckGateShouldRequireCallerTransaction() {
        assertThatThrownBy(() -> reconciliationGateApplicationService.checkGate(
                clearingGateRequest(), WindOperatorFactory.system()))
                .isInstanceOf(IllegalTransactionStateException.class)
                .hasMessageContaining("existing transaction");
    }

    @Test
    void testCheckGateShouldRejectTenantDifferentFromCurrentContext() {
        CheckReconciliationGateRequest request = new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> checkGate(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
    }

    /**
     * 场景：清算候选生成前存在命中 CLEARING 阻断范围的重大差错。
     * 输入：BLOCKED 状态的对账差错和清算准入检查请求。
     * 输出：准入决策阻断，并返回差错流水、责任方、证据和下一步解释。
     * 红线：准入检查只读差错状态，不得生成 route、posting、ledger transaction 或 ledger entry。
     */
    @Test
    void testCheckGateShouldBlockClearingWhenScopedDifferenceIsUnresolved() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createCurrentDifference(minimumCreateRequest());

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getStageRef().getStageKind()).isEqualTo("CLEARING_CONFIRM_ITEM");
        assertThat(result.getStageRef().getStageIdentity().getValue()).isEqualTo("clearing-candidate-001");
        assertThat(result.getPairDecisions())
                .extracting(ReconciliationGatePairDecisionDTO::getCurrentRunResultSn)
                .containsExactly(runResultSnForMatch(clearingMatchResultSn));
        ReconciliationGatePairDecisionDTO blockingDifference = result.getPairDecisions().getFirst();
        assertThat(blockingDifference.getBlockerCodes()).isNotEmpty();
        assertThat(blockingDifference.getRequiredPairRef()).isNotNull();
        assertThat(blockingDifference.getEvidenceRefs()).contains("processor-gate-file-digest-001");
        assertThat(result.getEvidenceRefs()).contains("report:clearing-recon-run-001",
                "processor-gate-file-digest-001");
        assertThat(result.getExplanation()).contains("mandatory pair");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：清算候选生成前已有明确 BALANCED 运行结果且没有命中差错。
     * 输入：正向运行结果、空差错表和清算准入检查请求。
     * 输出：准入通过，并返回本次运行的正向证据。
     * 红线：不得把“差错表为空”单独解释成对账通过。
     */
    @Test
    void testCheckGateShouldPassWhenNoScopedDifferenceExists() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).as("decision=%s", result).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions().getFirst().getCurrentRunResultSn()).isEqualTo(clearingRunResultSn);
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
        assertPassedEvidence(result, "report:clearing-recon-run-001");
        assertThat(result.getExplanation()).contains("current lineage 已对平");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：原本对平的当前批次随后被显式替代。
     * 结果：旧运行结果立即失去准入资格，即使其结论仍为 BALANCED。
     */
    @Test
    void testCheckGateShouldBlockReplacedCurrentBatch() {
        reconciliationBatchApplicationService.replaceBatch(new ReplaceReconciliationBatchRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(RERUN_BATCH_SN)
                        .setRuleVersion("recon-rule-v2")
                        .setReason("外部结算文件解析版本错误")
                        .setEvidenceRef("evidence:parser-incident-001"),
                WindOperatorFactory.system());

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes())
                .contains(ReconciliationGateBlockerCode.RUN_NOT_COMPLETED);
    }

    /**
     * 场景：旧批次差错因来源或匹配证据无效被标记为 INVALIDATED。
     * 结果：它不再表示真实资金差异，不阻断后继已对平的当前批次。
     */
    @Test
    void testCheckGateShouldIgnoreInvalidatedDifference() {
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest());
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET state = 'INVALIDATED'
                WHERE tenant_id = ? AND difference_sn = ?
                """, TENANT_ID, requiredDifferenceSn(clearingMatchResultSn));

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
    }

    @Test
    void testCheckGateShouldFailClosedWhenDifferenceCountExceedsCapacity() {
        List<Object[]> arguments = IntStream.rangeClosed(
                        1, MAX_GATE_DIFFERENCE_COUNT + 1)
                .mapToObj(index -> new Object[]{
                        "capacity-difference-" + index,
                        TENANT_ID,
                        RERUN_BATCH_SN,
                        "capacity-match-" + index,
                        RERUN_BATCH_SN,
                        "capacity-evidence-" + index
                })
                .toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO t_reconciliation_difference (
                    difference_sn, tenant_id, reconciliation_batch_sn,
                    reconciliation_match_result_sn, scope_owner_namespace, scope_identity_value,
                    pair_owner_namespace, pair_identity_value, difference_type, severity, state,
                    currency, difference_amount, responsible_party_ref,
                    rule_namespace, rule_identity, rule_version, current_lineage_ref, evidence_ref
                ) VALUES (?, ?, ?, ?, 'test.scope', 'CLEARING_CONFIRM_ITEM:clearing-candidate-001',
                    'test.pair', 'test-pair:recon_gate_batch_001', 'STATUS_MISMATCH',
                    'S1_MAJOR', 'BLOCKED', 'USD', 1, 'processor:capacity',
                    'test.rule', 'strict-exact', 'recon-rule-v1', ?, ?)
                """, arguments);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes())
                .contains(ReconciliationGateBlockerCode.BLOCKING_DIFFERENCE_PRESENT);
    }

    @Test
    void testCheckGateShouldIgnoreResolvedHistoryWhenCheckingCapacity() {
        List<Object[]> arguments = IntStream.rangeClosed(
                        1, MAX_GATE_DIFFERENCE_COUNT + 1)
                .mapToObj(index -> new Object[]{
                        "resolved-capacity-difference-" + index,
                        TENANT_ID,
                        RERUN_BATCH_SN,
                        "resolved-capacity-match-" + index,
                        RERUN_BATCH_SN,
                        "resolved-capacity-evidence-" + index
                })
                .toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO t_reconciliation_difference (
                    difference_sn, tenant_id, reconciliation_batch_sn,
                    reconciliation_match_result_sn, scope_owner_namespace, scope_identity_value,
                    pair_owner_namespace, pair_identity_value, difference_type, severity, state,
                    currency, difference_amount, responsible_party_ref,
                    rule_namespace, rule_identity, rule_version, current_lineage_ref, evidence_ref
                ) VALUES (?, ?, ?, ?, 'test.scope', 'CLEARING_CONFIRM_ITEM:clearing-candidate-001',
                    'test.pair', 'test-pair:recon_gate_batch_001', 'STATUS_MISMATCH',
                    'S1_MAJOR', 'RESOLVED', 'USD', 1, 'processor:capacity',
                    'test.rule', 'strict-exact', 'recon-rule-v1', ?, ?)
                """, arguments);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
    }

    @Test
    void testCheckGateShouldUsePublishedCurrentLineageWithoutTraversingHistoricalCycle() {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET previous_batch_sn = ?
                WHERE tenant_id = ? AND sn = ?
                """, RERUN_BATCH_SN, TENANT_ID, RERUN_BATCH_SN);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getPairDecisions().getFirst().getCurrentBatchSn()).isEqualTo(RERUN_BATCH_SN);
    }

    @Test
    void testCheckGateShouldUsePublishedCurrentLineageWithoutTraversingHistoricalGap() {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET previous_batch_sn = 'missing-lineage-batch'
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, RERUN_BATCH_SN);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getPairDecisions().getFirst().getCurrentBatchSn()).isEqualTo(RERUN_BATCH_SN);
    }

    @Test
    void testCheckGateShouldKeepEvaluationBoundedToPublishedCurrentLineage() {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET previous_batch_sn = 'historical-lineage-outside-gate-read'
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, RERUN_BATCH_SN);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getPairDecisions().getFirst().getCurrentLineageRef()).isEqualTo(RERUN_BATCH_SN);
    }

    /**
     * 场景：运营报表只需解释当前 Gate 结论，不参与最终资金命令。
     * 结果：只读检查返回与当前事实一致的时点快照。
     * 红线：该结果不是授权凭证，最终资金命令仍必须调用 checkGate。
     */
    @Test
    void testInspectGateShouldReturnPointInTimeDecision() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions().getFirst().getCurrentRunResultSn()).isEqualTo(clearingRunResultSn);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：已对平批次被新的重跑批次引用，但调用方仍尝试消费旧运行结果。
     * 结果：旧结果立即失去门禁资格，即使新批次尚未完成也不得回退信任旧结论。
     */
    @Test
    void testCheckGateShouldBlockRunResultSupersededByRerunBatch() {
        reconciliationBatchApplicationService.createBatch(new CreateReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
                .setScopeIdentity(com.wind.funds.reconciliation.ReconciliationTestFixture.identity(
                        "test.scope", "CLEARING_CONFIRM_ITEM:clearing-candidate-001"))
                .setPairIdentity(com.wind.funds.reconciliation.ReconciliationTestFixture.identity(
                        "test.pair", "test-pair:recon_gate_batch_001"))
                .setCurrency(CurrencyIsoCode.USD)
                .setComparisonRuleRef(com.wind.funds.reconciliation.ReconciliationTestFixture.rule("recon-rule-v2"))
                .setWindowStart(LocalDateTime.of(2026, 7, 21, 0, 0))
                .setWindowEnd(LocalDateTime.of(2026, 7, 22, 0, 0))
                .setTimeSemantics("occurredAt")
                .setTimezoneId("Asia/Shanghai")
                .setPreviousBatchSn(RERUN_BATCH_SN), WindOperatorFactory.system());

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes())
                .contains(ReconciliationGateBlockerCode.RUN_NOT_COMPLETED);
    }

    /**
     * 场景：调用方没有可读取的对账运行结果。
     * 结果：准入失败关闭，不能用空差错表替代正向对账证据。
     */
    @Test
    void testCheckGateShouldBlockWhenRunResultDoesNotExist() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result WHERE sn = ?", clearingRunResultSn);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getPairDecisions()).hasSize(1);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes())
                .contains(ReconciliationGateBlockerCode.RUN_NOT_COMPLETED);
        assertThat(result.getEvidenceRefs()).contains("report:clearing-recon-run-001");
        assertThat(result.getExplanation()).contains("mandatory pair");
    }

    /**
     * 场景：完成态逐笔匹配结果包含差异。
     * 结果：即使尚未登记差错单也必须阻断，直到新批次形成 BALANCED 结果。
     */
    @Test
    void testCheckGateShouldBlockWhenRunResultIsNotBalanced() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result WHERE sn = ?", clearingRunResultSn);
        clearingRunResultSn = recordDifferenceRunResult("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", "recon-difference-batch-001",
                "report:clearing-recon-difference-001");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getEvidenceRefs()).contains("report:clearing-recon-difference-001");
        assertThat(result.getEvidenceRefs()).anyMatch(evidence -> evidence.startsWith("run:"));
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes())
                .contains(ReconciliationGateBlockerCode.RUN_NOT_BALANCED);
    }

    /**
     * 场景：调用方把结算对象的正向结果用于清算对象。
     * 结果：对象不匹配时失败关闭，不能复用其他对象的正向证据。
     */
    @Test
    void testCheckGateShouldResolveRequirementByExactStage() {
        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest().setStageRef(com.wind.funds.reconciliation.ReconciliationTestFixture.stage(
                        "SETTLEMENT_LOCK", "settlement-order-001")), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions().getFirst().getCurrentRunResultSn()).isEqualTo(settlementRunResultSn);
    }

    /**
     * 场景：账户日切等纯对账运行结果未绑定任何准入对象。
     * 结果：即使结果为 BALANCED，也不能被清算、结算或出款对象用于 Gate 放行。
     */
    @Test
    void testRunWithoutStageRequirementShouldNotAffectExistingStage() {
        executeStrictExact(null, null, "account-daily-recon-batch-001", null,
                "report:account-daily-recon-001", 1L);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions().getFirst().getCurrentRunResultSn()).isEqualTo(clearingRunResultSn);
    }

    /**
     * 场景：存在清算对象差错，但当前消费方是结算。
     * 输入：绑定清算对象的差错和结算准入检查请求。
     * 输出：结算准入不被不相关范围的差错误阻断。
     * 红线：阻断范围必须精确消费，不能把一个差错扩散成全链路阻断。
     */
    @Test
    void testCheckGateShouldPassWhenDifferenceScopeDoesNotMatch() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest());

        ReconciliationGateDecisionDTO result = checkGate(
                settlementGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getStageRef().getStageKind()).isEqualTo("SETTLEMENT_LOCK");
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
        assertPassedEvidence(result, "report:settlement-recon-run-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运行结果已对平，但对应 Gate 批次血缘头记录缺失。
     * 结果：准入失败关闭，不能退化为只凭调用方提交的 BALANCED 结果放行。
     */
    @Test
    void testCheckGateShouldBlockWhenCurrentLineageHeadIsMissing() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_batch_lineage WHERE current_batch_sn = ?",
                RERUN_BATCH_SN);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes())
                .contains(ReconciliationGateBlockerCode.REQUIRED_PAIR_RUN_NOT_FOUND);
    }

    /**
     * 场景：同一清算类型下存在两个不同清算候选的差错。
     * 输入：一个精确命中当前候选的对象级差错，以及一个同类型但不同候选的对象级差错。
     * 输出：当前清算准入只被精确命中的差错阻断，不被其他清算候选误阻断。
     * 红线：对象级 gate 不能退化成类型级全量阻断，也不能写入任何资金或账本事实。
     */
    @Test
    void testCheckGateShouldOnlyBlockExactClearingObjectWhenObjectScopedDifferenceExists() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createCurrentDifference(minimumCreateRequest());
        createCurrentDifference(
                minimumCreateRequest()
                        .setReconciliationMatchResultSn(recordDifferenceMatchResultSn(
                                "CLEARING_CONFIRM_ITEM", "clearing-candidate-other",
                                "recon-gate-other-difference-batch-001", "processor-gate-file-digest-002")));

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getPairDecisions())
                .allMatch(decision -> !decision.getBlockerCodes().isEmpty());
        ReconciliationGatePairDecisionDTO blockingDifference = result.getPairDecisions().getFirst();
        assertThat(blockingDifference.getRequiredPairRef().getScopeIdentity().getValue())
                .contains("clearing-candidate-001");
        assertThat(result.getEvidenceRefs()).contains("report:clearing-recon-run-001",
                "processor-gate-file-digest-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：结算单锁定前存在精确命中该结算单的对象级差错。
     * 输入：blockingObjectType=SETTLEMENT、blockingObjectSn=settlement-order-001 的未闭环差错。
     * 输出：结算准入被阻断，并回传阻断对象字段。
     * 红线：只做 gate 决策，不生成结算单、出款单、交易、route、posting 或账本事实。
     */
    @Test
    void testCheckGateShouldBlockExactSettlementObjectWhenObjectScopedDifferenceExists() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createCurrentDifference(minimumCreateRequest()
                .setReconciliationMatchResultSn(settlementMatchResultSn));

        ReconciliationGateDecisionDTO result = checkGate(
                settlementGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getStageRef().getStageKind()).isEqualTo("SETTLEMENT_LOCK");
        assertThat(result.getStageRef().getStageIdentity().getValue()).isEqualTo("settlement-order-001");
        ReconciliationGatePairDecisionDTO blockingDifference = result.getPairDecisions().getFirst();
        assertThat(blockingDifference.getBlockerCodes()).isNotEmpty();
        assertThat(blockingDifference.getRequiredPairRef().getScopeIdentity().getValue())
                .contains("settlement-order-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：结算差错与一个流水号相同的清算对象并存。
     * 输入：blockingObjectType=SETTLEMENT，blockingObjectSn=settlement-order-001。
     * 输出：结算准入被精确阻断，清算准入不会因为同流水号被误阻断。
     * 红线：对象类型和对象流水必须同时精确命中。
     */
    @Test
    void testCheckGateShouldMatchExactObjectTypeAndSn() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createCurrentDifference(minimumCreateRequest()
                .setReconciliationMatchResultSn(settlementMatchResultSn));

        ReconciliationGateDecisionDTO settlementResult = checkGate(
                settlementGateRequest(), WindOperatorFactory.system());
        String otherClearingRunResultSn = recordBalancedRunResult("CLEARING_CONFIRM_ITEM",
                "settlement-order-001", "recon-clearing-other-batch-001", "report:clearing-other-run-001");
        ReconciliationGateDecisionDTO clearingResult = checkGate(
                clearingGateRequest()
                        .setStageRef(com.wind.funds.reconciliation.ReconciliationTestFixture.stage(
                                "CLEARING_CONFIRM_ITEM", "settlement-order-001")),
                WindOperatorFactory.system());

        assertThat(settlementResult.isPassed()).isFalse();
        assertThat(settlementResult.getPairDecisions())
                .allMatch(decision -> !decision.getBlockerCodes().isEmpty());
        assertThat(clearingResult.isPassed()).isTrue();
        assertThat(clearingResult.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方未传入操作人。
     * 输入：合法清算准入请求和空操作人。
     * 输出：拒绝准入检查请求并给出明确错误。
     * 红线：不能在应用层产生模糊 NPE，也不能写入任何资金或账本事实。
     */
    @Test
    void testCheckGateShouldRejectMissingOperator() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> checkGate(clearingGateRequest(), null))
                .hasMessageContaining("对账准入检查操作人不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方未提供对账准入请求。
     * 结果：快速失败，不读取差错或运行结果，不形成任何资金事实。
     */
    @Test
    void testCheckGateShouldRejectNullRequest() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> checkGate(null, WindOperatorFactory.system()))
                .hasMessageContaining("对账准入检查请求不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错已经回链处理动作，但重新对账结果仍未对平。
     * 输入：ADJUST 动作、处理幂等键、原始事实引用和 balanced=false 的重跑结果。
     * 输出：清算准入继续阻断，并解释“处理动作不等于放行”。
     * 红线：存在 actionType 不得直接释放清算、结算或出款。
     */
    @Test
    void testCheckGateShouldKeepBlockedWhenAdjustmentLinkedButRerunUnbalanced() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", RECONCILIATION_BATCH_SN, "processor-gate-file-digest-001");
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordDifferenceRunResult("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-recon-unbalanced-001");
        String unbalancedRunResultSn = clearingRunResultSn;
        reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(), WindOperatorFactory.system());
        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        ReconciliationGatePairDecisionDTO blockingDifference = result.getPairDecisions().getFirst();
        assertThat(blockingDifference.getCurrentRunResultSn()).isEqualTo(unbalancedRunResultSn);
        assertThat(blockingDifference.getBlockerCodes()).isNotEmpty();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错已回链处理动作，并且重新对账已经对平。
     * 输入：RESOLVED 状态差错和清算准入检查请求。
     * 输出：准入普通通过，单独返回已闭环历史差错数量；证据只返回当前运行快照。
     * 红线：历史差错闭环不等于风险条件放行，也不代表已确认清算批次、锁定结算或提交出款。
     */
    @Test
    void testCheckGateShouldPassAndCountHistoryWhenScopedDifferenceResolvedByBalancedRerun() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        prepareAdjustedDifferenceWithBalancedRerun();
        reconciliationDifferenceApplicationService.recordRerunResult(minimumRerunRequest(), WindOperatorFactory.system());

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
        assertPassedEvidence(result, "report:clearing-recon-run-001");
        assertThat(result.getExplanation()).contains("current lineage 已对平");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错在批次 B 已对平关闭，后续批次 C 沿同一血缘继续形成 BALANCED 结果。
     * 结果：历史差错保持已关闭，当前正向结果可以通过 Gate。
     * 红线：新差错必须由新批次独立物化，不能让旧差错因当前头前移而永久重新阻断。
     */
    @Test
    void testCheckGateShouldKeepResolvedDifferenceClosedForLaterBalancedDescendant() {
        prepareAdjustedDifferenceWithBalancedRerun();
        reconciliationDifferenceApplicationService.recordRerunResult(minimumRerunRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordBalancedRunResult("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", "recon_gate_batch_001_rerun_002", RERUN_BATCH_SN,
                "report:clearing-recon-later-balanced-001");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
        assertPassedEvidence(result, "report:clearing-recon-later-balanced-001");
    }

    /**
     * 场景：同一差错回链处理动作后，有人尝试用相同处理单号替换幂等键或原始事实引用。
     * 输入：已回链的处理动作、漂移后的处理上下文和清算准入检查请求。
     * 输出：漂移请求被拒绝，准入仍按原差错状态阻断。
     * 红线：动作上下文漂移不能成为放行证据。
     */
    @Test
    void testAdjustmentContextDriftShouldNotAlterCurrentGateDecision() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        prepareAdjustedDifferenceWithBalancedRerun();

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setOriginalFactRef("external-balance-anomaly:changed"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理幂等请求原始事实引用不一致");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).hasSize(1);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes()).isEmpty();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private void prepareAdjustedDifferenceWithBalancedRerun() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", RECONCILIATION_BATCH_SN, "processor-gate-file-digest-001");
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordBalancedRunResult("CLEARING_CONFIRM_ITEM",
                "clearing-candidate-001", RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-recon-run-001");
    }

    private CheckReconciliationGateRequest clearingGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setStageRef(com.wind.funds.reconciliation.ReconciliationTestFixture.stage(
                        "CLEARING_CONFIRM_ITEM", "clearing-candidate-001"));
    }

    private CheckReconciliationGateRequest settlementGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setStageRef(com.wind.funds.reconciliation.ReconciliationTestFixture.stage(
                        "SETTLEMENT_LOCK", "settlement-order-001"));
    }

    private CreateReconciliationDifferenceRequest minimumCreateRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationMatchResultSn(clearingMatchResultSn)
                .setResponsiblePartyRef("processor:issuer-ledger")
                .setDescription("外部 processor 文件金额与内部账本金额不一致");
    }

    private void createDifferenceBeforeCurrentHeadAdvance(CreateReconciliationDifferenceRequest request) {
        com.wind.funds.reconciliation.ReconciliationTestFixture.withMatchBatchAsCurrentHead(
                jdbcTemplate, TENANT_ID, request.getReconciliationMatchResultSn(),
                () -> reconciliationDifferenceApplicationService.createDifference(
                        request, WindOperatorFactory.system()));
    }

    private void createCurrentDifference(CreateReconciliationDifferenceRequest request) {
        createDifferenceBeforeCurrentHeadAdvance(request);
        com.wind.funds.reconciliation.ReconciliationTestFixture.setMatchBatchAsCurrentHead(
                jdbcTemplate, TENANT_ID, request.getReconciliationMatchResultSn());
    }

    private void assertPassedEvidence(ReconciliationGateDecisionDTO result, String evidenceRef) {
        assertThat(result.getEvidenceRefs()).contains(evidenceRef);
        assertThat(result.getEvidenceRefs()).anyMatch(evidence -> evidence.startsWith("RGE"));
        assertThat(result.getEvidenceRefs()).anyMatch(evidence -> evidence.startsWith("run:"));
    }

    private String runResultSnForMatch(String matchResultSn) {
        return jdbcTemplate.queryForObject("""
                SELECT reconciliation_run_result_sn FROM t_reconciliation_match_result
                WHERE tenant_id = ? AND sn = ?
                """, String.class, TENANT_ID, matchResultSn);
    }

    private LinkReconciliationDifferenceAdjustmentRequest minimumAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn(clearingMatchResultSn))
                .setActionType(ReconciliationDifferenceActionType.ADJUST)
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setIdempotencyKey("idem-recon-gate-adjust-001")
                .setOriginalFactRef("external-balance-anomaly:issuer-ledger-gate-001")
                .setAdjustmentTransactionSn("funds_tx_adjust_gate_001")
                .setApprovalRef("approval-recon-gate-adjust-001")
                .setEvidenceRef("adjustment-evidence-gate-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private RecordReconciliationDifferenceRerunRequest minimumRerunRequest() {
        return new RecordReconciliationDifferenceRerunRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn(clearingMatchResultSn))
                .setReconciliationRunResultSn(clearingRunResultSn);
    }

    private String recordDifferenceMatchResultSn(String stageKind,
                                                  String gateObjectSn,
                                                  String reconciliationBatchSn,
                                                  String evidenceRef) {
        recordDifferenceRunResult(stageKind, gateObjectSn, reconciliationBatchSn, evidenceRef);
        return jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_match_result
                WHERE tenant_id = ?
                  AND reconciliation_batch_sn = ?
                  AND result_kind <> 'MATCHED'
                """, String.class, TENANT_ID, reconciliationBatchSn);
    }

    private String requiredDifferenceSn(String matchResultSn) {
        return jdbcTemplate.queryForObject("""
                SELECT difference_sn
                FROM t_reconciliation_difference
                WHERE tenant_id = ?
                  AND reconciliation_match_result_sn = ?
                """, String.class, TENANT_ID, matchResultSn);
    }

    private String recordBalancedRunResult(String stageKind,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String evidenceRef) {
        return recordBalancedRunResult(stageKind, gateObjectSn, reconciliationBatchSn, null, evidenceRef);
    }

    private String recordBalancedRunResult(String stageKind,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String previousBatchSn,
                                           String evidenceRef) {
        return executeStrictExact(stageKind, gateObjectSn, reconciliationBatchSn, previousBatchSn, evidenceRef, 1L);
    }

    private String recordDifferenceRunResult(String stageKind,
                                             String gateObjectSn,
                                             String reconciliationBatchSn,
                                             String evidenceRef) {
        return recordDifferenceRunResult(stageKind, gateObjectSn, reconciliationBatchSn, null, evidenceRef);
    }

    private String recordDifferenceRunResult(String stageKind,
                                             String gateObjectSn,
                                             String reconciliationBatchSn,
                                             String previousBatchSn,
                                             String evidenceRef) {
        return executeStrictExact(stageKind, gateObjectSn, reconciliationBatchSn, previousBatchSn, evidenceRef, 2L);
    }

    private String executeStrictExact(String stageKind,
                                   String gateObjectSn,
                                   String reconciliationBatchSn,
                                   String previousBatchSn,
                                   String evidenceRef,
                                   long comparisonAmount) {
        String referenceSourceRef = "internal:" + reconciliationBatchSn;
        String comparisonSourceRef = "external:" + reconciliationBatchSn;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, reconciliationBatchSn, stageKind, gateObjectSn,
                "recon-rule-v1", evidenceRef, referenceSourceRef,
                comparisonSourceRef, previousBatchSn, comparisonAmount, "CONFIRMED");
        return reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn), WindOperatorFactory.system()).getSn();
    }

    @Configuration
    @Import({
            ReconciliationBatchApplicationServiceImpl.class,
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class
    })
    static class Config {
    }

}
