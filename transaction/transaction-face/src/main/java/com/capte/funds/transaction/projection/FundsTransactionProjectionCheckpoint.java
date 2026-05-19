package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影重放检查点，是交易投影重放自己的处理水位。
 *
 * <p>职责：记录本次投影重放从哪个交易投影边界继续，保证任务可追溯、可暂停、可恢复。</p>
 *
 * <p>能力：与重放范围共同确定事实读取边界，并在结果中回传给运营、测试或审计侧确认。</p>
 *
 * <p>边界：只能用于交易投影重放，不得复用余额水位、归档 manifest 或报表指标 checkpoint。</p>
 */
@Builder
public record FundsTransactionProjectionCheckpoint(@NonNull FundsTransactionProjectionCheckpointType type,
                                                   @NonNull String checkpointSn) {
}
