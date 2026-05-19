package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 交易投影重放差异项。
 */
@Builder
public record FundsTransactionProjectionDifference(@NonNull String sourceSn,
                                                   @NonNull String fieldName,
                                                   @Nullable Object expectedValue,
                                                   @Nullable Object actualValue) {
}
