package com.wind.integration.funds.model.transaction;

import com.wind.integration.funds.model.FundsContextVariables;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Set;

/**
 * 权益 DSL 契约校验工具。
 */
public final class FundsBenefitSpecValidators {

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
}
