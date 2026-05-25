package com.wind.integration.funds.model.route;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * 不可变资金来源决策明细实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundingAllocationDecisionSpec(String allocationId,
                                                     SubjectRef subjectRef,
                                                     LedgerSubjectCode ledgerSubjectCode,
                                                     Money amount,
                                                     @Nullable Integer priority,
                                                     @Nullable String reason)
        implements FundingAllocationDecisionSpec {

    public ImmutableFundingAllocationDecisionSpec {
        if (priority == null) {
            throw new IllegalArgumentException("funding allocation priority is required");
        }
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("funding allocation amount must be positive");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("funding allocation reason is required");
        }
    }

    @Override
    public @NonNull String getAllocationId() {
        return allocationId;
    }

    @Override
    public @NonNull SubjectRef getSubjectRef() {
        return subjectRef;
    }

    @Override
    public @NonNull LedgerSubjectCode getLedgerSubjectCode() {
        return ledgerSubjectCode;
    }

    @Override
    public @NonNull Money getAmount() {
        return amount;
    }

    @Override
    public @Nullable Integer getPriority() {
        return priority;
    }

    @Override
    public @Nullable String getReason() {
        return reason;
    }
}
