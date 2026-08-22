package com.wind.funds.reconciliation.services.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static com.wind.funds.reconciliation.ReconciliationTestFixture.withMatchBatchAsCurrentHead;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 清算 / 结算对账准入消费服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ClearingSettlementGateConsumerServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClearingSettlementGateConsumerServiceTests extends AbstractFundsServiceTest {

    private static final String CLEARING_OBJECT_SN = "clearing-candidate-001";

    private static final String SETTLEMENT_OBJECT_SN = "settlement-order-001";

    private static final String RECONCILIATION_BATCH_SN = "recon_clearing_gate_batch_001";

    private static final String ADJUSTMENT_SN = "balance_adjust_clearing_gate_001";

    private static final String RERUN_BATCH_SN = "recon_clearing_gate_batch_001_rerun_001";

    @Autowired
    private ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String clearingRunResultSn;

    private String settlementRunResultSn;

    private String clearingMatchResultSn;

    private String settlementMatchResultSn;

    @BeforeEach
    void prepareReconciliationEvidence() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn("CLEARING_CONFIRM_ITEM",
                CLEARING_OBJECT_SN, RECONCILIATION_BATCH_SN, "processor-clearing-file-digest-001");
        clearingRunResultSn = recordBalancedRunResult("CLEARING_CONFIRM_ITEM",
                CLEARING_OBJECT_SN, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-gate-run-001");
        settlementMatchResultSn = recordDifferenceMatchResultSn("SETTLEMENT_LOCK",
                SETTLEMENT_OBJECT_SN, "recon_settlement_gate_difference_batch_001",
                "processor-settlement-file-digest-001");
        settlementRunResultSn = recordBalancedRunResult("SETTLEMENT_LOCK",
                SETTLEMENT_OBJECT_SN, "recon_settlement_gate_batch_001",
                "recon_settlement_gate_difference_batch_001",
                "report:settlement-gate-run-001");
    }

    @Test
    void testInspectGateShouldRejectTenantDifferentFromCurrentContext() {
        CheckReconciliationGateRequest request = clearingGateRequest().setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> reconciliationGateApplicationService.inspectGate(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
    }

    /**
     * 场景：清算候选生成前存在精确命中该候选的对象级未闭环差错。
     * 输入：blockingObjectType=CLEARING、blockingObjectSn=clearing-candidate-001 的差错和清算 gate 请求。
     * 输出：consumer 返回阻断、对象流水、差错流水、证据引用和解释摘要。
     * 红线：consumer 只读消费 gate，不生成清算候选、交易、route、posting、LedgerEntry 或余额投影。
     */
    @Test
    void testInspectClearingGateShouldBlockWhenObjectLevelDifferenceUnresolvedWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createCurrentDifference(clearingDifferenceRequest());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getStageRef()).isEqualTo(clearingGateRequest().getStageRef());
        assertThat(result.getPairDecisions()).isNotEmpty();
        assertThat(result.getEvidenceRefs()).contains("report:clearing-gate-run-001",
                "processor-clearing-file-digest-001");
        assertThat(result.getEvidenceRefs()).anyMatch(evidence -> evidence.startsWith("run:"));
        assertThat(result.getExplanation()).contains("mandatory pair");
        assertThat(result.getCheckedAt()).isNotNull();
        assertThat(result.getCheckedBy()).isEqualTo(WindOperatorFactory.system().getOperatorAsText());
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：结算单锁定前存在精确命中该结算单的对象级未闭环差错。
     * 输入：blockingObjectType=SETTLEMENT、blockingObjectSn=settlement-order-001 的差错和结算 gate 请求。
     * 输出：consumer 返回阻断并保留结算对象字段。
     * 红线：结算 gate 只做准入解释，不创建或锁定结算单，不写账务事实。
     */
    @Test
    void testInspectSettlementGateShouldBlockWhenObjectLevelDifferenceUnresolvedWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        createCurrentDifference(settlementDifferenceRequest());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.inspectGate(
                settlementGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getStageRef()).isEqualTo(settlementGateRequest().getStageRef());
        assertThat(result.getPairDecisions()).isNotEmpty();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一清算类型下存在另一个清算候选的对象级差错。
     * 输入：差错对象流水是 clearing-candidate-other，当前请求对象是 clearing-candidate-001。
     * 输出：consumer 对当前对象准入通过，不被同类型其他对象误阻断。
     * 红线：不能把对象级差错退化成类型级全量阻断。
     */
    @Test
    void testInspectClearingGateShouldPassWhenOnlyOtherClearingObjectBlocked() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(
                clearingDifferenceRequest()
                        .setReconciliationMatchResultSn(recordDifferenceMatchResultSn(
                                "CLEARING_CONFIRM_ITEM", "clearing-candidate-other",
                                "recon_clearing_gate_other_difference_batch_001",
                                "processor-clearing-other-file-digest-001")),
                WindOperatorFactory.system());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).hasSize(1);
        assertThat(result.getPairDecisions().getFirst().getBlockerCodes()).isEmpty();
        assertThat(result.getEvidenceRefs()).contains("report:clearing-gate-run-001");
        assertThat(result.getEvidenceRefs()).anyMatch(evidence -> evidence.startsWith("run:"));
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：命中清算对象的差错已完成处理动作回链并重新对账通过。
     * 输入：清算对象差错、ADJUST 处理动作和 balanced=true 的重跑结果。
     * 输出：consumer 普通通过，并返回已闭环历史差错数量和重跑证据。
     * 红线：历史差错闭环不等于风险条件放行，也不代表已确认清算或锁定结算。
     */
    @Test
    void testInspectClearingGateShouldPassWhenDifferenceResolvedByBalancedRerun() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        prepareAdjustedDifferenceWithBalancedRerun();
        reconciliationDifferenceApplicationService.recordRerunResult(clearingRerunRequest(), WindOperatorFactory.system());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getPairDecisions()).allMatch(decision -> decision.getBlockerCodes().isEmpty());
        assertThat(result.getEvidenceRefs()).contains("report:clearing-gate-run-001");
        assertThat(result.getEvidenceRefs()).anyMatch(evidence -> evidence.startsWith("run:"));
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private void prepareAdjustedDifferenceWithBalancedRerun() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn("CLEARING_CONFIRM_ITEM",
                CLEARING_OBJECT_SN, RECONCILIATION_BATCH_SN, "processor-clearing-file-digest-001");
        createHistoricalDifference(clearingDifferenceRequest());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                clearingAdjustmentRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordBalancedRunResult("CLEARING_CONFIRM_ITEM",
                CLEARING_OBJECT_SN, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-gate-run-001");
    }

    /**
     * 场景：调用方传入出款对象类型或空对象流水。
     * 输入：非清算 / 结算对象类型或缺少对象流水的请求。
     * 输出：consumer 拒绝请求。
     * 红线：该服务不能被复用为出款准入，也不能产生账务副作用。
     */
    @Test
    void testInspectGateShouldRejectUnsupportedOrIncompleteRequestWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> reconciliationGateApplicationService.inspectGate(
                clearingGateRequest().setStageRef(null), WindOperatorFactory.system()))
                .hasMessageContaining("Stage 引用不能为空");
        assertThatThrownBy(() -> reconciliationGateApplicationService.inspectGate(null, WindOperatorFactory.system()))
                .hasMessageContaining("对账准入检查请求不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private CheckReconciliationGateRequest clearingGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setStageRef(com.wind.funds.reconciliation.ReconciliationTestFixture.stage(
                        "CLEARING_CONFIRM_ITEM", CLEARING_OBJECT_SN));
    }

    private CheckReconciliationGateRequest settlementGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setStageRef(com.wind.funds.reconciliation.ReconciliationTestFixture.stage(
                        "SETTLEMENT_LOCK", SETTLEMENT_OBJECT_SN));
    }

    private CreateReconciliationDifferenceRequest clearingDifferenceRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationMatchResultSn(clearingMatchResultSn)
                .setResponsiblePartyRef("processor:issuer-ledger")
                .setDescription("外部 processor 清算文件金额与内部清算候选金额不一致");
    }

    private CreateReconciliationDifferenceRequest settlementDifferenceRequest() {
        return clearingDifferenceRequest()
                .setReconciliationMatchResultSn(settlementMatchResultSn);
    }

    private void createHistoricalDifference(CreateReconciliationDifferenceRequest request) {
        withMatchBatchAsCurrentHead(jdbcTemplate, TENANT_ID, request.getReconciliationMatchResultSn(),
                () -> reconciliationDifferenceApplicationService.createDifference(
                        request, WindOperatorFactory.system()));
    }

    private void createCurrentDifference(CreateReconciliationDifferenceRequest request) {
        createHistoricalDifference(request);
        com.wind.funds.reconciliation.ReconciliationTestFixture.setMatchBatchAsCurrentHead(
                jdbcTemplate, TENANT_ID, request.getReconciliationMatchResultSn());
    }

    private LinkReconciliationDifferenceAdjustmentRequest clearingAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn(clearingMatchResultSn))
                .setActionType(ReconciliationDifferenceActionType.ADJUST)
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setIdempotencyKey("idem-recon-clearing-adjust-001")
                .setOriginalFactRef("external-clearing-anomaly:issuer-ledger-001")
                .setAdjustmentTransactionSn("funds_tx_adjust_clearing_gate_001")
                .setApprovalRef("approval-recon-clearing-adjust-001")
                .setEvidenceRef("adjustment-evidence-clearing-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private RecordReconciliationDifferenceRerunRequest clearingRerunRequest() {
        return new RecordReconciliationDifferenceRerunRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn(clearingMatchResultSn))
                .setReconciliationRunResultSn(clearingRunResultSn);
    }

    private String recordDifferenceMatchResultSn(String stageKind,
                                                  String gateObjectSn,
                                                  String reconciliationBatchSn,
                                                  String evidenceRef) {
        String referenceSourceRef = "internal-difference:" + gateObjectSn;
        String comparisonSourceRef = "external-difference:" + gateObjectSn;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, reconciliationBatchSn, stageKind, gateObjectSn,
                "recon-rule-v1", evidenceRef, referenceSourceRef, comparisonSourceRef,
                null, 2L, "CONFIRMED");
        reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn), WindOperatorFactory.system());
        return jdbcTemplate.queryForObject("""
                SELECT sn FROM t_reconciliation_match_result
                WHERE tenant_id = ? AND reconciliation_batch_sn = ? AND result_kind <> 'MATCHED'
                """, String.class, TENANT_ID, reconciliationBatchSn);
    }

    private String requiredDifferenceSn(String matchResultSn) {
        return jdbcTemplate.queryForObject("""
                SELECT difference_sn FROM t_reconciliation_difference
                WHERE tenant_id = ? AND reconciliation_match_result_sn = ?
                """, String.class, TENANT_ID, matchResultSn);
    }

    private String recordBalancedRunResult(String stageKind,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String previousBatchSn,
                                           String evidenceRef) {
        String referenceSourceRef = "internal:" + gateObjectSn;
        String comparisonSourceRef = "external:" + gateObjectSn;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, reconciliationBatchSn, stageKind, gateObjectSn,
                "recon-rule-v1", evidenceRef, referenceSourceRef, comparisonSourceRef, previousBatchSn);
        return reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn), WindOperatorFactory.system()).getSn();
    }

    @Configuration
    @Import({
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class
    })
    static class Config {
    }
}
