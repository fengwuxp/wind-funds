package com.wind.funds.model.route;

import com.wind.funds.route.ref.SubjectRef;
import com.wind.transaction.core.Money;

import java.util.Objects;

final class FundingAllocationDecisionValidator {

    private FundingAllocationDecisionValidator() {
    }

    static void requireSubjectAmountCurrency(SubjectRef subjectRef, Money amount) {
        if (subjectRef.getCurrency() != null && !subjectRef.getCurrency().isBlank()
                && !Objects.equals(subjectRef.getCurrency(), amount.getCurrency().name())) {
            throw new IllegalArgumentException("funding allocation amount currency must match subjectRef currency");
        }
    }
}
