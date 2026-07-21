package com.wind.funds.reconciliation.application.difference.report.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.difference.report.ReconciliationDifferenceReportApplicationService;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceReportCompleteness;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceReportDTO;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.GetReconciliationDifferenceReportRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
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

    private static final String DIFFERENCE_SN = "recon_report_diff_001";

    private static final String RECONCILIATION_BATCH_SN = "recon_report_batch_001";

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

    @BeforeEach
    void cleanReconciliationDifference() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
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
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperator.system());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperator.system());

        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getDifferenceSn()).isEqualTo(DIFFERENCE_SN);
        assertThat(result.getStatus()).isEqualTo(ReconciliationDifferenceStatus.BLOCKED);
        assertThat(result.getDifferenceType()).isEqualTo(ReconciliationDifferenceType.AMOUNT_MISMATCH);
        assertThat(result.getSeverity()).isEqualTo(ReconciliationDifferenceSeverity.S1_MAJOR);
        assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(result.getDifferenceAmount()).isEqualTo(50L);
        assertThat(result.getBlockingScope()).isEqualTo("CLEARING");
        assertThat(result.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getBlockingObjectSn()).isEqualTo(CLEARING_OBJECT_SN);
        assertThat(result.getGateDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
        assertThat(result.getEvidenceRefs())
                .containsExactly("processor-report-file-digest-001", RUN_RESULT_EVIDENCE_REF);
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.COMPLETE);
        assertThat(result.getExplanation()).contains("阻断");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
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
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperator.system());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest()
                        .setIncludeGateDecision(false)
                        .setReconciliationRunResultSn(null)
                        .setIncludeEvidenceRefs(false),
                WindOperator.system());

        assertThat(result.getDifferenceSn()).isEqualTo(DIFFERENCE_SN);
        assertThat(result.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getBlockingObjectSn()).isEqualTo(CLEARING_OBJECT_SN);
        assertThat(result.getGateDecisionStatus()).isNull();
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
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperator.system());

        assertThatThrownBy(() -> reconciliationDifferenceReportApplicationService.getReport(
                reportRequest().setReconciliationRunResultSn(null), WindOperator.system()))
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
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperator.system());
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET adjustment_sn = ?
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, "legacy-adjustment-with-missing-evidence", TENANT_ID, DIFFERENCE_SN);

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperator.system());

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
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperator.system());
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = ?
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, ReconciliationDifferenceStatus.RESOLVED.name(), TENANT_ID, DIFFERENCE_SN);

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ReconciliationDifferenceStatus.RESOLVED);
        assertThat(result.getLastRerunSn()).isNull();
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.MISSING_RERUN_RESULT);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史类型级差错缺少对象级 gate 定位字段。
     * 输入：blockingScope=CLEARING，但 blockingObjectType 和 blockingObjectSn 为空。
     * 输出：报告返回 MISSING_GATE_DECISION，提示准入 gate 证据不足。
     * 红线：报告只暴露 gate 定位缺口，不补对象引用、不生成清算、结算或账本事实。
     */
    @Test
    void testGetReportShouldExposeMissingGateDecisionForLegacyTypeLevelDifference() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest()
                .setBlockingObjectType(null)
                .setBlockingObjectSn(null), WindOperator.system());

        ReconciliationDifferenceReportDTO result = reconciliationDifferenceReportApplicationService.getReport(
                reportRequest(), WindOperator.system());

        assertThat(result.getBlockingScope()).isEqualTo("CLEARING");
        assertThat(result.getBlockingObjectType()).isNull();
        assertThat(result.getBlockingObjectSn()).isNull();
        assertThat(result.getGateDecisionStatus()).isNull();
        assertThat(result.getCompleteness()).isEqualTo(ReconciliationDifferenceReportCompleteness.MISSING_GATE_DECISION);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private GetReconciliationDifferenceReportRequest reportRequest() {
        String runResultSn = reconciliationRunResultApplicationService.recordRunResult(
                balancedRunResultRequest(), WindOperator.system()).getSn();
        return new GetReconciliationDifferenceReportRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setIncludeGateDecision(true)
                .setReconciliationRunResultSn(runResultSn)
                .setIncludeEvidenceRefs(true);
    }

    private RecordReconciliationRunResultRequest balancedRunResultRequest() {
        return new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(RECONCILIATION_BATCH_SN)
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn(CLEARING_OBJECT_SN)
                .setRuleVersion("recon-rule-v1")
                .setInternalSourceDigest("c".repeat(64))
                .setExternalSourceDigest("d".repeat(64))
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setInternalSourceRef("internal:" + CLEARING_OBJECT_SN)
                        .setExternalSourceRef("external:" + CLEARING_OBJECT_SN)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef(RUN_RESULT_EVIDENCE_REF + "#line-1")))
                .setEvidenceRefs(List.of(RUN_RESULT_EVIDENCE_REF));
    }

    private CreateReconciliationDifferenceRequest clearingDifferenceRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setReconciliationBatchSn(RECONCILIATION_BATCH_SN)
                .setSourceRecordSn(SOURCE_RECORD_SN)
                .setSourceQuality(ReconciliationSourceQuality.UNVERIFIED)
                .setMatchStrength(ReconciliationMatchStrength.CANDIDATE_MATCH)
                .setDifferenceType(ReconciliationDifferenceType.AMOUNT_MISMATCH)
                .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                .setCurrency(CurrencyIsoCode.USD)
                .setDifferenceAmount(50L)
                .setResponsiblePartyRef("processor:issuer-ledger")
                .setBlockingScope("CLEARING")
                .setBlockingObjectType(ReconciliationGateObjectType.CLEARING)
                .setBlockingObjectSn(CLEARING_OBJECT_SN)
                .setRuleVersion("recon-rule-v1")
                .setEvidenceRef("processor-report-file-digest-001")
                .setDescription("外部 processor 清算文件金额与内部清算候选金额不一致");
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
