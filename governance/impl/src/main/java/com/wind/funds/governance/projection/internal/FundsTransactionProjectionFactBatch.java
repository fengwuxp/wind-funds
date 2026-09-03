package com.wind.funds.governance.projection.internal;

import com.wind.funds.governance.projection.FundsTransactionProjectionCheckpoint;
import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 一批交易投影重放事实及其下一检查点。
 *
 * @param facts 当前批次的重放事实
 * @param nextCheckpoint 下一批次检查点
 * @param hasMore 是否仍有后续批次
 * @author wuxp
 * @since 2026-08-31
 */
@Builder
public record FundsTransactionProjectionFactBatch(@NonNull List<FundsTransactionProjectionFact> facts,
                                                  @NonNull FundsTransactionProjectionCheckpoint nextCheckpoint,
                                                  boolean hasMore) {

    public FundsTransactionProjectionFactBatch {
        facts = List.copyOf(facts);
    }
}
