package com.wind.funds.transaction.application.instrument.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.services.FundsTransactionQueryService;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.application.spend.SpendControlAdmissionApplicationService;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.service.SpendControlMovementService;
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
import java.util.Optional;
import java.util.TreeMap;

/**
 * 支付工具授权处理器。
 *
 * <p>作为支付工具交易应用服务的内部协作者，完成授权重放校验、支付工具与资金责任准入、
 * Spend Rule 准入和账户主体型授权交易组装，不作为独立的 wallet-face 契约暴露。</p>
 *
 * @author Codex
 * @date 2026-06-18
 */
@Service
@AllArgsConstructor
public class PaymentInstrumentAuthorizationProcessor {

    private static final String SHA256_PREFIX = "sha256:";

    private static final String CONTROL_RESERVATION_MOVEMENT_DOMAIN = "SPEND_CONTROL_AUTHORIZATION_RESERVE";

    private static final String CONTROL_RESERVATION_MOVEMENT_PREFIX = "SCR";

    private static final int CONTROL_RESERVATION_MOVEMENT_MAX_LENGTH = 64;

    private final PaymentInstrumentPreTransactionSnapshotApplicationService preTransactionSnapshotApplicationService;

    private final SpendControlAdmissionApplicationService spendControlAdmissionApplicationService;

    private final SpendControlMovementService spendControlMovementService;

    private final FundsAuthorizationTransactionService authorizationTransactionService;

    private final FundsTransactionQueryService fundsTransactionQueryService;

