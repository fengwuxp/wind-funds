package com.wind.funds.reconciliation.application.difference.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceState;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.Size;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationDifferenceApplicationServiceTests extends AbstractFundsServiceTest {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String reconciliationMatchResultSn;

    @BeforeEach
    void cleanReconciliationDifference() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        reconciliationMatchResultSn = recordInitialDifferenceRunResult();
    }

    @Test
    void testCreateDifferenceShouldRejectTenantDifferentFromCurrentContext() {
        CreateReconciliationDifferenceRequest request = new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
        assertThat(countDifferenceRows()).isZero();
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

        assertThat(result.getDifferenceSn()).startsWith("RDF");
        assertThat(result.getReconciliationBatchSn()).isEqualTo(RECONCILIATION_BATCH_SN);
        assertThat(result.getReconciliationMatchResultSn()).isEqualTo(reconciliationMatchResultSn);
        assertThat(result.getDifferenceType()).isEqualTo(ReconciliationDifferenceType.AMOUNT_MISMATCH);
        assertThat(result.getScopeIdentity().getValue()).contains("reconciliation-difference-001");
        assertThat(result.getCurrentLineageRef()).isNotBlank();
        assertThat(result.getState()).isEqualTo(ReconciliationDifferenceState.BLOCKED);
        assertThat(result.getRerunCount()).isZero();
        assertThat(result.getCreatedBy()).isEqualTo(WindOperatorFactory.system().getOperatorAsText());
        assertThat(replay.getId()).isEqualTo(result.getId());
        assertThat(countDifferenceRows()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testCreateDifferenceShouldRejectLateMaterializationAfterLineageAdvances() throws Exception {
        CountDownLatch lineageLocked = new CountDownLatch(1);
        CountDownLatch allowLineageCommit = new CountDownLatch(1);
        CountDownLatch createStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> lineageAdvance = executor.submit(() -> {
                TenantContextHolder.setTenantId(TENANT_ID);
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        jdbcTemplate.update("""
                                UPDATE t_reconciliation_batch_lineage
                                SET current_batch_sn = 'recon_batch_mvp_001_rerun_committed'
                                WHERE tenant_id = ?
                                  AND scope_owner_namespace = 'test.scope'
                                  AND scope_identity_value = 'CLEARING_CONFIRM_ITEM:reconciliation-difference-001'
                                """, TENANT_ID);
                        lineageLocked.countDown();
                        await(allowLineageCommit);
                    });
                } finally {
                    TenantContextHolder.clear();
                }
            });
            assertThat(lineageLocked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<DifferenceCreateAttempt> lateCreate = executor.submit(() -> {
                TenantContextHolder.setTenantId(TENANT_ID);
                createStarted.countDown();
                try {
                    ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.createDifference(
                            minimumCreateRequest(), WindOperatorFactory.system());
                    return new DifferenceCreateAttempt(true, result.getDifferenceSn(), null);
                } catch (RuntimeException exception) {
                    return new DifferenceCreateAttempt(false, null, exception.getMessage());
                } finally {
                    TenantContextHolder.clear();
                }
            });
            assertThat(createStarted.await(10, TimeUnit.SECONDS)).isTrue();
            allowLineageCommit.countDown();

            lineageAdvance.get(10, TimeUnit.SECONDS);
            DifferenceCreateAttempt result = lateCreate.get(10, TimeUnit.SECONDS);
            assertThat(result.succeeded()).isFalse();
            assertThat(result.message()).contains("不是当前批次血缘头");
            assertThat(countDifferenceRows()).isZero();
        } finally {
            allowLineageCommit.countDown();
            executor.shutdownNow();
        }
    }

    /**
     * 场景：清算或结算消费方登记一个对象级阻断差错。
     * 输入：blockingObjectType 和 blockingObjectSn。
     * 输出：差错结果回传阻断对象字段，重复提交保持幂等。
     * 红线：对象级字段只是差错命中键，不生成清算、结算、route、posting 或账本事实。
     */
    @Test
    void testCreateDifferenceShouldKeepObjectScopeInResultAndIdempotentReplay() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        CreateReconciliationDifferenceRequest request = minimumCreateRequest();
        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.createDifference(
                request, WindOperatorFactory.system());
        ReconciliationDifferenceDTO replay = reconciliationDifferenceApplicationService.createDifference(
                request, WindOperatorFactory.system());

        assertThat(result.getScopeIdentity().getValue()).contains("reconciliation-difference-001");
        assertThat(replay.getId()).isEqualTo(result.getId());
        assertThat(countDifferenceRows()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方使用未持久化的逐笔匹配结果登记 Gate 阻断差错。
     * 输入：reconciliationMatchResultSn 指向不存在的 ReconciliationMatchResult。
     * 输出：快速失败，不写入差错事实。
     * 红线：差错不得成为调用方可自报批次、金额、规则或证据的第二真相源。
     */
    @Test
    void testCreateDifferenceShouldRejectUnpersistedMatchResult() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setReconciliationMatchResultSn("RMR_missing_001"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账匹配结果不存在");

        assertThat(countDifferenceRows()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：逐笔匹配结论是状态不一致，不存在可表达的金额差。
     * 结果：差错保留明确类型，但不伪造币种或零金额。
     */
    @Test
    void testCreateDifferenceShouldKeepNonAmountDifferenceFieldsEmpty() {
        String batchSn = "recon_batch_status_mismatch_001";
        executeStrictExactStatusMismatch(batchSn);
        String matchResultSn = jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_match_result
                WHERE tenant_id = ?
                  AND reconciliation_batch_sn = ?
                """, String.class, TENANT_ID, batchSn);

        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setReconciliationMatchResultSn(matchResultSn),
                WindOperatorFactory.system());

        assertThat(result.getDifferenceType()).isEqualTo(ReconciliationDifferenceType.STATUS_MISMATCH);
        assertThat(result.getCurrency()).isNull();
        assertThat(result.getDifferenceAmount()).isNull();
    }

    /**
     * 场景：匹配结论使用合法的长证据引用并物化 Gate 差错。
     * 结果：差错完整保留证据引用，不因匹配表与差错表字段宽度不一致而失败。
     */
    @Test
    void testCreateDifferenceShouldPreserveLongMatchEvidenceRef() {
        String batchSn = "recon_batch_long_evidence_001";
        String evidenceRef = "report:" + "e".repeat(193);
        executeStrictExact(false, batchSn, null, evidenceRef);
        String matchResultSn = jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_match_result
                WHERE tenant_id = ?
                  AND reconciliation_batch_sn = ?
                """, String.class, TENANT_ID, batchSn);

        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setReconciliationMatchResultSn(matchResultSn),
                WindOperatorFactory.system());

        assertThat(result.getEvidenceRef()).isEqualTo(evidenceRef);
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
        String runResultSn = executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        ReconciliationDifferenceDTO closed = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system());
        ReconciliationDifferenceDTO closedReplay = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system());

        assertThat(linked.getState()).isEqualTo(ReconciliationDifferenceState.ADJUSTING);
        assertThat(linked.getActionType()).isEqualTo(ReconciliationDifferenceActionType.ADJUST);
        assertThat(linked.getAdjustmentSn()).isEqualTo(ADJUSTMENT_SN);
        assertThat(linked.getAdjustmentIdempotencyKey()).isEqualTo("idem-recon-adjust-001");
        assertThat(linked.getOriginalFactRef()).isEqualTo("external-balance-anomaly:issuer-ledger-001");
        assertThat(linked.getAdjustmentTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(linkedReplay.getId()).isEqualTo(linked.getId());
        assertThat(countDifferenceActionRows()).isOne();
        assertThat(closed.getState()).isEqualTo(ReconciliationDifferenceState.RESOLVED);
        assertThat(closed.getLastRerunSn()).isEqualTo(runResultSn);
        assertThat(closed.getRerunCount()).isOne();
        assertThat(closedReplay.getRerunCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：差错依赖的来源或匹配证据已被替代批次确认无效。
     * 结果：失效差错不再进入调账、冲正、挂账或核销等真实差错处置链。
     */
    @Test
    void testAdjustmentLinkShouldRejectInvalidatedDifference() {
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        invalidateDifference(difference.getDifferenceSn());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("对账差错依赖证据已失效")
                .hasMessageContaining(difference.getDifferenceSn());

        assertThat(countDifferenceActionRows()).isZero();
    }

    /**
     * 场景：失效差错对应的旧处置流程尝试回链后继重跑结果。
     * 结果：拒绝回链，新批次发现的差异必须形成新的匹配结果和差错事实。
     */
    @Test
    void testRerunResultShouldRejectInvalidatedDifference() {
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        invalidateDifference(difference.getDifferenceSn());
        String runResultSn = executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("对账差错依赖证据已失效")
                .hasMessageContaining(difference.getDifferenceSn());
    }

    @Test
    void testCreateDifferenceShouldRejectValuesWiderThanPersistenceContract() {
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setReconciliationMatchResultSn(
                        "x".repeat(CreateReconciliationDifferenceRequest.MAX_MATCH_RESULT_SN_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("匹配结果流水号长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setResponsiblePartyRef(
                        "x".repeat(CreateReconciliationDifferenceRequest.MAX_RESPONSIBLE_PARTY_REF_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("责任方引用长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setDescription(
                        "x".repeat(CreateReconciliationDifferenceRequest.MAX_DESCRIPTION_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("说明长度不能超过");

        assertThat(countDifferenceRows()).isZero();
    }

    @Test
    void testAdjustmentLinkShouldRejectValuesWiderThanPersistenceContract() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setDifferenceSn(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_DIFFERENCE_SN_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("差错流水号长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setAdjustmentSn(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_ADJUSTMENT_SN_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("处理动作号长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setIdempotencyKey(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_IDEMPOTENCY_KEY_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("幂等键长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setOriginalFactRef(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_ORIGINAL_FACT_REF_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("原始事实引用长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setAdjustmentTransactionSn(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_TRANSACTION_SN_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("资金交易流水号长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setApprovalRef(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_APPROVAL_REF_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("审批引用长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setEvidenceRef(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_EVIDENCE_REF_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("证据引用长度不能超过");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setReason(
                        "x".repeat(LinkReconciliationDifferenceAdjustmentRequest.MAX_REASON_LENGTH + 1)),
                WindOperatorFactory.system()))
                .hasMessageContaining("处理原因长度不能超过");

        assertThat(countDifferenceActionRows()).isZero();
        assertThat(differenceStatus(requiredDifferenceSn())).isEqualTo(ReconciliationDifferenceState.BLOCKED.name());
    }

    @Test
    void testRequestPersistenceWidthsShouldBeDeclaredForBeanValidation() throws NoSuchFieldException {
        assertSizeConstraint(CreateReconciliationDifferenceRequest.class, "reconciliationMatchResultSn",
                CreateReconciliationDifferenceRequest.MAX_MATCH_RESULT_SN_LENGTH);
        assertSizeConstraint(CreateReconciliationDifferenceRequest.class, "responsiblePartyRef",
                CreateReconciliationDifferenceRequest.MAX_RESPONSIBLE_PARTY_REF_LENGTH);
        assertSizeConstraint(CreateReconciliationDifferenceRequest.class, "description",
                CreateReconciliationDifferenceRequest.MAX_DESCRIPTION_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "differenceSn",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_DIFFERENCE_SN_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "adjustmentSn",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_ADJUSTMENT_SN_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "idempotencyKey",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_IDEMPOTENCY_KEY_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "originalFactRef",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_ORIGINAL_FACT_REF_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "adjustmentTransactionSn",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_TRANSACTION_SN_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "approvalRef",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_APPROVAL_REF_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "evidenceRef",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_EVIDENCE_REF_LENGTH);
        assertSizeConstraint(LinkReconciliationDifferenceAdjustmentRequest.class, "reason",
                LinkReconciliationDifferenceAdjustmentRequest.MAX_REASON_LENGTH);
    }

    /**
     * 场景：第一次处理后重新对账仍未对平，业务确认需要执行第二个处理动作。
     * 结果：第二个动作追加为新事实，差错主表只投影最新动作并重新进入 ADJUSTING。
     * 红线：不得覆盖或丢失第一次处理动作，也不得在上一次动作尚未重跑时并行追加。
     */
    @Test
    void testAdjustmentLinkShouldAppendNextActionAfterUnbalancedRerun() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        String unbalancedRunResultSn = executeStrictExact(false, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        ReconciliationDifferenceDTO rerun = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(unbalancedRunResultSn), WindOperatorFactory.system());

        ReconciliationDifferenceDTO linked = reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest()
                        .setActionType(ReconciliationDifferenceActionType.RECOVER)
                        .setAdjustmentSn("recovery_recon_002")
                        .setIdempotencyKey("idem-recon-recovery-002")
                        .setAdjustmentTransactionSn(null)
                        .setApprovalRef("approval-recon-recovery-002")
                        .setEvidenceRef("recovery-evidence-002")
                        .setReason("首次调账后仍不平，已由上层发起追偿"),
                WindOperatorFactory.system());

        assertThat(rerun.getState()).isEqualTo(ReconciliationDifferenceState.RECONCILING);
        assertThat(linked.getState()).isEqualTo(ReconciliationDifferenceState.ADJUSTING);
        assertThat(linked.getActionType()).isEqualTo(ReconciliationDifferenceActionType.RECOVER);
        assertThat(linked.getAdjustmentSn()).isEqualTo("recovery_recon_002");
        assertThat(countDifferenceActionRows()).isEqualTo(2);
    }

    /**
     * 场景：重跑仍未对平，但本轮差异来自另一组来源记录。
     * 结果：拒绝把新差异回链到旧差错，避免不同业务事实共用一条处置生命周期。
     */
    @Test
    void testRerunShouldRejectDifferentDifferenceIdentity() {
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = executeStrictExact(false, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN,
                "report:different#line-1", "internal:different", "external:different", "different-comparison");

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), WindOperatorFactory.system()))
                .hasMessageContaining("差错身份");

        assertThat(differenceStatus(difference.getDifferenceSn()))
                .isEqualTo(ReconciliationDifferenceState.ADJUSTING.name());
    }

    /**
     * 场景：调用方先生成了后继重跑结果，之后才尝试回链本轮处理动作。
     * 结果：拒绝回链，差错仍保持阻断且不新增处理动作。
     * 红线：处理动作必须发生在对应重跑批次创建之前，不能用动作之前生成的旧结果关闭差错。
     */
    @Test
    void testAdjustmentLinkShouldRejectWhenRerunBatchAlreadyExists() {
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("处理动作必须在重跑批次创建前回链");

        assertThat(differenceStatus(difference.getDifferenceSn()))
                .isEqualTo(ReconciliationDifferenceState.BLOCKED.name());
        assertThat(countDifferenceActionRows()).isZero();
    }

    /**
     * 场景：调用方复用既有动作幂等键，却更换了处理动作单号。
     * 结果：拒绝把同一业务动作登记为两个事实。
     */
    @Test
    void testAdjustmentLinkShouldRejectIdempotencyKeyReusedByAnotherAction() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest().setAdjustmentSn("balance_adjust_recon_changed"),
                WindOperatorFactory.system()))
                .hasMessageContaining("处理动作幂等键已被其他动作使用");
        assertThat(countDifferenceActionRows()).isOne();
    }

    /**
     * 场景：运营只回链调账单号、审批和证据，但没有声明处理动作分类和原始事实引用。
     * 输入：差错流水号、调账单号、资金交易流水、审批、凭证和原因。
     * 输出：拒绝回链处理结果。
     * 红线：差错处理不能退化为任意单号备注；必须能说明处理结果分类和被处理的原始事实。
     */
    @Test
    void testAdjustmentLinkShouldRejectMissingActionResultContext() {
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                adjustmentRequestWithoutActionResultContext(), WindOperatorFactory.system()))
                .hasMessageContaining("对账差错处理动作类型不能为空");
    }

    /**
     * 场景：差错处理回链请求完整，但调用方未提供操作人。
     * 结果：以稳定业务异常快速失败，不能暴露空指针或改变差错状态。
     */
    @Test
    void testAdjustmentLinkShouldRejectMissingOperator() {
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), null))
                .hasMessageContaining("对账差错处理回链操作人不能为空");

        assertThat(differenceStatus(difference.getDifferenceSn()))
                .isEqualTo(ReconciliationDifferenceState.BLOCKED.name());
    }

    /**
     * 场景：同一逐笔匹配结果被重复提交，但责任归属或说明被改写。
     * 输入：第一次登记 processor 责任，第二次改为 merchant 或修改说明。
     * 输出：拒绝第二次提交，差错单仍只有一条。
     * 红线：逐笔匹配结果业务唯一键不能静默覆盖上层确认的责任归属。
     */
    @Test
    void testCreateDifferenceShouldRejectIdempotentConflict() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setResponsiblePartyRef("merchant:unexpected"), WindOperatorFactory.system()))
                .hasMessageContaining("对账差错幂等请求责任方不一致");
        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest().setDescription("changed-description"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错幂等请求说明不一致");

        assertThat(countDifferenceRows()).isOne();
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
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        String runResultSn = executeStrictExact(true, "recon_batch_mvp_001_rerun_002", RECONCILIATION_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn).setDifferenceSn(difference.getDifferenceSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("差错关闭必须先关联处理动作或调账结果");
    }

    /**
     * 场景：重跑结果已经持久化，但调用方未提供记录操作人。
     * 结果：以稳定业务异常快速失败，差错保持处理中且不消费该重跑结果。
     */
    @Test
    void testRerunShouldRejectMissingOperator() {
        ReconciliationDifferenceDTO difference = reconciliationDifferenceApplicationService.createDifference(
                minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(
                minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = executeStrictExact(true, "recon_batch_mvp_001_rerun_missing_operator",
                RECONCILIATION_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(runResultSn), null))
                .hasMessageContaining("对账差错重跑操作人不能为空");

        assertThat(differenceStatus(difference.getDifferenceSn()))
                .isEqualTo(ReconciliationDifferenceState.ADJUSTING.name());
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
        assertThat(unchanged.getState()).isEqualTo(ReconciliationDifferenceState.ADJUSTING);
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
        String runResultSn = executeStrictExact(true, RERUN_BATCH_SN, "unrelated_reconciliation_batch");

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
        String runResultSn = executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, "recon_batch_mvp_001_rerun_002", "CLEARING_CONFIRM_ITEM",
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
        executeStrictExact(false, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        String latestBatchSn = "recon_batch_mvp_001_rerun_002";
        String latestRunResultSn = executeStrictExact(true, latestBatchSn, RERUN_BATCH_SN);

        ReconciliationDifferenceDTO result = reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(latestRunResultSn), WindOperatorFactory.system());

        assertThat(result.getState()).isEqualTo(ReconciliationDifferenceState.RESOLVED);
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
        String runResultSn = executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        jdbcTemplate.update("""
                        UPDATE t_reconciliation_run_result
                        SET scope_identity_value = ?
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
        reconciliationDifferenceApplicationService.createDifference(minimumCreateRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(minimumAdjustmentRequest(), WindOperatorFactory.system());
        String runResultSn = executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch
                SET scope_identity_value = 'CLEARING_CONFIRM_ITEM:clearing-candidate-other'
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, RERUN_BATCH_SN);
        jdbcTemplate.update("""
                UPDATE t_reconciliation_run_result
                SET scope_identity_value = 'CLEARING_CONFIRM_ITEM:clearing-candidate-other'
                WHERE tenant_id = ? AND sn = ?
                """, TENANT_ID, runResultSn);

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
    void testAdjustmentLinkShouldRejectActionResultContextConflict() {
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
        String firstRunResultSn = executeStrictExact(true, RERUN_BATCH_SN, RECONCILIATION_BATCH_SN);
        reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(firstRunResultSn), WindOperatorFactory.system());
        String nextRunResultSn = executeStrictExact(false, "recon_batch_mvp_001_rerun_002", RERUN_BATCH_SN);

        assertThatThrownBy(() -> reconciliationDifferenceApplicationService.recordRerunResult(
                minimumRerunRequest(nextRunResultSn),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账差错已关闭");
    }

    private CreateReconciliationDifferenceRequest minimumCreateRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationMatchResultSn(reconciliationMatchResultSn)
                .setResponsiblePartyRef("processor:issuer-ledger")
                .setDescription("外部 processor 文件金额与内部账本金额不一致");
    }

    private LinkReconciliationDifferenceAdjustmentRequest minimumAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn())
                .setActionType(ReconciliationDifferenceActionType.ADJUST)
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setIdempotencyKey("idem-recon-adjust-001")
                .setOriginalFactRef("external-balance-anomaly:issuer-ledger-001")
                .setAdjustmentTransactionSn(FUNDS_TRANSACTION_SN)
                .setApprovalRef("approval-recon-adjust-001")
                .setEvidenceRef("adjustment-evidence-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private LinkReconciliationDifferenceAdjustmentRequest adjustmentRequestWithoutActionResultContext() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn())
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setAdjustmentTransactionSn(FUNDS_TRANSACTION_SN)
                .setApprovalRef("approval-recon-adjust-001")
                .setEvidenceRef("adjustment-evidence-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private RecordReconciliationDifferenceRerunRequest minimumRerunRequest(String reconciliationRunResultSn) {
        return new RecordReconciliationDifferenceRerunRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(requiredDifferenceSn())
                .setReconciliationRunResultSn(reconciliationRunResultSn);
    }

    private String recordInitialDifferenceRunResult() {
        String comparisonSourceRef = "external:" + RECONCILIATION_BATCH_SN;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, RECONCILIATION_BATCH_SN, "CLEARING_CONFIRM_ITEM",
                "reconciliation-difference-001", "recon-rule-v1", "processor-file-digest-001",
                SOURCE_RECORD_SN, comparisonSourceRef, null, 2L, "CONFIRMED");
        reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(RECONCILIATION_BATCH_SN), WindOperatorFactory.system());
        return jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_match_result
                WHERE tenant_id = ?
                  AND reconciliation_batch_sn = ?
                  AND result_kind <> 'MATCHED'
                """, String.class, TENANT_ID, RECONCILIATION_BATCH_SN);
    }

    private String executeStrictExact(boolean balanced, String batchSn, String previousBatchSn) {
        return executeStrictExact(balanced, batchSn, previousBatchSn, "report:" + batchSn + "#line-1");
    }

    private String executeStrictExactStatusMismatch(String batchSn) {
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, batchSn, "CLEARING_CONFIRM_ITEM",
                "reconciliation-difference-001", "recon-rule-v1", "report:" + batchSn + "#line-1",
                SOURCE_RECORD_SN, "external:" + RECONCILIATION_BATCH_SN, null, 1L, "SETTLED");
        return reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batchSn), WindOperatorFactory.system()).getSn();
    }

    private String executeStrictExact(boolean balanced,
                                   String batchSn,
                                   String previousBatchSn,
                                   String evidenceRef) {
        return executeStrictExact(balanced, batchSn, previousBatchSn, evidenceRef,
                SOURCE_RECORD_SN, "external:" + RECONCILIATION_BATCH_SN);
    }

    private String executeStrictExact(boolean balanced,
                                   String batchSn,
                                   String previousBatchSn,
                                   String evidenceRef,
                                   String referenceSourceRef,
                                   String comparisonSourceRef) {
        return executeStrictExact(balanced, batchSn, previousBatchSn, evidenceRef,
                referenceSourceRef, comparisonSourceRef, null);
    }

    private String executeStrictExact(boolean balanced,
                                   String batchSn,
                                   String previousBatchSn,
                                   String evidenceRef,
                                   String referenceSourceRef,
                                   String comparisonSourceRef,
                                   String comparisonIdentityValue) {
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, batchSn, "CLEARING_CONFIRM_ITEM",
                "reconciliation-difference-001", "recon-rule-v1", evidenceRef,
                referenceSourceRef, comparisonSourceRef, previousBatchSn,
                balanced ? 1L : 2L, "CONFIRMED", comparisonIdentityValue);
        return reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batchSn), WindOperatorFactory.system()).getSn();
    }

    private Integer countDifferenceRows() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_reconciliation_difference
                WHERE tenant_id = ?
                  AND reconciliation_match_result_sn = ?
                """, Integer.class, TENANT_ID, reconciliationMatchResultSn);
    }

    private Integer countDifferenceActionRows() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_reconciliation_difference_action
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, Integer.class, TENANT_ID, requiredDifferenceSn());
    }

    private String differenceStatus(String differenceSn) {
        return jdbcTemplate.queryForObject("""
                SELECT status
                FROM t_reconciliation_difference
                WHERE tenant_id = ?
                  AND difference_sn = ?
                """, String.class, TENANT_ID, differenceSn);
    }

    private void invalidateDifference(String differenceSn) {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'INVALIDATED'
                WHERE tenant_id = ? AND difference_sn = ?
                """, TENANT_ID, differenceSn);
    }

    private String requiredDifferenceSn() {
        return jdbcTemplate.queryForObject("""
                SELECT difference_sn
                FROM t_reconciliation_difference
                WHERE tenant_id = ?
                  AND reconciliation_match_result_sn = ?
                """, String.class, TENANT_ID, reconciliationMatchResultSn);
    }

    private static void assertSizeConstraint(Class<?> requestType,
                                             String fieldName,
                                             int expectedMax) throws NoSuchFieldException {
        Size constraint = requestType.getDeclaredField(fieldName).getAnnotation(Size.class);
        assertThat(constraint).isNotNull();
        assertThat(constraint.max()).isEqualTo(expectedMax);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待并发对账测试条件超时");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待并发对账测试条件被中断", exception);
        }
    }

    @Configuration
    @Import({
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class
    })
    static class Config {
    }

    private record DifferenceCreateAttempt(boolean succeeded, String differenceSn, String message) {
    }
}
