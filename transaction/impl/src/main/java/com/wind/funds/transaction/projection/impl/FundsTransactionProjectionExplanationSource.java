package com.wind.funds.transaction.projection.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.model.dto.FundsTransactionDTO;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.transaction.core.Money;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.time.LocalDateTime;

/**
 * 交易投影解释的稳定事实来源。
 *
 * <p>职责：把主写链路内存态事实或查询链路持久化事实归一成同一套投影解释口径。</p>
 *
 * <p>边界：本对象只做确定性解释转换，不查询数据库、不发布投影、不修改资金、账本或余额事实。</p>
 */
@Builder
record FundsTransactionProjectionExplanationSource(@NonNull String businessScene,
                                                          @NonNull String businessSn,
                                                          @NonNull String fundsTransactionSn,
                                                          @NonNull RouteSnapshotSpec routeSnapshot,
                                                          @Nullable String ownerType,
                                                          @Nullable String ownerId,
                                                          @Nullable LocalDateTime occurredTime,
                                                          @Nullable String ledgerTransactionSn,
                                                          boolean completed,
                                                          boolean failed,
                                                          @NonNull FundsTransactionEventType eventType,
                                                          @NonNull Money amount,
                                                          @NonNull Map<String, Object> contextVariables,
                                                          @Nullable String failureReasonOverride) {

    private static final String NOT_APPLICABLE = "N/A";

    private static final String FACT_STATUS_POSTED = "POSTED";

    private static final String FACT_STATUS_HELD = "HELD";

    private static final String FACT_STATUS_RELEASED = "RELEASED";

    private static final String FACT_STATUS_REJECTED = "REJECTED";

    private static final String FACT_STATUS_FAILED = "FAILED";

    private static final String FACT_STATUS_COMPLETED_NO_LEDGER = "COMPLETED_NO_LEDGER";

    private static final String FACT_STATUS_PROCESSING = "PROCESSING";

    private static final String DISPLAY_STATUS_SUCCEEDED = "SUCCEEDED";

    private static final String DISPLAY_STATUS_AUTHORIZED_HOLD = "AUTHORIZED_HOLD";

    private static final String DISPLAY_STATUS_FROZEN = "FROZEN";

    private static final String DISPLAY_STATUS_RELEASED = "RELEASED";

    private static final String DISPLAY_STATUS_REFUNDED = "REFUNDED";

    private static final String DISPLAY_STATUS_NO_AUTH_REFUNDED = "NO_AUTH_REFUNDED";

    private static final String DISPLAY_STATUS_DISPUTE_REFUNDED = "DISPUTE_REFUNDED";

    private static final String DISPLAY_STATUS_DECLINED = "DECLINED";

    private static final String DISPLAY_STATUS_FAILED = "FAILED";

    private static final String DISPLAY_STATUS_PROCESSING = "PROCESSING";

    private static final String OPERATION_STATUS_NO_ACTION_REQUIRED = "NO_ACTION_REQUIRED";

    private static final String OPERATION_STATUS_WAITING_CAPTURE_OR_RELEASE = "WAITING_CAPTURE_OR_RELEASE";

    private static final String OPERATION_STATUS_WAITING_UNFREEZE_OR_CONSUME = "WAITING_UNFREEZE_OR_CONSUME";

    private static final String OPERATION_STATUS_WAITING_FACT_COMPLETION = "WAITING_FACT_COMPLETION";

    private static final String STATUS_MEANING_FUNDS_POSTED = "FUNDS_POSTED";

    private static final String STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED =
            "AUTHORIZATION_HELD_NOT_CAPTURED";

    private static final String STATUS_MEANING_AUTHORIZATION_DECLINED_NO_FUNDS_POSTED =
            "AUTHORIZATION_DECLINED_NO_FUNDS_POSTED";

    private static final String STATUS_MEANING_BALANCE_FROZEN_NOT_CONSUMED =
            "BALANCE_FROZEN_NOT_CONSUMED";

    private static final String STATUS_MEANING_FUNDS_RELEASED = "FUNDS_RELEASED";

    private static final String STATUS_MEANING_FUNDS_REFUNDED = "FUNDS_REFUNDED";

    private static final String STATUS_MEANING_NO_AUTH_REFUND_POSTED = "NO_AUTH_REFUND_POSTED";

    private static final String STATUS_MEANING_DISPUTE_REFUND_POSTED = "DISPUTE_REFUND_POSTED";

    private static final String STATUS_MEANING_FACT_PROCESSING = "FACT_PROCESSING";

    private static final String STATUS_MEANING_FACT_FAILED = "FACT_FAILED";

    private static final String STATUS_MEANING_COMPLETED_WITHOUT_LEDGER = "COMPLETED_WITHOUT_LEDGER";

    private static final String NEXT_ACTION_WAIT_FOR_CAPTURE_OR_RELEASE = "WAIT_FOR_CAPTURE_OR_RELEASE";

    private static final String NEXT_ACTION_WAIT_FOR_UNFREEZE_OR_CONSUME = "WAIT_FOR_UNFREEZE_OR_CONSUME";

    private static final String NEXT_ACTION_WAIT_FOR_FACT_COMPLETION = "WAIT_FOR_FACT_COMPLETION";

    private static final String NEXT_ACTION_REVIEW_FAILURE = "REVIEW_FAILURE";

    private static final String UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION =
            "AUTHORIZATION_HOLD_IS_NOT_FINAL_CONSUMPTION";

    private static final String UNAVAILABLE_AUTHORIZATION_DECLINED = "AUTHORIZATION_DECLINED";

    private static final String UNAVAILABLE_BALANCE_FREEZE_NOT_CONSUMPTION = "BALANCE_FREEZE_IS_NOT_CONSUMPTION";

    private static final String UNAVAILABLE_TRANSACTION_FAILED = "TRANSACTION_FAILED";

    private static final String PAYMENT_INSTRUMENT_REF_CONTEXT_KEY = "paymentInstrumentRef";

    private static final String EXTERNAL_ACCOUNT_REF_CONTEXT_KEY = "externalAccountRef";

    private static final String TRANSACTION_SUMMARY_CONTEXT_KEY = "transactionSummary";

    private static final String INSTRUMENT_SN_FIELD = "instrumentSn";

    private static final String INSTRUMENT_TYPE_FIELD = "instrumentType";

    private static final String INSTRUMENT_NO_FIELD = "instrumentNo";

    private static final String OWNER_ID_FIELD = "ownerId";

    private static final String OWNER_TYPE_FIELD = "ownerType";

    private static final String TENANT_ID_FIELD = "tenantId";

    private static final String CURRENCY_FIELD = "currency";

    private static final String STATE_FIELD = "state";

    private static final String BINDING_SNAPSHOT_FIELD = "bindingSnapshot";

    private static final String BINDING_SN_FIELD = "bindingSn";

    private static final String BINDING_VERSION_FIELD = "bindingVersion";

    private static final String DESCRIPTION_FIELD = "description";

    private static final String EXTERNAL_ACCOUNT_ID_FIELD = "externalAccountId";

    private static final String EXTERNAL_ACCOUNT_TYPE_FIELD = "externalAccountType";

    private static final String PROVIDER_CODE_FIELD = "providerCode";

    private static final String CHANNEL_CODE_FIELD = "channelCode";

    private static final String COUNTRY_CODE_FIELD = "countryCode";

    private static final String AMOUNT_FIELD = "amount";

    private static final String AUTHORIZED_AMOUNT_FIELD = "authorizedAmount";

    private static final String COMPLETED_AMOUNT_FIELD = "completedAmount";

    private static final String REVERSED_AMOUNT_FIELD = "reversedAmount";

    private static final String REFUNDED_AMOUNT_FIELD = "refundedAmount";

    private static final String DECLINED_AMOUNT_FIELD = "declinedAmount";

    private static final String FEE_AMOUNT_FIELD = "feeAmount";

    private static final String REMAINING_AUTHORIZATION_AMOUNT_FIELD = "remainingAuthorizationAmount";

    private static final String SPEND_RULE_DECISION_RECORD_ID_FIELD = "decisionRecordId";

    private static final String SPEND_RULE_ID_FIELD = "ruleId";

    private static final String SPEND_RULE_VERSION_FIELD = "ruleVersion";

    private static final String SPEND_RULE_BINDING_SN_FIELD = "spendRuleBindingSn";

    private static final String SPEND_RULE_SCOPE_TYPE_FIELD = "scopeType";

    private static final String SPEND_RULE_SCOPE_ID_FIELD = "scopeId";

    private static final String SPEND_RULE_DECISION_SN_FIELD = "decisionSn";

    private static final String SPEND_RULE_DECISION_RESULT_FIELD = "decisionResult";

    private static final String SPEND_RULE_DECISION_DIGEST_FIELD = "decisionDigest";

    private static final String CONTROL_SCOPE_ID_FIELD = "controlScopeId";

    private static final String CONTROL_RESERVATION_SN_FIELD = "controlReservationSn";

    private static final String PERIOD_ID_FIELD = "periodId";

    private static final List<String> SPEND_RULE_DECISION_EXPLAIN_FIELDS = List.of(
            SPEND_RULE_DECISION_RECORD_ID_FIELD,
            SPEND_RULE_ID_FIELD,
            SPEND_RULE_VERSION_FIELD,
            SPEND_RULE_BINDING_SN_FIELD,
            SPEND_RULE_SCOPE_TYPE_FIELD,
            SPEND_RULE_SCOPE_ID_FIELD,
            SPEND_RULE_DECISION_SN_FIELD,
            SPEND_RULE_DECISION_RESULT_FIELD,
            SPEND_RULE_DECISION_DIGEST_FIELD,
            CONTROL_SCOPE_ID_FIELD,
            CONTROL_RESERVATION_SN_FIELD,
            PERIOD_ID_FIELD);

    public FundsTransactionProjectionExplanationSource {
        AssertUtils.hasText(businessScene, "交易投影解释业务场景不能为空");
        AssertUtils.hasText(businessSn, "交易投影解释业务流水不能为空");
        AssertUtils.hasText(fundsTransactionSn, "交易投影解释资金交易流水不能为空");
        AssertUtils.notNull(routeSnapshot, "交易投影解释 RouteSnapshot 不能为空");
        AssertUtils.notNull(eventType, "交易投影解释事件类型不能为空");
        AssertUtils.notNull(amount, "交易投影解释金额不能为空");
        contextVariables = contextVariables == null ? Map.of() : Map.copyOf(contextVariables);
    }

    /**
     * 生成交易投影解释摘要。
     *
     * @return 交易投影解释摘要
     */
    public @NonNull FundsTransactionProjectionExplanation explanation() {
        return buildExplanation(null);
    }

    /**
     * 结合交易聚合累计量生成当前态解释摘要。
     *
     * <p>该入口用于单笔查询。事件发布仍调用 {@link #explanation()}，保持事件事实与当前聚合态分离。</p>
     *
     * @param transaction 交易聚合事实
     * @return 当前态交易投影解释摘要
     */
    public @NonNull FundsTransactionProjectionExplanation explanation(@NonNull FundsTransactionDTO transaction) {
        AssertUtils.notNull(transaction, "交易投影当前态解释的交易聚合不能为空");
        return buildExplanation(transaction);
    }

    private @NonNull FundsTransactionProjectionExplanation buildExplanation(
            @Nullable FundsTransactionDTO transaction) {
        boolean outstandingAuthorization = hasOutstandingAuthorization(transaction);
        boolean partiallyRefundedAuthorization = hasPartiallyRefundedAuthorization(transaction);
        return FundsTransactionProjectionExplanation.builder()
                .businessScene(businessScene)
                .businessSn(businessSn)
                .fundsTransactionSn(fundsTransactionSn)
                .tenantId(routeSnapshot.getTenantId())
                .eventType(eventType)
                .ownerType(resolveOwnerType())
                .ownerId(resolveOwnerId())
                .amount(amount.getAmount())
                .currency(amount.getCurrency())
                .occurredTime(occurredTime == null ? routeSnapshot.getResolvedAt() : occurredTime)
                .routeSnapshotId(routeSnapshot.getSnapshotId())
                .routeCode(routeSnapshot.getRouteCode())
                .ledgerTransactionSn(ledgerTransactionSn)
                .factStatus(outstandingAuthorization ? FACT_STATUS_HELD : resolveFactStatus())
                .displayStatus(outstandingAuthorization
                        ? DISPLAY_STATUS_AUTHORIZED_HOLD
                        : partiallyRefundedAuthorization ? DISPLAY_STATUS_SUCCEEDED : resolveDisplayStatus())
                .operationStatus(outstandingAuthorization
                        ? OPERATION_STATUS_WAITING_CAPTURE_OR_RELEASE : resolveOperationStatus())
                .statusMeaning(outstandingAuthorization
                        ? STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED
                        : partiallyRefundedAuthorization ? STATUS_MEANING_FUNDS_POSTED : resolveStatusMeaning())
                .amountSource(resolveAmountSource())
                .failureReason(resolveFailureReason())
                .unavailableReason(outstandingAuthorization
                        ? UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION : resolveUnavailableReason())
                .nextAction(outstandingAuthorization ? NEXT_ACTION_WAIT_FOR_CAPTURE_OR_RELEASE : resolveNextAction())
                .evidenceRefs(evidenceRefs())
                .explanationContext(resolveExplanationContext(transaction))
                .externalRuleVerificationStatus(NOT_APPLICABLE)
                .build();
    }

    private @NonNull String resolveOwnerType() {
        if (StringUtils.hasText(ownerType)) {
            return ownerType;
        }
        return routeSnapshot.getParticipants().getFirst().getSubjectRef().getSubjectType().name();
    }

    private @NonNull String resolveOwnerId() {
        if (StringUtils.hasText(ownerId)) {
            return ownerId;
        }
        return routeSnapshot.getParticipants().getFirst().getSubjectRef().getSubjectId();
    }

    private @NonNull String resolveFactStatus() {
        if (failed) {
            return FACT_STATUS_FAILED;
        }
        if (!completed) {
            return FACT_STATUS_PROCESSING;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn) ? FACT_STATUS_HELD : FACT_STATUS_REJECTED;
            case FREEZE -> FACT_STATUS_HELD;
            case REVERSAL, UNFREEZE -> FACT_STATUS_RELEASED;
            default -> StringUtils.hasText(ledgerTransactionSn)
                    ? FACT_STATUS_POSTED
                    : FACT_STATUS_COMPLETED_NO_LEDGER;
        };
    }

    private @NonNull String resolveDisplayStatus() {
        if (failed) {
            return DISPLAY_STATUS_FAILED;
        }
        if (!completed) {
            return DISPLAY_STATUS_PROCESSING;
        }
        if (isDisputeRefund()) {
            return DISPLAY_STATUS_DISPUTE_REFUNDED;
        }
        if (isNoAuthRefund()) {
            return DISPLAY_STATUS_NO_AUTH_REFUNDED;
        }
        if (isRefundEvent()) {
            return DISPLAY_STATUS_REFUNDED;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? DISPLAY_STATUS_AUTHORIZED_HOLD
                    : DISPLAY_STATUS_DECLINED;
            case FREEZE -> DISPLAY_STATUS_FROZEN;
            case REVERSAL, UNFREEZE -> DISPLAY_STATUS_RELEASED;
            default -> DISPLAY_STATUS_SUCCEEDED;
        };
    }

    private @NonNull String resolveOperationStatus() {
        if (failed) {
            return OPERATION_STATUS_NO_ACTION_REQUIRED;
        }
        if (!completed) {
            return OPERATION_STATUS_WAITING_FACT_COMPLETION;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? OPERATION_STATUS_WAITING_CAPTURE_OR_RELEASE
                    : OPERATION_STATUS_NO_ACTION_REQUIRED;
            case FREEZE -> OPERATION_STATUS_WAITING_UNFREEZE_OR_CONSUME;
            default -> OPERATION_STATUS_NO_ACTION_REQUIRED;
        };
    }

    private @NonNull String resolveStatusMeaning() {
        if (failed) {
            return STATUS_MEANING_FACT_FAILED;
        }
        if (!completed) {
            return STATUS_MEANING_FACT_PROCESSING;
        }
        if (isDisputeRefund()) {
            return STATUS_MEANING_DISPUTE_REFUND_POSTED;
        }
        if (isNoAuthRefund()) {
            return STATUS_MEANING_NO_AUTH_REFUND_POSTED;
        }
        if (isRefundEvent()) {
            return STATUS_MEANING_FUNDS_REFUNDED;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED
                    : STATUS_MEANING_AUTHORIZATION_DECLINED_NO_FUNDS_POSTED;
            case FREEZE -> STATUS_MEANING_BALANCE_FROZEN_NOT_CONSUMED;
            case REVERSAL, UNFREEZE -> STATUS_MEANING_FUNDS_RELEASED;
            default -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_FUNDS_POSTED
                    : STATUS_MEANING_COMPLETED_WITHOUT_LEDGER;
        };
    }

    private @NonNull String resolveAmountSource() {
        String ledgerRef = StringUtils.hasText(ledgerTransactionSn) ? ledgerTransactionSn : NOT_APPLICABLE;
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("instructionAmount=" + amount.getAmount() + " " + amount.getCurrency().name());
        joiner.add("routeLegCount=" + routeSnapshot.getLegs().size());
        joiner.add("routeSnapshot=" + routeSnapshot.getSnapshotId());
        joiner.add("ledgerTransaction=" + ledgerRef);
        return joiner.toString();
    }

    private @NonNull String resolveFailureReason() {
        if (StringUtils.hasText(failureReasonOverride)) {
            return failureReasonOverride;
        }
        if (failed) {
            return UNAVAILABLE_TRANSACTION_FAILED;
        }
        if (!completed) {
            return NOT_APPLICABLE;
        }
        if (eventType != FundsTransactionEventType.AUTHORIZE || StringUtils.hasText(ledgerTransactionSn)) {
            return NOT_APPLICABLE;
        }
        Object declineReason = contextVariables.get(FundsInstructionContextKeys.DECLINE_REASON);
        String reason = declineReason == null ? null : declineReason.toString();
        return StringUtils.hasText(reason) ? reason : UNAVAILABLE_AUTHORIZATION_DECLINED;
    }

    private @NonNull String resolveUnavailableReason() {
        if (failed) {
            return UNAVAILABLE_TRANSACTION_FAILED;
        }
        if (!completed) {
            return NOT_APPLICABLE;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? UNAVAILABLE_AUTHORIZATION_HOLD_NOT_FINAL_CONSUMPTION
                    : UNAVAILABLE_AUTHORIZATION_DECLINED;
            case FREEZE -> UNAVAILABLE_BALANCE_FREEZE_NOT_CONSUMPTION;
            default -> NOT_APPLICABLE;
        };
    }

    private @NonNull String resolveNextAction() {
        if (failed) {
            return NEXT_ACTION_REVIEW_FAILURE;
        }
        if (!completed) {
            return NEXT_ACTION_WAIT_FOR_FACT_COMPLETION;
        }
        if (eventType == FundsTransactionEventType.AUTHORIZE && StringUtils.hasText(ledgerTransactionSn)) {
            return NEXT_ACTION_WAIT_FOR_CAPTURE_OR_RELEASE;
        }
        if (eventType == FundsTransactionEventType.FREEZE) {
            return NEXT_ACTION_WAIT_FOR_UNFREEZE_OR_CONSUME;
        }
        return NOT_APPLICABLE;
    }

    private @NonNull List<String> evidenceRefs() {
        List<String> refs = new ArrayList<>();
        if (StringUtils.hasText(fundsTransactionSn)) {
            refs.add("fundsTransaction:" + fundsTransactionSn);
        }
        refs.add("routeSnapshot:" + routeSnapshot.getSnapshotId());
        if (StringUtils.hasText(ledgerTransactionSn)) {
            refs.add("ledgerTransaction:" + ledgerTransactionSn);
        }
        addPaymentInstrumentEvidence(refs);
        addExternalAccountEvidence(refs);
        addSpendRuleDecisionEvidence(refs);
        addContextEvidence(refs, FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF, "externalDisputeRef");
        addContextEvidence(refs, FundsInstructionContextKeys.DISPUTE_VOUCHER_REF, "disputeVoucherRef");
        addContextEvidence(refs, FundsInstructionContextKeys.EXTERNAL_REFERENCE_SN, "externalReferenceSn");
        return List.copyOf(refs);
    }

    private void addContextEvidence(@NonNull List<String> refs,
                                    @NonNull String contextKey,
                                    @NonNull String evidencePrefix) {
        String value = contextString(contextKey);
        if (StringUtils.hasText(value)) {
            refs.add(evidencePrefix + ":" + value);
        }
    }

    private void addPaymentInstrumentEvidence(@NonNull List<String> refs) {
        PaymentInstrumentRefSpec paymentInstrumentRef = routeSnapshot.getPaymentInstrumentRef();
        if (paymentInstrumentRef == null) {
            return;
        }
        if (StringUtils.hasText(paymentInstrumentRef.getInstrumentSn())) {
            refs.add("paymentInstrument:" + paymentInstrumentRef.getInstrumentSn());
        }
        Map<String, Object> bindingSnapshot = paymentInstrumentRef.getBindingSnapshot();
        Object bindingSn = bindingSnapshot.get(BINDING_SN_FIELD);
        if (bindingSn == null || !StringUtils.hasText(bindingSn.toString())) {
            return;
        }
        Object bindingVersion = bindingSnapshot.get(BINDING_VERSION_FIELD);
        String versionSuffix = bindingVersion == null ? "" : ":v" + bindingVersion;
        refs.add("paymentInstrumentBinding:" + bindingSn + versionSuffix);
    }

    private void addExternalAccountEvidence(@NonNull List<String> refs) {
        ExternalAccountRefSpec externalAccountRef = routeSnapshot.getExternalAccountRef();
        if (externalAccountRef != null && StringUtils.hasText(externalAccountRef.getExternalAccountId())) {
            refs.add("externalAccount:" + externalAccountRef.getExternalAccountId());
        }
    }

    private void addSpendRuleDecisionEvidence(@NonNull List<String> refs) {
        Map<String, Object> decision = spendRuleDecisionContext();
        if (decision.isEmpty()) {
            return;
        }
        String ruleId = decisionText(decision, SPEND_RULE_ID_FIELD);
        String ruleVersion = decisionText(decision, SPEND_RULE_VERSION_FIELD);
        if (StringUtils.hasText(ruleId)) {
            refs.add("spendRule:" + ruleId);
        }
        if (StringUtils.hasText(ruleId) && StringUtils.hasText(ruleVersion)) {
            refs.add("spendRuleVersion:" + ruleId + ":" + ruleVersion);
        }
        addSpendRuleDecisionEvidence(refs, decision, SPEND_RULE_BINDING_SN_FIELD, "spendRuleBinding");
        addSpendRuleDecisionEvidence(refs, decision, SPEND_RULE_DECISION_SN_FIELD, "spendRuleDecision");
        addSpendRuleDecisionEvidence(refs, decision, SPEND_RULE_DECISION_RECORD_ID_FIELD, "spendRuleDecisionRecord");
    }

    private void addSpendRuleDecisionEvidence(@NonNull List<String> refs,
                                              @NonNull Map<String, Object> decision,
                                              @NonNull String field,
                                              @NonNull String prefix) {
        String value = decisionText(decision, field);
        if (StringUtils.hasText(value)) {
            refs.add(prefix + ":" + value);
        }
    }

    private @NonNull Map<String, Object> resolveExplanationContext(@Nullable FundsTransactionDTO transaction) {
        Map<String, Object> result = new LinkedHashMap<>();
        putPaymentInstrumentRef(result);
        putExternalAccountRef(result);
        putSpendRuleDecision(result);
        putTransactionSummary(result, transaction);
        if (isDisputeRefund()) {
            putContextValue(result, FundsInstructionContextKeys.REFUND_MODE);
            putContextValue(result, FundsInstructionContextKeys.DISPUTE_MODE);
            putContextValue(result, FundsInstructionContextKeys.DISPUTE_REASON);
            putContextValue(result, FundsInstructionContextKeys.DISPUTE_VOUCHER_REF);
            putContextValue(result, FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF);
        } else if (isNoAuthRefund()) {
            putContextValue(result, FundsInstructionContextKeys.REFUND_MODE);
            putContextValue(result, FundsInstructionContextKeys.EXTERNAL_REFERENCE_SN);
            putContextValue(result, FundsInstructionContextKeys.REFUND_REASON);
        }
        return Map.copyOf(result);
    }

    private void putExternalAccountRef(@NonNull Map<String, Object> values) {
        ExternalAccountRefSpec externalAccountRef = routeSnapshot.getExternalAccountRef();
        if (externalAccountRef == null) {
            return;
        }
        Map<String, Object> refValues = new LinkedHashMap<>();
        putIfText(refValues, EXTERNAL_ACCOUNT_ID_FIELD, externalAccountRef.getExternalAccountId());
        putIfText(refValues, EXTERNAL_ACCOUNT_TYPE_FIELD, externalAccountRef.getExternalAccountType());
        putIfText(refValues, PROVIDER_CODE_FIELD, externalAccountRef.getProviderCode());
        putIfText(refValues, CHANNEL_CODE_FIELD, externalAccountRef.getChannelCode());
        putIfText(refValues, CURRENCY_FIELD,
                externalAccountRef.getCurrency() == null ? null : externalAccountRef.getCurrency().name());
        putIfText(refValues, COUNTRY_CODE_FIELD, externalAccountRef.getCountryCode());
        Object externalTransactionId = externalAccountRef.getContextVariables()
                .get(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID);
        if (externalTransactionId != null) {
            putIfText(refValues, FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID,
                    externalTransactionId.toString());
        }
        values.put(EXTERNAL_ACCOUNT_REF_CONTEXT_KEY, Map.copyOf(refValues));
    }

    private void putTransactionSummary(@NonNull Map<String, Object> values,
                                       @Nullable FundsTransactionDTO transaction) {
        if (transaction == null) {
            return;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        if (transaction.getState() != null) {
            summary.put(STATE_FIELD, transaction.getState().name());
        }
        putIfNotNull(summary, AMOUNT_FIELD, transaction.getAmount());
        if (transaction.getCurrency() != null) {
            summary.put(CURRENCY_FIELD, transaction.getCurrency().name());
        }
        putIfNotNull(summary, AUTHORIZED_AMOUNT_FIELD, transaction.getAuthorizedAmount());
        putIfNotNull(summary, COMPLETED_AMOUNT_FIELD, transaction.getCompletedAmount());
        putIfNotNull(summary, REVERSED_AMOUNT_FIELD, transaction.getReversedAmount());
        putIfNotNull(summary, REFUNDED_AMOUNT_FIELD, transaction.getRefundedAmount());
        putIfNotNull(summary, DECLINED_AMOUNT_FIELD, transaction.getDeclinedAmount());
        putIfNotNull(summary, FEE_AMOUNT_FIELD, transaction.getFeeAmount());
        if (transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION) {
            summary.put(REMAINING_AUTHORIZATION_AMOUNT_FIELD, remainingAuthorizationAmount(transaction));
        }
        values.put(TRANSACTION_SUMMARY_CONTEXT_KEY, Map.copyOf(summary));
    }

    private boolean hasOutstandingAuthorization(@Nullable FundsTransactionDTO transaction) {
        return transaction != null
                && transaction.getTransactionMode() == FundsTransactionMode.AUTHORIZATION
                && transaction.getState() == FundsTransactionState.OPEN
                && remainingAuthorizationAmount(transaction) > 0;
    }

    private boolean hasPartiallyRefundedAuthorization(@Nullable FundsTransactionDTO transaction) {
        if (transaction == null
                || transaction.getTransactionMode() != FundsTransactionMode.AUTHORIZATION
                || !isRefundEvent()
                || isDisputeRefund()
                || isNoAuthRefund()) {
            return false;
        }
        long completedAmount = amountOrZero(transaction.getCompletedAmount());
        long refundedAmount = amountOrZero(transaction.getRefundedAmount());
        return refundedAmount > 0 && refundedAmount < completedAmount;
    }

    private long remainingAuthorizationAmount(@NonNull FundsTransactionDTO transaction) {
        long remainingAmount = amountOrZero(transaction.getAuthorizedAmount())
                - amountOrZero(transaction.getCompletedAmount())
                - amountOrZero(transaction.getReversedAmount());
        return Math.max(remainingAmount, 0L);
    }

    private long amountOrZero(@Nullable Long value) {
        return value == null ? 0L : value;
    }

    private void putPaymentInstrumentRef(@NonNull Map<String, Object> values) {
        PaymentInstrumentRefSpec paymentInstrumentRef = routeSnapshot.getPaymentInstrumentRef();
        if (paymentInstrumentRef == null) {
            return;
        }
        Map<String, Object> refValues = new LinkedHashMap<>();
        putIfText(refValues, INSTRUMENT_SN_FIELD, paymentInstrumentRef.getInstrumentSn());
        putIfText(refValues, INSTRUMENT_TYPE_FIELD, paymentInstrumentRef.getInstrumentType());
        putIfText(refValues, INSTRUMENT_NO_FIELD, paymentInstrumentRef.getInstrumentNo());
        putIfText(refValues, OWNER_ID_FIELD, paymentInstrumentRef.getOwnerId());
        putIfText(refValues, OWNER_TYPE_FIELD, paymentInstrumentRef.getOwnerType());
        putIfNotNull(refValues, TENANT_ID_FIELD, paymentInstrumentRef.getTenantId());
        putIfText(refValues, CURRENCY_FIELD,
                paymentInstrumentRef.getCurrency() == null ? null : paymentInstrumentRef.getCurrency().name());
        putIfText(refValues, STATE_FIELD, paymentInstrumentRef.getState());
        refValues.put(BINDING_SNAPSHOT_FIELD, paymentInstrumentRef.getBindingSnapshot());
        putIfText(refValues, DESCRIPTION_FIELD, paymentInstrumentRef.getDescription());
        values.put(PAYMENT_INSTRUMENT_REF_CONTEXT_KEY, Map.copyOf(refValues));
    }

    private void putSpendRuleDecision(@NonNull Map<String, Object> values) {
        Map<String, Object> decision = spendRuleDecisionContext();
        if (!decision.isEmpty()) {
            values.put(FundsInstructionContextKeys.SPEND_RULE_DECISION, Map.copyOf(decision));
        }
    }

    private @NonNull Map<String, Object> spendRuleDecisionContext() {
        Object value = contextVariables.get(FundsInstructionContextKeys.SPEND_RULE_DECISION);
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : SPEND_RULE_DECISION_EXPLAIN_FIELDS) {
            Object fieldValue = source.get(field);
            if (fieldValue != null) {
                result.put(field, fieldValue);
            }
        }
        return Map.copyOf(result);
    }

    private @Nullable String decisionText(@NonNull Map<String, Object> decision, @NonNull String field) {
        Object value = decision.get(field);
        return value == null ? null : value.toString();
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

    private void putContextValue(@NonNull Map<String, Object> values, @NonNull String contextKey) {
        Object value = contextVariables.get(contextKey);
        if (value != null) {
            values.put(contextKey, value);
        }
    }

    private boolean isDisputeRefund() {
        return eventType == FundsTransactionEventType.AUTH_REFUND
                && FundsInstructionContextKeys.REFUND_MODE_DISPUTE.equals(
                contextString(FundsInstructionContextKeys.REFUND_MODE));
    }

    private boolean isNoAuthRefund() {
        return eventType == FundsTransactionEventType.AUTH_REFUND
                && FundsInstructionContextKeys.REFUND_MODE_NO_AUTH.equals(
                contextString(FundsInstructionContextKeys.REFUND_MODE));
    }

    private boolean isRefundEvent() {
        return eventType == FundsTransactionEventType.REFUND
                || eventType == FundsTransactionEventType.AUTH_REFUND;
    }

    private @Nullable String contextString(@NonNull String contextKey) {
        Object value = contextVariables.get(contextKey);
        return value == null ? null : value.toString();
    }
}
