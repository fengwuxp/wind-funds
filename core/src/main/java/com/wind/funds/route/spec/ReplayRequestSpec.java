package com.wind.funds.route.spec;

import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.route.enums.RouteReplayType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * RouteSnapshot 回放请求。
 */
public interface ReplayRequestSpec {

    @NonNull
    RouteReplayType getReplayType();

    @Nullable
    default FundsTransactionEventType getEventType() {
        return null;
    }

    @NonNull
    String getBusinessScene();

    @NonNull
    String getBusinessSn();

    @Nullable
    default String getReferenceBusinessSn() {
        return null;
    }

    @Nullable
    default String getReferenceSnapshotId() {
        return null;
    }

    @Nullable
    default Money getAmount() {
        return null;
    }

    @Nullable
    default Money getOriginalAmount() {
        return null;
    }

    @Nullable
    default BigDecimal getExchangeRate() {
        return null;
    }

    @NonNull
    default List<String> getReplayLegIds() {
        return List.of();
    }

    @NonNull
    LocalDateTime getEventTime();

    @Nullable
    default String getDescription() {
        return null;
    }

    @Nullable
    default FundsOperationActorSpec getOperator() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}
