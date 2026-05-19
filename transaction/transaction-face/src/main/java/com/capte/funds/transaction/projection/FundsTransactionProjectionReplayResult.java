package com.capte.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放结果，是一次投影重放任务的执行摘要。
 *
 * <p>职责：返回任务号、模式、视图域、重放范围、读取事实数、重建行数、差异项和 checkpoint。</p>
 *
 * <p>能力：支撑测试断言、运营复核和审计追踪，帮助判断本次重放是仅核对、写影子投影还是写正式投影。</p>
 *
 * <p>边界：该结果只描述投影重放执行情况，不代表新的交易事实、账务事实或余额变更结果。</p>
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
