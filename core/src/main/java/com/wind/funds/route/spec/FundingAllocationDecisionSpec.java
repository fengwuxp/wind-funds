package com.wind.funds.route.spec;

import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 资金来源决策明细。
 */
public interface FundingAllocationDecisionSpec {

    @NonNull
    String getAllocationId();

    @NonNull
    SubjectRef getSubjectRef();

    @NonNull
    LedgerSubjectCode getLedgerSubjectCode();

    @NonNull
    Money getAmount();

    @Nullable
    default AccountHierarchySnapshotSpec getAccountHierarchySnapshot() {
        return null;
    }

    @Nullable
    default Integer getPriority() {
        return null;
    }

    @Nullable
    default String getReason() {
        return null;
    }
}
