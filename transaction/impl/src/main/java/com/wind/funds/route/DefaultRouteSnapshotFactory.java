package com.wind.funds.route;

import com.wind.funds.route.model.ImmutableRouteSnapshotSpec;
import com.wind.funds.transaction.support.FundsRouteCodes;
import com.wind.funds.route.spec.ResolvedRouteSpec;
import com.wind.funds.route.spec.RouteSnapshotSpec;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 默认路径快照工厂。
 */
@Component
public class DefaultRouteSnapshotFactory implements RouteSnapshotFactory {

    private static final String SNAPSHOT_ID_SUFFIX = "_ROUTE";

    @Override
    public @NonNull RouteSnapshotSpec createSnapshot(@NonNull ResolvedRouteSpec resolvedRoute) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(resolvedRoute.getTenantId())
                .snapshotId(resolvedRoute.getBusinessSn() + SNAPSHOT_ID_SUFFIX)
                .snapshotSchemaVersion(FundsRouteCodes.CURRENT_ROUTE_SNAPSHOT_SCHEMA_VERSION)
                .routeCode(resolvedRoute.getRouteCode())
                .routeVersion(resolvedRoute.getRouteVersion())
                .businessScene(resolvedRoute.getBusinessScene())
                .businessSn(resolvedRoute.getBusinessSn())
                .instructionType(resolvedRoute.getInstructionType())
                .eventType(resolvedRoute.getEventType())
                .transactionType(resolvedRoute.getTransactionType())
                .participants(List.copyOf(resolvedRoute.getParticipants()))
                .legs(List.copyOf(resolvedRoute.getLegs()))
                .routingDecision(resolvedRoute.getRoutingDecision())
                .paymentInstrumentRef(resolvedRoute.getPaymentInstrumentRef())
                .externalAccountRef(resolvedRoute.getExternalAccountRef())
                .platformAccounts(resolvedRoute.getPlatformAccounts())
                .resolvedAt(resolvedRoute.getResolvedAt())
                .expiresAt(resolvedRoute.getExpiresAt())
                .description(resolvedRoute.getDescription())
                .contextVariables(Map.copyOf(resolvedRoute.getContextVariables()))
                .build();
    }
}
