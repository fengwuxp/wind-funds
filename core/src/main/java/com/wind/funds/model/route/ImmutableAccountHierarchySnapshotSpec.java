package com.wind.funds.model.route;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * 不可变账户层级快照实现。
 */
@Builder
@FieldNameConstants
public record ImmutableAccountHierarchySnapshotSpec(SubjectRef accountRef,
                                                    @Nullable SubjectRef parentAccountRef,
                                                    @Nullable SubjectRef rootAccountRef,
                                                    Map<String, Object> contextVariables)
        implements AccountHierarchySnapshotSpec {

    public ImmutableAccountHierarchySnapshotSpec {
        requireAccountSubject(accountRef, "account hierarchy");
        requireOptionalAccountSubject(parentAccountRef, "parent account");
        requireOptionalAccountSubject(rootAccountRef, "root account");
        requireCompatibleRelation(accountRef, parentAccountRef, "parent account");
        requireCompatibleRelation(accountRef, rootAccountRef, "root account");
        requireCompatibleRelation(parentAccountRef, rootAccountRef);
        contextVariables = RouteContextVariablesValidator.immutableContext(contextVariables, "accountHierarchySnapshot");
    }

    @Override
    public @NonNull SubjectRef getAccountRef() {
        return accountRef;
    }

    @Override
    public @Nullable SubjectRef getParentAccountRef() {
        return parentAccountRef;
    }

    @Override
    public @Nullable SubjectRef getRootAccountRef() {
        return rootAccountRef;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    private static void requireOptionalAccountSubject(@Nullable SubjectRef subjectRef, String label) {
        if (subjectRef != null) {
            requireAccountSubject(subjectRef, label);
        }
    }

    private static void requireAccountSubject(@Nullable SubjectRef subjectRef, String label) {
        if (subjectRef == null || !StringUtils.hasText(subjectRef.getSubjectId())) {
            throw new IllegalArgumentException(label + " subject is required");
        }
        if (subjectRef.getSubjectType() != FundsSubjectType.FUNDING_ACCOUNT
                && subjectRef.getSubjectType() != FundsSubjectType.CREDIT_ACCOUNT) {
            throw new IllegalArgumentException(label + " subject must be funding or credit account");
        }
    }

    private static void requireCompatibleRelation(SubjectRef accountRef,
                                                  @Nullable SubjectRef relationRef,
                                                  String label) {
        if (relationRef == null) {
            return;
        }
        if (sameAccount(accountRef, relationRef)) {
            throw new IllegalArgumentException(label + " must not reference account itself");
        }
        if (!compatible(accountRef.getTenantId(), relationRef.getTenantId())) {
            throw new IllegalArgumentException(label + " tenant must match account tenant");
        }
        if (!compatible(accountRef.getCurrency(), relationRef.getCurrency())) {
            throw new IllegalArgumentException(label + " currency must match account currency");
        }
    }

    private static void requireCompatibleRelation(@Nullable SubjectRef parentAccountRef,
                                                  @Nullable SubjectRef rootAccountRef) {
        if (parentAccountRef == null || rootAccountRef == null) {
            return;
        }
        if (!compatible(parentAccountRef.getTenantId(), rootAccountRef.getTenantId())) {
            throw new IllegalArgumentException("root account tenant must match parent account tenant");
        }
        if (!compatible(parentAccountRef.getCurrency(), rootAccountRef.getCurrency())) {
            throw new IllegalArgumentException("root account currency must match parent account currency");
        }
    }

    private static boolean sameAccount(SubjectRef accountRef, SubjectRef relationRef) {
        return accountRef.getSubjectType() == relationRef.getSubjectType()
                && Objects.equals(accountRef.getSubjectId(), relationRef.getSubjectId())
                && compatible(accountRef.getTenantId(), relationRef.getTenantId());
    }

    private static boolean compatible(@Nullable Object left, @Nullable Object right) {
        return left == null || right == null || Objects.equals(left, right);
    }
}
