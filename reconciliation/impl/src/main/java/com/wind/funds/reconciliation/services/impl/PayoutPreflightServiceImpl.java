package com.wind.funds.reconciliation.services.impl;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.application.gate.ReconciliationGateApplicationService;
import com.wind.funds.reconciliation.enums.ExternalRuleVerificationStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingLevel;
import com.wind.funds.reconciliation.enums.PayoutPreflightBlockingReasonCode;
import com.wind.funds.reconciliation.enums.PayoutPreflightDisplayStatus;
import com.wind.funds.reconciliation.enums.PayoutPreflightDecisionResult;
import com.wind.funds.reconciliation.enums.PayoutPreflightAction;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.funds.reconciliation.model.dto.ExternalRuleVerificationEvidenceDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightBlockingReasonDTO;
import com.wind.funds.reconciliation.model.dto.PayoutPreflightResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckPayoutPreflightRequest;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.service.PayoutPreflightService;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.core.context.TenantContextHolder;
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
 * 出款证据预检服务实现。
 *
 * <p>职责：在清结算出款提交前检查调用方提供的证据，返回可解释的预检通过或阻断结果。</p>
 *
 * <p>边界：本实现不读取结算、账户或通道权威事实，结果不是出款提交授权；
 * 不创建出款单、不调用通道、不写交易或账本事实。</p>
 */
@NullMarked
@Service
@AllArgsConstructor
public class PayoutPreflightServiceImpl implements PayoutPreflightService {

    private static final long PREFLIGHT_RESULT_EXPIRE_MINUTES = 5L;

    private static final String SYSTEM_CONFIRMATION_OWNER = "SYSTEM";

    private static final String OPERATIONS_CONFIRMATION_OWNER = "OPERATIONS";

    private final ReconciliationGateApplicationService reconciliationGateApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayoutPreflightResultDTO checkPayoutPreflight(CheckPayoutPreflightRequest request, WindOperator operator) {
        validateRequest(request);
        AssertUtils.notNull(operator, "出款前准入检查操作人不能为空");
        List<PayoutPreflightBlockingReasonDTO> blockingReasons = new ArrayList<>();
        addBlockingReasonIfMissing(blockingReasons, request.getPayoutAccountRef(),
                PayoutPreflightBlockingReasonCode.PAYOUT_ACCOUNT_REF_MISSING,
                "payoutAccountRef", "出款账户引用缺失", SYSTEM_CONFIRMATION_OWNER);
        addBlockingReasonIfMissing(blockingReasons, request.getPayeeEndpointRef(),
                PayoutPreflightBlockingReasonCode.PAYEE_ENDPOINT_REF_MISSING,
                "payeeEndpointRef", "收款端点引用缺失", OPERATIONS_CONFIRMATION_OWNER);
        addBlockingReasonIfMissing(blockingReasons, request.getChannelRef(),
                PayoutPreflightBlockingReasonCode.CHANNEL_REF_MISSING,
                "channelRef", "出款通道引用缺失", SYSTEM_CONFIRMATION_OWNER);
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
                .setManualReviewRequired(requiresManualReview(blockingReasons))
                .setDecisionResult(resolveDecisionResult(passed))
                .setDisplayStatus(resolveDisplayStatus(passed, blockingReasons))
                .setAction(resolveAction(passed))
                .setExternalRuleVerificationStatus(resolveExternalRuleVerificationStatus(request))
                .setStageRef(reconciliationGateDecision.getStageRef())
                .setCheckedAt(checkedAt)
                .setCheckedBy(operator.getOperatorAsText())
                .setExpiresAt(checkedAt.plusMinutes(PREFLIGHT_RESULT_EXPIRE_MINUTES))
                .setEvidenceRefs(evidenceRefs(request, reconciliationGateDecision));
    }

