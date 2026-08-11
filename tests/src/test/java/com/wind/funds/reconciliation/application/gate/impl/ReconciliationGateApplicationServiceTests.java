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
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateBlockingDifferenceDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
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
        clearingMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", RECONCILIATION_BATCH_SN, "processor-gate-file-digest-001");
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-recon-run-001");
        settlementMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.SETTLEMENT,
                "settlement-order-001", "recon-settlement-difference-batch-001",
                "processor-settlement-file-digest-001");
        settlementRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.SETTLEMENT,
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
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest());

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getGateObjectSn()).isEqualTo("clearing-candidate-001");
        assertThat(result.getBlockingDifferences())
                .extracting(ReconciliationGateBlockingDifferenceDTO::getDifferenceSn)
                .containsExactly(requiredDifferenceSn(clearingMatchResultSn));
        ReconciliationGateBlockingDifferenceDTO blockingDifference = result.getBlockingDifferences().getFirst();
        assertThat(blockingDifference.getState()).isEqualTo(ReconciliationDifferenceState.BLOCKED);
        assertThat(blockingDifference.getResponsiblePartyRef()).isEqualTo("processor:issuer-ledger");
        assertThat(blockingDifference.getEvidenceRef()).isEqualTo("processor-gate-file-digest-001#line-1");
        assertThat(blockingDifference.getBlockingReason()).contains("未闭环");
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-recon-run-001",
                "processor-gate-file-digest-001#line-1");
        assertThat(result.getExplanation()).contains("阻断");
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

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getReconciliationRunResultSn()).isEqualTo(clearingRunResultSn);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-recon-run-001");
        assertThat(result.getExplanation()).contains("准入通过");
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
        assertThat(result.getExplanation()).contains("不是当前批次血缘头");
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
                SET status = 'INVALIDATED'
                WHERE tenant_id = ? AND difference_sn = ?
                """, TENANT_ID, requiredDifferenceSn(clearingMatchResultSn));

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getResolvedDifferenceCount()).isZero();
    }

    @Test
    void testCheckGateShouldFailClosedWhenDifferenceCountExceedsCapacity() {
        List<Object[]> arguments = IntStream.rangeClosed(
                        1, ReconciliationGateApplicationServiceImpl.MAX_GATE_DIFFERENCE_COUNT + 1)
                .mapToObj(index -> new Object[]{
                        "capacity-difference-" + index,
                        TENANT_ID,
                        RECONCILIATION_BATCH_SN,
                        "capacity-match-" + index,
                        "capacity-evidence-" + index
                })
                .toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO t_reconciliation_difference (
                    difference_sn, tenant_id, reconciliation_batch_sn,
                    reconciliation_match_result_sn, source_quality, match_strength,
                    difference_type, severity, status, responsible_party_ref,
                    blocking_object_type, blocking_object_sn, rule_version, evidence_ref
                ) VALUES (?, ?, ?, ?, 'VERIFIED', 'UNMATCHED', 'STATUS_MISMATCH',
                    'S1_MAJOR', 'BLOCKED', 'processor:capacity', 'CLEARING',
                    'clearing-candidate-001', 'recon-rule-v1', ?)
                """, arguments);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getExplanation()).contains("差错数量超过单次检查容量");
    }

    @Test
    void testCheckGateShouldIgnoreResolvedHistoryWhenCheckingCapacity() {
        List<Object[]> arguments = IntStream.rangeClosed(
                        1, ReconciliationGateApplicationServiceImpl.MAX_GATE_DIFFERENCE_COUNT + 1)
                .mapToObj(index -> new Object[]{
                        "resolved-capacity-difference-" + index,
                        TENANT_ID,
                        RECONCILIATION_BATCH_SN,
                        "resolved-capacity-match-" + index,
                        "resolved-capacity-evidence-" + index,
                        RERUN_BATCH_SN
                })
                .toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO t_reconciliation_difference (
                    difference_sn, tenant_id, reconciliation_batch_sn,
                    reconciliation_match_result_sn, source_quality, match_strength,
                    difference_type, severity, status, responsible_party_ref,
                    blocking_object_type, blocking_object_sn, rule_version, evidence_ref,
                    last_rerun_batch_sn, last_rerun_balanced
                ) VALUES (?, ?, ?, ?, 'VERIFIED', 'UNMATCHED', 'STATUS_MISMATCH',
                    'S1_MAJOR', 'RESOLVED', 'processor:capacity', 'CLEARING',
                    'clearing-candidate-001', 'recon-rule-v1', ?, ?, TRUE)
                """, arguments);

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getResolvedDifferenceCount())
                .isEqualTo(ReconciliationGateApplicationServiceImpl.MAX_GATE_DIFFERENCE_COUNT + 1);
        assertThat(result.getBlockingDifferences()).isEmpty();
    }

    @Test
    void testCheckGateShouldRejectCyclicLineage() {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET previous_batch_sn = ?
                WHERE tenant_id = ? AND sn = ?
                """, RERUN_BATCH_SN, TENANT_ID, RERUN_BATCH_SN);

        assertThatThrownBy(() -> checkGate(
                clearingGateRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("血缘存在循环");
    }

    @Test
    void testCheckGateShouldRejectBrokenLineage() {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET previous_batch_sn = 'missing-lineage-batch'
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, RERUN_BATCH_SN);

        assertThatThrownBy(() -> checkGate(
                clearingGateRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("血缘断裂");
    }

    @Test
    void testCheckGateShouldRejectLineageExceedingMaximumDepth() {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET previous_batch_sn = 'lineage-depth-1'
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, RERUN_BATCH_SN);
        List<Object[]> arguments = IntStream.rangeClosed(
                        1, ReconciliationGateApplicationServiceImpl.MAX_GATE_LINEAGE_DEPTH)
                .mapToObj(index -> new Object[]{
                        "lineage-depth-" + index,
                        TENANT_ID,
                        index == ReconciliationGateApplicationServiceImpl.MAX_GATE_LINEAGE_DEPTH
                                ? RECONCILIATION_BATCH_SN
                                : "lineage-depth-" + (index + 1),
                        "%064d".formatted(index)
                })
                .toList();
        jdbcTemplate.batchUpdate("""
                INSERT INTO t_reconciliation_batch (
                    sn, tenant_id, reconciliation_scope_ref, gate_object_type, gate_object_sn,
                    rule_version, window_start, window_end, timezone_id, previous_batch_sn,
                    status, batch_digest, created_by
                ) VALUES (?, ?, 'clearing:clearing-candidate-001', 'CLEARING',
                    'clearing-candidate-001', 'recon-rule-v1', '2026-07-21 00:00:00',
                    '2026-07-22 00:00:00', 'Asia/Shanghai', ?, 'COMPLETED', ?, 'SYSTEM')
                """, arguments);

        assertThatThrownBy(() -> checkGate(
                clearingGateRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("血缘深度不能超过")
                .hasMessageContaining(String.valueOf(
                        ReconciliationGateApplicationServiceImpl.MAX_GATE_LINEAGE_DEPTH));
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
        assertThat(result.getReconciliationRunResultSn()).isEqualTo(clearingRunResultSn);
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
                .setReconciliationScopeRef("clearing:clearing-candidate-001")
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn("clearing-candidate-001")
                .setRuleVersion("recon-rule-v2")
                .setWindowStart(LocalDateTime.of(2026, 7, 21, 0, 0))
                .setWindowEnd(LocalDateTime.of(2026, 7, 22, 0, 0))
                .setTimezoneId("Asia/Shanghai")
                .setPreviousBatchSn(RERUN_BATCH_SN), WindOperatorFactory.system());

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getExplanation()).contains("不是当前批次血缘头");
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
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).isEmpty();
        assertThat(result.getExplanation()).contains("运行结果不存在");
    }

    /**
     * 场景：完成态逐笔匹配结果包含差异。
     * 结果：即使尚未登记差错单也必须阻断，直到新批次形成 BALANCED 结果。
     */
    @Test
    void testCheckGateShouldBlockWhenRunResultIsNotBalanced() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result WHERE sn = ?", clearingRunResultSn);
        clearingRunResultSn = recordDifferenceRunResult(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", "recon-difference-batch-001",
                "report:clearing-recon-difference-001");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-recon-difference-001");
        assertThat(result.getExplanation()).contains("DIFFERENCE_FOUND");
    }

    /**
     * 场景：调用方把结算对象的正向结果用于清算对象。
     * 结果：对象不匹配时失败关闭，不能复用其他对象的正向证据。
     */
    @Test
    void testCheckGateShouldBlockWhenRunResultObjectDoesNotMatch() {
        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest().setReconciliationRunResultSn(settlementRunResultSn), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getExplanation()).contains("准入对象不匹配");
    }

    /**
     * 场景：账户日切等纯对账运行结果未绑定任何准入对象。
     * 结果：即使结果为 BALANCED，也不能被清算、结算或出款对象用于 Gate 放行。
     */
    @Test
    void testCheckGateShouldBlockRunResultWithoutGateObject() {
        String runResultSn = recordRunResult(null, null, "account-daily-recon-batch-001", null,
                "report:account-daily-recon-001",
                new ReconciliationMatchResultItem()
                        .setReferenceSourceRef("bank-statement:account-001")
                        .setComparisonSourceRef("ledger:account-001")
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef("report:account-daily-recon-001#line-1"));

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest().setReconciliationRunResultSn(runResultSn), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getExplanation()).contains("未绑定准入对象");
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
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.SETTLEMENT);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).containsExactly("report:settlement-recon-run-001");
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
        assertThat(result.getExplanation()).contains("不是当前批次血缘头");
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
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest());
        createDifferenceBeforeCurrentHeadAdvance(
                minimumCreateRequest()
                        .setReconciliationMatchResultSn(recordDifferenceMatchResultSn(
                                ReconciliationGateObjectType.CLEARING, "clearing-candidate-other",
                                "recon-gate-other-difference-batch-001", "processor-gate-file-digest-002")));

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getBlockingDifferences())
                .extracting(ReconciliationGateBlockingDifferenceDTO::getDifferenceSn)
                .containsExactly(requiredDifferenceSn(clearingMatchResultSn));
        ReconciliationGateBlockingDifferenceDTO blockingDifference = result.getBlockingDifferences().getFirst();
        assertThat(blockingDifference.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(blockingDifference.getBlockingObjectSn()).isEqualTo("clearing-candidate-001");
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-recon-run-001",
                "processor-gate-file-digest-001#line-1");
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
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest()
                .setReconciliationMatchResultSn(settlementMatchResultSn));

        ReconciliationGateDecisionDTO result = checkGate(
                settlementGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.SETTLEMENT);
        assertThat(result.getGateObjectSn()).isEqualTo("settlement-order-001");
        ReconciliationGateBlockingDifferenceDTO blockingDifference = result.getBlockingDifferences().getFirst();
        assertThat(blockingDifference.getDifferenceSn()).isEqualTo(requiredDifferenceSn(settlementMatchResultSn));
        assertThat(blockingDifference.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.SETTLEMENT);
        assertThat(blockingDifference.getBlockingObjectSn()).isEqualTo("settlement-order-001");
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
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest()
                .setReconciliationMatchResultSn(settlementMatchResultSn));

        ReconciliationGateDecisionDTO settlementResult = checkGate(
                settlementGateRequest(), WindOperatorFactory.system());
        String otherClearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                "settlement-order-001", "recon-clearing-other-batch-001", "report:clearing-other-run-001");
        ReconciliationGateDecisionDTO clearingResult = checkGate(
                clearingGateRequest()
                        .setGateObjectSn("settlement-order-001")
                        .setReconciliationRunResultSn(otherClearingRunResultSn),
                WindOperatorFactory.system());

        assertThat(settlementResult.isPassed()).isFalse();
        assertThat(settlementResult.getBlockingDifferences())
                .extracting(ReconciliationGateBlockingDifferenceDTO::getDifferenceSn)
                .containsExactly(requiredDifferenceSn(settlementMatchResultSn));
        assertThat(clearingResult.isPassed()).isTrue();
        assertThat(clearingResult.getBlockingDifferences()).isEmpty();
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
                .hasMessageContaining("对账差错准入检查操作人不能为空");

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
        clearingMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", RECONCILIATION_BATCH_SN, "processor-gate-file-digest-001");
        createDifferenceBeforeCurrentHeadAdvance(minimumCreateRequest());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordDifferenceRunResult(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-recon-unbalanced-001");
        String unbalancedRunResultSn = clearingRunResultSn;
        reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", "recon_gate_batch_001_rerun_002", RERUN_BATCH_SN,
                "report:clearing-recon-after-unbalanced-001");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        ReconciliationGateBlockingDifferenceDTO blockingDifference = result.getBlockingDifferences().getFirst();
        assertThat(blockingDifference.getState()).isEqualTo(ReconciliationDifferenceState.RECONCILING);
        assertThat(blockingDifference.getActionType()).isEqualTo(ReconciliationDifferenceActionType.ADJUST);
        assertThat(blockingDifference.getAdjustmentSn()).isEqualTo(ADJUSTMENT_SN);
        assertThat(blockingDifference.getLastRerunSn()).isEqualTo(unbalancedRunResultSn);
        assertThat(blockingDifference.getLastRerunBalanced()).isFalse();
        assertThat(blockingDifference.getBlockingReason()).contains("重新对账未对平");
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
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getResolvedDifferenceCount()).isOne();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-recon-run-001");
        assertThat(result.getExplanation()).contains("重新对账对平").contains("准入通过");
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
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", "recon_gate_batch_001_rerun_002", RERUN_BATCH_SN,
                "report:clearing-recon-later-balanced-001");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getResolvedDifferenceCount()).isOne();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-recon-later-balanced-001");
    }

    /**
     * 场景：同一差错回链处理动作后，有人尝试用相同处理单号替换幂等键或原始事实引用。
     * 输入：已回链的处理动作、漂移后的处理上下文和清算准入检查请求。
     * 输出：漂移请求被拒绝，准入仍按原差错状态阻断。
     * 红线：动作上下文漂移不能成为放行证据。
     */
    @Test
    void testCheckGateShouldNotReleaseWhenAdjustmentContextDriftIsRejected() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        prepareAdjustedDifferenceWithBalancedRerun();

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setOriginalFactRef("external-balance-anomaly:changed"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理幂等请求原始事实引用不一致");

        ReconciliationGateDecisionDTO result = checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getBlockingDifferences()).hasSize(1);
        assertThat(result.getBlockingDifferences().getFirst().getOriginalFactRef())
                .isEqualTo("external-balance-anomaly:issuer-ledger-gate-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private void prepareAdjustedDifferenceWithBalancedRerun() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", RECONCILIATION_BATCH_SN, "processor-gate-file-digest-001");
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                "clearing-candidate-001", RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-recon-run-001");
    }

    private CheckReconciliationGateRequest clearingGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn("clearing-candidate-001")
                .setReconciliationRunResultSn(clearingRunResultSn);
    }

    private CheckReconciliationGateRequest settlementGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.SETTLEMENT)
                .setGateObjectSn("settlement-order-001")
                .setReconciliationRunResultSn(settlementRunResultSn);
    }

    private CreateReconciliationDifferenceRequest minimumCreateRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationMatchResultSn(clearingMatchResultSn)
                .setResponsiblePartyRef("processor:issuer-ledger")
                .setDescription("外部 processor 文件金额与内部账本金额不一致");
    }

    private void createDifferenceBeforeCurrentHeadAdvance(CreateReconciliationDifferenceRequest request) {
        DifferenceBatchIdentity identity = jdbcTemplate.queryForObject("""
                SELECT m.reconciliation_batch_sn, b.gate_object_type, b.gate_object_sn
                FROM t_reconciliation_match_result m
                JOIN t_reconciliation_batch b
                  ON b.tenant_id = m.tenant_id
                 AND b.sn = m.reconciliation_batch_sn
                WHERE m.tenant_id = ?
                  AND m.sn = ?
                """, (resultSet, rowNum) -> new DifferenceBatchIdentity(
                resultSet.getString("reconciliation_batch_sn"),
                resultSet.getString("gate_object_type"),
                resultSet.getString("gate_object_sn")), TENANT_ID, request.getReconciliationMatchResultSn());
        String currentBatchSn = jdbcTemplate.queryForObject("""
                SELECT current_batch_sn
                FROM t_reconciliation_batch_lineage
                WHERE tenant_id = ?
                  AND gate_object_type = ?
                  AND gate_object_sn = ?
                """, String.class, TENANT_ID, identity.gateObjectType(), identity.gateObjectSn());
        try {
            jdbcTemplate.update("""
                    UPDATE t_reconciliation_batch_lineage
                    SET current_batch_sn = ?
                    WHERE tenant_id = ?
                      AND gate_object_type = ?
                      AND gate_object_sn = ?
                    """, identity.batchSn(), TENANT_ID, identity.gateObjectType(), identity.gateObjectSn());
            reconciliationDifferenceApplicationService.createDifference(request, WindOperatorFactory.system());
        } finally {
            jdbcTemplate.update("""
                    UPDATE t_reconciliation_batch_lineage
                    SET current_batch_sn = ?
                    WHERE tenant_id = ?
                      AND gate_object_type = ?
                      AND gate_object_sn = ?
                    """, currentBatchSn, TENANT_ID, identity.gateObjectType(), identity.gateObjectSn());
        }
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

    private String recordDifferenceMatchResultSn(ReconciliationGateObjectType gateObjectType,
                                                  String gateObjectSn,
                                                  String reconciliationBatchSn,
                                                  String evidenceRef) {
        recordDifferenceRunResult(gateObjectType, gateObjectSn, reconciliationBatchSn, evidenceRef);
        return jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_match_result
                WHERE tenant_id = ?
                  AND reconciliation_batch_sn = ?
                  AND difference_type IS NOT NULL
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

    private String recordBalancedRunResult(ReconciliationGateObjectType gateObjectType,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String evidenceRef) {
        return recordBalancedRunResult(gateObjectType, gateObjectSn, reconciliationBatchSn, null, evidenceRef);
    }

    private String recordBalancedRunResult(ReconciliationGateObjectType gateObjectType,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String previousBatchSn,
                                           String evidenceRef) {
        return recordRunResult(gateObjectType, gateObjectSn, reconciliationBatchSn, previousBatchSn, evidenceRef,
                new ReconciliationMatchResultItem()
                        .setReferenceSourceRef("internal:" + gateObjectSn)
                        .setComparisonSourceRef("external:" + gateObjectSn)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef(evidenceRef + "#line-1"));
    }

    private String recordDifferenceRunResult(ReconciliationGateObjectType gateObjectType,
                                             String gateObjectSn,
                                             String reconciliationBatchSn,
                                             String evidenceRef) {
        return recordDifferenceRunResult(gateObjectType, gateObjectSn, reconciliationBatchSn, null, evidenceRef);
    }

    private String recordDifferenceRunResult(ReconciliationGateObjectType gateObjectType,
                                             String gateObjectSn,
                                             String reconciliationBatchSn,
                                             String previousBatchSn,
                                             String evidenceRef) {
        return recordRunResult(gateObjectType, gateObjectSn, reconciliationBatchSn, previousBatchSn, evidenceRef,
                new ReconciliationMatchResultItem()
                        .setReferenceSourceRef("internal:" + gateObjectSn)
                        .setComparisonSourceRef("external:" + gateObjectSn)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.UNMATCHED)
                        .setDifferenceType(ReconciliationDifferenceType.STATUS_MISMATCH)
                        .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                        .setDifferenceAmount(0L)
                        .setEvidenceRef(evidenceRef + "#line-1"));
    }

    private String recordRunResult(ReconciliationGateObjectType gateObjectType,
                                   String gateObjectSn,
                                   String reconciliationBatchSn,
                                   String previousBatchSn,
                                   String evidenceRef,
                                   ReconciliationMatchResultItem matchResult) {
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, reconciliationBatchSn, gateObjectType, gateObjectSn,
                "recon-rule-v1", evidenceRef, matchResult.getReferenceSourceRef(),
                matchResult.getComparisonSourceRef(), previousBatchSn);
        return reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn)
                .setMatchResults(List.of(matchResult)), WindOperatorFactory.system()).getSn();
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

    private record DifferenceBatchIdentity(String batchSn, String gateObjectType, String gateObjectSn) {
    }
}
