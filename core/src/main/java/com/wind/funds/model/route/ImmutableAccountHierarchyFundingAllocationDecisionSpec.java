package com.wind.funds.model.route;

import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 带账户层级快照的不可变资金来源决策明细实现。
 */
@Builder
@FieldNameConstants
public record ImmutableAccountHierarchyFundingAllocationDecisionSpec(String allocationId,
                                                                     SubjectRef subjectRef,
                                                                     LedgerSubjectCode ledgerSubjectCode,
                                                                     Money amount,
                                                                     AccountHierarchySnapshotSpec accountHierarchySnapshot,
                                                                     @Nullable Integer priority,
                                                                     @Nullable String reason)
        implements FundingAllocationDecisionSpec {

    public ImmutableAccountHierarchyFundingAllocationDecisionSpec {
        if (priority == null) {
            throw new IllegalArgumentException("funding allocation priority is required");
        }
        if (accountHierarchySnapshot == null) {
            throw new IllegalArgumentException("accountHierarchySnapshot is required");
        }
        if (amount.getAmount() <= 0) {
            throw new IllegalArgumentException("funding allocation amount must be positive");
        }
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("funding allocation reason is required");
        }
        validateAccountHierarchySnapshot(subjectRef, accountHierarchySnapshot);
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
    public @NonNull AccountHierarchySnapshotSpec getAccountHierarchySnapshot() {
        return accountHierarchySnapshot;
    }

    @Override
    public @Nullable Integer getPriority() {
        return priority;
    }

    @Override
    public @Nullable String getReason() {
        return reason;
    }

    private static void validateAccountHierarchySnapshot(SubjectRef subjectRef,
                                                         AccountHierarchySnapshotSpec snapshot) {
        SubjectRef accountRef = snapshot.getAccountRef();
        if (!Objects.equals(accountRef.getSubjectId(), subjectRef.getSubjectId())
                || accountRef.getSubjectType() != subjectRef.getSubjectType()) {
            throw new IllegalArgumentException(
                    "accountHierarchySnapshot accountRef must match funding allocation subjectRef");
        }
        if (!Objects.equals(accountRef.getTenantId(), subjectRef.getTenantId())) {
            throw new IllegalArgumentException(
                    "accountHierarchySnapshot accountRef tenant must match funding allocation subjectRef tenant");
        }
    }
}
