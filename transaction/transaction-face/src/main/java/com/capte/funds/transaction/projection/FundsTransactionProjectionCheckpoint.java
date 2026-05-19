package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影重放检查点。
 */
@Builder
public record FundsTransactionProjectionCheckpoint(@NonNull FundsTransactionProjectionCheckpointType type,
                                                   @NonNull String checkpointSn) {
}
