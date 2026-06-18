package com.wind.funds.reconciliation.services.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingReasonCode;
import com.wind.funds.reconciliation.enums.PayoutPreflightDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightFactStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightOperationStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightBlockingReasonDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckPayoutPreflightRequest;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.service.PayoutOrderService;
import com.wind.common.exception.AssertUtils;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 出款单服务实现。
 *
 * <p>职责：在清结算出款提交前执行准入门禁，返回可解释的放行或阻断结果。</p>
 *
 * <p>边界：本实现不创建出款单、不调用通道、不写交易或账本事实。</p>
 */
@NullMarked
@Service
@AllArgsConstructor
public class PayoutOrderServiceImpl implements PayoutOrderService {

    private static final long PREFLIGHT_RESULT_EXPIRE_MINUTES = 5L;

    private static final String SYSTEM_CONFIRMATION_OWNER = "SYSTEM";

    private static final String OPERATIONS_CONFIRMATION_OWNER = "OPERATIONS";

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(readOnly = true)
    public PayoutPreflightResultDTO checkPayoutPreflight(CheckPayoutPreflightRequest request, WindOperator operator) {
        validateRequest(request);
        AssertUtils.notNull(operator, "出款前准入检查操作人不能为空");
        List<PayoutPreflightBlockingReasonDTO> blockingReasons = new ArrayList<>();
        addBlockingReasonIfMissing(blockingReasons, request.getPayoutAccountRef(),
                PayoutPreflightBlockingReasonCode.PAYOUT_ACCOUNT_INVALID,
                "payoutAccountRef", "出款账户缺失或无效", SYSTEM_CONFIRMATION_OWNER);
        addBlockingReasonIfMissing(blockingReasons, request.getPayeeEndpointRef(),
                PayoutPreflightBlockingReasonCode.PAYEE_ENDPOINT_INVALID,
                "payeeEndpointRef", "收款端点缺失或无效", OPERATIONS_CONFIRMATION_OWNER);
        addBlockingReasonIfMissing(blockingReasons, request.getChannelRef(),
                PayoutPreflightBlockingReasonCode.CHANNEL_UNAVAILABLE,
                "channelRef", "出款通道缺失或不可用", SYSTEM_CONFIRMATION_OWNER);
        addExternalRuleBlockingReasonIfUnverified(blockingReasons, request.getExternalRuleVerificationEvidence());
        addBlockingReasonIfMissing(blockingReasons, request.getApprovalRef(),
                PayoutPreflightBlockingReasonCode.APPROVAL_REQUIRED,
                "approvalRef", "审批证据缺失", OPERATIONS_CONFIRMATION_OWNER);
        ReconciliationGateDecisionDTO reconciliationGateDecision = checkReconciliationGate(request, operator);
        addReconciliationGateBlockingReasonIfBlocked(blockingReasons, reconciliationGateDecision);

        LocalDateTime checkedAt = LocalDateTime.now();
        boolean passed = blockingReasons.isEmpty();
        return new PayoutPreflightResultDTO()
                .setPassed(passed)
                .setBlockingLevel(passed ? PayoutPreflightBlockingLevel.PASSED : PayoutPreflightBlockingLevel.BLOCKED)
                .setBlockingReasons(List.copyOf(blockingReasons))
                .setManualReviewRequired(!passed)
                .setFactStatus(resolveFactStatus(passed))
                .setDisplayStatus(resolveDisplayStatus(passed))
                .setOperationStatus(resolveOperationStatus(passed))
                .setExternalRuleVerificationStatus(resolveExternalRuleVerificationStatus(request))
                .setCheckedAt(checkedAt)
                .setCheckedBy(String.valueOf(operator.getOperatorId()))
                .setExpiresAt(checkedAt.plusMinutes(PREFLIGHT_RESULT_EXPIRE_MINUTES))
                .setEvidenceRefs(evidenceRefs(request, reconciliationGateDecision));
    }

