package com.wind.funds.model.route;

import com.wind.funds.route.ref.SubjectRef;
import com.wind.transaction.core.Money;
import org.springframework.util.StringUtils;

import java.util.Objects;

final class FundingAllocationDecisionValidator {

    private FundingAllocationDecisionValidator() {
    }

    static void requireSubjectAmountCurrency(SubjectRef subjectRef, Money amount) {
        if (StringUtils.hasText(subjectRef.getCurrency())
                && !Objects.equals(subjectRef.getCurrency(), amount.getCurrency().name())) {
            throw new IllegalArgumentException("funding allocation amount currency must match subjectRef currency");
        }
    }
}
