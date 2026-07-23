package com.wind.funds.model.route;

import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteLegSpec;
import com.wind.funds.route.spec.RouteParticipantSpec;
import com.wind.funds.route.spec.RoutingDecisionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 不可变 ResolvedRoute 实现。
 */
@Builder
public record ImmutableResolvedRouteSpec(@Nullable Long tenantId,
                                         String routeCode,
                                         String routeVersion,
                                         String businessScene,
                                         String businessSn,
                                         FundsInstructionType instructionType,
                                         FundsTransactionEventType eventType,
                                         DefaultFundsTransactionType transactionType,
                                         List<RouteParticipantSpec> participants,
                                         List<RouteLegSpec> legs,
                                         @Nullable RoutingDecisionSpec routingDecision,
                                         @Nullable PaymentInstrumentRefSpec paymentInstrumentRef,
                                         @Nullable ExternalAccountRefSpec externalAccountRef,
                                         @Nullable PlatformAccountsSnapshotSpec platformAccounts,
                                         LocalDateTime resolvedAt,
                                         @Nullable LocalDateTime expiresAt,
                                         @Nullable String description,
                                         Map<String, Object> contextVariables) implements ResolvedRouteSpec {

    public ImmutableResolvedRouteSpec {
        participants = List.copyOf(participants == null ? List.of() : participants);
        legs = List.copyOf(legs == null ? List.of() : legs);
        RouteFactContractValidator.validateResolvedRoute(routeCode,
                routeVersion,
                businessScene,
                businessSn,
                instructionType,
                eventType,
                transactionType,
                resolvedAt);
        contextVariables = RouteContextVariablesValidator.immutableContext(contextVariables, "resolvedRoute");
    }

    @Override
    public @NonNull String getRouteCode() {
        return routeCode;
    }

    @Override
    public @NonNull String getRouteVersion() {
        return routeVersion;
    }

    @Override
    public @NonNull String getBusinessScene() {
        return businessScene;
    }

    @Override
    public @NonNull String getBusinessSn() {
        return businessSn;
    }

    @Override
    public @NonNull FundsInstructionType getInstructionType() {
        return instructionType;
    }

    @Override
    public @NonNull FundsTransactionEventType getEventType() {
        return eventType;
    }

    @Override
    public @NonNull DefaultFundsTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public @NonNull List<RouteParticipantSpec> getParticipants() {
        return participants;
    }

    @Override
    public @NonNull List<RouteLegSpec> getLegs() {
        return legs;
    }

    @Override
    public @NonNull LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }


    @Override
    public @Nullable Long getTenantId() {
        return tenantId;
    }

    @Override
    public @Nullable RoutingDecisionSpec getRoutingDecision() {
        return routingDecision;
    }

    @Override
    public @Nullable PaymentInstrumentRefSpec getPaymentInstrumentRef() {
        return paymentInstrumentRef;
    }

    @Override
    public @Nullable ExternalAccountRefSpec getExternalAccountRef() {
        return externalAccountRef;
    }

    @Override
    public @Nullable PlatformAccountsSnapshotSpec getPlatformAccounts() {
        return platformAccounts;
    }

    @Override
    public @Nullable LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

}
