package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放结果。
 */
@Builder
public record FundsTransactionProjectionReplayResult(@NonNull String taskSn,
                                                     @NonNull FundsTransactionProjectionReplayMode mode,
                                                     @NonNull String viewDomain,
                                                     @NonNull FundsTransactionProjectionReplayRange range,
                                                     int loadedFactCount,
                                                     int rebuiltRowCount,
                                                     @NonNull List<FundsTransactionProjectionDifference> differences,
                                                     @NonNull FundsTransactionProjectionCheckpoint checkpoint) {
}
