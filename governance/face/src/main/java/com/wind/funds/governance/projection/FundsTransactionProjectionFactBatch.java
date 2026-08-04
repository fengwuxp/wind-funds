package com.wind.funds.governance.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 一批交易投影重放事实及其下一检查点。
 */
@Builder
public record FundsTransactionProjectionFactBatch(@NonNull List<FundsTransactionProjectionFact> facts,
                                                  @NonNull FundsTransactionProjectionCheckpoint nextCheckpoint,
                                                  boolean hasMore) {

    public FundsTransactionProjectionFactBatch {
        facts = List.copyOf(facts);
    }
}
