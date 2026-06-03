package com.wind.funds.model.transaction;

import com.wind.funds.spec.transaction.FundsBenefitRefundPolicySpec;
import com.wind.funds.transaction.enums.FundsBenefitPartialRefundStrategy;
import com.wind.funds.transaction.enums.FundsBenefitRefundDisposition;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 不可变权益退款策略实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundsBenefitRefundPolicySpec(FundsBenefitPartialRefundStrategy partialRefundStrategy,
                                                    List<FundsBenefitRefundDisposition> dispositions,
                                                    @Nullable Money refundableAmount,
                                                    @Nullable Money nonRefundableAmount,
                                                    @Nullable String refundRuleVersion,
                                                    @Nullable String refundPolicyCode,
                                                    @Nullable String refundDecisionId,
                                                    @Nullable String decisionSource,
                                                    @Nullable LocalDateTime decisionTime,
                                                    Map<String, Object> contextVariables)
        implements FundsBenefitRefundPolicySpec {

    public ImmutableFundsBenefitRefundPolicySpec {
        partialRefundStrategy = partialRefundStrategy == null
                ? FundsBenefitPartialRefundStrategy.ORIGINAL_SNAPSHOT
                : partialRefundStrategy;
        if (dispositions == null || dispositions.isEmpty()) {
            throw new IllegalArgumentException("fundsBenefit.refundPolicy.dispositions must not be empty");
        }
        dispositions = List.copyOf(dispositions);
        validateMoney(refundableAmount, "fundsBenefit.refundPolicy.refundableAmount");
        validateMoney(nonRefundableAmount, "fundsBenefit.refundPolicy.nonRefundableAmount");
        if (dispositions.contains(FundsBenefitRefundDisposition.NO_REFUND)
                && !StringUtils.hasText(refundRuleVersion)
                && !StringUtils.hasText(refundDecisionId)
                && !StringUtils.hasText(decisionSource)) {
            throw new IllegalArgumentException("fundsBenefit.refundPolicy NO_REFUND requires rule version or decision reference");
        }
        contextVariables = FundsBenefitSpecValidators.immutableContext(
                contextVariables, "fundsBenefit.refundPolicy");
    }

    @Override
    public @NonNull FundsBenefitPartialRefundStrategy getPartialRefundStrategy() {
        return partialRefundStrategy;
    }

    @Override
    public @NonNull List<FundsBenefitRefundDisposition> getDispositions() {
        return dispositions;
    }

    @Override
    public @Nullable Money getRefundableAmount() {
        return refundableAmount;
    }

    @Override
    public @Nullable Money getNonRefundableAmount() {
        return nonRefundableAmount;
    }

    @Override
    public @Nullable String getRefundRuleVersion() {
        return refundRuleVersion;
    }

    @Override
    public @Nullable String getRefundPolicyCode() {
        return refundPolicyCode;
    }

    @Override
    public @Nullable String getRefundDecisionId() {
        return refundDecisionId;
    }

    @Override
    public @Nullable String getDecisionSource() {
        return decisionSource;
    }

    @Override
    public @Nullable LocalDateTime getDecisionTime() {
        return decisionTime;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    private static void validateMoney(@Nullable Money money, String path) {
        if (money != null && money.getAmount() < 0) {
            throw new IllegalArgumentException(path + " must not be negative");
        }
    }
}
