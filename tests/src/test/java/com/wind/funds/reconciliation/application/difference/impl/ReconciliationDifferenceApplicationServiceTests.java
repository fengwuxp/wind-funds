package com.wind.funds.reconciliation.application.difference.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
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
 * 对账差错生命周期应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationDifferenceApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationDifferenceApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String DIFFERENCE_SN = "recon_diff_mvp_001";

    private static final String RECONCILIATION_BATCH_SN = "recon_batch_mvp_001";

    private static final String SOURCE_RECORD_SN = "processor_file_line_001";

    private static final String ADJUSTMENT_SN = "balance_adjust_recon_001";

    private static final String FUNDS_TRANSACTION_SN = "funds_tx_adjust_recon_001";

    private static final String RERUN_BATCH_SN = "recon_batch_mvp_001_rerun_001";

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanReconciliationDifference() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    /**
     * 场景：外部文件行未完成来源质量验证且匹配强度只是候选匹配。
     * 输入：对账批次、来源记录、差异类型、差异金额、责任方、规则版本和证据引用。
     * 输出：生成差错单并进入阻断状态，重复提交同一差错流水号保持幂等。
     * 红线：差错登记只形成运营对象，不得生成 route、posting、ledger transaction 或 ledger entry。
     */
    @Test
    void testCreateDifferenceShouldBlockAndKeepLedgerFactsUnchanged() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        ReconciliationDifferenceDTO replay = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());

        assertThat(result.getDifferenceSn()).isEqualTo(DIFFERENCE_SN);
        assertThat(result.getReconciliationBatchSn()).isEqualTo(RECONCILIATION_BATCH_SN);
        assertThat(result.getSourceRecordSn()).isEqualTo(SOURCE_RECORD_SN);
        assertThat(result.getDifferenceType()).isEqualTo(ReconciliationDifferenceType.AMOUNT_MISMATCH);
        assertThat(result.getSourceQuality()).isEqualTo(ReconciliationSourceQuality.UNVERIFIED);
        assertThat(result.getMatchStrength()).isEqualTo(ReconciliationMatchStrength.CANDIDATE_MATCH);
        assertThat(result.getStatus()).isEqualTo(ReconciliationDifferenceStatus.BLOCKED);
        assertThat(result.getBlockingScope()).isEqualTo("CLEARING,PAYOUT");
        assertThat(result.getRerunCount()).isZero();
        assertThat(result.getCreatedBy()).isEqualTo(WindOperatorFactory.system().getOperatorAsText());
        assertThat(replay.getId()).isEqualTo(result.getId());
        assertThat(countDifferenceRows(DIFFERENCE_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：清算或结算消费方登记一个对象级阻断差错。
     * 输入：blockingScope、blockingObjectType 和 blockingObjectSn。
     * 输出：差错结果回传阻断对象字段，重复提交保持幂等。
     * 红线：对象级字段只是差错命中键，不生成清算、结算、route、posting 或账本事实。
     */
    @Test
    void testCreateDifferenceShouldKeepObjectScopeInResultAndIdempotentReplay() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        CreateReconciliationDifferenceRequest request = minimumCreateRequest()
                .setBlockingScope("SETTLEMENT")
                .setBlockingObjectType(ReconciliationGateObjectType.SETTLEMENT)
                .setBlockingObjectSn("settlement-order-001");
        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.createDifference(
                request, WindOperatorFactory.system());
        ReconciliationDifferenceDTO replay = reconciliationDifferenceApplicationService.createDifference(
                request, WindOperatorFactory.system());

        assertThat(result.getBlockingScope()).isEqualTo("SETTLEMENT");
        assertThat(result.getBlockingObjectType()).isEqualTo(ReconciliationGateObjectType.SETTLEMENT);
        assertThat(result.getBlockingObjectSn()).isEqualTo("settlement-order-001");
        assertThat(replay.getId()).isEqualTo(result.getId());
        assertThat(countDifferenceRows(DIFFERENCE_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：对账差错只传阻断对象类型或只传阻断对象流水。
     * 输入：不完整的对象级阻断字段。
     * 输出：拒绝创建差错。
     * 红线：对象级阻断不能形成半截命中键，也不能生成 route、posting 或账本事实。
     */
    @Test
    void testCreateDifferenceShouldRejectIncompleteObjectScope() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest()
                        .setBlockingObjectType(ReconciliationGateObjectType.CLEARING),
                WindOperatorFactory.system()))
                .hasMessageContaining("创建对账差错阻断对象类型和流水号必须同时填写或同时为空");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest()
                        .setBlockingObjectSn("clearing-candidate-001"),
                WindOperatorFactory.system()))
                .hasMessageContaining("创建对账差错阻断对象类型和流水号必须同时填写或同时为空");

        assertThat(countDifferenceRows(DIFFERENCE_SN)).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错已由交易层余额调账处理后，运营回链调账结果并重新对账。
     * 输入：差错流水号、调账单号、资金交易流水、审批和重跑结果。
     * 输出：先进入调账处理中，再在重新对账通过后关闭；重复回链和重复重跑不增加次数。
     * 红线：reconciliation 只记录处理结果和重跑证据，不直接发起资金事实或改历史分录。
     */
    @Test
    void testAdjustmentLinkAndRerunShouldBeIdempotentAndCloseAfterBalancedRerun() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());

        ReconciliationDifferenceDTO linked = reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        ReconciliationDifferenceDTO linkedReplay = reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = recordRunResult(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        ReconciliationDifferenceDTO closed = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system());
        ReconciliationDifferenceDTO closedReplay = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system());

        assertThat(linked.getStatus()).isEqualTo(ReconciliationDifferenceStatus.ADJUSTING);
        assertThat(linked.getActionType()).isEqualTo(ReconciliationDifferenceActionType.ADJUST);
        assertThat(linked.getAdjustmentSn()).isEqualTo(ADJUSTMENT_SN);
        assertThat(linked.getAdjustmentIdempotencyKey()).isEqualTo("idem-recon-adjust-001");
        assertThat(linked.getOriginalFactRef()).isEqualTo("external-balance-anomaly:issuer-ledger-001");
        assertThat(linked.getAdjustmentTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(linkedReplay.getId()).isEqualTo(linked.getId());
        assertThat(closed.getStatus()).isEqualTo(ReconciliationDifferenceStatus.RESOLVED);
        assertThat(closed.getLastRerunSn()).isEqualTo(runResultSn);
        assertThat(closed.getRerunCount()).isOne();
        assertThat(closedReplay.getRerunCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营只回链调账单号、审批和证据，但没有声明白名单处理动作和原始事实引用。
     * 输入：差错流水号、调账单号、资金交易流水、审批、凭证和原因。
     * 输出：拒绝回链处理结果。
     * 红线：差错处理不能退化为任意单号备注；必须能说明白名单动作和被处理的原始事实。
     */
    @Test
    void testAdjustmentLinkShouldRejectMissingWhitelistActionContext() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                adjustmentRequestWithoutWhitelistActionContext(), WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理动作类型不能为空");
    }

    /**
     * 场景：同一差错流水号被重复提交，但来源金额被改写。
     * 输入：第一次登记 50 USD，第二次用相同 differenceSn 登记 51 USD。
     * 输出：拒绝第二次提交，差错单仍只有一条。
     * 红线：差错流水号的幂等不能退化为只按主键去重，必须拒绝事实字段漂移。
     */
    @Test
    void testCreateDifferenceShouldRejectIdempotentConflict() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setDifferenceAmount(51L), WindOperatorFactory.system()))
                .hasMessageContaining("对账差错幂等请求差异金额不一致");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest()
                        .setBlockingObjectType(ReconciliationGateObjectType.CLEARING)
                        .setBlockingObjectSn("clearing-candidate-001"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错幂等请求阻断对象类型不一致");

        assertThat(countDifferenceRows(DIFFERENCE_SN)).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：运营未回链调账、冲正、挂账或核销处理结果，却试图用重跑直接关闭差错。
     * 输入：差错流水号和对账通过结果，但缺少处理动作引用。
     * 输出：拒绝关闭差错。
     * 红线：差错不得只靠改状态关闭，必须先有关联处理动作或资金事实引用。
     */
    @Test
    void testRerunShouldRejectClosingDifferenceWithoutAdjustmentLink() {
        reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setDifferenceSn("recon_diff_mvp_002"), WindOperatorFactory.system());
        String runResultSn = recordRunResult(true, "recon_batch_mvp_001_rerun_002", RECONCILIATION_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn).setDifferenceSn("recon_diff_mvp_002"),
                WindOperatorFactory.system()))
                .hasMessageContaining("差错关闭必须先关联处理动作或调账结果");
    }

    /**
     * 场景：调用方自报对账已通过，但没有任何已持久化的重跑运行结果。
     * 输入：已回链处理动作的差错，以及指向不存在运行结果的流水号。
     * 输出：拒绝关闭差错，状态保持处理中。
     * 红线：差错关闭必须依赖真实完成态对账运行结果，不能相信调用方自报结论。
     */
    @Test
    void testRerunShouldRejectCallerDeclaredResultWithoutPersistedRunResult() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest("missing-run-result"), WindOperatorFactory.system()))
                .hasMessageContaining("对账运行结果不存在");

        ReconciliationDifferenceDTO unchanged = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        assertThat(unchanged.getStatus()).isEqualTo(ReconciliationDifferenceStatus.ADJUSTING);
        assertThat(unchanged.getLastRerunSn()).isNull();
    }

    /**
     * 场景：调用方使用与当前差错批次无血缘关系的运行结果。
     * 输入：已回链处理动作的差错，以及从其他批次派生的已完成运行结果。
     * 输出：拒绝记录重跑结果。
     * 红线：重新对账结果必须从当前差错批次可达，不能串用其他链路的结果。
     */
    @Test
    void testRerunShouldRejectRunResultOutsideCurrentLineage() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = recordRunResult(true, RERUN_BATCH_SN, "unrelated_reconciliation_batch");

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("血缘");
    }

    /**
     * 场景：重跑运行结果已经存在后继批次。
     * 输入：已回链处理动作的差错、当前重跑结果和引用该结果批次的后继批次。
     * 输出：拒绝使用已被替代的运行结果关闭差错。
     * 红线：差错处理只能消费当前血缘末端结果，不能回退信任旧结论。
     */
    @Test
    void testRerunShouldRejectSupersededRunResult() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = recordRunResult(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, "recon_batch_mvp_001_rerun_002", ReconciliationGateObjectType.CLEARING,
                "reconciliation-difference-001", "recon-rule-v2", "report:rerun-002",
                "internal:rerun-002", "external:rerun-002", RERUN_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("已被后续批次替代");
    }

    /**
     * 场景：第一轮重跑未对平且尚未回写差错，随后已完成第二轮并对平。
     * 结果：允许差错直接绑定从当前锚点可达的唯一末端结果，不因漏回写中间结果而卡死。
     */
    @Test
    void testRerunShouldBindLatestDescendantWhenIntermediateResultWasNotRecorded() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        recordRunResult(false, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        String latestBatchSn = "recon_batch_mvp_001_rerun_002";
        String latestRunResultSn = recordRunResult(true, latestBatchSn, RERUN_BATCH_SN);

        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(latestRunResultSn), WindOperatorFactory.system());

        assertThat(result.getStatus()).isEqualTo(ReconciliationDifferenceStatus.RESOLVED);
        assertThat(result.getLastRerunSn()).isEqualTo(latestRunResultSn);
        assertThat(result.getLastRerunBatchSn()).isEqualTo(latestBatchSn);
        assertThat(result.getRerunCount()).isEqualTo(2);
    }

    /**
     * 场景：持久化运行结果的准入对象被错误地改成与完成批次不同的对象。
     * 结果：即使差错本身没有对象级命中键，也拒绝把该运行结果作为重跑证据。
     */
    @Test
    void testRerunShouldRejectBatchAndRunResultObjectMismatch() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = recordRunResult(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        jdbcTemplate.update("""
                        UPDATE t_reconciliation_run_result
                        SET gate_object_sn = ?
                        WHERE tenant_id = ?
                          AND sn = ?
                        """, "tampered-clearing-candidate", TENANT_ID, runResultSn);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("运行结果与批次准入对象不一致");
    }

    /**
     * 场景：对象级差错引用了其他清算对象的重跑结果。
     * 输入：精确阻断对象与运行结果准入对象不一致。
     * 输出：拒绝记录重跑结果。
     * 红线：对象级差错不能串用同类型其他对象的正向证据。
     */
    @Test
    void testRerunShouldRejectRunResultForOtherBlockingObject() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest()
                .setBlockingObjectType(ReconciliationGateObjectType.CLEARING)
                .setBlockingObjectSn("clearing-candidate-expected"), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = recordRunResult(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("阻断对象不一致");
    }

    /**
     * 场景：同一差错已回链调账动作后，又用相同处理动作号提交不同幂等键或原事实引用。
     * 输入：已回链处理动作的差错、同一 adjustmentSn 和漂移后的处理上下文。
     * 输出：拒绝覆盖既有处理上下文。
     * 红线：动作类型、幂等键和原事实引用属于审计事实，不允许二次回链静默改写。
     */
    @Test
    void testAdjustmentLinkShouldRejectWhitelistContextConflict() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setIdempotencyKey("idem-recon-adjust-changed"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理幂等请求幂等键不一致");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setOriginalFactRef("external-balance-anomaly:changed"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理幂等请求原始事实引用不一致");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setActionType(ReconciliationDifferenceActionType.REVERSE),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理幂等请求动作类型不一致");
    }

    /**
     * 场景：差错已经重新对账通过并关闭后，又收到新的未对平重跑结果。
     * 输入：已关闭差错和新的未对平持久化运行结果。
     * 输出：拒绝追加新重跑结果，状态不得从已关闭退回处理中。
     * 红线：已关闭差错不能被后续运行静默重开，需走新的差错单或人工治理流程。
     */
    @Test
    void testResolvedDifferenceShouldRejectNewRerunResult() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        String firstRunResultSn = recordRunResult(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(firstRunResultSn), WindOperatorFactory.system());
        String nextRunResultSn = recordRunResult(false, "recon_batch_mvp_001_rerun_002", RERUN_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(nextRunResultSn),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错已关闭");
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
                .setEvidenceRef("processor-file-digest-001")
                .setDescription("外部 processor 文件金额与内部账本金额不一致");
    }

    private LinkReconciliationDifferenceAdjustmentRequest minimumAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setActionType(ReconciliationDifferenceActionType.ADJUST)
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setIdempotencyKey("idem-recon-adjust-001")
                .setOriginalFactRef("external-balance-anomaly:issuer-ledger-001")
                .setAdjustmentTransactionSn(FUNDS_TRANSACTION_SN)
                .setApprovalRef("approval-recon-adjust-001")
                .setEvidenceRef("adjustment-evidence-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private LinkReconciliationDifferenceAdjustmentRequest adjustmentRequestWithoutWhitelistActionContext() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setAdjustmentTransactionSn(FUNDS_TRANSACTION_SN)
                .setApprovalRef("approval-recon-adjust-001")
                .setEvidenceRef("adjustment-evidence-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private RecordReconciliationDifferenceRerunRequest minimumRerunRequest(String reconciliationRunResultSn) {
        return new RecordReconciliationDifferenceRerunRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setReconciliationRunResultSn(reconciliationRunResultSn);
    }

    private String recordRunResult(boolean balanced, String batchSn, String previousBatchSn) {
        String referenceSourceRef = "internal:" + batchSn;
        String comparisonSourceRef = "external:" + batchSn;
        ReconciliationMatchResultItem matchResult = new ReconciliationMatchResultItem()
                .setReferenceSourceRef(referenceSourceRef)
                .setComparisonSourceRef(comparisonSourceRef)
                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                .setMatchStrength(balanced ? ReconciliationMatchStrength.EXACT_MATCH
                        : ReconciliationMatchStrength.UNMATCHED)
                .setEvidenceRef("report:" + batchSn + "#line-1");
        if (!balanced) {
            matchResult.setDifferenceType(ReconciliationDifferenceType.STATUS_MISMATCH)
                    .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                    .setDifferenceAmount(0L);
        }
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, batchSn, ReconciliationGateObjectType.CLEARING,
                "reconciliation-difference-001", "recon-rule-v1", "report:" + batchSn,
                referenceSourceRef, comparisonSourceRef, previousBatchSn);
        return reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batchSn)
                .setMatchResults(List.of(matchResult)), WindOperatorFactory.system()).getSn();
    }

    private Integer countDifferenceRows(String differenceSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_reconciliation_difference
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, Integer.class, TENANT_ID, differenceSn);
    }

    @Configuration
    @Import({
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class
    })
    static class Config {
    }
}
