package com.wind.integration.funds.model.transaction;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 权益 DSL 契约校验工具。
 */
final class FundsBenefitSpecValidators {

    private static final Set<String> RESERVED_CONTEXT_KEYS = Set.of(
            "benefitSnapshotId",
            "benefitSchemaVersion",
            "benefitGroupSn",
            "orderAmount",
            "userPayAmount",
            "merchantReceivableAmount",
            "componentSn",
            "benefitType",
            "componentType",
            "closureRole",
            "amount",
            "ledgerEffect",
            "fundingNature",
            "benefitReference",
            "refundPolicy",
            "partialRefundStrategy",
            "dispositions",
            "refundDisposition",
            "refundableAmount",
            "nonRefundableAmount",
            "refundRuleVersion",
            "refundDecisionId",
            "ruleVersion",
            "currentMarketingRule",
            "couponEligibility",
            "couponAvailable",
            "recalculatedDiscount",
            "bestCoupon",
            "activityRules",
            "userCouponBag");

    private FundsBenefitSpecValidators() {
    }

    static void hasText(@Nullable String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    static boolean hasText(@Nullable String value) {
        return StringUtils.hasText(value);
    }

    static Map<String, Object> immutableContext(Map<String, Object> contextVariables, String owner) {
        Map<String, Object> copied = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
        for (String key : copied.keySet()) {
            if (RESERVED_CONTEXT_KEYS.contains(key)) {
                throw new IllegalArgumentException(owner + ".contextVariables must not contain core benefit field: " + key);
            }
        }
        return copied;
    }
}
