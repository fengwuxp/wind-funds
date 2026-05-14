package com.capte.funds.route.support;

import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RouteParticipant 构建工厂。
 */
@Component
public class RouteParticipantFactory {

    public @NonNull RouteParticipantSpec createParticipant(@NonNull RouteParticipantRole role,
                                                           @NonNull SubjectRef subjectRef,
                                                           @Nullable String ledgerProfileCode,
                                                           @Nullable Money amount,
                                                           @Nullable String description,
                                                           @Nullable Map<String, Object> contextVariables) {
        return ImmutableRouteParticipantSpec.builder()
                .participantRole(role)
                .subjectRef(subjectRef)
                .ledgerProfileCode(ledgerProfileCode)
                .currency(amount == null ? null : amount.getCurrency().name())
                .amount(amount)
                .description(description)
                .contextVariables(contextVariables)
                .build();
    }

    public @NonNull List<RouteParticipantSpec> distinct(@NonNull List<RouteParticipantSpec> participants) {
        Map<String, RouteParticipantSpec> result = new LinkedHashMap<>(participants.size());
        for (RouteParticipantSpec participant : participants) {
            String key = participant.getParticipantRole().name()
                    + ":"
                    + participant.getSubjectRef().getSubjectType().name()
                    + ":"
                    + participant.getSubjectRef().getSubjectId();
            result.putIfAbsent(key, participant);
        }
        return List.copyOf(result.values());
    }
}
