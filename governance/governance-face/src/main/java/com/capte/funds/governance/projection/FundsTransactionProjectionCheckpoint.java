package com.capte.funds.governance.projection;

import com.capte.funds.governance.enums.ProjectionCheckpointType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

/**
 * 交易投影重放检查点，是交易投影重放自己的处理水位。
 *
 * <p>职责：记录本次投影重放从哪个交易投影边界继续，保证任务可追溯、可暂停、可恢复。</p>
 *
 * <p>能力：通过类型和流水号共同标识处理边界，并在结果中回传给运营、测试或审计侧确认。</p>
 *
 * <p>边界：只能用于交易投影重放，不承载其他投影域或批处理域的 checkpoint 语义。</p>
 */
@Builder
public record FundsTransactionProjectionCheckpoint(@NonNull ProjectionCheckpointType type,
                                                   @NonNull String checkpointSn) {
}
