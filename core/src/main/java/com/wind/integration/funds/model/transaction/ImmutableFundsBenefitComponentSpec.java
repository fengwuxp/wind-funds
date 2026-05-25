package com.wind.integration.funds.model.transaction;

import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.spec.transaction.FundsBenefitComponentSpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsBenefitRefundPolicySpec;
import com.wind.integration.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.integration.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.integration.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.integration.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.integration.funds.transaction.enums.FundsBenefitType;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 不可变权益金额组件实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundsBenefitComponentSpec(String componentSn,
                                                 int sequence,
                                                 FundsBenefitType benefitType,
                                                 FundsBenefitComponentType componentType,
                                                 FundsBenefitAmountClosureRole closureRole,
                                                 Money amount,
                                                 FundsBenefitLedgerEffect ledgerEffect,
                                                 FundsBenefitFundingNature fundingNature,
                                                 @Nullable SubjectRef bearerSubjectRef,
                                                 @Nullable SubjectRef beneficiarySubjectRef,
                                                 @Nullable SubjectRef fundingSubjectRef,
                                                 @Nullable String fundingAccountRole,
                                                 FundsBenefitReferenceSpec benefitReference,
                                                 @Nullable FundsBenefitRefundPolicySpec refundPolicy,
                                                 @Nullable String description,
                                                 Map<String, Object> contextVariables)
        implements FundsBenefitComponentSpec {

    public ImmutableFundsBenefitComponentSpec {
        FundsBenefitSpecValidators.requireText(componentSn, "fundsBenefit.componentSn must not be blank");
        if (benefitType == null) {
            throw new IllegalArgumentException("fundsBenefit.benefitType must not be null");
        }
        if (componentType == null) {
            throw new IllegalArgumentException("fundsBenefit.componentType must not be null");
        }
        if (closureRole == null) {
            throw new IllegalArgumentException("fundsBenefit.closureRole must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("fundsBenefit.amount must not be null");
        }
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("fundsBenefit.amount must be positive");
        }
        if (ledgerEffect == null) {
            throw new IllegalArgumentException("fundsBenefit.ledgerEffect must not be null");
        }
        if (fundingNature == null) {
            throw new IllegalArgumentException("fundsBenefit.fundingNature must not be null");
        }
        if (benefitReference == null) {
            throw new IllegalArgumentException("fundsBenefit.benefitReference must not be null");
        }
        validateConditionalReferences(ledgerEffect, fundingNature, benefitReference, fundingSubjectRef,
                fundingAccountRole, bearerSubjectRef);
        contextVariables = FundsBenefitSpecValidators.immutableContext(contextVariables, "fundsBenefit.component");
    }

    @Override
    public @NonNull String getComponentSn() {
        return componentSn;
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @Override
    public @NonNull FundsBenefitType getBenefitType() {
        return benefitType;
    }

    @Override
    public @NonNull FundsBenefitComponentType getComponentType() {
        return componentType;
    }

    @Override
    public @NonNull FundsBenefitAmountClosureRole getClosureRole() {
        return closureRole;
    }

    @Override
    public @NonNull Money getAmount() {
        return amount;
    }

    @Override
    public @NonNull FundsBenefitLedgerEffect getLedgerEffect() {
        return ledgerEffect;
    }

    @Override
    public @NonNull FundsBenefitFundingNature getFundingNature() {
        return fundingNature;
    }

    @Override
    public @Nullable SubjectRef getBearerSubjectRef() {
        return bearerSubjectRef;
    }

    @Override
    public @Nullable SubjectRef getBeneficiarySubjectRef() {
        return beneficiarySubjectRef;
    }

    @Override
    public @Nullable SubjectRef getFundingSubjectRef() {
        return fundingSubjectRef;
    }

    @Override
    public @Nullable String getFundingAccountRole() {
        return fundingAccountRole;
    }

    @Override
    public @NonNull FundsBenefitReferenceSpec getBenefitReference() {
        return benefitReference;
    }

    @Override
    public @Nullable FundsBenefitRefundPolicySpec getRefundPolicy() {
        return refundPolicy;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    private static void validateConditionalReferences(FundsBenefitLedgerEffect ledgerEffect,
                                                      FundsBenefitFundingNature fundingNature,
                                                      FundsBenefitReferenceSpec benefitReference,
                                                      @Nullable SubjectRef fundingSubjectRef,
                                                      @Nullable String fundingAccountRole,
                                                      @Nullable SubjectRef bearerSubjectRef) {
        if (ledgerEffect == FundsBenefitLedgerEffect.NO_LEDGER && bearerSubjectRef == null) {
            throw new IllegalArgumentException("fundsBenefit.bearerSubjectRef is required for NO_LEDGER");
        }
        if (ledgerEffect == FundsBenefitLedgerEffect.POSTING_REQUIRED
                && fundingSubjectRef == null
                && !StringUtils.hasText(fundingAccountRole)) {
            throw new IllegalArgumentException("fundsBenefit.funding source is required for POSTING_REQUIRED");
        }
        if (ledgerEffect == FundsBenefitLedgerEffect.HOLD_ONLY
                && !StringUtils.hasText(benefitReference.getHoldId())) {
            throw new IllegalArgumentException("fundsBenefit.holdId is required for HOLD_ONLY");
        }
        if (ledgerEffect == FundsBenefitLedgerEffect.RELEASE_ONLY
                && !StringUtils.hasText(benefitReference.getHoldId())
                && !StringUtils.hasText(benefitReference.getReleaseId())) {
            throw new IllegalArgumentException("fundsBenefit.holdId or releaseId is required for RELEASE_ONLY");
        }
        if (fundingNature == FundsBenefitFundingNature.PREPAID_LIABILITY
                && fundingSubjectRef == null
                && !StringUtils.hasText(benefitReference.getVoucherId())
                && !StringUtils.hasText(benefitReference.getBenefitInstanceId())) {
            throw new IllegalArgumentException("fundsBenefit prepaid liability reference is required");
        }
    }
}
