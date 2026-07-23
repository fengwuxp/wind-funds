package com.wind.funds.model.route;

import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.route.spec.AccountHierarchySnapshotSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * 不可变 RouteParticipant 实现。
 */
@Builder
@FieldNameConstants
public record ImmutableRouteParticipantSpec(RouteParticipantRole participantRole,
                                            SubjectRef subjectRef,
                                            @Nullable String ledgerProfileCode,
                                            @Nullable String currency,
                                            @Nullable Money amount,
                                            @Nullable String description,
                                            @Nullable AccountHierarchySnapshotSpec accountHierarchySnapshot,
                                            Map<String, Object> contextVariables) implements RouteParticipantSpec {

    public ImmutableRouteParticipantSpec {
        validateAccountHierarchySnapshot(subjectRef, accountHierarchySnapshot);
        contextVariables = RouteContextVariablesValidator.immutableContext(contextVariables, "routeParticipant");
    }

    @Override
    public @NonNull RouteParticipantRole getParticipantRole() {
        return participantRole;
    }

    @Override
    public @NonNull SubjectRef getSubjectRef() {
        return subjectRef;
    }

    @Override
    public @Nullable String getLedgerProfileCode() {
        return ledgerProfileCode;
    }

    @Override
    public @Nullable String getCurrency() {
        return currency;
    }

    @Override
    public @Nullable Money getAmount() {
        return amount;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public @Nullable AccountHierarchySnapshotSpec getAccountHierarchySnapshot() {
        return accountHierarchySnapshot;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }

    private static void validateAccountHierarchySnapshot(
            SubjectRef accountRef,
            @Nullable AccountHierarchySnapshotSpec hierarchySnapshot) {
        if (hierarchySnapshot == null) {
            return;
        }
        if (!accountRef.getSubjectType().isLedgerPostable()) {
            throw new IllegalArgumentException("账户层级快照只能用于可入账账户参与方");
        }
        SubjectRef parentAccountRef = hierarchySnapshot.getParentAccountRef();
        if (!parentAccountRef.getSubjectType().isLedgerPostable()) {
            throw new IllegalArgumentException("账户层级快照父账户必须是资金账户或信用账户");
        }
        if (sameAccount(accountRef, parentAccountRef)) {
            throw new IllegalArgumentException("账户层级快照父账户不能等于参与账户");
        }
        if (!compatible(accountRef.getTenantId(), parentAccountRef.getTenantId())) {
            throw new IllegalArgumentException("账户层级快照父账户租户必须一致");
        }
        if (!compatible(accountRef.getCurrency(), parentAccountRef.getCurrency())) {
            throw new IllegalArgumentException("账户层级快照父账户币种必须一致");
        }
    }

    private static boolean sameAccount(SubjectRef accountRef, SubjectRef parentAccountRef) {
        return accountRef.getSubjectType() == parentAccountRef.getSubjectType()
                && Objects.equals(accountRef.getSubjectId(), parentAccountRef.getSubjectId())
                && compatible(accountRef.getTenantId(), parentAccountRef.getTenantId());
    }

    private static boolean compatible(@Nullable Object left, @Nullable Object right) {
        return left == null || right == null || Objects.equals(left, right);
    }
}
