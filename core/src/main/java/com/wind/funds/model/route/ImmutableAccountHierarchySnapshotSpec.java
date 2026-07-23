package com.wind.funds.model.route;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;

/**
 * 不可变账户层级快照实现。
 */
@Builder
@FieldNameConstants
public record ImmutableAccountHierarchySnapshotSpec(String relationSn,
                                                    SubjectRef parentAccountRef)
        implements AccountHierarchySnapshotSpec {

    public ImmutableAccountHierarchySnapshotSpec {
        if (relationSn == null || relationSn.isBlank()) {
            throw new IllegalArgumentException("accountHierarchySnapshot relationSn is required");
        }
        requireAccountSubject(parentAccountRef, "parent account");
    }

    @Override
    public @NonNull String getRelationSn() {
        return relationSn;
    }

    @Override
    public @NonNull SubjectRef getParentAccountRef() {
        return parentAccountRef;
    }

    private static void requireAccountSubject(SubjectRef subjectRef, String label) {
        if (subjectRef == null
                || subjectRef.getSubjectId() == null
                || subjectRef.getSubjectId().isBlank()) {
            throw new IllegalArgumentException(label + " subject is required");
        }
        if (subjectRef.getSubjectType() != FundsSubjectType.FUNDING_ACCOUNT
                && subjectRef.getSubjectType() != FundsSubjectType.CREDIT_ACCOUNT) {
            throw new IllegalArgumentException(label + " subject must be funding or credit account");
        }
    }
}
