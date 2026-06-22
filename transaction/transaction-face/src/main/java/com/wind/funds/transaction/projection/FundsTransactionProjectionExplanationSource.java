package com.wind.funds.transaction.projection;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
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

/**
 * 交易投影解释的稳定事实来源。
 *
 * <p>职责：把主写链路内存态事实或查询链路持久化事实归一成同一套投影解释口径。</p>
 *
 * <p>边界：本对象只做确定性解释转换，不查询数据库、不发布投影、不修改资金、账本或余额事实。</p>
 */
@Builder
public record FundsTransactionProjectionExplanationSource(@NonNull String businessScene,
                                                          @NonNull String businessSn,
                                                          @NonNull String fundsTransactionSn,
                                                          @NonNull RouteSnapshotSpec routeSnapshot,
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

    private static final String DISPLAY_STATUS_COMPAT_CHARGEBACK_REFUNDED = "COMPAT_CHARGEBACK_REFUNDED";

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

    private static final String STATUS_MEANING_COMPAT_CHARGEBACK_POSTED = "COMPAT_CHARGEBACK_POSTED";

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

    private static final String CHARGEBACK_REASON_CONTEXT_KEY = "chargebackReason";

    private static final String CHARGEBACK_EVIDENCE_REF_CONTEXT_KEY = "evidenceRef";

    private static final String PAYMENT_INSTRUMENT_REF_CONTEXT_KEY = "paymentInstrumentRef";

    private static final String INSTRUMENT_ID_FIELD = "instrumentId";

    private static final String INSTRUMENT_TYPE_FIELD = "instrumentType";

    private static final String INSTRUMENT_NO_FIELD = "instrumentNo";

    private static final String OWNER_ID_FIELD = "ownerId";

    private static final String OWNER_TYPE_FIELD = "ownerType";

    private static final String TENANT_ID_FIELD = "tenantId";

    private static final String CURRENCY_FIELD = "currency";

    private static final String STATUS_FIELD = "status";

    private static final String BINDING_SNAPSHOT_FIELD = "bindingSnapshot";

    private static final String BINDING_SN_FIELD = "bindingSn";

    private static final String BINDING_VERSION_FIELD = "bindingVersion";

    private static final String DESCRIPTION_FIELD = "description";

    private static final String SPEND_RULE_DECISION_LOG_ID_FIELD = "decisionLogId";

    private static final String SPEND_RULE_ID_FIELD = "ruleId";

    private static final String SPEND_RULE_VERSION_FIELD = "ruleVersion";

    private static final String SPEND_RULE_ASSIGNMENT_SN_FIELD = "assignmentSn";

    private static final String SPEND_RULE_SCOPE_TYPE_FIELD = "scopeType";

    private static final String SPEND_RULE_SCOPE_ID_FIELD = "scopeId";

    private static final String SPEND_RULE_DECISION_SN_FIELD = "decisionSn";

    private static final String SPEND_RULE_DECISION_RESULT_FIELD = "decisionResult";

    private static final String SPEND_RULE_DECISION_DIGEST_FIELD = "decisionDigest";

    private static final String BUDGET_GROUP_SN_FIELD = "budgetGroupSn";

    private static final List<String> SPEND_RULE_DECISION_EXPLAIN_FIELDS = List.of(
            SPEND_RULE_DECISION_LOG_ID_FIELD,
            SPEND_RULE_ID_FIELD,
            SPEND_RULE_VERSION_FIELD,
            SPEND_RULE_ASSIGNMENT_SN_FIELD,
            SPEND_RULE_SCOPE_TYPE_FIELD,
            SPEND_RULE_SCOPE_ID_FIELD,
            SPEND_RULE_DECISION_SN_FIELD,
            SPEND_RULE_DECISION_RESULT_FIELD,
            SPEND_RULE_DECISION_DIGEST_FIELD,
            BUDGET_GROUP_SN_FIELD);

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
        return FundsTransactionProjectionExplanation.builder()
                .businessScene(businessScene)
                .businessSn(businessSn)
                .fundsTransactionSn(fundsTransactionSn)
                .routeSnapshotId(routeSnapshot.getSnapshotId())
                .routeCode(routeSnapshot.getRouteCode())
                .ledgerTransactionSn(ledgerTransactionSn)
                .factStatus(resolveFactStatus())
                .displayStatus(resolveDisplayStatus())
                .operationStatus(resolveOperationStatus())
                .statusMeaning(resolveStatusMeaning())
                .amountSource(resolveAmountSource())
                .failureReason(resolveFailureReason())
                .unavailableReason(resolveUnavailableReason())
                .nextAction(resolveNextAction())
                .evidenceRefs(evidenceRefs())
                .explanationContext(resolveExplanationContext())
                .externalRuleVerificationStatus(NOT_APPLICABLE)
                .build();
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
            case REVERSAL, EXPIRE, UNFREEZE -> FACT_STATUS_RELEASED;
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
        if (isCompatChargeback()) {
            return DISPLAY_STATUS_COMPAT_CHARGEBACK_REFUNDED;
        }
        if (isRefundEvent()) {
            return DISPLAY_STATUS_REFUNDED;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? DISPLAY_STATUS_AUTHORIZED_HOLD
                    : DISPLAY_STATUS_DECLINED;
            case FREEZE -> DISPLAY_STATUS_FROZEN;
            case REVERSAL, EXPIRE, UNFREEZE -> DISPLAY_STATUS_RELEASED;
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
        if (isCompatChargeback()) {
            return STATUS_MEANING_COMPAT_CHARGEBACK_POSTED;
        }
        if (isRefundEvent()) {
            return STATUS_MEANING_FUNDS_REFUNDED;
        }
        return switch (eventType) {
            case AUTHORIZE -> StringUtils.hasText(ledgerTransactionSn)
                    ? STATUS_MEANING_AUTHORIZATION_HELD_NOT_CAPTURED
                    : STATUS_MEANING_AUTHORIZATION_DECLINED_NO_FUNDS_POSTED;
            case FREEZE -> STATUS_MEANING_BALANCE_FROZEN_NOT_CONSUMED;
            case REVERSAL, EXPIRE, UNFREEZE -> STATUS_MEANING_FUNDS_RELEASED;
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
        addSpendRuleDecisionEvidence(refs);
        addContextEvidence(refs, FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF, "externalDisputeRef");
        addContextEvidence(refs, FundsInstructionContextKeys.DISPUTE_VOUCHER_REF, "disputeVoucherRef");
        addContextEvidence(refs, FundsInstructionContextKeys.EXTERNAL_REFERENCE_SN, "externalReferenceSn");
        addContextEvidence(refs, CHARGEBACK_EVIDENCE_REF_CONTEXT_KEY, "chargebackEvidenceRef");
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
        if (StringUtils.hasText(paymentInstrumentRef.getInstrumentId())) {
            refs.add("paymentInstrument:" + paymentInstrumentRef.getInstrumentId());
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
        addSpendRuleDecisionEvidence(refs, decision, SPEND_RULE_ASSIGNMENT_SN_FIELD, "spendRuleAssignment");
        addSpendRuleDecisionEvidence(refs, decision, SPEND_RULE_DECISION_SN_FIELD, "spendRuleDecision");
        addSpendRuleDecisionEvidence(refs, decision, SPEND_RULE_DECISION_LOG_ID_FIELD, "spendRuleDecisionLog");
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

    private @NonNull Map<String, Object> resolveExplanationContext() {
        Map<String, Object> result = new LinkedHashMap<>();
        putPaymentInstrumentRef(result);
        putSpendRuleDecision(result);
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
        } else if (isCompatChargeback()) {
            putContextValue(result, CHARGEBACK_REASON_CONTEXT_KEY);
            putContextValue(result, CHARGEBACK_EVIDENCE_REF_CONTEXT_KEY);
            putContextValue(result, FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF);
        }
        return Map.copyOf(result);
    }

    private void putPaymentInstrumentRef(@NonNull Map<String, Object> values) {
        PaymentInstrumentRefSpec paymentInstrumentRef = routeSnapshot.getPaymentInstrumentRef();
        if (paymentInstrumentRef == null) {
            return;
        }
        Map<String, Object> refValues = new LinkedHashMap<>();
        putIfText(refValues, INSTRUMENT_ID_FIELD, paymentInstrumentRef.getInstrumentId());
        putIfText(refValues, INSTRUMENT_TYPE_FIELD, paymentInstrumentRef.getInstrumentType());
        putIfText(refValues, INSTRUMENT_NO_FIELD, paymentInstrumentRef.getInstrumentNo());
        putIfText(refValues, OWNER_ID_FIELD, paymentInstrumentRef.getOwnerId());
        putIfText(refValues, OWNER_TYPE_FIELD, paymentInstrumentRef.getOwnerType());
        putIfNotNull(refValues, TENANT_ID_FIELD, paymentInstrumentRef.getTenantId());
        putIfText(refValues, CURRENCY_FIELD, paymentInstrumentRef.getCurrency());
        putIfText(refValues, STATUS_FIELD, paymentInstrumentRef.getStatus());
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

    private boolean isCompatChargeback() {
        return eventType == FundsTransactionEventType.CHARGEBACK;
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
