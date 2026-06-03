package com.wind.funds.spec.transaction;

import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.transaction.enums.FundsBenefitAmountClosureRole;
import com.wind.funds.transaction.enums.FundsBenefitComponentType;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.funds.transaction.enums.FundsBenefitType;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 权益金额组件。
 */
public interface FundsBenefitComponentSpec {

    @NonNull
    String getComponentSn();

    default int getSequence() {
        return 0;
    }

    @NonNull
    FundsBenefitType getBenefitType();

    @NonNull
    FundsBenefitComponentType getComponentType();

    @NonNull
    FundsBenefitAmountClosureRole getClosureRole();

    @NonNull
    Money getAmount();

    @NonNull
    FundsBenefitLedgerEffect getLedgerEffect();

    @NonNull
    FundsBenefitFundingNature getFundingNature();

    @Nullable
    default SubjectRef getBearerSubjectRef() {
        return null;
    }

    @Nullable
    default SubjectRef getBeneficiarySubjectRef() {
        return null;
    }

    @Nullable
    default SubjectRef getFundingSubjectRef() {
        return null;
    }

    @Nullable
    default String getFundingAccountRole() {
        return null;
    }

    @NonNull
    FundsBenefitReferenceSpec getBenefitReference();

    @Nullable
    default FundsBenefitRefundPolicySpec getRefundPolicy() {
        return null;
    }

    @Nullable
    default String getDescription() {
        return null;
    }

    @NonNull
    Map<String, Object> getContextVariables();
}
