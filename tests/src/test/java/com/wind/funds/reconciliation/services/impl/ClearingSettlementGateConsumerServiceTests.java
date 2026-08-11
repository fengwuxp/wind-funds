package com.wind.funds.reconciliation.services.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ClearingSettlementGateResultDTO;
import com.wind.funds.reconciliation.model.request.CheckClearingSettlementGateRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.service.ClearingSettlementGateConsumerService;
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
    private ClearingSettlementGateConsumerService clearingSettlementGateConsumerService;

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
        clearingMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, RECONCILIATION_BATCH_SN, "processor-clearing-file-digest-001");
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:clearing-gate-run-001");
        settlementMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.SETTLEMENT,
                SETTLEMENT_OBJECT_SN, "recon_settlement_gate_difference_batch_001",
                "processor-settlement-file-digest-001");
        settlementRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.SETTLEMENT,
                SETTLEMENT_OBJECT_SN, "recon_settlement_gate_batch_001",
                "recon_settlement_gate_difference_batch_001",
                "report:settlement-gate-run-001");
    }

    @Test
    void testInspectGateShouldRejectTenantDifferentFromCurrentContext() {
        CheckClearingSettlementGateRequest request = new CheckClearingSettlementGateRequest()
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> clearingSettlementGateConsumerService.inspectGate(
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
        createHistoricalDifference(clearingDifferenceRequest());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getGateObjectSn()).isEqualTo(CLEARING_OBJECT_SN);
        assertThat(result.getReconciliationRunResultSn()).isEqualTo(clearingRunResultSn);
        assertThat(result.getReconciliationResultDigest()).hasSize(64);
        assertThat(result.getBlockingDifferences())
                .extracting(blockingDifference -> blockingDifference.getDifferenceSn())
                .containsExactly(requiredDifferenceSn(clearingMatchResultSn));
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-gate-run-001",
                "processor-clearing-file-digest-001");
        assertThat(result.getExplanation()).contains("阻断");
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
        createHistoricalDifference(settlementDifferenceRequest());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.inspectGate(
                settlementGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.SETTLEMENT);
        assertThat(result.getGateObjectSn()).isEqualTo(SETTLEMENT_OBJECT_SN);
        assertThat(result.getBlockingDifferences())
                .extracting(blockingDifference -> blockingDifference.getBlockingObjectSn())
                .containsExactly(SETTLEMENT_OBJECT_SN);
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
                                ReconciliationGateObjectType.CLEARING, "clearing-candidate-other",
                                "recon_clearing_gate_other_difference_batch_001",
                                "processor-clearing-other-file-digest-001")),
                WindOperatorFactory.system());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-gate-run-001");
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

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.inspectGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getResolvedDifferenceCount()).isOne();
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-gate-run-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private void prepareAdjustedDifferenceWithBalancedRerun() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        clearingMatchResultSn = recordDifferenceMatchResultSn(ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, RECONCILIATION_BATCH_SN, "processor-clearing-file-digest-001");
        createHistoricalDifference(clearingDifferenceRequest());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                clearingAdjustmentRequest(), WindOperatorFactory.system());
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
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

        assertThatThrownBy(() -> clearingSettlementGateConsumerService.inspectGate(
                clearingGateRequest().setGateObjectType(ReconciliationGateObjectType.PAYOUT), WindOperatorFactory.system()))
                .hasMessageContaining("清算结算对账准入消费对象类型仅支持 CLEARING 或 SETTLEMENT");
        assertThatThrownBy(() -> clearingSettlementGateConsumerService.inspectGate(
                clearingGateRequest().setGateObjectSn(" "), WindOperatorFactory.system()))
                .hasMessageContaining("清算结算对账准入消费对象流水号不能为空");
        assertThatThrownBy(() -> clearingSettlementGateConsumerService.inspectGate(null, WindOperatorFactory.system()))
                .hasMessageContaining("清算结算对账准入检查请求不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private CheckClearingSettlementGateRequest clearingGateRequest() {
        return new CheckClearingSettlementGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn(CLEARING_OBJECT_SN)
                .setReconciliationRunResultSn(clearingRunResultSn);
    }

    private CheckClearingSettlementGateRequest settlementGateRequest() {
        return new CheckClearingSettlementGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.SETTLEMENT)
                .setGateObjectSn(SETTLEMENT_OBJECT_SN)
                .setReconciliationRunResultSn(settlementRunResultSn);
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

    private String recordDifferenceMatchResultSn(ReconciliationGateObjectType gateObjectType,
                                                  String gateObjectSn,
                                                  String reconciliationBatchSn,
                                                  String evidenceRef) {
        String referenceSourceRef = "internal-difference:" + gateObjectSn;
        String comparisonSourceRef = "external-difference:" + gateObjectSn;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, reconciliationBatchSn, gateObjectType, gateObjectSn,
                "recon-rule-v1", evidenceRef, referenceSourceRef, comparisonSourceRef);
        reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn)
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setReferenceSourceRef(referenceSourceRef)
                        .setComparisonSourceRef(comparisonSourceRef)
                        .setSourceQuality(ReconciliationSourceQuality.UNVERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.CANDIDATE_MATCH)
                        .setDifferenceType(ReconciliationDifferenceType.AMOUNT_MISMATCH)
                        .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                        .setCurrency(CurrencyIsoCode.USD)
                        .setDifferenceAmount(50L)
                        .setEvidenceRef(evidenceRef))), WindOperatorFactory.system());
        return jdbcTemplate.queryForObject("""
                SELECT sn FROM t_reconciliation_match_result
                WHERE tenant_id = ? AND reconciliation_batch_sn = ? AND difference_type IS NOT NULL
                """, String.class, TENANT_ID, reconciliationBatchSn);
    }

    private String requiredDifferenceSn(String matchResultSn) {
        return jdbcTemplate.queryForObject("""
                SELECT difference_sn FROM t_reconciliation_difference
                WHERE tenant_id = ? AND reconciliation_match_result_sn = ?
                """, String.class, TENANT_ID, matchResultSn);
    }

    private String recordBalancedRunResult(ReconciliationGateObjectType gateObjectType,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String previousBatchSn,
                                           String evidenceRef) {
        String referenceSourceRef = "internal:" + gateObjectSn;
        String comparisonSourceRef = "external:" + gateObjectSn;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, reconciliationBatchSn, gateObjectType, gateObjectSn,
                "recon-rule-v1", evidenceRef, referenceSourceRef, comparisonSourceRef, previousBatchSn);
        return reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn)
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setReferenceSourceRef(referenceSourceRef)
                        .setComparisonSourceRef(comparisonSourceRef)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef(evidenceRef + "#line-1"))), WindOperatorFactory.system()).getSn();
    }

    @Configuration
    @Import({
            ClearingSettlementGateConsumerServiceImpl.class,
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class
    })
    static class Config {
    }
}
