package com.wind.funds.reconciliation.application.difference.report.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.difference.report.ReconciliationDifferenceReportApplicationService;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceReportCompleteness;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceState;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceReportDTO;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.GetReconciliationDifferenceReportRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 对账差异报告应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationDifferenceReportApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationDifferenceReportApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String RECONCILIATION_BATCH_SN = "recon_report_batch_001";

    private static final String RERUN_BATCH_SN = "recon_report_batch_rerun_001";

    private static final String ACTION_RERUN_BATCH_SN = "recon_report_action_rerun_001";

    private static final String ACTION_LATEST_BATCH_SN = "recon_report_action_rerun_002";

    private static final String SOURCE_RECORD_SN = "processor_report_line_001";

    private static final String CLEARING_OBJECT_SN = "clearing-candidate-report-001";

    private static final String RUN_RESULT_EVIDENCE_REF = "report:clearing-recon-report-001";

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationDifferenceReportApplicationService reconciliationDifferenceReportApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String reconciliationMatchResultSn;

    @BeforeEach
    void cleanReconciliationDifference() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        reconciliationMatchResultSn = recordInitialDifferenceRunResult();
    }

    @Test
    void testGetReportShouldRejectTenantDifferentFromCurrentContext() {
        GetReconciliationDifferenceReportRequest request = new GetReconciliationDifferenceReportRequest()
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> reconciliationDifferenceReportApplicationService.getReport(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
    }

    /**
     * 场景：运营查询一个仍阻断清算候选的对象级对账差错报告。
     * 输入：blockingObjectType=CLEARING、blockingObjectSn=clearing-candidate-report-001 的 BLOCKED 差错。
     * 输出：报告解释差错基础信息、阻断对象、gate 阻断状态和证据引用。
     * 红线：报告查询只读聚合差错事实，不生成交易、route、posting、LedgerEntry 或余额投影。
     */
    @Test
    void testGetReportShouldExplainObjectLevelBlockingDifferenceWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperatorFactory.system());

        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getDifferenceSn()).isEqualTo(requiredDifferenceSn());
        assertThat(result.getState()).isEqualTo(ReconciliationDifferenceState.BLOCKED);
        assertThat(result.getDifferenceType()).isEqualTo(ReconciliationDifferenceType.AMOUNT_MISMATCH);
        assertThat(result.getSeverity()).isEqualTo(ReconciliationDifferenceSeverity.S1_MAJOR);
        assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(result.getDifferenceAmount()).isEqualTo(50L);
        assertThat(result.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getBlockingObjectSn()).isEqualTo(CLEARING_OBJECT_SN);
        assertThat(result.getGateDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getEvidenceRefs())
                .containsExactly("processor-report-file-digest-001", RUN_RESULT_EVIDENCE_REF);
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.COMPLETE);
        assertThat(result.getExplanation()).contains("阻断");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错依赖的完成态批次证据已被替代。
     * 结果：报告明确解释为证据失效，不误报为已对平或仍需继续处置。
     */
    @Test
    void testGetReportShouldExplainInvalidatedEvidence() {
        reconciliationDifferenceApplicationService.createDifference(
                clearingDifferenceRequest(), WindOperatorFactory.system());
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'INVALIDATED'
                WHERE tenant_id = ? AND difference_sn = ?
                """, TENANT_ID, requiredDifferenceSn());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest().setIncludeGateDecision(false), WindOperatorFactory.system());

        assertThat(result.getState()).isEqualTo(ReconciliationDifferenceState.INVALIDATED);
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.COMPLETE);
        assertThat(result.getExplanation()).contains("证据").contains("无效").contains("不再参与准入");
    }

    /**
     * 场景：运营只需要差错基础解释，不需要 gate 摘要和证据引用列表。
     * 输入：includeGateDecision=false、includeEvidenceRefs=false。
     * 输出：报告仍返回差错事实和阻断对象，但不返回 gate 状态和证据列表。
     * 红线：报告开关只影响只读展示，不改变差错、交易或账本事实。
     */
    @Test
    void testGetReportShouldRespectGateAndEvidenceViewSwitches() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest()
                        .setIncludeGateDecision(false)
                        .setReconciliationRunResultSn(null)
                        .setIncludeEvidenceRefs(false),
                WindOperatorFactory.system());

        assertThat(result.getDifferenceSn()).isEqualTo(requiredDifferenceSn());
        assertThat(result.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getBlockingObjectSn()).isEqualTo(CLEARING_OBJECT_SN);
        assertThat(result.getGateDecisionResult()).isNull();
        assertThat(result.getGateExplanation()).isNull();
        assertThat(result.getEvidenceRefs()).isEmpty();
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.COMPLETE);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营要求差错报告包含对象级 gate 决策，但未指定本轮对账运行结果。
     * 输入：includeGateDecision=true、reconciliationRunResultSn 为空。
     * 输出：快速失败，禁止报告服务隐式选择最新运行结果或仅凭差错缺失推断准入。
     */
    @Test
    void testGetReportShouldRequireExplicitRunResultWhenIncludingObjectGateDecision() {
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceReportApplicationService.getReport(
                reportRequest().setReconciliationRunResultSn(null), WindOperatorFactory.system()))
                .hasMessageContaining("对账运行结果流水号不能为空");
    }

    /**
     * 场景：历史差错已有处理动作号，但动作证据链不完整。
     * 输入：差错记录存在 adjustment_sn，但缺少 actionType、幂等键、原始事实或处理证据。
     * 输出：报告返回 INCOMPLETE_ACTION_EVIDENCE，提示报告不可作为完整闭环证据。
     * 红线：报告只识别证据缺口，不补事实、不调账、不修复历史记录。
     */
    @Test
    void testGetReportShouldExposeIncompleteActionEvidenceWithoutRepairingDifference() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET adjustment_sn = ?
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, "legacy-adjustment-with-missing-evidence", TENANT_ID, requiredDifferenceSn());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperatorFactory.system());

        assertThat(result.getAdjustmentSn()).isEqualTo("legacy-adjustment-with-missing-evidence");
        assertThat(result.getActionType()).isNull();
        assertThat(result.getAdjustmentIdempotencyKey()).isNull();
        assertThat(result.getOriginalFactRef()).isNull();
        assertThat(result.getAdjustmentEvidenceRef()).isNull();
        assertThat(result.getCompleteness())
                .isEqualTo(ReconciliationDifferenceReportCompleteness.INCOMPLETE_ACTION_EVIDENCE);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史差错已被标记为已解决，但缺少最后一次重跑结果。
     * 输入：差错记录 status=RESOLVED，last_rerun_sn 为空。
     * 输出：报告返回 MISSING_RERUN_RESULT，提示闭环证据不足。
     * 红线：报告只暴露重跑证据缺口，不补事实、不重跑、不修复历史记录。
     */
    @Test
    void testGetReportShouldExposeMissingRerunResultWithoutRepairingDifference() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = ?
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, ReconciliationDifferenceState.RESOLVED.name(), TENANT_ID, requiredDifferenceSn());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperatorFactory.system());

        assertThat(result.getState()).isEqualTo(ReconciliationDifferenceState.RESOLVED);
        assertThat(result.getLastRerunSn()).isNull();
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.MISSING_RERUN_RESULT);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：第一次处理后重跑仍有差异，上层完成第二次处理，再查询单笔差错报告。
     * 输出：报告按发生顺序返回两次业务动作事实，主表最新动作快照仍指向第二次处理。
     * 红线：动作历史来自 append-only 事实表，不以 Web 审计替代，也不因主表快照更新而丢失。
     */
    @Test
    void testGetReportShouldReturnCompleteAppendOnlyActionHistory() {
        reconciliationDifferenceApplicationService.createDifference(
                clearingDifferenceRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                adjustmentRequest("report-adjustment-001", "report-adjustment-idem-001",
                        ReconciliationDifferenceActionType.ADJUST),
                WindOperatorFactory.system());
        String firstRerunSn = recordRunResult(false, ACTION_RERUN_BATCH_SN,
                RECONCILIATION_BATCH_SN, "report:action-rerun-001#line-1");
        reconciliationDifferenceApplicationService.recordRerunResult(
                new RecordReconciliationDifferenceRerunRequest()
                        .setTenantId(TENANT_ID)
                        .setDifferenceSn(requiredDifferenceSn())
                        .setReconciliationRunResultSn(firstRerunSn),
                WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                adjustmentRequest("report-adjustment-002", "report-adjustment-idem-002",
                        ReconciliationDifferenceActionType.SUPPLEMENT_FACT),
                WindOperatorFactory.system());
        String currentRunSn = recordRunResult(true, ACTION_LATEST_BATCH_SN,
                ACTION_RERUN_BATCH_SN, "report:action-rerun-002#line-1");

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                new GetReconciliationDifferenceReportRequest()
                        .setTenantId(TENANT_ID)
                        .setDifferenceSn(requiredDifferenceSn())
                        .setIncludeGateDecision(true)
                        .setReconciliationRunResultSn(currentRunSn)
                        .setIncludeEvidenceRefs(true),
                WindOperatorFactory.system());

        assertThat(result.getAdjustmentSn()).isEqualTo("report-adjustment-002");
        assertThat(result.getActionHistory())
                .extracting("adjustmentSn", "actionType")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "report-adjustment-001", ReconciliationDifferenceActionType.ADJUST),
                        org.assertj.core.groups.Tuple.tuple(
                                "report-adjustment-002", ReconciliationDifferenceActionType.SUPPLEMENT_FACT));
    }

    private GetReconciliationDifferenceReportRequest reportRequest() {
        String runResultSn = reconciliationRunResultApplicationService.recordRunResult(
                balancedRunResultRequest(), WindOperatorFactory.system()).getSn();
        return new GetReconciliationDifferenceReportRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn())
                .setIncludeGateDecision(true)
                .setReconciliationRunResultSn(runResultSn)
                .setIncludeEvidenceRefs(true);
    }

    private RecordReconciliationRunResultRequest balancedRunResultRequest() {
        String referenceSourceRef = "internal:" + CLEARING_OBJECT_SN;
        String comparisonSourceRef = "external:" + CLEARING_OBJECT_SN;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, RERUN_BATCH_SN, ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, "recon-rule-v1", RUN_RESULT_EVIDENCE_REF,
                referenceSourceRef, comparisonSourceRef, RECONCILIATION_BATCH_SN);
        return new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(RERUN_BATCH_SN)
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setReferenceSourceRef(referenceSourceRef)
                        .setComparisonSourceRef(comparisonSourceRef)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef(RUN_RESULT_EVIDENCE_REF + "#line-1")));
    }

    private String recordRunResult(boolean balanced,
                                   String batchSn,
                                   String previousBatchSn,
                                   String evidenceRef) {
        String referenceSourceRef = SOURCE_RECORD_SN;
        String comparisonSourceRef = "external-difference:" + CLEARING_OBJECT_SN;
        ReconciliationMatchResultItem matchResult = new ReconciliationMatchResultItem()
                .setReferenceSourceRef(referenceSourceRef)
                .setComparisonSourceRef(comparisonSourceRef)
                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                .setMatchStrength(balanced ? ReconciliationMatchStrength.EXACT_MATCH
                        : ReconciliationMatchStrength.UNMATCHED)
                .setEvidenceRef(evidenceRef);
        if (!balanced) {
            matchResult.setDifferenceType(ReconciliationDifferenceType.STATUS_MISMATCH)
                    .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR);
        }
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, batchSn, ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, "recon-rule-v1", evidenceRef,
                referenceSourceRef, comparisonSourceRef, previousBatchSn);
        return reconciliationRunResultApplicationService.recordRunResult(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batchSn)
                        .setMatchResults(List.of(matchResult)),
                WindOperatorFactory.system()).getSn();
    }

    private LinkReconciliationDifferenceAdjustmentRequest adjustmentRequest(
            String adjustmentSn,
            String idempotencyKey,
            ReconciliationDifferenceActionType actionType) {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn())
                .setActionType(actionType)
                .setAdjustmentSn(adjustmentSn)
                .setIdempotencyKey(idempotencyKey)
                .setOriginalFactRef("processor-report-file-digest-001")
                .setAdjustmentTransactionSn("funds-transaction:" + adjustmentSn)
                .setApprovalRef("approval:" + adjustmentSn)
                .setEvidenceRef("evidence:" + adjustmentSn)
                .setReason("上层处理完成，等待重新对账");
    }

    private CreateReconciliationDifferenceRequest clearingDifferenceRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationMatchResultSn(reconciliationMatchResultSn)
                .setResponsiblePartyRef("processor:issuer-ledger")
                .setDescription("外部 processor 清算文件金额与内部清算候选金额不一致");
    }

    private String recordInitialDifferenceRunResult() {
        String comparisonSourceRef = "external-difference:" + CLEARING_OBJECT_SN;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, RECONCILIATION_BATCH_SN, ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, "recon-rule-v1", "processor-report-file-digest-001",
                SOURCE_RECORD_SN, comparisonSourceRef);
        reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(RECONCILIATION_BATCH_SN)
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setReferenceSourceRef(SOURCE_RECORD_SN)
                        .setComparisonSourceRef(comparisonSourceRef)
                        .setSourceQuality(ReconciliationSourceQuality.UNVERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.CANDIDATE_MATCH)
                        .setDifferenceType(ReconciliationDifferenceType.AMOUNT_MISMATCH)
                        .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setDifferenceAmount(50L)
                        .setEvidenceRef("processor-report-file-digest-001"))), WindOperatorFactory.system());
        return jdbcTemplate.queryForObject("""
                SELECT sn FROM t_reconciliation_match_result
                WHERE tenant_id = ? AND reconciliation_batch_sn = ? AND difference_type IS NOT NULL
                """, String.class, TENANT_ID, RECONCILIATION_BATCH_SN);
    }

    private String requiredDifferenceSn() {
        return jdbcTemplate.queryForObject("""
                SELECT difference_sn FROM t_reconciliation_difference
                WHERE tenant_id = ? AND reconciliation_match_result_sn = ?
                """, String.class, TENANT_ID, reconciliationMatchResultSn);
    }

    @Configuration
    @Import({
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class,
            ReconciliationDifferenceReportApplicationServiceImpl.class
    })
    static class Config {
    }
}
