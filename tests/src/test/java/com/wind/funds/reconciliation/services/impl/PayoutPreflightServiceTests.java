package com.wind.funds.reconciliation.services.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingReasonCode;
import com.wind.funds.reconciliation.enums.PayoutPreflightDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightFactStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightOperationStatus;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightBlockingReasonDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.request.CheckPayoutPreflightRequest;
import com.wind.funds.reconciliation.service.PayoutOrderService;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private PayoutOrderService payoutOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                minimumPayoutPreflightRequest(), WindOperator.system());

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
        assertThat(result.getCheckedBy()).isEqualTo(String.valueOf(WindOperator.system().getOperatorId()));
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
                readyPayoutPreflightRequest(), WindOperator.system());

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getBlockingLevel()).isEqualTo(PayoutPreflightBlockingLevel.PASSED);
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getFactStatus()).isEqualTo(PayoutPreflightFactStatus.PREFLIGHT_PASSED);
        assertThat(result.getDisplayStatus()).isEqualTo(PayoutPreflightDisplayStatus.READY_TO_SUBMIT);
        assertThat(result.getOperationStatus()).isEqualTo(PayoutPreflightOperationStatus.SUBMITTABLE);
        assertThat(result.getExternalRuleVerificationStatus()).isEqualTo(ExternalRuleVerificationStatus.VERIFIED);
        assertThat(result.getEvidenceRefs())
                .containsExactly("rule-evidence-001", "approval-001");
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
                readyPayoutPreflightRequest().setPayoutSn(null), WindOperator.system());

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
                WindOperator.system());

        assertThat(result.isPassed()).isFalse();
        assertThat(result.getExternalRuleVerificationStatus())
                .isEqualTo(ExternalRuleVerificationStatus.UNVERIFIED);
        assertThat(result.getBlockingReasons())
                .extracting(PayoutPreflightBlockingReasonDTO::getCode)
                .containsExactly(PayoutPreflightBlockingReasonCode.EXTERNAL_RULE_UNVERIFIED);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    private CheckPayoutPreflightRequest minimumPayoutPreflightRequest() {
        return new CheckPayoutPreflightRequest()
                .setTenantId(TENANT_ID)
                .setSettlementSn(SETTLEMENT_SN)
                .setPayoutSn(PAYOUT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setAmount(10_00L)
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

    @Configuration
    @Import(PayoutOrderServiceImpl.class)
    static class Config {
    }
}
