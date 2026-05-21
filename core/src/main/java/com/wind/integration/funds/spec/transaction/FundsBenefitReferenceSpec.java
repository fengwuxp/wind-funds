package com.wind.integration.funds.spec.transaction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 权益外部引用。
 */
public interface FundsBenefitReferenceSpec {

    @Nullable
    default String getCampaignId() {
        return null;
    }

    @Nullable
    default String getCouponId() {
        return null;
    }

    @Nullable
    default String getVoucherId() {
        return null;
    }

    @Nullable
    default String getBenefitInstanceId() {
        return null;
    }

    @Nullable
    default String getHoldId() {
        return null;
    }

    @Nullable
    default String getWriteOffId() {
        return null;
    }

    @Nullable
    default String getReleaseId() {
        return null;
    }

    @Nullable
    default String getRuleVersion() {
        return null;
    }

    @Nullable
    default String getExternalDecisionId() {
        return null;
    }

    @NonNull
    Map<String, Object> getContextVariables();
}
