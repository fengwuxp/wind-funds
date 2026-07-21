package com.wind.funds.reconciliation.services.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.difference.ReconciliationDifferenceApplicationService;
import com.wind.funds.reconciliation.application.difference.impl.ReconciliationDifferenceApplicationServiceImpl;
import com.wind.funds.reconciliation.application.gate.impl.ReconciliationGateApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.application.run.impl.ReconciliationRunResultApplicationServiceImpl;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingReasonCode;
import com.wind.funds.reconciliation.enums.PayoutPreflightDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightFactStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightOperationStatus;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightBlockingReasonDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.request.CheckPayoutPreflightRequest;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.service.PayoutOrderService;
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

import java.time.LocalDate;
import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 出款前准入门禁服务层流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PayoutPreflightServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PayoutPreflightServiceTests extends AbstractFundsServiceTest {

    private static final String SETTLEMENT_SN = "settlement_preflight_001";

    private static final String PAYOUT_SN = "payout_preflight_001";

    private static final String IDEMPOTENCY_KEY = "idem_payout_preflight_001";

    private static final String DIFFERENCE_SN = "recon_payout_gate_diff_001";

    private static final String RECONCILIATION_BATCH_SN = "recon_payout_gate_batch_001";

    private static final String SOURCE_RECORD_SN = "processor_payout_gate_line_001";

    private static final String ADJUSTMENT_SN = "balance_adjust_payout_gate_001";

    private static final String RERUN_SN = "recon_payout_gate_rerun_001";

    private static final String RERUN_BATCH_SN = "recon_payout_gate_batch_001_rerun_001";

    @Autowired
    private PayoutOrderService payoutOrderService;

    @Autowired
    private ReconciliationDifferenceApplicationService reconciliationDifferenceApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String payoutRunResultSn;

    private String preCreateRunResultSn;

    @BeforeEach
    void prepareReconciliationEvidence() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference");
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
        payoutRunResultSn = recordBalancedRunResult(PAYOUT_SN, RERUN_BATCH_SN,
                "report:payout-recon-run-001");
        preCreateRunResultSn = recordBalancedRunResult(SETTLEMENT_SN, "recon_payout_precreate_batch_001",
                "report:payout-precreate-recon-run-001");
    }

    /**
     * 场景：调用方未提供出款前准入请求。
     * 结果：快速失败，不读取对账证据，不生成任何出款或账本事实。
     */
    @Test
    void testCheckPayoutPreflightShouldRejectNullRequest() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> payoutOrderService.checkPayoutPreflight(null, WindOperatorFactory.system()))
                .hasMessageContaining("出款前准入检查请求不能为空");

        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：提交出款前缺少账户、收款端点、通道、外部规则核验证据和审批证据。
     * 输入：结算单、出款单、金额和幂等键已给出，但所有出款准入证据缺失。
     * 输出：准入结果为阻断，并列出可解释的 blockingReasons。
     * 红线：出款前准入只做放行决策，不生成 ledger transaction、posting plan 或 entry。
     */
    @Test
    void testCheckPayoutPreflightShouldBlockWhenRequiredGateEvidenceMissingWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        PayoutPreflightResultDTO result = payoutOrderService.checkPayoutPreflight(
                minimumPayoutPreflightRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getBlockingLevel()).isEqualTo(PayoutPreflightBlockingLevel.BLOCKED);
        assertThat(result.getFactStatus()).isEqualTo(PayoutPreflightFactStatus.PREFLIGHT_BLOCKED);
        assertThat(result.getDisplayStatus()).isEqualTo(PayoutPreflightDisplayStatus.WAITING_EVIDENCE);
        assertThat(result.getOperationStatus()).isEqualTo(PayoutPreflightOperationStatus.BLOCKED);
        assertThat(result.getExternalRuleVerificationStatus())
                .isEqualTo(ExternalRuleVerificationStatus.UNVERIFIED);
        assertThat(result.getBlockingReasons())
                .extracting(PayoutPreflightBlockingReasonDTO::getCode)
                .containsExactly(
                        PayoutPreflightBlockingReasonCode.PAYOUT_ACCOUNT_INVALID,
                        PayoutPreflightBlockingReasonCode.PAYEE_ENDPOINT_INVALID,
                        PayoutPreflightBlockingReasonCode.CHANNEL_UNAVAILABLE,
                        PayoutPreflightBlockingReasonCode.EXTERNAL_RULE_UNVERIFIED,
                        PayoutPreflightBlockingReasonCode.APPROVAL_REQUIRED);
        assertThat(result.getCheckedAt()).isNotNull();
        assertThat(result.getCheckedBy()).isEqualTo(WindOperatorFactory.system().getOperatorAsText());
        assertThat(result.getExpiresAt()).isAfter(result.getCheckedAt());
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：提交出款前的账户、收款端点、通道、外部规则核验证据和审批证据齐备。
     * 输入：结算单、出款单、金额、幂等键和全部准入证据。
     * 输出：准入结果为通过，保留核验证据引用用于后续审计链路。
     * 红线：准入通过仍不代表已经出款或入账，不得生成账务事实。
     */
    @Test
    void testCheckPayoutPreflightShouldPassWhenRequiredGateEvidenceReadyWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        PayoutPreflightResultDTO result = payoutOrderService.checkPayoutPreflight(
                readyPayoutPreflightRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getBlockingLevel()).isEqualTo(PayoutPreflightBlockingLevel.PASSED);
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getFactStatus()).isEqualTo(PayoutPreflightFactStatus.PREFLIGHT_PASSED);
        assertThat(result.getDisplayStatus()).isEqualTo(PayoutPreflightDisplayStatus.READY_TO_SUBMIT);
        assertThat(result.getOperationStatus()).isEqualTo(PayoutPreflightOperationStatus.SUBMITTABLE);
        assertThat(result.getExternalRuleVerificationStatus()).isEqualTo(ExternalRuleVerificationStatus.VERIFIED);
        assertThat(result.getReconciliationRunResultSn()).isEqualTo(payoutRunResultSn);
        assertThat(result.getReconciliationResultDigest()).hasSize(64);
        assertThat(result.getEvidenceRefs())
                .containsExactly("rule-evidence-001", "approval-001", "report:payout-recon-run-001");
        assertThat(result.getCheckedAt()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(result.getCheckedAt());
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：创建出款单前先按结算单做准入检查，尚未生成 payoutSn。
     * 输入：结算单、金额、幂等键和全部准入证据齐备，出款单号为空。
     * 输出：准入结果通过，并返回服务端解释状态。
     * 红线：创建前检查不得强制要求已有出款单，也不得写入账务事实。
     */
    @Test
    void testCheckPayoutPreflightShouldAllowPreCreateCheckWithoutPayoutSn() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        PayoutPreflightResultDTO result = payoutOrderService.checkPayoutPreflight(
                readyPayoutPreflightRequest()
                        .setPayoutSn(null)
                        .setReconciliationRunResultSn(preCreateRunResultSn),
                WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getFactStatus()).isEqualTo(PayoutPreflightFactStatus.PREFLIGHT_PASSED);
        assertThat(result.getDisplayStatus()).isEqualTo(PayoutPreflightDisplayStatus.READY_TO_SUBMIT);
        assertThat(result.getOperationStatus()).isEqualTo(PayoutPreflightOperationStatus.SUBMITTABLE);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部规则只有证据号或缺少规则来源、版本、生效日、适用范围、法域、核验日、确认方、状态。
     * 输入：其他出款准入证据齐备，但外部规则核验证据缺少完整核验口径。
     * 输出：准入结果阻断，不能把单个 evidenceRef 当作已核验。
     * 红线：规则未核验或字段不完整时，不生成出款、route、posting 或 entry。
     */
    @Test
    void testCheckPayoutPreflightShouldBlockWhenExternalRuleEvidenceIncompleteWithoutLedgerFactsMutation() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        PayoutPreflightResultDTO result = payoutOrderService.checkPayoutPreflight(
                readyPayoutPreflightRequest()
                        .setExternalRuleVerificationEvidence(new ExternalRuleVerificationEvidenceDTO()
                                .setEvidenceRef("rule-evidence-incomplete")
                                .setStatus(ExternalRuleVerificationStatus.VERIFIED)),
                WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getExternalRuleVerificationStatus())
                .isEqualTo(ExternalRuleVerificationStatus.UNVERIFIED);
        assertThat(result.getBlockingReasons())
                .extracting(PayoutPreflightBlockingReasonDTO::getCode)
                .containsExactly(PayoutPreflightBlockingReasonCode.EXTERNAL_RULE_UNVERIFIED);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：出款前存在命中 PAYOUT 阻断范围的未闭环对账差错。
     * 输入：出款账户、收款端点、通道、外部规则和审批证据均齐备，但 PAYOUT 差错仍为 BLOCKED。
     * 输出：出款准入结果阻断，并把对账差错证据纳入解释和证据引用。
     * 红线：消费对账差错准入不得创建出款、交易、route、posting 或 ledger entry。
     */
    @Test
    void testCheckPayoutPreflightShouldBlockWhenPayoutReconciliationGateBlocked() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(payoutDifferenceRequest(), WindOperatorFactory.system());

        PayoutPreflightResultDTO result = payoutOrderService.checkPayoutPreflight(
                readyPayoutPreflightRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getBlockingLevel()).isEqualTo(PayoutPreflightBlockingLevel.BLOCKED);
        assertThat(result.getFactStatus()).isEqualTo(PayoutPreflightFactStatus.PREFLIGHT_BLOCKED);
        assertThat(result.getDisplayStatus()).isEqualTo(PayoutPreflightDisplayStatus.WAITING_EVIDENCE);
        assertThat(result.getOperationStatus()).isEqualTo(PayoutPreflightOperationStatus.BLOCKED);
        assertThat(result.getBlockingReasons())
                .extracting(PayoutPreflightBlockingReasonDTO::getCode)
                .containsExactly(PayoutPreflightBlockingReasonCode.RECONCILIATION_BLOCKED);
        PayoutPreflightBlockingReasonDTO blockingReason = result.getBlockingReasons().getFirst();
        assertThat(blockingReason.getGuardName()).isEqualTo("reconciliationGate");
        assertThat(blockingReason.getMessage()).contains("对账差错");
        assertThat(blockingReason.getEvidenceRef()).isEqualTo("report:payout-recon-run-001");
        assertThat(result.getEvidenceRefs())
                .containsExactly("rule-evidence-001", "approval-001", "report:payout-recon-run-001",
                        "processor-payout-file-digest-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：命中 PAYOUT 范围的差错已完成白名单处理并重新对账通过。
     * 输入：出款准入基础证据齐备，差错已回链处理动作和重跑对平证据。
     * 输出：出款准入可以继续提交，但保留差错、处理动作和重跑证据引用。
     * 红线：条件放行仍不代表已创建出款单或已经出款成功。
     */
    @Test
    void testCheckPayoutPreflightShouldPassWhenPayoutReconciliationGateConditionallyPassed() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        reconciliationDifferenceApplicationService.createDifference(payoutDifferenceRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.linkAdjustmentResult(payoutAdjustmentRequest(), WindOperatorFactory.system());
        reconciliationDifferenceApplicationService.recordRerunResult(payoutRerunRequest(), WindOperatorFactory.system());

        PayoutPreflightResultDTO result = payoutOrderService.checkPayoutPreflight(
                readyPayoutPreflightRequest(), WindOperatorFactory.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getBlockingLevel()).isEqualTo(PayoutPreflightBlockingLevel.PASSED);
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getFactStatus()).isEqualTo(PayoutPreflightFactStatus.PREFLIGHT_PASSED);
        assertThat(result.getOperationStatus()).isEqualTo(PayoutPreflightOperationStatus.SUBMITTABLE);
        assertThat(result.getEvidenceRefs())
                .containsExactly("rule-evidence-001", "approval-001", "report:payout-recon-run-001",
                        "processor-payout-file-digest-001",
                        "adjustment-evidence-payout-001", "rerun-report-payout-001");
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private CheckPayoutPreflightRequest minimumPayoutPreflightRequest() {
        return new CheckPayoutPreflightRequest()
                .setTenantId(TENANT_ID)
                .setSettlementSn(SETTLEMENT_SN)
                .setPayoutSn(PAYOUT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setAmount(10_00L)
                .setReconciliationRunResultSn(payoutRunResultSn)
                .setIdempotencyKey(IDEMPOTENCY_KEY);
    }

    private CheckPayoutPreflightRequest readyPayoutPreflightRequest() {
        return minimumPayoutPreflightRequest()
                .setPayoutAccountRef("funding-account-001")
                .setPayeeEndpointRef("bank-account-001")
                .setChannelRef("ach-standard")
                .setExternalRuleVerificationEvidence(completeExternalRuleVerificationEvidence())
                .setApprovalRef("approval-001");
    }

    private ExternalRuleVerificationEvidenceDTO completeExternalRuleVerificationEvidence() {
        return new ExternalRuleVerificationEvidenceDTO()
                .setEvidenceRef("rule-evidence-001")
                .setRuleSource("ACH payout operating rule")
                .setVersionOrPublishedAt("2026-05")
                .setEffectiveDate(LocalDate.of(2026, 5, 1))
                .setApplicableScope("US merchant USD payout")
                .setJurisdiction("US")
                .setVerifiedAt(LocalDate.of(2026, 5, 23))
                .setConfirmedBy("compliance-ops")
                .setStatus(ExternalRuleVerificationStatus.VERIFIED);
    }

    private CreateReconciliationDifferenceRequest payoutDifferenceRequest() {
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
                .setBlockingScope("PAYOUT")
                .setRuleVersion("recon-rule-v1")
                .setEvidenceRef("processor-payout-file-digest-001")
                .setDescription("外部 processor 出款文件金额与内部待出款金额不一致");
    }

    private LinkReconciliationDifferenceAdjustmentRequest payoutAdjustmentRequest() {
        return new LinkReconciliationDifferenceAdjustmentRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setActionType(ReconciliationDifferenceActionType.ADJUST)
                .setAdjustmentSn(ADJUSTMENT_SN)
                .setIdempotencyKey("idem-recon-payout-adjust-001")
                .setOriginalFactRef("external-payout-anomaly:issuer-ledger-001")
                .setAdjustmentTransactionSn("funds_tx_adjust_payout_gate_001")
                .setApprovalRef("approval-recon-payout-adjust-001")
                .setEvidenceRef("adjustment-evidence-payout-001")
                .setReason("已由余额控制调账纠偏，等待重新对账");
    }

    private RecordReconciliationDifferenceRerunRequest payoutRerunRequest() {
        return new RecordReconciliationDifferenceRerunRequest()
                .setTenantId(TENANT_ID)
                .setDifferenceSn(DIFFERENCE_SN)
                .setRerunSn(RERUN_SN)
                .setRerunBatchSn(RERUN_BATCH_SN)
                .setRuleVersion("recon-rule-v1")
                .setBalanced(true)
                .setEvidenceRef("rerun-report-payout-001")
                .setResultDigest("sha256:payout-rerun-balanced-001")
                .setDescription("调账后重新对账通过");
    }

    private String recordBalancedRunResult(String gateObjectSn,
                                           String reconciliationBatchSn,
                                           String evidenceRef) {
        return reconciliationRunResultApplicationService.recordRunResult(new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(reconciliationBatchSn)
                .setGateObjectType(ReconciliationGateObjectType.PAYOUT)
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
            PayoutOrderServiceImpl.class,
            ReconciliationDifferenceApplicationServiceImpl.class,
            ReconciliationRunResultApplicationServiceImpl.class,
            ReconciliationGateApplicationServiceImpl.class
    })
    static class Config {
    }
}