    @Transactional(rollbackFor = Exception.class)
    public @NonNull String authorizeByInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                 @NonNull WindOperator operator) {
        return authorize(request, operator);
    }

    private String authorize(AuthorizeByPaymentInstrumentRequest request, WindOperator operator) {
        validateRequest(request);
        String establishedAuthorizationSn = findEstablishedAuthorizationReplay(request);
        if (establishedAuthorizationSn != null) {
            return establishedAuthorizationSn;
        }
        AuthorizationAdmissionDecision admissionDecision = resolveAdmissionDecision(request);
        String authorizationSn = authorizationTransactionService.authorize(convertToAuthorizeRequest(request,
                admissionDecision.snapshot(),
                admissionDecision.spendControlDecision(),
                admissionDecision.controlReservation()), operator);
        recordControlReservation(request, authorizationSn, admissionDecision);
        return authorizationSn;
    }

    private @Nullable String findEstablishedAuthorizationReplay(AuthorizeByPaymentInstrumentRequest request) {
        Optional<FundsTransactionDTO> existing = fundsTransactionQueryService.findFundsTransactionByBusiness(
                request.getTenantId(), request.getBusinessScene(), request.getBusinessSn());
        if (existing.isEmpty() || !isEstablished(existing.get().getStatus())) {
            return null;
        }
        FundsTransactionDTO transaction = existing.get();
        JSONObject context = JSON.parseObject(transaction.getContextVariables());
        if (context == null || !StringUtils.hasText(context.getString("instrumentSn"))) {
            return null;
        }
        assertEstablishedAuthorizationMatches(request, transaction, context);
        return transaction.getSn();
    }

    private boolean isEstablished(FundsTransactionStatus status) {
        return status == FundsTransactionStatus.OPEN
                || status == FundsTransactionStatus.CLOSED
                || status == FundsTransactionStatus.REJECTED;
    }

    private void assertEstablishedAuthorizationMatches(AuthorizeByPaymentInstrumentRequest request,
                                                        FundsTransactionDTO transaction,
                                                        JSONObject context) {
        AssertUtils.isTrue(transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION
                        && transaction.getTransactionType() == DefaultFundsTransactionType.PAY
                        && Objects.equals(transaction.getAmount(), request.getAmount())
                        && transaction.getCurrency() == request.getCurrency()
                        && Objects.equals(context.getString("instrumentSn"), request.getInstrumentSn())
                        && (request.getExpectedBindingVersion() == null
                        || Objects.equals(context.getInteger("instrumentBindingVersion"),
                        request.getExpectedBindingVersion()))
                        && Objects.equals(context.getBoolean(FundsInstructionContextKeys.APPROVED), request.getApproved())
                        && Objects.equals(context.getString(FundsInstructionContextKeys.DECLINE_REASON),
                        request.getDeclineReason())
                        && Objects.equals(context.getString(FundsInstructionContextKeys.TRANSACTION_COUNTRY),
                        enumName(request.getTransactionCountry())),
                "已成立授权请求参数不一致，transactionSn = {}，businessSn = {}",
                transaction.getSn(), request.getBusinessSn());
        assertEstablishedSpendDecisionMatches(request,
                context.getJSONObject(FundsInstructionContextKeys.SPEND_RULE_DECISION),
                transaction.getSn());
    }

    private void assertEstablishedSpendDecisionMatches(AuthorizeByPaymentInstrumentRequest request,
                                                       @Nullable JSONObject decision,
                                                       String transactionSn) {
        if (decision == null) {
            AssertUtils.isTrue(!hasSpendControlEvidence(request)
                            && (Boolean.FALSE.equals(request.getApproved())
                            || !StringUtils.hasText(request.getControlScopeId())),
                    "已成立授权 Spend Rule 证据不一致，transactionSn = {}", transactionSn);
            return;
        }
        AssertUtils.isTrue(Objects.equals(decision.getString("controlScopeId"), request.getControlScopeId())
                        && Objects.equals(decision.getString("periodId"), request.getPeriodId())
                        && matchesOptionalText(request.getSpendRuleId(), decision.getString("ruleId"))
                        && matchesOptionalText(request.getSpendRuleVersion(), decision.getString("ruleVersion"))
                        && matchesOptionalText(request.getSpendRuleBindingSn(), decision.getString("spendRuleBindingSn"))
                        && matchesOptionalEnum(request.getSpendRuleScopeType(), decision.getString("scopeType"))
                        && matchesOptionalText(request.getSpendRuleScopeId(), decision.getString("scopeId"))
                        && Objects.equals(request.getSpendDecisionSn(), decision.getString("decisionSn"))
                        && matchesOptionalEnum(request.getSpendDecisionResult(), decision.getString("decisionResult"))
                        && matchesOptionalText(request.getSpendDecisionDigest(), decision.getString("decisionDigest"))
                        && !StringUtils.hasText(request.getSpendDecisionRejectReason()),
                "已成立授权 Spend Rule 证据不一致，transactionSn = {}", transactionSn);
    }

    private boolean matchesOptionalText(@Nullable String provided, @Nullable String persisted) {
        return !StringUtils.hasText(provided) || Objects.equals(provided, persisted);
    }

    private boolean matchesOptionalEnum(@Nullable Enum<?> provided, @Nullable String persisted) {
        return provided == null || Objects.equals(provided.name(), persisted);
    }

    private @Nullable String enumName(@Nullable Enum<?> value) {
        return value == null ? null : value.name();
    }

    private void validateRequest(AuthorizeByPaymentInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
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
        AssertUtils.isTrue(!hasUnreferencedSpendControlPayload(request),
                "裸 Spend Rule 结果或摘要不能用于授权准入，必须提供可回读的 decisionRef");
    }

    private boolean hasSpendControlEvidence(AuthorizeByPaymentInstrumentRequest request) {
        // controlScopeId selects binding candidates; it is not decision evidence by itself.
        return StringUtils.hasText(request.getSpendRuleId())
                || StringUtils.hasText(request.getSpendRuleVersion())
                || StringUtils.hasText(request.getSpendRuleBindingSn())
                || request.getSpendRuleScopeType() != null
                || StringUtils.hasText(request.getSpendRuleScopeId())
                || StringUtils.hasText(request.getSpendDecisionSn())
                || request.getSpendDecisionResult() != null
                || StringUtils.hasText(request.getSpendDecisionDigest())
                || StringUtils.hasText(request.getSpendDecisionRejectReason());
    }

    private boolean hasUnreferencedSpendControlPayload(AuthorizeByPaymentInstrumentRequest request) {
        return hasSpendControlEvidence(request) && !StringUtils.hasText(request.getSpendDecisionSn());
    }

    private AuthorizationAdmissionDecision resolveAdmissionDecision(AuthorizeByPaymentInstrumentRequest request) {
        if (Boolean.TRUE.equals(request.getApproved()) || hasSpendControlEvidence(request)) {
            SpendControlAdmissionDecisionDTO decision =
                    spendControlAdmissionApplicationService.resolveSpendControlAdmission(toSpendControlRequest(request));
            AssertUtils.isTrue(Boolean.TRUE.equals(decision.getAdmitted()),
                    "Spend Rule 准入未通过，spendDecisionSn = {}, rejectReason = {}",
                    decision.getSpendDecisionSn(),
                    decision.getRejectReason());
            AssertUtils.notNull(decision.getPreTransactionSnapshot(), "Spend Rule 准入缺少预交易快照");
            return new AuthorizationAdmissionDecision(decision.getPreTransactionSnapshot(),
                    decision,
                    resolveControlReservation(request, decision));
        }
        return new AuthorizationAdmissionDecision(preTransactionSnapshotApplicationService
                .resolvePreTransactionSnapshot(toPreTransactionRequest(request)), null, null);
    }

    private @Nullable ControlReservation resolveControlReservation(
            AuthorizeByPaymentInstrumentRequest request,
            SpendControlAdmissionDecisionDTO decision) {
        if (!Boolean.TRUE.equals(request.getApproved())
                || decision.getSpendDecisionResult() != SpendControlDecisionResult.PASSED
                || !StringUtils.hasText(decision.getControlScopeId())) {
            return null;
        }
        AssertUtils.hasText(decision.getPeriodId(), "预算控制预留缺少控制周期标识");
        AssertUtils.notNull(decision.getTargetAccountId(), "预算控制预留缺少目标账户");
        return new ControlReservation(controlReservationMovementSn(request), decision.getPeriodId());
    }

    private String controlReservationMovementSn(AuthorizeByPaymentInstrumentRequest request) {
        String digest = FundsStableHashSupport.sha256(CONTROL_RESERVATION_MOVEMENT_DOMAIN
                + "|" + request.getTenantId()
                + "|" + request.getBusinessScene()
                + "|" + request.getBusinessSn());
        return CONTROL_RESERVATION_MOVEMENT_PREFIX + digest.substring(0,
                CONTROL_RESERVATION_MOVEMENT_MAX_LENGTH - CONTROL_RESERVATION_MOVEMENT_PREFIX.length());
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
                .setSpendRuleBindingSn(request.getSpendRuleBindingSn())
                .setSpendRuleScopeType(request.getSpendRuleScopeType())
                .setSpendRuleScopeId(request.getSpendRuleScopeId())
                .setSpendDecisionSn(request.getSpendDecisionSn())
                .setSpendDecisionResult(request.getSpendDecisionResult())
                .setSpendDecisionDigest(request.getSpendDecisionDigest())
                .setControlScopeId(request.getControlScopeId())
                .setPeriodId(request.getPeriodId())
                .setRejectReason(request.getSpendDecisionRejectReason());
    }

    private FundsAuthorizationTransactionAuthorizeRequest convertToAuthorizeRequest(
            AuthorizeByPaymentInstrumentRequest request,
            PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision,
            @Nullable ControlReservation controlReservation) {
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
                .setContextVariables(admissionContextVariables(snapshot, spendControlDecision, controlReservation))
                .setDescription(request.getDescription());
    }

    private @NonNull ReadonlyContextVariables admissionContextVariables(
            PaymentInstrumentPreTransactionSnapshotDTO snapshot,
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision,
            @Nullable ControlReservation controlReservation) {
        Map<String, Object> values = new LinkedHashMap<>(walletAdmissionContext(snapshot));
        Map<String, Object> decision = spendRuleDecisionSnapshot(spendControlDecision, controlReservation);
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
                || !FundsSubjectType.CREDIT_ACCOUNT.name().equals(authorizationAccountId.type())
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
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision,
            @Nullable ControlReservation controlReservation) {
        if (spendControlDecision == null) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        putIfNotNull(values, "decisionRecordId", spendControlDecision.getSpendDecisionRecordId());
        putIfText(values, "ruleId", spendControlDecision.getSpendRuleId());
        putIfText(values, "ruleVersion", spendControlDecision.getSpendRuleVersion());
        putIfText(values, "spendRuleBindingSn", spendControlDecision.getSpendRuleBindingSn());
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
        putIfText(values, "periodId", spendControlDecision.getPeriodId());
        if (controlReservation != null) {
            values.put("controlReservationSn", controlReservation.movementSn());
        }
        return Map.copyOf(values);
    }

    private void recordControlReservation(AuthorizeByPaymentInstrumentRequest request,
                                          String authorizationSn,
                                          AuthorizationAdmissionDecision admissionDecision) {
        ControlReservation reservation = admissionDecision.controlReservation();
        SpendControlAdmissionDecisionDTO decision = admissionDecision.spendControlDecision();
        if (reservation == null || decision == null) {
            return;
        }
        spendControlMovementService.recordMovement(new RecordSpendControlMovementRequest()
                .setTenantId(request.getTenantId())
                .setMovementSn(reservation.movementSn())
                .setMovementType(SpendControlMovementType.RESERVED)
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setTransactionSn(authorizationSn)
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setTargetAccountId(decision.getTargetAccountId())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setSpendRuleId(decision.getSpendRuleId())
                .setSpendRuleVersion(decision.getSpendRuleVersion())
                .setSpendDecisionSn(decision.getSpendDecisionSn())
                .setSpendDecisionResult(decision.getSpendDecisionResult())
                .setSpendDecisionDigest(decision.getSpendDecisionDigest())
                .setControlScopeId(decision.getControlScopeId())
                .setPeriodId(reservation.periodId())
                .setMovementDigest(controlReservationDigest(request, authorizationSn, decision, reservation))
                .setDescription("支付工具授权预算控制预留"));
    }

    private String controlReservationDigest(AuthorizeByPaymentInstrumentRequest request,
                                            String authorizationSn,
                                            SpendControlAdmissionDecisionDTO decision,
                                            ControlReservation reservation) {
        Map<String, Object> values = new TreeMap<>();
        values.put("amount", request.getAmount());
        values.put("businessScene", request.getBusinessScene());
        values.put("businessSn", request.getBusinessSn());
        values.put("controlScopeId", decision.getControlScopeId());
        values.put("currency", request.getCurrency().name());
        values.put("instrumentSn", request.getInstrumentSn());
        values.put("movementSn", reservation.movementSn());
        values.put("periodId", reservation.periodId());
        values.put("spendDecisionSn", decision.getSpendDecisionSn());
        values.put("spendRuleId", decision.getSpendRuleId());
        values.put("spendRuleVersion", decision.getSpendRuleVersion());
        values.put("targetAccountId",
                decision.getTargetAccountId().type() + ":" + decision.getTargetAccountId().id());
        values.put("tenantId", request.getTenantId());
        values.put("transactionSn", authorizationSn);
        return SHA256_PREFIX + FundsStableHashSupport.sha256Json(values);
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
            @Nullable SpendControlAdmissionDecisionDTO spendControlDecision,
            @Nullable ControlReservation controlReservation) {
    }

    private record ControlReservation(@NonNull String movementSn, @NonNull String periodId) {
    }
}
