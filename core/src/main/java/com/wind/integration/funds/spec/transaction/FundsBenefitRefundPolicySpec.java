package com.wind.integration.funds.spec.transaction;

import com.wind.integration.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.integration.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 权益退款处置策略。
 */
public interface FundsBenefitRefundPolicySpec {

    @NonNull
    default FundsBenefitPartialRefundStrategy getPartialRefundStrategy() {
        return FundsBenefitPartialRefundStrategy.ORIGINAL_SNAPSHOT;
    }

    @NonNull
    List<FundsBenefitRefundDisposition> getDispositions();

    @Nullable
    default Money getRefundableAmount() {
        return null;
    }

    @Nullable
    default Money getNonRefundableAmount() {
        return null;
    }

    @Nullable
    default String getRefundRuleVersion() {
        return null;
    }

    @Nullable
    default String getRefundPolicyCode() {
        return null;
    }

    @Nullable
    default String getRefundDecisionId() {
        return null;
    }

    @Nullable
    default String getDecisionSource() {
        return null;
    }

    @Nullable
    default LocalDateTime getDecisionTime() {
        return null;
    }

    @NonNull
    Map<String, Object> getContextVariables();
}
