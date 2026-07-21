package com.wind.funds.reconciliation.application.clearing.impl;

import com.capte.domain.core.operator.WindOperator;
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
import com.wind.funds.reconciliation.enums.ClearingSplittableDetailStatus;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ClearingSplittableDetailDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.IdentifyClearingSplittableDetailRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
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

    private static final String LEDGER_TRANSACTION_SN = "clearing_ledger_tx_001";

    private static final String POSTING_PLAN_SN = "clearing_posting_plan_001";

    private static final String LEDGER_ENTRY_SN = "clearing_ledger_entry_001";

    private static final String BUSINESS_SCENE = "MERCHANT_PAY";

    private static final String BUSINESS_SN = "merchant_pay_001";

    private static final String MERCHANT_SUBJECT_ID = "merchant_settlement_001";

    private static final long AMOUNT = 9800L;

    @Autowired
    private ClearingSplittableDetailApplicationService clearingSplittableDetailApplicationService;

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String reconciliationRunResultSn;

    @BeforeEach
    void prepareSourceFacts() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
        jdbcTemplate.update("DELETE FROM t_clearing_splittable_detail");
        deleteSourceFacts();
        insertSourceFacts(FundsTransactionStatus.CLOSED, FundsTransactionDetailStatus.SUCCEEDED,
                LedgerSubjectCode.CLEARING,
                "{\"routeCode\":\"DIRECT_PAY_STANDARD\",\"routeVersion\":\"v1\",\"legs\":[{\"legId\":\"merchant-clearing\"}]}",
                0L, 0L);
        reconciliationRunResultSn = recordBalancedRunResult();
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
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.SPLIT_READY);
        assertThat(result.getExclusionReason()).isNull();
        assertThat(result.getFundsTransactionSn()).isEqualTo(FUNDS_TRANSACTION_SN);
        assertThat(result.getFundsTransactionDetailSn()).isEqualTo(FUNDS_TRANSACTION_DETAIL_SN);
        assertThat(result.getLedgerTransactionSn()).isEqualTo(LEDGER_TRANSACTION_SN);
        assertThat(result.getPostingPlanSn()).isEqualTo(POSTING_PLAN_SN);
        assertThat(result.getLedgerEntrySn()).isEqualTo(LEDGER_ENTRY_SN);
        assertThat(result.getSubjectType()).isEqualTo("FUNDING_ACCOUNT");
        assertThat(result.getSubjectId()).isEqualTo(MERCHANT_SUBJECT_ID);
        assertThat(result.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(result.getPrincipalAmount()).isEqualTo(AMOUNT);
        assertThat(result.getReconciliationDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.PASSED);
        assertThat(result.getReconciliationRunResultSn()).isEqualTo(reconciliationRunResultSn);
        assertThat(result.getReconciliationResultDigest()).hasSize(64);
        assertThat(result.getReconciliationEvidenceRefs()).containsExactly("report:merchant-clearing-recon-run-001");
        assertThat(result.getSourceDigest()).hasSize(64);
        assertThat(detailCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：来源资金和账本事实完整，但没有可读取的正向对账运行结果。
     * 结果：稳定排除，不能用“没有登记差错”替代清分前对账。
     */
    @Test
    void testIdentifyShouldExcludeWhenReconciliationRunResultIsMissing() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result WHERE sn = ?", reconciliationRunResultSn);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED);
        assertThat(result.getReconciliationDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
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
                minimumRequest(), WindOperator.system());
        ClearingSplittableDetailDTO replay = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(replay.getSn()).isEqualTo(first.getSn());
        assertThat(replay.getSourceDigest()).isEqualTo(first.getSourceDigest());
        assertThat(detailCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：交易已成功但缺少 route snapshot。
     * 结果：记录 EXCLUDED 和稳定排除原因，不进入后续清分。
     * 红线：来源不完整不得静默放行或从余额反推明细。
     */
    @Test
    void testIdentifyShouldExcludeWhenRouteSnapshotIsMissing() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET route_snapshot = NULL WHERE sn = ?",
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.SOURCE_FACT_INCOMPLETE);
        assertThat(detailCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：来源事实完整，但该交易明细存在未闭环的清分前重大对账差错。
     * 结果：记录 EXCLUDED、对账阻断结论和证据引用。
     * 红线：重大差错不得生成可清分结果，也不得修改历史资金事实。
     */
    @Test
    void testIdentifyShouldExcludeWhenPreSplitReconciliationIsBlocked() {
        reconciliationDifferenceApplicationService.createDifference(blockingDifferenceRequest(),
                WindOperator.system());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.RECONCILIATION_BLOCKED);
        assertThat(result.getReconciliationDecisionStatus()).isEqualTo(ReconciliationGateDecisionStatus.BLOCKED);
        assertThat(result.getReconciliationEvidenceRefs()).containsExactly("report:merchant-clearing-recon-run-001",
                "merchant-clearing-recon-evidence-001");
        assertThat(detailCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：授权聚合仍有剩余占用而保持 OPEN，但本次完成明细已成功并命中 CLEARING。
     * 结果：本次成功明细可以进入清分。
     * 红线：不能因主聚合尚未关闭而误伤 VCC 部分完成等已成立资金事实。
     */
    @Test
    void testIdentifyShouldAllowOpenAggregateWhenCurrentDetailSucceeded() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET status = ? WHERE sn = ?",
                FundsTransactionStatus.OPEN.name(), FUNDS_TRANSACTION_SN);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.SPLIT_READY);
        assertThat(result.getExclusionReason()).isNull();
    }

    /**
     * 场景：资金交易最终失败但残留了不应被消费的来源 fixture。
     * 结果：稳定排除，不生成 SPLIT_READY。
     */
    @Test
    void testIdentifyShouldExcludeFailedTransaction() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET status = ? WHERE sn = ?",
                FundsTransactionStatus.FAILED.name(), FUNDS_TRANSACTION_SN);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason())
                .isEqualTo(ClearingSplittableExclusionReason.TRANSACTION_NOT_ELIGIBLE);
    }

    /**
     * 场景：成功分录落在 AVAILABLE 而不是待清算 CLEARING。
     * 结果：稳定排除，不允许从余额或报表反推可清分明细。
     */
    @Test
    void testIdentifyShouldExcludeNonClearingEntry() {
        jdbcTemplate.update("UPDATE t_ledger_entry SET ledger_subject_code = ? WHERE sn = ?",
                LedgerSubjectCode.AVAILABLE.name(), LEDGER_ENTRY_SN);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason())
                .isEqualTo(ClearingSplittableExclusionReason.LEDGER_ENTRY_NOT_CLEARING);
    }

    /**
     * 场景：route snapshot 只是非空文本，但无法提供路径编码、版本和 leg。
     * 结果：按来源事实不完整排除。
     */
    @Test
    void testIdentifyShouldExcludeMalformedRouteSnapshot() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET route_snapshot = ? WHERE sn = ?",
                "{\"routeCode\":\"DIRECT_PAY_STANDARD\"}", FUNDS_TRANSACTION_SN);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.SOURCE_FACT_INCOMPLETE);
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

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
        assertThat(result.getExclusionReason()).isEqualTo(ClearingSplittableExclusionReason.SOURCE_FACT_INCOMPLETE);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：来源交易在进入清分前已经发生部分退款。
     * 结果：按退款事实排除，不把争议案件或授权拒绝推导成独立资金金额。
     */
    @Test
    void testIdentifyShouldExcludeWhenRefundExists() {
        jdbcTemplate.update("UPDATE t_funds_transaction SET refunded_amount = ? WHERE sn = ?", 100L,
                FUNDS_TRANSACTION_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ClearingSplittableDetailDTO result = clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest(), WindOperator.system());

        assertThat(result.getStatus()).isEqualTo(ClearingSplittableDetailStatus.EXCLUDED);
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
        clearingSplittableDetailApplicationService.identifySplittableDetail(minimumRequest(), WindOperator.system());

        assertThatThrownBy(() -> clearingSplittableDetailApplicationService.identifySplittableDetail(
                minimumRequest().setRuleVersion("2"), WindOperator.system()))
                .hasMessageContaining("来源事实或规则已变化");
        assertThat(detailCount()).isOne();
    }

    private IdentifyClearingSplittableDetailRequest minimumRequest() {
        return new IdentifyClearingSplittableDetailRequest()
                .setTenantId(TENANT_ID)
                .setFundsTransactionSn(FUNDS_TRANSACTION_SN)
                .setFundsTransactionDetailSn(FUNDS_TRANSACTION_DETAIL_SN)
                .setLedgerEntrySn(LEDGER_ENTRY_SN)
                .setReconciliationRunResultSn(reconciliationRunResultSn)
                .setClearingPeriod("2026-07-21")
                .setRuleCode("MERCHANT_DAILY_SPLIT")
                .setRuleVersion("1");
    }

    private String recordBalancedRunResult() {
        return reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn("clearing_recon_batch_balanced_001")
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn(FUNDS_TRANSACTION_DETAIL_SN)
                .setRuleVersion("recon-rule-1")
                .setInternalSourceDigest("a".repeat(64))
                .setExternalSourceDigest("b".repeat(64))
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setInternalSourceRef("internal:" + FUNDS_TRANSACTION_DETAIL_SN)
                        .setExternalSourceRef("external:" + FUNDS_TRANSACTION_DETAIL_SN)
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                        .setEvidenceRef("report:merchant-clearing-recon-run-001#line-1")))
                .setEvidenceRefs(List.of("report:merchant-clearing-recon-run-001")),
                WindOperator.system()).getSn();
    }

    private CreateReconciliationDifferenceRequest blockingDifferenceRequest() {
        return new CreateReconciliationDifferenceRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn("clearing_recon_difference_001")
                .setReconciliationBatchSn("clearing_recon_batch_001")
                .setSourceRecordSn(FUNDS_TRANSACTION_DETAIL_SN)
                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                .setDifferenceType(ReconciliationDifferenceType.AMOUNT_MISMATCH)
                .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                .setCurrency(CurrencyIsoCode.USD)
                .setDifferenceAmount(AMOUNT)
                .setResponsiblePartyRef("merchant:" + MERCHANT_SUBJECT_ID)
                .setBlockingScope(ReconciliationGateObjectType.CLEARING.name())
                .setBlockingObjectType(ReconciliationGateObjectType.CLEARING)
                .setBlockingObjectSn(FUNDS_TRANSACTION_DETAIL_SN)
                .setRuleVersion("recon-rule-1")
                .setEvidenceRef("merchant-clearing-recon-evidence-001");
    }

    private void insertSourceFacts(FundsTransactionStatus transactionStatus,
                                   FundsTransactionDetailStatus detailStatus,
                                   LedgerSubjectCode ledgerSubjectCode,
                                   String routeSnapshot,
                                   long refundedAmount,
                                   long declinedAmount) {
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction (
                            sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                            status, amount, currency, completed_amount, refunded_amount, declined_amount,
                            route_snapshot, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                        """,
                FUNDS_TRANSACTION_SN, TENANT_ID, FundsTransactionMode.DIRECT.name(),
                DefaultFundsTransactionType.PAY.name(), BUSINESS_SCENE, BUSINESS_SN,
                transactionStatus.name(), AMOUNT, CurrencyIsoCode.USD.name(), AMOUNT, refundedAmount,
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
                detailStatus.name());
        jdbcTemplate.update("""
                        INSERT INTO t_ledger_transaction (
                            sn, tenant_id, funds_transaction_sn, instruction_type, event_type, transaction_type,
                            business_scene, business_sn, amount, currency, original_amount, original_currency,
                            exchange_rate, debit_amount, credit_amount, sha256
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?)
                        """,
                LEDGER_TRANSACTION_SN, TENANT_ID, FUNDS_TRANSACTION_SN,
                FundsInstructionType.DIRECT_TRANSACTION.name(),
                FundsTransactionEventType.PAY.name(), DefaultFundsTransactionType.PAY.name(), BUSINESS_SCENE,
                BUSINESS_SN, AMOUNT, CurrencyIsoCode.USD.name(), AMOUNT, CurrencyIsoCode.USD.name(), AMOUNT,
                AMOUNT, "ledger-transaction-sha256");
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
                CurrencyIsoCode.USD.name(), AMOUNT, AMOUNT, "posting-plan-sha256");
        jdbcTemplate.update("""
                        INSERT INTO t_ledger_entry (
                            sn, tenant_id, ledger_transaction_sn, posting_plan_sn, funds_transaction_sn, ledger_id,
                            subject_id, subject_type, ledger_subject_code, ledger_subject_category, entry_side,
                            posting_role, balance_constraint_type, intent, posting_scope, balance_effect_type,
                            phase_code, business_scene, business_sn, amount, currency, original_amount,
                            original_currency, exchange_rate, sha256
                        ) VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
                        """,
                LEDGER_ENTRY_SN, TENANT_ID, LEDGER_TRANSACTION_SN, POSTING_PLAN_SN, FUNDS_TRANSACTION_SN,
                MERCHANT_SUBJECT_ID, "FUNDING_ACCOUNT", ledgerSubjectCode.name(),
                LedgerSubjectCategory.CLEARING.name(), EntrySide.CREDIT.name(), LedgerPostingRole.DETAIL.name(),
                LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE.name(), LedgerPostingIntentType.TRANSFER.name(),
                LedgerPostingScope.BETWEEN_SUBJECTS.name(), LedgerBalanceEffectType.INCREASE.name(),
                LedgerPhaseCode.TRANSFER.name(), BUSINESS_SCENE, BUSINESS_SN, AMOUNT, CurrencyIsoCode.USD.name(),
                AMOUNT, CurrencyIsoCode.USD.name(), "ledger-entry-sha256");
    }

    private void deleteSourceFacts() {
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE sn = ?", LEDGER_ENTRY_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE sn = ?", POSTING_PLAN_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", LEDGER_TRANSACTION_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE sn = ?", FUNDS_TRANSACTION_DETAIL_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE sn = ?", FUNDS_TRANSACTION_SN);
    }

    private int detailCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_clearing_splittable_detail", Integer.class);
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
