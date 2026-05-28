package com.wind.integration.funds.model.transaction;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.wind.integration.funds.model.FundsContextVariables;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 权益 DSL 契约校验工具。
 */
public final class FundsBenefitSpecValidators {

    private static final String NON_FIELD_NAME_CHARACTER_PATTERN = "[^a-z0-9]";

    private static final Pattern RAW_FIELD_NAME_PATTERN = Pattern.compile(
            "(?:\"([^\"]+)\"|(?<![A-Za-z0-9_-])([A-Za-z][A-Za-z0-9_ -]*))\\s*:");

    private static final Set<String> RESERVED_CONTEXT_KEYS = Set.of(
            ImmutableFundsBenefitSnapshotSpec.Fields.benefitSnapshotId,
            ImmutableFundsBenefitSnapshotSpec.Fields.benefitSchemaVersion,
            ImmutableFundsBenefitSnapshotSpec.Fields.benefitGroupSn,
            ImmutableFundsBenefitSnapshotSpec.Fields.orderAmount,
            ImmutableFundsBenefitSnapshotSpec.Fields.userPayAmount,
            ImmutableFundsBenefitSnapshotSpec.Fields.merchantReceivableAmount,
            ImmutableFundsBenefitComponentSpec.Fields.componentSn,
            ImmutableFundsBenefitComponentSpec.Fields.benefitType,
            ImmutableFundsBenefitComponentSpec.Fields.componentType,
            ImmutableFundsBenefitComponentSpec.Fields.closureRole,
            ImmutableFundsBenefitComponentSpec.Fields.amount,
            ImmutableFundsBenefitComponentSpec.Fields.ledgerEffect,
            ImmutableFundsBenefitComponentSpec.Fields.fundingNature,
            ImmutableFundsBenefitComponentSpec.Fields.benefitReference,
            ImmutableFundsBenefitComponentSpec.Fields.refundPolicy,
            ImmutableFundsBenefitRefundPolicySpec.Fields.partialRefundStrategy,
            ImmutableFundsBenefitRefundPolicySpec.Fields.dispositions,
            "refundDisposition",
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundableAmount,
            ImmutableFundsBenefitRefundPolicySpec.Fields.nonRefundableAmount,
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundRuleVersion,
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundDecisionId,
            ImmutableFundsBenefitReferenceSpec.Fields.ruleVersion,
            "currentMarketingRule",
            "couponEligibility",
            "couponAvailable",
            "recalculatedDiscount",
            "bestCoupon",
            "activityRules",
            "userCouponBag");

    private static final Set<String> RESERVED_INSTRUCTION_CONTEXT_KEYS = Set.of(
            ImmutableFundsBenefitSnapshotSpec.Fields.orderAmount,
            ImmutableFundsBenefitSnapshotSpec.Fields.userPayAmount,
            ImmutableFundsBenefitSnapshotSpec.Fields.merchantReceivableAmount,
            ImmutableFundsBenefitComponentSpec.Fields.amount,
            ImmutableFundsBenefitComponentSpec.Fields.ledgerEffect,
            ImmutableFundsBenefitComponentSpec.Fields.fundingNature,
            ImmutableFundsBenefitComponentSpec.Fields.refundPolicy,
            ImmutableFundsBenefitRefundPolicySpec.Fields.partialRefundStrategy,
            ImmutableFundsBenefitRefundPolicySpec.Fields.dispositions,
            "refundDisposition",
            ImmutableFundsBenefitRefundPolicySpec.Fields.refundableAmount,
            ImmutableFundsBenefitRefundPolicySpec.Fields.nonRefundableAmount,
            "currentMarketingRule",
            "couponEligibility",
            "couponAvailable",
            "recalculatedDiscount",
            "bestCoupon",
            "activityRules",
            "userCouponBag");

    private FundsBenefitSpecValidators() {
    }

    static void requireText(@Nullable String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    static Map<String, Object> immutableContext(Map<String, Object> contextVariables, String owner) {
        Map<String, Object> copied = FundsContextVariables.immutableCopy(contextVariables);
        rejectReservedContextKeys(copied, owner);
        return copied;
    }

    public static Map<String, Object> immutableInstructionContext(Map<String, Object> contextVariables, String owner) {
        Map<String, Object> copied = FundsContextVariables.immutableCopy(contextVariables);
        rejectReservedInstructionContextKeys(copied, owner);
        return copied;
    }

    public static void rejectInstructionContextVariables(@Nullable String contextVariables, String owner) {
        if (!StringUtils.hasText(contextVariables)) {
            return;
        }
        try {
            rejectReservedInstructionContextKeys(JSON.parse(contextVariables), owner);
        } catch (JSONException ignored) {
            rejectReservedRawInstructionContextKeys(contextVariables, owner);
        }
    }

    private static void rejectReservedContextKeys(@Nullable Object value, String owner) {
        rejectReservedContextKeys(value, owner, RESERVED_CONTEXT_KEYS);
    }

    private static void rejectReservedInstructionContextKeys(@Nullable Object value, String owner) {
        rejectReservedContextKeys(value, owner, RESERVED_INSTRUCTION_CONTEXT_KEYS);
    }

    private static void rejectReservedContextKeys(@Nullable Object value, String owner, Set<String> reservedKeys) {
        if (value instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (entry.getKey() instanceof String key && reservedKeys.contains(key)) {
                    throw new IllegalArgumentException(
                            owner + ".contextVariables must not contain core benefit field: " + key);
                }
                rejectReservedContextKeys(entry.getValue(), owner, reservedKeys);
            }
            return;
        }
        if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                rejectReservedContextKeys(item, owner, reservedKeys);
            }
            return;
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                rejectReservedContextKeys(Array.get(value, index), owner, reservedKeys);
            }
        }
    }

    private static void rejectReservedRawInstructionContextKeys(String contextVariables, String owner) {
        Matcher matcher = RAW_FIELD_NAME_PATTERN.matcher(contextVariables);
        while (matcher.find()) {
            String fieldName = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            if (isReservedInstructionContextKey(fieldName)) {
                throw new IllegalArgumentException(
                        owner + ".contextVariables must not contain core benefit field: " + fieldName);
            }
        }
    }

    private static boolean isReservedInstructionContextKey(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        for (String reservedKey : RESERVED_INSTRUCTION_CONTEXT_KEYS) {
            if (reservedKey.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "")
                    .equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
