package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 交易投影重放范围。
 */
@Builder
public record FundsTransactionProjectionReplayRange(@Nullable String sourceSn,
                                                    @Nullable String ownerType,
                                                    @Nullable String ownerId,
                                                    @Nullable LocalDateTime startTime,
                                                    @Nullable LocalDateTime endTime,
                                                    @Nullable String batchType,
                                                    @Nullable String batchSn) {

    public boolean isBounded() {
        return hasText(sourceSn)
                || (hasText(ownerType) && hasText(ownerId))
                || (startTime != null && endTime != null && startTime.isBefore(endTime))
                || (hasText(batchType) && hasText(batchSn));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
