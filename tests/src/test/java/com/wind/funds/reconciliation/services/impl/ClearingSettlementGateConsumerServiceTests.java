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
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
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

    private static final String DIFFERENCE_SN = "recon_clearing_gate_diff_001";

    private static final String RECONCILIATION_BATCH_SN = "recon_clearing_gate_batch_001";

    private static final String SOURCE_RECORD_SN = "processor_clearing_gate_line_001";

    private static final String ADJUSTMENT_SN = "balance_adjust_clearing_gate_001";

    private static final String RERUN_SN = "recon_clearing_gate_rerun_001";

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

    @BeforeEach
    void prepareReconciliationEvidence() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
        clearingRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.CLEARING,
                CLEARING_OBJECT_SN, RERUN_BATCH_SN, "report:clearing-gate-run-001");
        settlementRunResultSn = recordBalancedRunResult(ReconciliationGateObjectType.SETTLEMENT,
                SETTLEMENT_OBJECT_SN, "recon_settlement_gate_batch_001", "report:settlement-gate-run-001");
    }

    /**
     * 场景：清算候选生成前存在精确命中该候选的对象级未闭环差错。
     * 输入：blockingObjectType=CLEARING、blockingObjectSn=clearing-candidate-001 的差错和清算 gate 请求。
     * 输出：consumer 返回阻断、对象流水、差错流水、证据引用和解释摘要。
     * 红线：consumer 只读消费 gate，不生成清算候选、交易、route、posting、LedgerEntry 或余额投影。
     */
    @Test
    void testCheckClearingGateShouldBlockWhenObjectLevelDifferenceUnresolvedWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getGateObjectSn()).isEqualTo(CLEARING_OBJECT_SN);
        assertThat(result.getReconciliationRunResultSn()).isEqualTo(clearingRunResultSn);
        assertThat(result.getReconciliationResultDigest()).hasSize(64);
        assertThat(result.getBlockingDifferences())
                .extracting(blockingDifference -> blockingDifference.getDifferenceSn())
                .containsExactly(DIFFERENCE_SN);
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-gate-run-001",
                "processor-clearing-file-digest-001");
        assertThat(result.getExplanation()).contains("阻断");
        assertThat(result.getOperationStatus()).isEqualTo("BLOCKED");
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
    void testCheckSettlementGateShouldBlockWhenObjectLevelDifferenceUnresolvedWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(settlementDifferenceRequest(),
                WindOperatorFactory.system());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.checkGate(
                settlementGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
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
    void testCheckClearingGateShouldPassWhenOnlyOtherClearingObjectBlocked() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(
                clearingDifferenceRequest()
                        .setBlockingObjectSn("clearing-candidate-other"),
                WindOperatorFactory.system());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-gate-run-001");
        assertThat(result.getOperationStatus()).isEqualTo("PASSED");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：命中清算对象的差错已完成处理动作回链并重新对账通过。
     * 输入：清算对象差错、ADJUST 处理动作和 balanced=true 的重跑结果。
     * 输出：consumer 条件放行，并返回原始差错、处理动作和重跑证据。
     * 红线：条件放行不代表已确认清算或锁定结算。
     */
    @Test
    void testCheckClearingGateShouldConditionallyPassWhenDifferenceResolvedByBalancedRerun() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(clearingDifferenceRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(clearingAdjustmentRequest(),
                WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.recordRerunResult(clearingRerunRequest(), WindOperatorFactory.system());

        ClearingSettlementGateResultDTO result = clearingSettlementGateConsumerService.checkGate(
                clearingGateRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.CONDITIONALLY_PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).containsExactly("report:clearing-gate-run-001",
                "processor-clearing-file-digest-001",
                "adjustment-evidence-clearing-001", "rerun-report-clearing-001");
        assertThat(result.getOperationStatus()).isEqualTo("CONDITIONALLY_PASSED");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方传入出款对象类型或空对象流水。
     * 输入：非清算 / 结算对象类型或缺少对象流水的请求。
     * 输出：consumer 拒绝请求。
     * 红线：该服务不能被复用为出款准入，也不能产生账务副作用。
     */
    @Test
    void testCheckGateShouldRejectUnsupportedOrIncompleteRequestWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> clearingSettlementGateConsumerService.checkGate(
                clearingGateRequest().setGateObjectType(ReconciliationGateObjectType.PAYOUT), WindOperatorFactory.system()))
                .hasMessageContaining("清算结算对账准入消费对象类型仅支持 CLEARING 或 SETTLEMENT");
        assertThatThrownBy(() -> clearingSettlementGateConsumerService.checkGate(
                clearingGateRequest().setGateObjectSn(" "), WindOperatorFactory.system()))
                .hasMessageContaining("清算结算对账准入消费对象流水号不能为空");
        assertThatThrownBy(() -> clearingSettlementGateConsumerService.checkGate(null, WindOperatorFactory.system()))
                .hasMessageContaining("清算结算对账准入检查请求不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private CheckClearingSettlementGateRequest clearingGateRequest() {
        return new CheckClearingSettlementGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn(CLEARING_OBJECT_SN)
                .setReconciliationRunResultSn(clearingRunResultSn)
                .setCurrency(CurrencyIsoCode.USD)
                .setAmount(10_00L)
                .setIdempotencyKey("idem-clearing-gate-001");
    }

    private CheckClearingSettlementGateRequest settlementGateRequest() {
        return new CheckClearingSettlementGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.SETTLEMENT)
                .setGateObjectSn(SETTLEMENT_OBJECT_SN)
                .setReconciliationRunResultSn(settlementRunResultSn)
                .setCurrency(CurrencyIsoCode.USD)
                .setAmount(10_00L)
                .setIdempotencyKey("idem-settlement-gate-001");
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
                .setEvidenceRef("processor-clearing-file-digest-001")
                .setDescription("外部 processor 清算文件金额与内部清算候选金额不一致");
    }

    private CreateReconciliationDifferenceRequest settlementDifferenceRequest() {
        return clearingDifferenceRequest()
                .setBlockingScope("SETTLEMENT")
                .setBlockingObjectType(ReconciliationGateObjectType.SETTLEMENT)
                .setBlockingObjectSn(SETTLEMENT_OBJECT_SN);
    }

    private LinkReconciliationDifferenceAdjustmentRequest clearingAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
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
                .setDifferenceSn(DIFFERENCE_SN)
                .setRerunSn(RERUN_SN)
                .setRerunBatchSn(RERUN_BATCH_SN)
                .setRuleVersion("recon-rule-v1")
                .setBalanced(true)
                .setEvidenceRef("rerun-report-clearing-001")
                .setResultDigest("sha256:clearing-rerun-balanced-001")
                .setDescription("调账后重新对账通过");
    }

    private String recordBalancedRunResult(ReconciliationGateObjectType gateObjectType,
                                           String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String evidenceRef) {
        return reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn)
                .setGateObjectType(gateObjectType)
                .setGateObjectSn(gateObjectSn)
                .setRuleVersion("recon-rule-v1")
                .setInternalSourceDigest("a".repeat(64))
                .setExternalSourceDigest("b".repeat(64))
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setInternalSourceRef("internal:" + gateObjectSn)
                        .setExternalSourceRef("external:" + gateObjectSn)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef(evidenceRef + "#line-1")))
                .setEvidenceRefs(List.of(evidenceRef)), WindOperatorFactory.system()).getSn();
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
