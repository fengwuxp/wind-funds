package com.wind.integration.funds.model.transaction;

import com.wind.integration.funds.model.FundsContextVariables;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 权益 DSL 契约校验工具。
 */
final class FundsBenefitSpecValidators {

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

    private FundsBenefitSpecValidators() {
    }

    static void requireText(@Nullable String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    static Map<String, Object> immutableContext(Map<String, Object> contextVariables, String owner) {
        Map<String, Object> copied = FundsContextVariables.immutableCopy(contextVariables);
        for (String key : copied.keySet()) {
            if (RESERVED_CONTEXT_KEYS.contains(key)) {
                throw new IllegalArgumentException(owner + ".contextVariables must not contain core benefit field: " + key);
            }
        }
        return copied;
    }
}
