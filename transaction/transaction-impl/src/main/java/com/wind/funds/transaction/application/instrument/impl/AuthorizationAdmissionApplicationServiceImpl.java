package com.wind.funds.transaction.application.instrument.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.instrument.AuthorizationAdmissionApplicationService;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlAdmissionApplicationService;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.support.SpendRuleDigestValidator;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付工具授权准入应用服务实现。
 *
 * @author Codex
 * @date 2026-06-18
 */
@Service
@AllArgsConstructor
public class AuthorizationAdmissionApplicationServiceImpl implements AuthorizationAdmissionApplicationService {

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final SpendControlAdmissionApplicationService spendControlAdmissionApplicationService;

    private final FundsAuthorizationTransactionService authorizationTransactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String authorizeByInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                 @NonNull WindOperator operator) {
        return authorize(request, operator);
    }

    private String authorize(AuthorizeByPaymentInstrumentRequest request, WindOperator operator) {
        validateRequest(request);
        AuthorizationAdmissionDecision admissionDecision = resolveAdmissionDecision(request);
        return authorizationTransactionService.authorize(convertToAuthorizeRequest(request,
                admissionDecision.snapshot(),
                admissionDecision.spendControlDecision()), operator);
    }

    private void validateRequest(AuthorizeByPaymentInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "支付工具授权 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAmount(), "授权金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "授权金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "授权币种不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "授权业务流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "授权业务场景不能为空");
        AssertUtils.notNull(request.getApproved(), "授权是否通过不能为空");
        if (Boolean.FALSE.equals(request.getApproved())) {
            AssertUtils.hasText(request.getDeclineReason(), "授权拒绝原因不能为空");
        }
        if (hasSpendControlEvidence(request)) {
            assertCompleteSpendControlEvidence(request);
        }
    }

    private boolean hasSpendControlEvidence(AuthorizeByPaymentInstrumentRequest request) {
        return StringUtils.hasText(request.getSpendRuleId())
                || StringUtils.hasText(request.getSpendRuleVersion())
                || StringUtils.hasText(request.getSpendRuleAssignmentSn())
                || request.getSpendRuleScopeType() != null
                || StringUtils.hasText(request.getSpendRuleScopeId())
                || StringUtils.hasText(request.getSpendDecisionSn())
                || request.getSpendDecisionResult() != null
                || StringUtils.hasText(request.getSpendDecisionDigest())
                || StringUtils.hasText(request.getControlScopeId())
                || StringUtils.hasText(request.getSpendDecisionRejectReason());
    }

    private void assertCompleteSpendControlEvidence(AuthorizeByPaymentInstrumentRequest request) {
        AssertUtils.hasText(request.getSpendRuleId(), "Spend Rule 标识不能为空");
        AssertUtils.hasText(request.getSpendRuleVersion(), "Spend Rule 版本不能为空");
        AssertUtils.notNull(request.getSpendRuleScopeType(), "Spend Rule 控制范围类型不能为空");
        AssertUtils.hasText(request.getSpendRuleScopeId(), "Spend Rule 控制范围标识不能为空");
        AssertUtils.hasText(request.getSpendDecisionSn(), "Spend Rule 决策流水号不能为空");
        AssertUtils.notNull(request.getSpendDecisionResult(), "Spend Rule 决策结果不能为空");
        SpendRuleDigestValidator.assertSha256Digest(request.getSpendDecisionDigest(), "Spend Rule 决策摘要");
        if (request.getSpendDecisionResult() == SpendControlDecisionResult.REJECTED) {
            AssertUtils.hasText(request.getSpendDecisionRejectReason(), "Spend Rule 拒绝原因不能为空");
        }
    }

    private AuthorizationAdmissionDecision resolveAdmissionDecision(AuthorizeByPaymentInstrumentRequest request) {
        if (hasSpendControlEvidence(request)) {
            SpendControlAdmissionDecisionDTO decision =
                    spendControlAdmissionApplicationService.resolveSpendControlAdmission(toSpendControlRequest(request));
            AssertUtils.isTrue(Boolean.TRUE.equals(decision.getAdmitted()),
                    "Spend Rule 准入未通过，spendDecisionSn = {}, rejectReason = {}",
                    decision.getSpendDecisionSn(),
                    decision.getRejectReason());
            AssertUtils.notNull(decision.getPreTransactionSnapshot(), "Spend Rule 准入缺少预交易快照");
            return new AuthorizationAdmissionDecision(decision.getPreTransactionSnapshot(), decision);
        }
        return new AuthorizationAdmissionDecision(preTransactionSnapshotApplicationService
                .resolvePreTransactionSnapshot(toPreTransactionRequest(request)), null);
    }

    private ResolvePaymentInstrumentPreTransactionSnapshotRequest toPreTransactionRequest(
            AuthorizeByPaymentInstrumentRequest request) {
        return new ResolvePaymentInstrumentPreTransactionSnapshotRequest()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(request.getExpectedBindingVersion())
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn());
    }

    private ResolveSpendControlAdmissionRequest toSpendControlRequest(AuthorizeByPaymentInstrumentRequest request) {
        return new ResolveSpendControlAdmissionRequest()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(request.getExpectedBindingVersion())
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setSpendRuleId(request.getSpendRuleId())
                .setSpendRuleVersion(request.getSpendRuleVersion())
                .setSpendRuleAssignmentSn(request.getSpendRuleAssignmentSn())
                .setSpendRuleScopeType(request.getSpendRuleScopeType())
                .setSpendRuleScopeId(request.getSpendRuleScopeId())
                .setSpendDecisionSn(request.getSpendDecisionSn())
                .setSpendDecisionResult(request.getSpendDecisionResult())
                .setSpendDecisionDigest(request.getSpendDecisionDigest())
                .setControlScopeId(request.getControlScopeId())
                .setRejectReason(request.getSpendDecisionRejectReason());
    }

    private FundsAuthorizationTransactionAuthorizeRequest convertToAuthorizeRequest(
            AuthorizeByPaymentInstrumentRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision) {
        FundsAccountId authorizationAccountId = authorizationAccountId(snapshot);
        FundsAccountId linkedFundingAccountId = linkedFundingAccountId(authorizationAccountId,
                snapshot.getTargetAccountId());
        return new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(authorizationAccountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(request.getAmount(),
                        request.getCurrency())))
                .setBusinessSn(request.getBusinessSn())
                .setBusinessScene(request.getBusinessScene())
                .setApproved(request.getApproved())
                .setAuthorizedTime(request.getAuthorizedTime())
                .setTransactionCountry(request.getTransactionCountry())
                .setDeclineReason(request.getDeclineReason())
                .setPaymentInstrumentRef(paymentInstrumentRef(request, snapshot.getPaymentInstrumentCapability()))
                .setLinkedFundingAccountId(linkedFundingAccountId)
                .setContextVariables(admissionContextVariables(snapshot, spendControlDecision))
                .setDescription(request.getDescription());
    }

    private @NonNull ReadonlyContextVariables admissionContextVariables(
            PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision) {
        Map<String, Object> values = new LinkedHashMap<>(walletAdmissionContext(snapshot));
        Map<String, Object> decision = spendRuleDecisionSnapshot(spendControlDecision);
        if (!decision.isEmpty()) {
            values.put(FundsInstructionContextKeys.SPEND_RULE_DECISION, decision);
        }
        return ReadonlyContextVariables.of(values);
    }

    private FundsAccountId authorizationAccountId(PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        PaymentInstrumentCapabilityDecisionDTO instrument = snapshot.getPaymentInstrumentCapability();
        return FundsAccountId.immutable(instrument.getSubjectId(), instrument.getSubjectType());
    }

    private @Nullable FundsAccountId linkedFundingAccountId(FundsAccountId authorizationAccountId,
                                                            FundsAccountId targetAccountId) {
        if (Objects.equals(authorizationAccountId, targetAccountId)
                || !FundsSubjectType.FUNDING_ACCOUNT.name().equals(targetAccountId.type())) {
            return null;
        }
        return targetAccountId;
    }

    private @NonNull Map<String, Object> walletAdmissionContext(PaymentInstrumentPreTransactionSnapshotDTO snapshot) {
        PaymentInstrumentCapabilityDecisionDTO instrument = snapshot.getPaymentInstrumentCapability();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("instrumentSn", snapshot.getInstrumentSn());
        values.put("instrumentAction", PaymentInstrumentAction.AUTHORIZE.name());
        values.put("instrumentBindingRole", snapshot.getBindingRole().name());
        values.put("instrumentBindingSn", instrument.getBindingSn());
        values.put("instrumentBindingVersion", instrument.getBindingVersion());
        values.put("fundingRelationSn", snapshot.getFundingResponsibility().getRelationSn());
        values.put("fundingRelationType", snapshot.getRelationType().name());
        values.put("targetAccountId", snapshot.getTargetAccountId().id());
        values.put("targetAccountType", snapshot.getTargetAccountId().type());
        return Map.copyOf(values);
    }

    private @NonNull Map<String, Object> spendRuleDecisionSnapshot(
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision) {
        if (spendControlDecision == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        putIfNotNull(values, "decisionRecordId", spendControlDecision.getSpendDecisionRecordId());
        putIfText(values, "ruleId", spendControlDecision.getSpendRuleId());
        putIfText(values, "ruleVersion", spendControlDecision.getSpendRuleVersion());
        putIfText(values, "assignmentSn", spendControlDecision.getSpendRuleAssignmentSn());
        if (spendControlDecision.getSpendRuleScopeType() != null) {
            values.put("scopeType", spendControlDecision.getSpendRuleScopeType().name());
        }
        putIfText(values, "scopeId", spendControlDecision.getSpendRuleScopeId());
        putIfText(values, "decisionSn", spendControlDecision.getSpendDecisionSn());
        if (spendControlDecision.getSpendDecisionResult() != null) {
            values.put("decisionResult", spendControlDecision.getSpendDecisionResult().name());
        }
        putIfText(values, "decisionDigest", spendControlDecision.getSpendDecisionDigest());
        putIfText(values, "controlScopeId", spendControlDecision.getControlScopeId());
        return Map.copyOf(values);
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(AuthorizeByPaymentInstrumentRequest request,
                                                          PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        assertPaymentInstrumentSnapshotReady(instrumentDecision);
        return ImmutablePaymentInstrumentRefSpec.builder()
                .tenantId(instrumentDecision.getTenantId())
                .instrumentId(instrumentDecision.getInstrumentSn())
                .instrumentType(instrumentDecision.getInstrumentType())
                .instrumentNo(instrumentDecision.getInstrumentNo())
                .ownerId(instrumentDecision.getOwnerId())
                .ownerType(instrumentDecision.getOwnerType().name())
                .currency(instrumentDecision.getCurrency().name())
                .status(instrumentDecision.getStatus().name())
                .bindingSnapshot(bindingSnapshot(request, instrumentDecision))
                .description(instrumentDecision.getDescription())
                .build();
    }

    private void assertPaymentInstrumentSnapshotReady(PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        AssertUtils.hasText(instrumentDecision.getInstrumentSn(), "支付工具快照工具号不能为空");
        AssertUtils.hasText(instrumentDecision.getInstrumentNo(), "支付工具快照展示号不能为空");
        AssertUtils.hasText(instrumentDecision.getOwnerId(), "支付工具快照归属主体 ID 不能为空");
        AssertUtils.notNull(instrumentDecision.getOwnerType(), "支付工具快照归属主体类型不能为空");
        AssertUtils.hasText(instrumentDecision.getInstrumentType(), "支付工具快照类型不能为空");
        AssertUtils.notNull(instrumentDecision.getCurrency(), "支付工具快照币种不能为空");
        AssertUtils.notNull(instrumentDecision.getStatus(), "支付工具快照状态不能为空");
        AssertUtils.hasText(instrumentDecision.getBindingSn(), "支付工具绑定快照绑定号不能为空");
        AssertUtils.notNull(instrumentDecision.getBindingVersion(), "支付工具绑定快照版本不能为空");
        AssertUtils.notNull(instrumentDecision.getBindingRole(), "支付工具绑定快照角色不能为空");
        AssertUtils.notNull(instrumentDecision.getSubjectType(), "支付工具绑定快照主体类型不能为空");
        AssertUtils.hasText(instrumentDecision.getSubjectId(), "支付工具绑定快照主体 ID 不能为空");
    }

    private Map<String, Object> bindingSnapshot(AuthorizeByPaymentInstrumentRequest request,
                                                PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingSn", instrumentDecision.getBindingSn());
        values.put("bindingVersion", instrumentDecision.getBindingVersion());
        values.put("bindingRole", instrumentDecision.getBindingRole().name());
        values.put("subjectType", instrumentDecision.getSubjectType().name());
        values.put("subjectId", instrumentDecision.getSubjectId());
        values.put("admissionAction", PaymentInstrumentAction.AUTHORIZE.name());
        values.put("admissionDecision", Boolean.TRUE.equals(request.getApproved()) ? "APPROVED" : "DECLINED");
        return values;
    }

    private void putIfText(@NonNull Map<String, Object> values, @NonNull String key, @Nullable String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value);
        }
    }

    private void putIfNotNull(@NonNull Map<String, Object> values, @NonNull String key, @Nullable Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    private record AuthorizationAdmissionDecision(
            @NonNull PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision) {
    }
}