    private void validateRequest(CheckPayoutPreflightRequest request) {
        AssertUtils.notNull(request, "出款前准入检查请求不能为空");
        AssertUtils.notNull(request.getTenantId(), "出款前准入检查租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "出款前准入检查 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getSettlementSn(), "出款前准入检查结算单号不能为空");
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
        return reconciliationGateApplicationService.inspectGate(new CheckReconciliationGateRequest()
                .setTenantId(request.getTenantId())
                .setStageRef(payoutGateStageRef(request)), operator);
    }

    private GateStageRef payoutGateStageRef(CheckPayoutPreflightRequest request) {
        boolean existingPayout = StringUtils.hasText(request.getPayoutSn());
        return new GateStageRef()
                .setStageKind(existingPayout ? "PAYOUT_SUBMIT" : "PAYOUT_CREATE_PREFLIGHT")
                .setStageIdentity(new StableIdentity()
                        .setOwnerNamespace(existingPayout ? "payout-order" : "settlement-order")
                        .setValue(existingPayout ? request.getPayoutSn() : request.getSettlementSn()));
    }

    private void addReconciliationGateBlockingReasonIfBlocked(
            List<PayoutPreflightBlockingReasonDTO> blockingReasons,
            ReconciliationGateDecisionDTO reconciliationGateDecision) {
        if (reconciliationGateDecision.getDecisionResult() != ReconciliationGateDecisionResult.BLOCKED) {
            return;
        }
        blockingReasons.add(reconciliationBlockingReason(
                reconciliationGateDecision.getExplanation(), firstEvidenceRef(reconciliationGateDecision),
                null, null));
    }

    private PayoutPreflightBlockingReasonDTO reconciliationBlockingReason(String message,
                                                                          @Nullable String evidenceRef,
                                                                          @Nullable String differenceSn,
                                                                          @Nullable String responsiblePartyRef) {
        return new PayoutPreflightBlockingReasonDTO()
                .setCode(PayoutPreflightBlockingReasonCode.RECONCILIATION_BLOCKED)
                .setMessage("对账差错未闭环，出款准入阻断：" + message)
                .setGuardName("reconciliationGate")
                .setSeverity(PayoutPreflightBlockingLevel.BLOCKED)
                .setRecoverable(true)
                .setEvidenceRef(evidenceRef)
                .setRelatedDifferenceSn(differenceSn)
                .setConfirmationOwner(OPERATIONS_CONFIRMATION_OWNER)
                .setResponsiblePartyRef(responsiblePartyRef);
    }

    private boolean requiresManualReview(List<PayoutPreflightBlockingReasonDTO> blockingReasons) {
        return blockingReasons.stream()
                .anyMatch(reason -> OPERATIONS_CONFIRMATION_OWNER.equals(reason.getConfirmationOwner()));
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

    private PayoutPreflightDecisionResult resolveDecisionResult(boolean passed) {
        return passed ? PayoutPreflightDecisionResult.PREFLIGHT_PASSED
                : PayoutPreflightDecisionResult.PREFLIGHT_BLOCKED;
    }

    private PayoutPreflightDisplayStatus resolveDisplayStatus(
            boolean passed,
            List<PayoutPreflightBlockingReasonDTO> blockingReasons) {
        if (passed) {
            return PayoutPreflightDisplayStatus.PREFLIGHT_PASSED;
        }
        boolean reconciliationBlocked = blockingReasons.stream()
                .anyMatch(reason -> reason.getCode() == PayoutPreflightBlockingReasonCode.RECONCILIATION_BLOCKED);
        return reconciliationBlocked ? PayoutPreflightDisplayStatus.RECONCILIATION_REQUIRED
                : PayoutPreflightDisplayStatus.WAITING_EVIDENCE;
    }

    private PayoutPreflightAction resolveAction(boolean passed) {
        return passed ? PayoutPreflightAction.SUBMISSION_REVALIDATION_REQUIRED
                : PayoutPreflightAction.BLOCKED;
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
