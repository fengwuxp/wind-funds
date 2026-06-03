package com.wind.funds.governance.projection;

import com.wind.funds.governance.enums.ProjectionReplayMode;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影重放请求，是一次交易投影视图核对或重建任务的入口契约。
 *
 * <p>职责：描述本次重放由哪个任务发起、采用什么模式、作用于哪个视图域、使用哪个有界范围以及
 * 从哪个交易投影 checkpoint 继续。</p>
 *
 * <p>能力：让交易层实现可以统一完成差异核对、影子投影重建和正式投影重建。</p>
 *
 * <p>边界：该请求只表达只读投影的重放意图，不表达交易补单、账务补账、余额修正或清结算重跑。</p>
 */
@Builder
public record FundsTransactionProjectionReplayRequest(@NonNull String taskSn,
                                                      @NonNull ProjectionReplayMode mode,
                                                      @NonNull String viewDomain,
                                                      @NonNull FundsTransactionProjectionReplayRange replayRange,
                                                      @NonNull FundsTransactionProjectionCheckpoint checkpoint) {
}
