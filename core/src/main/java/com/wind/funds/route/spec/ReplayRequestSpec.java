package com.wind.funds.route.spec;

import com.wind.funds.route.enums.RouteReplayType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.operator.WindOperator;
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

    /**
     * 返回触发本次回放的运行时操作者上下文。
     *
     * <p>该对象不属于可持久化回放快照；审计需要在事实写入边界投影稳定身份字段。</p>
     *
     * @return 当前运行时操作者，无操作者上下文时返回 {@code null}
     */
    @Nullable
    default WindOperator getOperator() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}
