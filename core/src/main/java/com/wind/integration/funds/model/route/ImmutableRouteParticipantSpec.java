package com.wind.integration.funds.model.route;

import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

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
                                            Map<String, Object> contextVariables) implements RouteParticipantSpec {

    public ImmutableRouteParticipantSpec {
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
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
