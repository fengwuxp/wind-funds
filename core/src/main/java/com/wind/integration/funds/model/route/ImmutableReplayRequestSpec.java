package com.wind.integration.funds.model.route;

import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ReplayRequestSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 不可变 Route 回放请求实现。
 */
@Getter
public final class ImmutableReplayRequestSpec implements ReplayRequestSpec {

    private final RouteReplayType replayType;

    private final FundsTransactionEventType eventType;

    private final String businessScene;

    private final String businessSn;

    private final String referenceBusinessSn;

    private final String referenceSnapshotId;

    private final Money amount;

    private final Money originalAmount;

    private final BigDecimal exchangeRate;

    private final List<String> replayLegIds;

    private final LocalDateTime eventTime;

    private final String description;

    private final FundsOperationActorSpec operator;

    private final Map<String, Object> contextVariables;

    @Builder
    private ImmutableReplayRequestSpec(RouteReplayType replayType,
                                       @Nullable FundsTransactionEventType eventType,
                                       String businessScene,
                                       String businessSn,
                                       @Nullable String referenceBusinessSn,
                                       @Nullable String referenceSnapshotId,
                                       @Nullable Money amount,
                                       @Nullable Money originalAmount,
                                       @Nullable BigDecimal exchangeRate,
                                       @Nullable List<String> replayLegIds,
                                       LocalDateTime eventTime,
                                       @Nullable String description,
                                       @Nullable FundsOperationActorSpec operator,
                                       @Nullable Map<String, Object> contextVariables) {
        this.replayType = replayType;
        this.eventType = eventType;
        this.businessScene = businessScene;
        this.businessSn = businessSn;
        this.referenceBusinessSn = referenceBusinessSn;
        this.referenceSnapshotId = referenceSnapshotId;
        this.amount = amount;
        this.originalAmount = originalAmount;
        this.exchangeRate = exchangeRate;
        this.replayLegIds = List.copyOf(replayLegIds == null ? List.of() : replayLegIds);
        this.eventTime = eventTime;
        this.description = description;
        this.operator = operator;
        this.contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
    }

    @Override
    public @NonNull RouteReplayType getReplayType() {
        return replayType;
    }

    @Override
    public @Nullable FundsTransactionEventType getEventType() {
        return eventType;
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
    public @NonNull List<String> getReplayLegIds() {
        return replayLegIds;
    }

    @Override
    public @NonNull LocalDateTime getEventTime() {
        return eventTime;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
