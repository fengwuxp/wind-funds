package com.wind.integration.funds.model.transaction;

import com.wind.integration.funds.spec.transaction.FundsBenefitReferenceSpec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 不可变权益外部引用实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundsBenefitReferenceSpec(@Nullable String campaignId,
                                                 @Nullable String couponId,
                                                 @Nullable String voucherId,
                                                 @Nullable String benefitInstanceId,
                                                 @Nullable String holdId,
                                                 @Nullable String writeOffId,
                                                 @Nullable String releaseId,
                                                 @Nullable String ruleVersion,
                                                 @Nullable String externalDecisionId,
                                                 Map<String, Object> contextVariables)
        implements FundsBenefitReferenceSpec {

    public ImmutableFundsBenefitReferenceSpec {
        contextVariables = FundsBenefitSpecValidators.immutableContext(
                contextVariables, "fundsBenefit.reference");
    }

    @Override
    public @Nullable String getCampaignId() {
        return campaignId;
    }

    @Override
    public @Nullable String getCouponId() {
        return couponId;
    }

    @Override
    public @Nullable String getVoucherId() {
        return voucherId;
    }

    @Override
    public @Nullable String getBenefitInstanceId() {
        return benefitInstanceId;
    }

    @Override
    public @Nullable String getHoldId() {
        return holdId;
    }

    @Override
    public @Nullable String getWriteOffId() {
        return writeOffId;
    }

    @Override
    public @Nullable String getReleaseId() {
        return releaseId;
    }

    @Override
    public @Nullable String getRuleVersion() {
        return ruleVersion;
    }

    @Override
    public @Nullable String getExternalDecisionId() {
        return externalDecisionId;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
