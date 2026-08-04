package com.wind.funds.transaction.projection;

import com.wind.funds.transaction.enums.FundsTransactionEventType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 有界交易投影事实扫描条件。
 */
@Builder
public record FundsTransactionProjectionScanQuery(@NonNull Long tenantId,
                                                  @NonNull Set<FundsTransactionEventType> eventTypes,
                                                  @Nullable String sourceSn,
                                                  @Nullable String ownerType,
                                                  @Nullable String ownerId,
                                                  @Nullable LocalDateTime startTime,
                                                  @Nullable LocalDateTime endTime,
                                                  @Nullable FundsTransactionProjectionScanCursor cursor,
                                                  int maxBatchSize) {

    public FundsTransactionProjectionScanQuery {
        eventTypes = eventTypes == null ? Set.of() : Set.copyOf(eventTypes);
    }

    public boolean isBounded() {
        return StringUtils.hasText(sourceSn)
                || (StringUtils.hasText(ownerType) && StringUtils.hasText(ownerId))
                || (startTime != null && endTime != null && startTime.isBefore(endTime));
    }

    public FundsTransactionProjectionScanQuery withCursor(FundsTransactionProjectionScanCursor value) {
        return new FundsTransactionProjectionScanQuery(tenantId, eventTypes, sourceSn, ownerType, ownerId,
                startTime, endTime, value, maxBatchSize);
    }
}
