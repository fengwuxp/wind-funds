package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影重放请求。
 */
@Builder
public record FundsTransactionProjectionReplayRequest(@NonNull String taskSn,
                                                      @NonNull FundsTransactionProjectionReplayMode mode,
                                                      @NonNull String viewDomain,
                                                      @NonNull FundsTransactionProjectionReplayRange replayRange,
                                                      @NonNull FundsTransactionProjectionCheckpoint checkpoint) {
}
