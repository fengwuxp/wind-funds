package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 交易投影重建行。
 */
@Builder
public record FundsTransactionProjectionRow(@NonNull String projectionSn,
                                            @NonNull String viewDomain,
                                            @NonNull String ownerType,
                                            @NonNull String ownerId,
                                            @NonNull String sourceSn,
                                            @NonNull String displayType,
                                            @NonNull String displayStatus,
                                            long amount,
                                            @NonNull String currency,
                                            @NonNull LocalDateTime occurredTime,
                                            @NonNull Map<String, Object> payload) {
}