    private void validateRequest(CheckPayoutPreflightRequest request) {
        AssertUtils.notNull(request.getTenantId(), "出款前准入检查租户 ID 不能为空");
        AssertUtils.hasText(request.getSettlementSn(), "出款前准入检查结算单号不能为空");
        AssertUtils.notNull(request.getCurrency(), "出款前准入检查币种不能为空");
        AssertUtils.notNull(request.getAmount(), "出款前准入检查金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0, "出款前准入检查金额必须大于 0");
        AssertUtils.hasText(request.getIdempotencyKey(), "出款前准入检查幂等键不能为空");
    }

    private void addBlockingReasonIfMissing(List<PayoutPreflightBlockingReasonDTO> blockingReasons,
                                            @Nullable String value,
                                            PayoutPreflightBlockingReasonCode code,
                                            String guardName,
                                            String message,
                                            String confirmationOwner) {
        if (StringUtils.hasText(value)) {
            return;
        }
        blockingReasons.add(new PayoutPreflightBlockingReasonDTO()
                .setCode(code)
                .setMessage(message)
                .setGuardName(guardName)
                .setSeverity(PayoutPreflightBlockingLevel.BLOCKED)
                .setRecoverable(true)
                .setConfirmationOwner(confirmationOwner));
    }

    private void addExternalRuleBlockingReasonIfUnverified(
            List<PayoutPreflightBlockingReasonDTO> blockingReasons,
            @Nullable ExternalRuleVerificationEvidenceDTO evidence) {
        if (isExternalRuleVerified(evidence)) {
            return;
        }
        blockingReasons.add(new PayoutPreflightBlockingReasonDTO()
                .setCode(PayoutPreflightBlockingReasonCode.EXTERNAL_RULE_UNVERIFIED)
                .setMessage("外部规则核验证据缺失或不完整")
                .setGuardName("externalRuleVerificationEvidence")
                .setSeverity(PayoutPreflightBlockingLevel.BLOCKED)
                .setRecoverable(true)
                .setEvidenceRef(evidence == null ? null : evidence.getEvidenceRef())
                .setConfirmationOwner(OPERATIONS_CONFIRMATION_OWNER));
    }

    private ExternalRuleVerificationStatus resolveExternalRuleVerificationStatus(CheckPayoutPreflightRequest request) {
        if (isExternalRuleVerified(request.getExternalRuleVerificationEvidence())) {
            return ExternalRuleVerificationStatus.VERIFIED;
        }
        return ExternalRuleVerificationStatus.UNVERIFIED;
    }

    private ReconciliationGateDecisionDTO checkReconciliationGate(CheckPayoutPreflightRequest request,
                                                                 WindOperator operator) {
        return reconciliationGateApplicationService.checkGate(new CheckReconciliationGateRequest()
                .setTenantId(request.getTenantId())
                .setGateObjectType(ReconciliationGateObjectType.PAYOUT)
                .setGateObjectSn(payoutGateObjectSn(request)), operator);
    }

    private String payoutGateObjectSn(CheckPayoutPreflightRequest request) {
        if (StringUtils.hasText(request.getPayoutSn())) {
            return request.getPayoutSn();
        }
        return request.getSettlementSn();
    }

    private void addReconciliationGateBlockingReasonIfBlocked(
            List<PayoutPreflightBlockingReasonDTO> blockingReasons,
            ReconciliationGateDecisionDTO reconciliationGateDecision) {
        if (reconciliationGateDecision.getDecisionStatus() != ReconciliationGateDecisionStatus.BLOCKED) {
            return;
        }
        blockingReasons.add(new PayoutPreflightBlockingReasonDTO()
                .setCode(PayoutPreflightBlockingReasonCode.RECONCILIATION_BLOCKED)
                .setMessage("对账差错未闭环，出款准入阻断：" + reconciliationGateDecision.getExplanation())
                .setGuardName("reconciliationGate")
                .setSeverity(PayoutPreflightBlockingLevel.BLOCKED)
                .setRecoverable(true)
                .setEvidenceRef(firstEvidenceRef(reconciliationGateDecision))
                .setConfirmationOwner(OPERATIONS_CONFIRMATION_OWNER));
    }

    private @Nullable String firstEvidenceRef(ReconciliationGateDecisionDTO reconciliationGateDecision) {
        List<String> evidenceRefs = reconciliationGateDecision.getEvidenceRefs();
        if (evidenceRefs == null || evidenceRefs.isEmpty()) {
            return null;
        }
        return evidenceRefs.getFirst();
    }

    private List<String> evidenceRefs(CheckPayoutPreflightRequest request,
                                      ReconciliationGateDecisionDTO reconciliationGateDecision) {
        Set<String> result = new LinkedHashSet<>();
        ExternalRuleVerificationEvidenceDTO externalRuleEvidence = request.getExternalRuleVerificationEvidence();
        if (externalRuleEvidence != null && StringUtils.hasText(externalRuleEvidence.getEvidenceRef())) {
            result.add(externalRuleEvidence.getEvidenceRef());
        }
        addText(result, request.getApprovalRef());
        List<String> gateEvidenceRefs = reconciliationGateDecision.getEvidenceRefs();
        if (gateEvidenceRefs != null) {
            gateEvidenceRefs.forEach(evidenceRef -> addText(result, evidenceRef));
        }
        return List.copyOf(result);
    }

    private void addText(Set<String> result, @Nullable String value) {
        if (StringUtils.hasText(value)) {
            result.add(value);
        }
    }

    private PayoutPreflightFactStatus resolveFactStatus(boolean passed) {
        return passed ? PayoutPreflightFactStatus.PREFLIGHT_PASSED
                : PayoutPreflightFactStatus.PREFLIGHT_BLOCKED;
    }

    private PayoutPreflightDisplayStatus resolveDisplayStatus(boolean passed) {
        return passed ? PayoutPreflightDisplayStatus.READY_TO_SUBMIT
                : PayoutPreflightDisplayStatus.WAITING_EVIDENCE;
    }

    private PayoutPreflightOperationStatus resolveOperationStatus(boolean passed) {
        return passed ? PayoutPreflightOperationStatus.SUBMITTABLE
                : PayoutPreflightOperationStatus.BLOCKED;
    }

    private boolean isExternalRuleVerified(@Nullable ExternalRuleVerificationEvidenceDTO evidence) {
        if (evidence == null || evidence.getStatus() != ExternalRuleVerificationStatus.VERIFIED) {
            return false;
        }
        return StringUtils.hasText(evidence.getEvidenceRef())
                && StringUtils.hasText(evidence.getRuleSource())
                && StringUtils.hasText(evidence.getVersionOrPublishedAt())
                && evidence.getEffectiveDate() != null
                && StringUtils.hasText(evidence.getApplicableScope())
                && StringUtils.hasText(evidence.getJurisdiction())
                && evidence.getVerifiedAt() != null
                && StringUtils.hasText(evidence.getConfirmedBy());
    }
}
