package com.wind.funds.reconciliation.application.gate.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateBlockingDifferenceDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
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

    private static final String DIFFERENCE_SN = "recon_gate_diff_001";

    private static final String RECONCILIATION_BATCH_SN = "recon_gate_batch_001";

    private static final String SOURCE_RECORD_SN = "processor_gate_line_001";

    private static final String ADJUSTMENT_SN = "balance_adjust_gate_001";

    private static final String RERUN_SN = "recon_gate_rerun_001";

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanReconciliationDifference() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
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
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperator.system());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.checkGate(
                clearingGateRequest(), WindOperator.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.CLEARING);
        assertThat(result.getGateObjectSn()).isEqualTo("clearing-candidate-001");
        assertThat(result.getBlockingDifferences())
                .extracting(ReconciliationGateBlockingDifferenceDTO::getDifferenceSn)
                .containsExactly(DIFFERENCE_SN);
        ReconciliationGateBlockingDifferenceDTO blockingDifference = result.getBlockingDifferences().getFirst();
        assertThat(blockingDifference.getStatus()).isEqualTo(ReconciliationDifferenceStatus.BLOCKED);
        assertThat(blockingDifference.getResponsiblePartyRef()).isEqualTo("processor:issuer-ledger");
        assertThat(blockingDifference.getEvidenceRef()).isEqualTo("processor-gate-file-digest-001");
        assertThat(blockingDifference.getBlockingReason()).contains("未闭环");
        assertThat(result.getEvidenceRefs()).containsExactly("processor-gate-file-digest-001");
        assertThat(result.getExplanation()).contains("阻断");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：清算候选生成前没有命中阻断范围的差错。
     * 输入：空差错表和清算准入检查请求。
     * 输出：准入通过，不返回阻断差错或证据引用。
     * 红线：空差错准入仍只读检查，不得生成任何资金或账本事实。
     */
    @Test
    void testCheckGateShouldPassWhenNoScopedDifferenceExists() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.checkGate(
                clearingGateRequest(), WindOperator.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).isEmpty();
        assertThat(result.getExplanation()).contains("准入通过");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：存在清算和出款阻断差错，但当前消费方是结算。
     * 输入：blockingScope 为 CLEARING,PAYOUT 的差错和结算准入检查请求。
     * 输出：结算准入不被不相关范围的差错误阻断。
     * 红线：阻断范围必须精确消费，不能把一个差错扩散成全链路阻断。
     */
    @Test
    void testCheckGateShouldPassWhenDifferenceScopeDoesNotMatch() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperator.system());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.checkGate(
                settlementGateRequest(), WindOperator.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.PASSED);
        assertThat(result.getGateObjectType()).isEqualTo(ReconciliationGateObjectType.SETTLEMENT);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs()).isEmpty();
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

        assertThatThrownBy(() -> reconciliationGateApplicationService.checkGate(clearingGateRequest(), null))
                .hasMessageContaining("对账差错准入检查操作人不能为空");

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
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperator.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperator.system());
        reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest()
                        .setBalanced(false)
                        .setResultDigest("sha256:gate-rerun-unbalanced"),
                WindOperator.system());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.checkGate(
                clearingGateRequest(), WindOperator.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
        ReconciliationGateBlockingDifferenceDTO blockingDifference = result.getBlockingDifferences().getFirst();
        assertThat(blockingDifference.getStatus()).isEqualTo(ReconciliationDifferenceStatus.RECONCILING);
        assertThat(blockingDifference.getActionType()).isEqualTo(ReconciliationDifferenceActionType.ADJUST);
        assertThat(blockingDifference.getAdjustmentSn()).isEqualTo(ADJUSTMENT_SN);
        assertThat(blockingDifference.getLastRerunSn()).isEqualTo(RERUN_SN);
        assertThat(blockingDifference.getLastRerunBalanced()).isFalse();
        assertThat(blockingDifference.getBlockingReason()).contains("重新对账未对平");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错已回链白名单处理动作，并且重新对账已经对平。
     * 输入：RESOLVED 状态差错和清算准入检查请求。
     * 输出：准入条件放行，返回处理动作、重跑流水和证据摘要。
     * 红线：条件放行仍然不代表已确认清算批次、锁定结算或提交出款。
     */
    @Test
    void testCheckGateShouldConditionallyPassWhenScopedDifferenceResolvedByBalancedRerun() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperator.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperator.system());
        reconciliationDifferenceApplicationService.recordRerunResult(minimumRerunRequest(), WindOperator.system());

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.checkGate(
                clearingGateRequest(), WindOperator.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.CONDITIONALLY_PASSED);
        assertThat(result.getBlockingDifferences()).isEmpty();
        assertThat(result.getEvidenceRefs())
                .containsExactly("processor-gate-file-digest-001", "adjustment-evidence-gate-001",
                        "rerun-report-gate-001");
        assertThat(result.getExplanation()).contains("重新对账已对平");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
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
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperator.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperator.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setOriginalFactRef("external-balance-anomaly:changed"),
                WindOperator.system()))
                .hasMessageContaining("对账差错处理幂等请求原始事实引用不一致");

        ReconciliationGateDecisionDTO result = reconciliationGateApplicationService.checkGate(
                clearingGateRequest(), WindOperator.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
        assertThat(result.getBlockingDifferences()).hasSize(1);
        assertThat(result.getBlockingDifferences().getFirst().getOriginalFactRef())
                .isEqualTo("external-balance-anomaly:issuer-ledger-gate-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private CheckReconciliationGateRequest clearingGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn("clearing-candidate-001");
    }

    private CheckReconciliationGateRequest settlementGateRequest() {
        return new CheckReconciliationGateRequest()
                .setTenantId(TENANT_ID)
                .setGateObjectType(ReconciliationGateObjectType.SETTLEMENT)
                .setGateObjectSn("settlement-order-001");
    }

    private CreateReconciliationDifferenceRequest minimumCreateRequest() {
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
                .setBlockingScope("CLEARING,PAYOUT")
                .setRuleVersion("recon-rule-v1")
                .setEvidenceRef("processor-gate-file-digest-001")
                .setDescription("外部 processor 文件金额与内部账本金额不一致");
    }

    private LinkReconciliationDifferenceAdjustmentRequest minimumAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
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
                .setDifferenceSn(DIFFERENCE_SN)
                .setRerunSn(RERUN_SN)
                .setRerunBatchSn("recon_gate_batch_001_rerun_001")
                .setRuleVersion("recon-rule-v1")
                .setBalanced(true)
                .setEvidenceRef("rerun-report-gate-001")
                .setResultDigest("sha256:gate-rerun-balanced-001")
                .setDescription("调账后重新对账通过");
    }

    @Configuration
    @Import({
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class
    })
    static class Config {
    }
}
