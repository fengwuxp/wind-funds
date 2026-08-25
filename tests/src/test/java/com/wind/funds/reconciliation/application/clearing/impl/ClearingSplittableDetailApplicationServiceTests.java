package com.wind.funds.reconciliation.application.clearing.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerPostingScope;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.reconciliation.application.clearing.ClearingSplittableDetailApplicationService;
import com.wind.funds.reconciliation.application.clearing.impl.ClearingSplittableDetailApplicationServiceImpl;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ClearingSplittableAdmissionResult;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 可清分明细准入服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ClearingSplittableDetailApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClearingSplittableDetailApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String FUNDS_TRANSACTION_SN = "clearing_funds_tx_001";

    private static final String FUNDS_TRANSACTION_DETAIL_SN = "clearing_funds_detail_001";

    private static final String PAYER_DETAIL_SN = "clearing_funds_payer_detail_001";

    private static final String LEDGER_TRANSACTION_SN = "clearing_ledger_tx_001";

    private static final String POSTING_PLAN_SN = "clearing_posting_plan_001";

    private static final String LEDGER_ENTRY_SN = "clearing_ledger_entry_001";

    private static final String PAYER_LEDGER_ENTRY_SN = "clearing_payer_ledger_entry_001";

    private static final String DIFFERENCE_BATCH_SN = "clearing_recon_difference_batch_001";

    private static final String BUSINESS_SCENE = "MERCHANT_PAY";

    private static final String BUSINESS_SN = "merchant_pay_001";

    private static final String OTHER_BUSINESS_SN = "merchant_pay_002";

    private static final String MERCHANT_SUBJECT_ID = "merchant_settlement_001";

    private static final String PAYER_SUBJECT_ID = "payer_funding_001";

    private static final long AMOUNT = 9800L;

    private static final LocalDateTime LEDGER_TRANSACTION_TIME = LocalDateTime.of(2026, 7, 21, 10, 0);

    private static final String LEDGER_TRANSACTION_DIGEST_DOMAIN = "ledger.persisted-transaction.v1";

    private static final String LEDGER_POSTING_PLAN_DIGEST_DOMAIN = "ledger.persisted-plan.v1";

    private static final String LEDGER_ENTRY_DIGEST_DOMAIN = "ledger.persisted-entry.v1";

    @Autowired
    private ClearingSplittableDetailApplicationService clearingSplittableDetailApplicationService;

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String reconciliationRunResultSn;

    private String reconciliationMatchResultSn;

    @BeforeEach
    void prepareSourceFacts() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM t_clearing_splittable_detail");
        deleteSourceFacts();
        insertSourceFacts(FundsTransactionState.CLOSED, FundsTransactionDetailState.SUCCEEDED,
                LedgerSubjectCode.CLEARING,
                routeSnapshot(false),
                0L, 0L);
        reconciliationRunResultSn = recordBalancedRunResult();
    }

    @Test
    void testIdentifyShouldRejectTenantDifferentFromCurrentContext() {
        IdentifyClearingSplittableDetailRequest request = new IdentifyClearingSplittableDetailRequest()
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
    }

    @Test
    void testIdentifyShouldRejectNonFundsActionIdentityWithoutSideEffects() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest().setSourceActionFactRef(new StableIdentity()
                        .setOwnerNamespace("merchant")
                        .setValue(actionIdentity())), WindOperatorFactory.system()))
                .hasMessageContaining("ownerNamespace 必须为 funds");

        assertThat(detailCount()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testIdentifyShouldSelectPayeeFromCompleteFeeBearingGroup() {
        insertFeeFacts(100L);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.SPLIT_READY);
        assertThat(result.getFundsTransactionDetailSn()).isEqualTo(FUNDS_TRANSACTION_DETAIL_SN);
        assertThat(result.getLedgerEntrySn()).isEqualTo(LEDGER_ENTRY_SN);
        assertThat(result.getSubjectId()).isEqualTo(MERCHANT_SUBJECT_ID);
        assertThat(result.getAmount()).isEqualTo(AMOUNT);
    }

    @Test
    void testIdentifyShouldFailClosedWhenRecordedSiblingIsMissing() {
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE sn = ?", PAYER_DETAIL_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    @Test
    void testIdentifyShouldFailClosedWhenSiblingLedgerReferencesDiffer() {
        jdbcTemplate.update("UPDATE t_funds_transaction_detail SET ledger_transaction_sn = ? WHERE sn = ?",
                "different-ledger-transaction", PAYER_DETAIL_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 场景：可清分来源的 Ledger transaction/plan/entry 任一层摘要被改写。
     * 输入：完整 ActionFact、通过的 Gate 来源与 canonical Ledger fixture，仅篡改目标层 sha256。
     * 输出：identify 在候选和 Gate evidence 消费前拒绝，Ledger/Balance 事实不变。
     * 红线：Clearing 不得接纳、修复或复制验证失败的 Ledger aggregate。
     */
    @ParameterizedTest(name = "tampered persisted {0} digest")
    @ValueSource(strings = {"TRANSACTION", "POSTING_PLAN", "LEDGER_ENTRY"})
    void testIdentifyShouldFailClosedWhenPersistedLedgerDigestIsTampered(String layer) {
        String targetSn = tamperPersistedDigest(layer);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        Throwable failure = catchThrowable(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()));
        assertThat(failure)
                .as("clearing must reject tampered " + stableLayerLabel(layer)
                        + " ledger digest before side effects")
                .isNotNull();
        assertThat(failure).hasMessageContaining(integrityLayerName(layer)).hasMessageContaining(targetSn);

        assertThat(detailCount()).isZero();
        assertThat(gateEvidenceCount()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private String tamperPersistedDigest(String layer) {
        return switch (layer) {
            case "TRANSACTION" -> {
                jdbcTemplate.update("UPDATE t_ledger_transaction SET sha256 = ? WHERE sn = ?",
                        "tampered-transaction-digest", LEDGER_TRANSACTION_SN);
                yield LEDGER_TRANSACTION_SN;
            }
            case "POSTING_PLAN" -> {
                jdbcTemplate.update("UPDATE t_ledger_posting_plan SET sha256 = ? WHERE sn = ?",
                        "tampered-posting-plan-digest", POSTING_PLAN_SN);
                yield POSTING_PLAN_SN;
            }
            case "LEDGER_ENTRY" -> {
                jdbcTemplate.update("UPDATE t_ledger_entry SET sha256 = ? WHERE sn = ?",
                        "tampered-ledger-entry-digest", LEDGER_ENTRY_SN);
                yield LEDGER_ENTRY_SN;
            }
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        };
    }

    private String stableLayerLabel(String layer) {
        return switch (layer) {
            case "TRANSACTION" -> "transaction";
            case "POSTING_PLAN" -> "plan";
            case "LEDGER_ENTRY" -> "entry";
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        };
    }

    private String integrityLayerName(String layer) {
        return switch (layer) {
            case "TRANSACTION" -> "transaction";
            case "POSTING_PLAN" -> "posting plan";
            case "LEDGER_ENTRY" -> "ledger entry";
            default -> throw new IllegalArgumentException("Unsupported persisted digest layer: " + layer);
        };
    }

    /**
     * 场景：商户付款已经成功，完整 route/posting/ledger 事实命中商户 CLEARING 分录。
     * 结果：生成一条 SPLIT_READY 明细并保留来源、规则和对账结论。
     * 红线：识别可清分明细不得创建或修改任何资金、route、posting 或账本事实。
     */
    @Test
    void testIdentifyShouldCreateSplitReadyDetailFromPostedClearingFact() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.SPLIT_READY);
        assertThat(result.getExclusionReason()).isNull();
        assertThat(result.getFundsTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(result.getFundsTransactionDetailSn()).isEqualTo(FUNDS_TRANSACTION_DETAIL_SN);
        assertThat(result.getLedgerTransactionSn()).isEqualTo(LEDGER_TRANSACTION_SN);
        assertThat(result.getPostingPlanSn()).isEqualTo(POSTING_PLAN_SN);
        assertThat(result.getLedgerEntrySn()).isEqualTo(LEDGER_ENTRY_SN);
        assertThat(result.getSubjectType()).isEqualTo("FUNDING_ACCOUNT");
        assertThat(result.getSubjectId()).isEqualTo(MERCHANT_SUBJECT_ID);
        assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(result.getAmount()).isEqualTo(AMOUNT);
        assertThat(result.getReconciliationDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.PASSED);
        assertThat(result.getGateEvidenceRef()).isNotBlank();
        assertThat(result.getReconciliationEvidenceRefs())
                .contains("report:merchant-clearing-recon-run-001", result.getGateEvidenceRef());
        assertThat(result.getSourceDigest()).hasSize(64);
        assertThat(detailCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：来源资金和账本事实完整，但没有可读取的正向对账运行结果。
     * 结果：返回临时阻断结论但不占用可清分明细唯一键，不能用“没有登记差错”替代清分前对账。
     */
    @Test
    void testIdentifyShouldExcludeWhenReconciliationRunResultIsMissing() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result WHERE sn = ?", reconciliationRunResultSn);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED);
        assertThat(result.getReconciliationDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getSn()).isNull();
        assertThat(detailCount()).isZero();
    }

    /**
     * 场景：同一来源分录和同一规则被重复扫描。
     * 结果：返回第一次识别结果且数据库仍只有一条记录。
     * 红线：重复扫描不得重复入池，也不得产生账务副作用。
     */
    @Test
    void testIdentifyShouldReuseExistingDetailForSameSourceDigest() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO first = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());
        ClearingSplittableDetailDTO replay = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(replay.getSn()).isEqualTo(first.getSn());
        assertThat(replay.getSourceDigest()).isEqualTo(first.getSourceDigest());
        assertThat(detailCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：可清分候选形成后，来源交易又成功退款并推进乐观锁版本。
     * 结果：相同来源分录重放时拒绝复用旧候选。
     * 红线：SPLIT_READY 不是清分执行授权，最终清分必须以冻结版本识别来源变化。
     */
    @Test
    void testIdentifyShouldRejectReplayWhenSourceTransactionVersionAdvances() {
        clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());
        jdbcTemplate.update("""
                UPDATE t_funds_transaction
                SET refunded_amount = ?, version = version + 1
                WHERE sn = ?
                """, 100L, FUNDS_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isOne();
    }

    @Test
    void testIdentifyShouldRejectReplayWhenSourceBusinessIdentityDrifts() {
        clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());
        jdbcTemplate.update("UPDATE t_funds_transaction_detail SET business_sn = ? WHERE sn = ?",
                OTHER_BUSINESS_SN, FUNDS_TRANSACTION_DETAIL_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isOne();
    }

    @Test
    void testIdentifyShouldRejectReplayWhenSourceLifecycleDrifts() {
        clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());
        jdbcTemplate.update("UPDATE t_funds_transaction SET transaction_mode = ? WHERE sn = ?",
                FundsTransactionMode.AUTHORIZATION.name(), FUNDS_TRANSACTION_SN);
        jdbcTemplate.update("UPDATE t_ledger_transaction SET instruction_type = ? WHERE sn = ?",
                FundsInstructionType.AUTHORIZATION_TRANSACTION.name(), LEDGER_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isOne();
    }

    /**
     * 场景：交易已成功但缺少 route snapshot。
     * 结果：来源 ActionFact 无法成立，在候选持久化前拒绝。
     * 红线：来源不完整不得静默放行或从余额反推明细。
     */
    @Test
    void testIdentifyShouldExcludeWhenRouteSnapshotIsMissing() {
        jdbcTemplate.update("""
                UPDATE t_funds_transaction
                SET status = ?, refunded_amount = ?, route_snapshot = NULL
                WHERE sn = ?
                """, FundsTransactionState.OPEN.name(), 100L, FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：来源事实完整，但该交易明细存在未闭环的清分前重大对账差错。
     * 结果：返回临时 EXCLUDED 和对账阻断证据但不落候选；阻断解除后同一分录可重新识别。
     * 红线：重大差错不得生成可清分结果，也不得修改历史资金事实。
     */
    @Test
    void testIdentifyShouldExcludeWhenPreSplitReconciliationIsBlocked() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        com.wind.funds.reconciliation.ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
        reconciliationMatchResultSn = recordDifferenceMatchResultSn();
        reconciliationDifferenceApplicationService.createDifference(blockingDifferenceRequest(),
                WindOperatorFactory.system());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED);
        assertThat(result.getReconciliationDecisionResult()).isEqualTo(ReconciliationGateDecisionResult.BLOCKED);
        assertThat(result.getReconciliationEvidenceRefs()).containsExactly("merchant-clearing-recon-evidence-001",
                "run:" + reconciliationRunResultSn);
        assertThat(result.getSn()).isNull();
        assertThat(detailCount()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);

        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'RESOLVED', last_rerun_balanced = TRUE,
                    last_rerun_batch_sn = 'clearing_recon_batch_balanced_001'
                WHERE tenant_id = ?
                """, TENANT_ID);
        reconciliationRunResultSn = recordBalancedRerunResult();
        ClearingSplittableDetailDTO retry = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(retry.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.SPLIT_READY);
        assertThat(retry.getSn()).isNotBlank();
        assertThat(detailCount()).isOne();
    }

    /**
     * 直接支付成功后必须关闭，OPEN 表示生命周期事实不完整。
     */
    @Test
    void testIdentifyShouldExcludeOpenDirectPay() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET status = ? WHERE sn = ?",
                FundsTransactionState.OPEN.name(), FUNDS_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 直接支付不允许用其他业务流水伪装成同一父交易的后继事件。
     */
    @Test
    void testIdentifyShouldExcludeDirectPayWhenDetailBusinessSnDiffersFromParent() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET status = ?, refunded_amount = ? WHERE sn = ?",
                FundsTransactionState.OPEN.name(), 100L, FUNDS_TRANSACTION_SN);
        jdbcTemplate.update("UPDATE t_funds_transaction_detail SET business_sn = ? WHERE sn = ?",
                OTHER_BUSINESS_SN, FUNDS_TRANSACTION_DETAIL_SN);
        jdbcTemplate.update("UPDATE t_ledger_transaction SET business_sn = ? WHERE sn = ?",
                OTHER_BUSINESS_SN, LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("UPDATE t_ledger_entry SET business_sn = ? WHERE sn = ?",
                OTHER_BUSINESS_SN, LEDGER_ENTRY_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 交易明细、账本交易头和分录必须表达同一业务事件。
     */
    @Test
    void testIdentifyShouldExcludeWhenLedgerTransactionBusinessIdentityDiffers() {
        Map<String, Object> payeeEntryFacts = ledgerEntryDigestFacts(
                LEDGER_ENTRY_SN, POSTING_PLAN_SN, 1L, MERCHANT_SUBJECT_ID,
                LedgerSubjectCode.CLEARING.name(), LedgerSubjectCategory.CLEARING.name(), EntrySide.CREDIT.name(),
                LedgerBalanceEffectType.INCREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> payerEntryFacts = ledgerEntryDigestFacts(
                PAYER_LEDGER_ENTRY_SN, POSTING_PLAN_SN, 2L, PAYER_SUBJECT_ID,
                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(), EntrySide.DEBIT.name(),
                LedgerBalanceEffectType.DECREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> postingPlanFacts = ledgerPostingPlanDigestFacts(
                POSTING_PLAN_SN, "merchant-clearing", LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        String transactionDigest = ledgerTransactionDigest(AMOUNT, OTHER_BUSINESS_SN, List.of(
                postingPlanAggregate(postingPlanFacts, List.of(payeeEntryFacts, payerEntryFacts))));
        jdbcTemplate.update("UPDATE t_ledger_transaction SET business_sn = ?, sha256 = ? WHERE sn = ?",
                OTHER_BUSINESS_SN, transactionDigest, LEDGER_TRANSACTION_SN);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.SOURCE_FACT_MISMATCH);
    }

    /**
     * 授权完成进入 SETTLEMENT，不得伪装成商户待清分 CLEARING 事实。
     */
    @Test
    void testIdentifyShouldExcludeAuthorizationCompletionFromMerchantClearing() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET transaction_mode = ? WHERE sn = ?",
                FundsTransactionMode.AUTHORIZATION.name(), FUNDS_TRANSACTION_SN);
        jdbcTemplate.update("UPDATE t_funds_transaction_detail SET event_type = ?, funds_effect_type = ? WHERE sn = ?",
                FundsTransactionEventType.COMPLETE.name(), FundsEffectType.CONSUME.name(), FUNDS_TRANSACTION_DETAIL_SN);
        jdbcTemplate.update("UPDATE t_ledger_transaction SET instruction_type = ?, event_type = ? WHERE sn = ?",
                FundsInstructionType.AUTHORIZATION_TRANSACTION.name(), FundsTransactionEventType.COMPLETE.name(),
                LEDGER_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 场景：资金交易最终失败但残留了不应被消费的来源 fixture。
     * 结果：稳定排除，不生成 SPLIT_READY。
     */
    @Test
    void testIdentifyShouldExcludeFailedTransaction() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET status = ? WHERE sn = ?",
                FundsTransactionState.FAILED.name(), FUNDS_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 场景：成功分录落在 AVAILABLE 而不是待清算 CLEARING。
     * 结果：稳定排除，不允许从余额或报表反推可清分明细。
     */
    @Test
    void testIdentifyShouldExcludeNonClearingEntry() {
        Map<String, Object> payeeEntryFacts = ledgerEntryDigestFacts(
                LEDGER_ENTRY_SN, POSTING_PLAN_SN, 1L, MERCHANT_SUBJECT_ID,
                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(), EntrySide.CREDIT.name(),
                LedgerBalanceEffectType.INCREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> payerEntryFacts = ledgerEntryDigestFacts(
                PAYER_LEDGER_ENTRY_SN, POSTING_PLAN_SN, 2L, PAYER_SUBJECT_ID,
                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(), EntrySide.DEBIT.name(),
                LedgerBalanceEffectType.DECREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> postingPlanFacts = ledgerPostingPlanDigestFacts(
                POSTING_PLAN_SN, "merchant-clearing", LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        String transactionDigest = ledgerTransactionDigest(AMOUNT, BUSINESS_SN, List.of(
                postingPlanAggregate(postingPlanFacts, List.of(payeeEntryFacts, payerEntryFacts))));
        jdbcTemplate.update("""
                        UPDATE t_ledger_entry
                        SET ledger_subject_code = ?, ledger_subject_category = ?, sha256 = ?
                        WHERE sn = ?
                        """,
                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(),
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, payeeEntryFacts),
                LEDGER_ENTRY_SN);
        jdbcTemplate.update("UPDATE t_ledger_transaction SET sha256 = ? WHERE sn = ?",
                transactionDigest, LEDGER_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("缺少唯一 PAYEE/CLEARING credit");
        assertThat(detailCount()).isZero();
    }

    /**
     * 输入：来源分录命中 CLEARING，但分录方向为借记、余额效果为减少。
     *
     * <p>预期：识别为逆向或扣减事实并排除，不能把正金额字段当成正向待清分本金。</p>
     */
    @Test
    void testIdentifyShouldExcludeClearingDebitDecreaseEntry() {
        Map<String, Object> payeeEntryFacts = ledgerEntryDigestFacts(
                LEDGER_ENTRY_SN, POSTING_PLAN_SN, 1L, MERCHANT_SUBJECT_ID,
                LedgerSubjectCode.CLEARING.name(), LedgerSubjectCategory.CLEARING.name(), EntrySide.DEBIT.name(),
                LedgerBalanceEffectType.DECREASE.name(), LedgerPhaseCode.REFUND.name(), AMOUNT);
        Map<String, Object> payerEntryFacts = ledgerEntryDigestFacts(
                PAYER_LEDGER_ENTRY_SN, POSTING_PLAN_SN, 2L, PAYER_SUBJECT_ID,
                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(), EntrySide.DEBIT.name(),
                LedgerBalanceEffectType.DECREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> postingPlanFacts = ledgerPostingPlanDigestFacts(
                POSTING_PLAN_SN, "merchant-clearing", LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        String transactionDigest = ledgerTransactionDigest(AMOUNT, BUSINESS_SN, List.of(
                postingPlanAggregate(postingPlanFacts, List.of(payeeEntryFacts, payerEntryFacts))));
        jdbcTemplate.update("""
                        UPDATE t_ledger_entry
                        SET entry_side = ?, balance_effect_type = ?, phase_code = ?, sha256 = ?
                        WHERE sn = ?
                        """,
                EntrySide.DEBIT.name(), LedgerBalanceEffectType.DECREASE.name(),
                LedgerPhaseCode.REFUND.name(),
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, payeeEntryFacts),
                LEDGER_ENTRY_SN);
        jdbcTemplate.update("UPDATE t_ledger_transaction SET sha256 = ? WHERE sn = ?",
                transactionDigest, LEDGER_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService
                .identifySplittableDetail(minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("无唯一账本分录");
        assertThat(detailCount()).isZero();
    }

    /**
     * 输入：来源交易、明细和账本交易均为退款事实，但分录仍伪装为 CLEARING 贷记增加。
     *
     * <p>预期：按交易事实语义排除，退款或冲正不能进入正向清分候选。</p>
     */
    @Test
    void testIdentifyShouldExcludeRefundTransactionEvenWhenEntryLooksLikeInflow() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET transaction_type = ? WHERE sn = ?",
                DefaultFundsTransactionType.REFUND.name(), FUNDS_TRANSACTION_SN);
        jdbcTemplate.update("""
                        UPDATE t_funds_transaction_detail
                        SET transaction_type = ?, event_type = ?, funds_effect_type = ?
                        WHERE sn = ?
                        """,
                DefaultFundsTransactionType.REFUND.name(), FundsTransactionEventType.REFUND.name(),
                FundsEffectType.RETURN.name(), FUNDS_TRANSACTION_DETAIL_SN);
        jdbcTemplate.update("""
                        UPDATE t_ledger_transaction
                        SET transaction_type = ?, event_type = ?
                        WHERE sn = ?
                        """,
                DefaultFundsTransactionType.REFUND.name(), FundsTransactionEventType.REFUND.name(),
                LEDGER_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService
                .identifySplittableDetail(minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 场景：route snapshot 只是非空文本，但无法提供路径编码、版本和 leg。
     * 结果：按来源事实不完整排除。
     */
    @Test
    void testIdentifyShouldExcludeMalformedRouteSnapshot() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET route_snapshot = ? WHERE sn = ?",
                "{\"routeCode\":\"DIRECT_PAY_STANDARD\"}", FUNDS_TRANSACTION_SN);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("不存在、不支持或记录不完整");
        assertThat(detailCount()).isZero();
    }

    /**
     * 场景：账本分录引用了 posting plan，但对应计划事实已经缺失。
     * 结果：按来源事实不完整排除。
     * 红线：不得只凭分录上的 postingPlanSn 推断记账计划存在。
     */
    @Test
    void testIdentifyShouldExcludeWhenPostingPlanIsMissing() {
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE sn = ?", POSTING_PLAN_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("ledger entry")
                .hasMessageContaining(LEDGER_ENTRY_SN);
        assertThat(detailCount()).isZero();
        assertThat(gateEvidenceCount()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：来源交易在进入清分前已经发生部分退款。
     * 结果：按退款事实排除，不把争议案件或授权拒绝推导成独立资金金额。
     */
    @Test
    void testIdentifyShouldExcludeWhenRefundExists() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET status = ?, refunded_amount = ? WHERE sn = ?",
                FundsTransactionState.OPEN.name(), 100L, FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(result.getAdmissionResult()).isEqualTo(ClearingSplittableAdmissionResult.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.REFUND_EXISTS);
        assertThat(result.getRefundAmount()).isEqualTo(100L);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一账本分录已经按规则版本 1 识别，调用方又以规则版本 2 重放。
     * 结果：拒绝覆盖原准入事实。
     */
    @Test
    void testIdentifyShouldRejectChangedRuleForSameLedgerEntry() {
        clearingSplittableDetailApplicationService.identifySplittableDetail(minimumRequest(), WindOperatorFactory.system());

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest().setSplitRuleVersion("2"), WindOperatorFactory.system()))
                .hasMessageContaining("来源事实或规则已变化");
        assertThat(detailCount()).isOne();
    }

    private IdentifyClearingSplittableDetailRequest minimumRequest() {
        return new IdentifyClearingSplittableDetailRequest()
                .setTenantId(TENANT_ID)
                .setSourceActionFactRef(new StableIdentity()
                        .setOwnerNamespace("funds")
                        .setValue(actionIdentity()))
                .setBusinessLine("ACQUIRING")
                .setSplitPeriod("2026-07-21")
                .setSplitRuleCode("MERCHANT_DAILY_SPLIT")
                .setSplitRuleVersion("1");
    }

    private String recordBalancedRunResult() {
        return recordBalancedRunResult(null);
    }

    private String recordBalancedRerunResult() {
        return recordBalancedRunResult(DIFFERENCE_BATCH_SN);
    }

    private String recordBalancedRunResult(String previousBatchSn) {
        String batchSn = "clearing_recon_batch_balanced_001";
        String referenceSourceRef = "internal:" + FUNDS_TRANSACTION_DETAIL_SN;
        String comparisonSourceRef = "external:" + FUNDS_TRANSACTION_DETAIL_SN;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, batchSn, "CLEARING_SPLITTABLE_IDENTIFY",
                actionIdentity(), "recon-rule-1", "report:merchant-clearing-recon-run-001",
                referenceSourceRef, comparisonSourceRef, previousBatchSn);
        return reconciliationRunResultApplicationService.executeStrictExact(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batchSn),
                WindOperatorFactory.system()).getSn();
    }

    private CreateReconciliationDifferenceRequest blockingDifferenceRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationMatchResultSn(reconciliationMatchResultSn)
                .setResponsiblePartyRef("merchant:" + MERCHANT_SUBJECT_ID)
                .setDescription("商户清算候选与外部清算来源金额不一致");
    }

    private String recordDifferenceMatchResultSn() {
        String referenceSourceRef = "internal-difference:" + FUNDS_TRANSACTION_DETAIL_SN;
        String comparisonSourceRef = "external-difference:" + FUNDS_TRANSACTION_DETAIL_SN;
        com.wind.funds.reconciliation.ReconciliationTestFixture.prepareReadyBatch(
                jdbcTemplate, TENANT_ID, DIFFERENCE_BATCH_SN, "CLEARING_SPLITTABLE_IDENTIFY",
                actionIdentity(), "recon-rule-1", "merchant-clearing-recon-evidence-001",
                referenceSourceRef, comparisonSourceRef, null, 2L, "CONFIRMED");
        reconciliationRunResultSn = reconciliationRunResultApplicationService.executeStrictExact(
                new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(DIFFERENCE_BATCH_SN), WindOperatorFactory.system()).getSn();
        return jdbcTemplate.queryForObject("""
                SELECT sn FROM t_reconciliation_match_result
                WHERE tenant_id = ? AND reconciliation_batch_sn = ? AND result_kind <> 'MATCHED'
                """, String.class, TENANT_ID, DIFFERENCE_BATCH_SN);
    }

    private void insertSourceFacts(FundsTransactionState transactionState,
                                   FundsTransactionDetailState detailState,
                                   LedgerSubjectCode ledgerSubjectCode,
                                   String routeSnapshot,
                                   long refundedAmount,
                                   long declinedAmount) {
        Map<String, Object> payeeEntryFacts = ledgerEntryDigestFacts(
                LEDGER_ENTRY_SN, POSTING_PLAN_SN, 1L, MERCHANT_SUBJECT_ID,
                ledgerSubjectCode.name(), LedgerSubjectCategory.CLEARING.name(), EntrySide.CREDIT.name(),
                LedgerBalanceEffectType.INCREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> payerEntryFacts = ledgerEntryDigestFacts(
                PAYER_LEDGER_ENTRY_SN, POSTING_PLAN_SN, 2L, PAYER_SUBJECT_ID,
                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(), EntrySide.DEBIT.name(),
                LedgerBalanceEffectType.DECREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        Map<String, Object> postingPlanFacts = ledgerPostingPlanDigestFacts(
                POSTING_PLAN_SN, "merchant-clearing", LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        String transactionDigest = ledgerTransactionDigest(AMOUNT, BUSINESS_SN, List.of(
                postingPlanAggregate(postingPlanFacts, List.of(payeeEntryFacts, payerEntryFacts))));
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction (
                            sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                            status, amount, currency, completed_amount, refunded_amount, declined_amount,
                            route_snapshot, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                FUNDS_TRANSACTION_SN, TENANT_ID, FundsTransactionMode.DIRECT.name(),
                DefaultFundsTransactionType.PAY.name(), BUSINESS_SCENE, BUSINESS_SN,
                transactionState.name(), AMOUNT, CurrencyIsoCode.USD.name(), AMOUNT, refundedAmount,
                declinedAmount, routeSnapshot);
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction_detail (
                            sn, tenant_id, transaction_sn, business_scene, business_sn, transaction_type,
                            event_type, subject_id, subject_type, participant_role, request_hash,
                            funds_effect_type, ledger_transaction_sn, amount, currency, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                FUNDS_TRANSACTION_DETAIL_SN, TENANT_ID, FUNDS_TRANSACTION_SN, BUSINESS_SCENE, BUSINESS_SN,
                DefaultFundsTransactionType.PAY.name(), FundsTransactionEventType.PAY.name(),
                MERCHANT_SUBJECT_ID, "FUNDING_ACCOUNT", RouteParticipantRole.PAYEE.name(), "request-hash-001",
                FundsEffectType.DIRECT.name(), LEDGER_TRANSACTION_SN, AMOUNT, CurrencyIsoCode.USD.name(),
                detailState.name());
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction_detail (
                            sn, tenant_id, transaction_sn, business_scene, business_sn, transaction_type,
                            event_type, subject_id, subject_type, participant_role, request_hash,
                            funds_effect_type, ledger_transaction_sn, amount, currency, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                PAYER_DETAIL_SN, TENANT_ID, FUNDS_TRANSACTION_SN, BUSINESS_SCENE, BUSINESS_SN,
                DefaultFundsTransactionType.PAY.name(), FundsTransactionEventType.PAY.name(),
                PAYER_SUBJECT_ID, "FUNDING_ACCOUNT", RouteParticipantRole.PAYER.name(), "request-hash-payer-001",
                FundsEffectType.DIRECT.name(), LEDGER_TRANSACTION_SN, AMOUNT, CurrencyIsoCode.USD.name(),
                detailState.name());
        jdbcTemplate.update("""
                        INSERT INTO t_ledger_transaction (
                            sn, tenant_id, funds_transaction_sn, instruction_type, event_type, transaction_type,
                            business_scene, business_sn, amount, currency, original_amount, original_currency,
                            exchange_rate, debit_amount, credit_amount, transaction_time, sha256
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                        """,
                LEDGER_TRANSACTION_SN, TENANT_ID, FUNDS_TRANSACTION_SN,
                FundsInstructionType.DIRECT_TRANSACTION.name(),
                FundsTransactionEventType.PAY.name(), DefaultFundsTransactionType.PAY.name(), BUSINESS_SCENE,
                BUSINESS_SN, AMOUNT, CurrencyIsoCode.USD.name(), AMOUNT, CurrencyIsoCode.USD.name(), AMOUNT,
                AMOUNT, LEDGER_TRANSACTION_TIME, transactionDigest);
        jdbcTemplate.update("""
                        INSERT INTO t_ledger_posting_plan (
                            sn, tenant_id, ledger_transaction_sn, funds_transaction_sn, route_leg_id, intent,
                            posting_scope, balance_effect_type, phase_code, amount, currency, debit_amount,
                            credit_amount, sha256
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                POSTING_PLAN_SN, TENANT_ID, LEDGER_TRANSACTION_SN, FUNDS_TRANSACTION_SN, "merchant-clearing",
                LedgerPostingIntentType.TRANSFER.name(), LedgerPostingScope.BETWEEN_SUBJECTS.name(),
                LedgerBalanceEffectType.INCREASE.name(), LedgerPhaseCode.TRANSFER.name(), AMOUNT,
                CurrencyIsoCode.USD.name(), AMOUNT, AMOUNT,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_POSTING_PLAN_DIGEST_DOMAIN, postingPlanFacts));
        jdbcTemplate.update("""
                        INSERT INTO t_ledger_entry (
                            sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn, ledger_id,
                            subject_id, subject_type, ledger_subject_code, ledger_subject_category, entry_side,
                            posting_role, balance_constraint_type, intent, posting_scope, balance_effect_type,
                            phase_code, business_scene, business_sn, amount, currency, original_amount,
                            original_currency, exchange_rate, transaction_time, sha256
                        ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                        """,
                LEDGER_ENTRY_SN, TENANT_ID, LEDGER_TRANSACTION_SN, POSTING_PLAN_SN, FUNDS_TRANSACTION_SN,
                MERCHANT_SUBJECT_ID, "FUNDING_ACCOUNT", ledgerSubjectCode.name(),
                LedgerSubjectCategory.CLEARING.name(), EntrySide.CREDIT.name(), LedgerPostingRole.DETAIL.name(),
                LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE.name(), LedgerPostingIntentType.TRANSFER.name(),
                LedgerPostingScope.BETWEEN_SUBJECTS.name(), LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), BUSINESS_SCENE, BUSINESS_SN, AMOUNT, CurrencyIsoCode.USD.name(),
                AMOUNT, CurrencyIsoCode.USD.name(), LEDGER_TRANSACTION_TIME,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, payeeEntryFacts));
        jdbcTemplate.update("""
                        INSERT INTO t_ledger_entry (
                            sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn, ledger_id,
                            subject_id, subject_type, ledger_subject_code, ledger_subject_category, entry_side,
                            posting_role, balance_constraint_type, intent, posting_scope, balance_effect_type,
                            phase_code, business_scene, business_sn, amount, currency, original_amount,
                            original_currency, exchange_rate, transaction_time, sha256
                        ) VALUES (?, ?, ?, ?, ?, 2, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                        """,
                PAYER_LEDGER_ENTRY_SN, TENANT_ID, LEDGER_TRANSACTION_SN, POSTING_PLAN_SN, FUNDS_TRANSACTION_SN,
                PAYER_SUBJECT_ID, "FUNDING_ACCOUNT", LedgerSubjectCode.AVAILABLE.name(),
                LedgerSubjectCategory.ASSET.name(), EntrySide.DEBIT.name(), LedgerPostingRole.DETAIL.name(),
                LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE.name(), LedgerPostingIntentType.TRANSFER.name(),
                LedgerPostingScope.BETWEEN_SUBJECTS.name(), LedgerBalanceEffectType.DECREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), BUSINESS_SCENE, BUSINESS_SN, AMOUNT, CurrencyIsoCode.USD.name(),
                AMOUNT, CurrencyIsoCode.USD.name(), LEDGER_TRANSACTION_TIME,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, payerEntryFacts));
    }

    private void deleteSourceFacts() {
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE sn LIKE 'clearing_fee_%'");
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE sn = ?", LEDGER_ENTRY_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE sn = ?", PAYER_LEDGER_ENTRY_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE sn LIKE 'clearing_fee_%'");
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE sn = ?", POSTING_PLAN_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE sn LIKE 'clearing_fee_%'");
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE sn = ?", FUNDS_TRANSACTION_DETAIL_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE sn = ?", PAYER_DETAIL_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE sn = ?", FUNDS_TRANSACTION_SN);
    }

    private void insertFeeFacts(long feeAmount) {
        jdbcTemplate.update("UPDATE t_funds_transaction SET fee_amount = ?, route_snapshot = ? WHERE sn = ?",
                feeAmount, routeSnapshot(true), FUNDS_TRANSACTION_SN);
        jdbcTemplate.update("""
                INSERT INTO t_funds_transaction_detail (
                    sn, tenant_id, transaction_sn, business_scene, business_sn, transaction_type,
                    event_type, subject_id, subject_type, participant_role, request_hash,
                    funds_effect_type, ledger_transaction_sn, amount, currency, status
                ) VALUES ('clearing_fee_detail_001', ?, ?, ?, ?, 'PAY', 'PAY',
                    'platform_fee_001', 'FUNDING_ACCOUNT', 'FEE_RECEIVER', 'request-hash-fee-001',
                    'DIRECT', ?, ?, 'USD', 'SUCCEEDED')
                """, TENANT_ID, FUNDS_TRANSACTION_SN, BUSINESS_SCENE, BUSINESS_SN, LEDGER_TRANSACTION_SN, feeAmount);
        Map<String, Object> feePlanFacts = ledgerPostingPlanDigestFacts(
                "clearing_fee_plan_001", "FEE", LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.FEE.name(), feeAmount);
        jdbcTemplate.update("""
                INSERT INTO t_ledger_posting_plan (
                    sn, tenant_id, ledger_transaction_sn, funds_transaction_sn, route_leg_id, intent,
                    posting_scope, balance_effect_type, phase_code, amount, currency, debit_amount,
                    credit_amount, sha256
                ) VALUES ('clearing_fee_plan_001', ?, ?, ?, 'FEE', 'TRANSFER', 'BETWEEN_SUBJECTS',
                    'INCREASE', 'FEE', ?, 'USD', ?, ?, ?)
                """, TENANT_ID, LEDGER_TRANSACTION_SN, FUNDS_TRANSACTION_SN, feeAmount, feeAmount, feeAmount,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_POSTING_PLAN_DIGEST_DOMAIN, feePlanFacts));
        insertFeeEntry("clearing_fee_credit_entry_001", "platform_fee_001", "FEE",
                "REVENUE", "CREDIT", "INCREASE", feeAmount);
        insertFeeEntry("clearing_fee_debit_entry_001", PAYER_SUBJECT_ID, "AVAILABLE",
                "ASSET", "DEBIT", "DECREASE", feeAmount);
        Map<String, Object> primaryPlanFacts = ledgerPostingPlanDigestFacts(
                POSTING_PLAN_SN, "merchant-clearing", LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), AMOUNT);
        List<Map<String, Object>> aggregates = List.of(
                postingPlanAggregate(feePlanFacts, List.of(
                        ledgerEntryDigestFacts("clearing_fee_credit_entry_001", "clearing_fee_plan_001", 3L,
                                "platform_fee_001", "FEE", "REVENUE", "CREDIT", "INCREASE", "FEE", feeAmount),
                        ledgerEntryDigestFacts("clearing_fee_debit_entry_001", "clearing_fee_plan_001", 3L,
                                PAYER_SUBJECT_ID, "AVAILABLE", "ASSET", "DEBIT", "DECREASE", "FEE", feeAmount))),
                postingPlanAggregate(primaryPlanFacts, List.of(
                        ledgerEntryDigestFacts(LEDGER_ENTRY_SN, POSTING_PLAN_SN, 1L, MERCHANT_SUBJECT_ID,
                                LedgerSubjectCode.CLEARING.name(), LedgerSubjectCategory.CLEARING.name(),
                                EntrySide.CREDIT.name(), LedgerBalanceEffectType.INCREASE.name(),
                                LedgerPhaseCode.TRANSFER.name(), AMOUNT),
                        ledgerEntryDigestFacts(PAYER_LEDGER_ENTRY_SN, POSTING_PLAN_SN, 2L, PAYER_SUBJECT_ID,
                                LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCategory.ASSET.name(),
                                EntrySide.DEBIT.name(), LedgerBalanceEffectType.DECREASE.name(),
                                LedgerPhaseCode.TRANSFER.name(), AMOUNT))));
        long total = AMOUNT + feeAmount;
        jdbcTemplate.update("UPDATE t_ledger_transaction SET debit_amount = ?, credit_amount = ?, sha256 = ? "
                        + "WHERE sn = ?", total, total, ledgerTransactionDigest(total, BUSINESS_SN, aggregates),
                LEDGER_TRANSACTION_SN);
    }

    private void insertFeeEntry(String sn,
                                String subjectId,
                                String subjectCode,
                                String subjectCategory,
                                String entrySide,
                                String balanceEffect,
                                long amount) {
        Map<String, Object> entryFacts = ledgerEntryDigestFacts(
                sn, "clearing_fee_plan_001", 3L, subjectId, subjectCode, subjectCategory,
                entrySide, balanceEffect, LedgerPhaseCode.FEE.name(), amount);
        jdbcTemplate.update("""
                INSERT INTO t_ledger_entry (
                    sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn, ledger_id,
                    subject_id, subject_type, ledger_subject_code, ledger_subject_category, entry_side,
                    posting_role, balance_constraint_type, intent, posting_scope, balance_effect_type,
                    phase_code, business_scene, business_sn, amount, currency, original_amount,
                    original_currency, exchange_rate, transaction_time, sha256
                ) VALUES (?, ?, ?, 'clearing_fee_plan_001', ?, 3, ?, 'FUNDING_ACCOUNT', ?, ?, ?,
                    'DETAIL', 'MUST_NOT_BE_NEGATIVE', 'TRANSFER', 'BETWEEN_SUBJECTS', ?,
                    'FEE', ?, ?, ?, 'USD', ?, 'USD', 1, ?, ?)
                """, sn, TENANT_ID, LEDGER_TRANSACTION_SN, FUNDS_TRANSACTION_SN, subjectId,
                subjectCode, subjectCategory, entrySide, balanceEffect, BUSINESS_SCENE, BUSINESS_SN,
                amount, amount, LEDGER_TRANSACTION_TIME,
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, entryFacts));
    }

    private String ledgerTransactionDigest(long debitCreditAmount,
                                           String businessSn,
                                           List<Map<String, Object>> postingPlans) {
        Map<String, Object> transaction = new TreeMap<>();
        transaction.put("sn", LEDGER_TRANSACTION_SN);
        transaction.put("tenantId", TENANT_ID);
        transaction.put("instructionType", FundsInstructionType.DIRECT_TRANSACTION.name());
        transaction.put("eventType", FundsTransactionEventType.PAY.name());
        transaction.put("fundsTransactionSn", FUNDS_TRANSACTION_SN);
        transaction.put("transactionType", DefaultFundsTransactionType.PAY.name());
        transaction.put("businessScene", BUSINESS_SCENE);
        transaction.put("businessSn", businessSn);
        transaction.put("amount", AMOUNT);
        transaction.put("currency", CurrencyIsoCode.USD.name());
        transaction.put("originalAmount", AMOUNT);
        transaction.put("originalCurrency", CurrencyIsoCode.USD.name());
        transaction.put("exchangeRate", BigDecimal.ONE);
        transaction.put("debitAmount", debitCreditAmount);
        transaction.put("creditAmount", debitCreditAmount);
        transaction.put("transactionTime", LEDGER_TRANSACTION_TIME);
        transaction.put("referenceLedgerTransactionSn", null);
        Map<String, Object> aggregate = new TreeMap<>();
        aggregate.put("transaction", transaction);
        aggregate.put("postingPlans", postingPlans);
        return FundsStableHashSupport.sha256CanonicalJson(LEDGER_TRANSACTION_DIGEST_DOMAIN, aggregate);
    }

    private Map<String, Object> ledgerPostingPlanDigestFacts(String sn,
                                                             String routeLegId,
                                                             String balanceEffectType,
                                                             String phaseCode,
                                                             long amount) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", sn);
        facts.put("tenantId", TENANT_ID);
        facts.put("ledgerTransactionSn", LEDGER_TRANSACTION_SN);
        facts.put("fundsTransactionSn", FUNDS_TRANSACTION_SN);
        facts.put("routeLegId", routeLegId);
        facts.put("intent", LedgerPostingIntentType.TRANSFER.name());
        facts.put("postingScope", LedgerPostingScope.BETWEEN_SUBJECTS.name());
        facts.put("balanceEffectType", balanceEffectType);
        facts.put("phaseCode", phaseCode);
        facts.put("amount", amount);
        facts.put("currency", CurrencyIsoCode.USD.name());
        facts.put("debitAmount", amount);
        facts.put("creditAmount", amount);
        return facts;
    }

    private Map<String, Object> ledgerEntryDigestFacts(String sn,
                                                       String postingPlanSn,
                                                       long ledgerId,
                                                       String subjectId,
                                                       String subjectCode,
                                                       String subjectCategory,
                                                       String entrySide,
                                                       String balanceEffectType,
                                                       String phaseCode,
                                                       long amount) {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("sn", sn);
        facts.put("tenantId", TENANT_ID);
        facts.put("ledgerTransactionSn", LEDGER_TRANSACTION_SN);
        facts.put("postingPlanSn", postingPlanSn);
        facts.put("fundsTransactionSn", FUNDS_TRANSACTION_SN);
        facts.put("ledgerId", ledgerId);
        facts.put("periodType", "LIFETIME");
        facts.put("periodId", "LIFETIME");
        facts.put("subjectId", subjectId);
        facts.put("subjectType", "FUNDING_ACCOUNT");
        facts.put("ledgerSubjectCode", subjectCode);
        facts.put("ledgerSubjectCategory", subjectCategory);
        facts.put("entrySide", entrySide);
        facts.put("postingRole", LedgerPostingRole.DETAIL.name());
        facts.put("balanceConstraintType", LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE.name());
        facts.put("intent", LedgerPostingIntentType.TRANSFER.name());
        facts.put("postingScope", LedgerPostingScope.BETWEEN_SUBJECTS.name());
        facts.put("balanceEffectType", balanceEffectType);
        facts.put("phaseCode", phaseCode);
        facts.put("businessScene", BUSINESS_SCENE);
        facts.put("businessSn", BUSINESS_SN);
        facts.put("amount", amount);
        facts.put("currency", CurrencyIsoCode.USD.name());
        facts.put("originalAmount", amount);
        facts.put("originalCurrency", CurrencyIsoCode.USD.name());
        facts.put("exchangeRate", BigDecimal.ONE);
        facts.put("transactionTime", LEDGER_TRANSACTION_TIME);
        return facts;
    }

    private Map<String, Object> postingPlanAggregate(Map<String, Object> plan,
                                                     List<Map<String, Object>> entries) {
        Map<String, Object> aggregate = new TreeMap<>();
        aggregate.put("plan", plan);
        aggregate.put("entries", entries);
        return aggregate;
    }

    private String actionIdentity() {
        return FUNDS_TRANSACTION_SN + ":primary:0";
    }

    private String routeSnapshot(boolean withFee) {
        Map<String, Object> payer = subject(PAYER_SUBJECT_ID);
        Map<String, Object> payee = subject(MERCHANT_SUBJECT_ID);
        Map<String, Object> values = new java.util.TreeMap<>();
        values.put("tenantId", TENANT_ID);
        values.put("snapshotId", "clearing-route-001");
        values.put("snapshotSchemaVersion", "1.0");
        values.put("routeCode", "DIRECT_PAY_STANDARD");
        values.put("routeVersion", "1.0");
        values.put("businessScene", BUSINESS_SCENE);
        values.put("businessSn", BUSINESS_SN);
        values.put("instructionType", "DIRECT_TRANSACTION");
        values.put("eventType", "PAY");
        values.put("transactionType", "PAY");
        List<Map<String, Object>> participants = new java.util.ArrayList<>();
        participants.add(participant("PAYER", payer, AMOUNT));
        participants.add(participant("PAYEE", payee, AMOUNT));
        List<Map<String, Object>> legs = new java.util.ArrayList<>();
        legs.add(leg("PAY", 0, payer, payee, AMOUNT));
        if (withFee) {
            Map<String, Object> feeReceiver = subject("platform_fee_001");
            participants.add(participant("FEE_RECEIVER", feeReceiver, 100L));
            legs.add(leg("FEE", 1, payer, feeReceiver, 100L));
        }
        values.put("participants", participants);
        values.put("legs", legs);
        return WindJson.toJsonString(values);
    }

    private Map<String, Object> participant(String role, Map<String, Object> subject, long amount) {
        return Map.of(
                "participantRole", role,
                "subjectRef", subject,
                "currency", "USD",
                "amount", money(amount),
                "contextVariables", Map.of());
    }

    private Map<String, Object> leg(String legId,
                                    int sequence,
                                    Map<String, Object> source,
                                    Map<String, Object> target,
                                    long amount) {
        return Map.ofEntries(
                Map.entry("legId", legId),
                Map.entry("sequence", sequence),
                Map.entry("legType", "INTERNAL_TRANSFER"),
                Map.entry("sourceNode", node("SOURCE", source)),
                Map.entry("targetNode", node("TARGET", target)),
                Map.entry("amount", money(amount)),
                Map.entry("originalAmount", money(amount)),
                Map.entry("exchangeRate", 1),
                Map.entry("replayPolicy", "FULL_ONLY"),
                Map.entry("contextVariables", Map.of()));
    }

    private Map<String, Object> node(String role, Map<String, Object> subject) {
        return Map.of("nodeType", "SUBJECT", "subjectRef", subject, "nodeRole", role);
    }

    private Map<String, Object> subject(String subjectId) {
        return Map.of(
                "tenantId", TENANT_ID,
                "subjectId", subjectId,
                "subjectType", "FUNDING_ACCOUNT",
                "currency", "USD");
    }

    private Map<String, Object> money(long amount) {
        return Map.of("amount", amount, "currency", "USD");
    }

    private int detailCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_splittable_detail", Integer.class);
    }

    private int gateEvidenceCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_reconciliation_stage_gate_evidence", Integer.class);
    }

    @Configuration
    @Import({
            DefaultFundsTransactionQueryService.class,
            LedgerTransactionServiceImpl.class,
            ClearingSplittableDetailApplicationServiceImpl.class,
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class
    })
    static class Config {
    }
}
