package com.wind.funds.transaction.projection;

import lombok.Builder;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 一批稳定边界内的交易投影事实。
 */
@Builder
public record FundsTransactionProjectionScanBatch(@NonNull List<FundsTransactionProjectionExplanation> facts,
                                                  @NonNull FundsTransactionProjectionScanCursor nextCursor,
                                                  boolean hasMore) {

    public FundsTransactionProjectionScanBatch {
        facts = List.copyOf(facts);
    }
}
