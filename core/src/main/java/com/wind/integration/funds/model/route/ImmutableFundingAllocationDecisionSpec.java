package com.wind.integration.funds.model.route;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 不可变资金来源决策明细实现。
 */
@Builder
public record ImmutableFundingAllocationDecisionSpec(String allocationId,
                                                     SubjectRef subjectRef,
                                                     LedgerSubjectCode ledgerSubjectCode,
                                                     Money amount,
                                                     @Nullable Integer priority,
                                                     @Nullable String reason)
        implements FundingAllocationDecisionSpec {

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
