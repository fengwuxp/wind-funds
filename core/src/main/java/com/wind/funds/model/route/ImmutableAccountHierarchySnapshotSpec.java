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
                                                    String hierarchyVersion,
                                                    Map<String, Object> contextVariables,
                                                    @Nullable String description)
        implements AccountHierarchySnapshotSpec {

    public ImmutableAccountHierarchySnapshotSpec {
        requireAccountSubject(accountRef, "account hierarchy");
        requireOptionalAccountSubject(parentAccountRef, "parent account");
        requireOptionalAccountSubject(rootAccountRef, "root account");
        requireCompatibleRelation(accountRef, parentAccountRef, "parent account");
        requireCompatibleRelation(accountRef, rootAccountRef, "root account");
        if (!StringUtils.hasText(hierarchyVersion)) {
            throw new IllegalArgumentException("account hierarchy version is required");
        }
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
    public @NonNull String getHierarchyVersion() {
        return hierarchyVersion;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
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
        if (!compatible(accountRef.getTenantId(), relationRef.getTenantId())) {
            throw new IllegalArgumentException(label + " tenant must match account tenant");
        }
        if (!compatible(accountRef.getCurrency(), relationRef.getCurrency())) {
            throw new IllegalArgumentException(label + " currency must match account currency");
        }
    }

    private static boolean compatible(@Nullable Object left, @Nullable Object right) {
        return left == null || right == null || Objects.equals(left, right);
    }
}
