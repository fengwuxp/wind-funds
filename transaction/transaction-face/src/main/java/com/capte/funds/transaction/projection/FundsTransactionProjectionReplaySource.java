package com.capte.funds.transaction.projection;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放事实来源端口。
 */
public interface FundsTransactionProjectionReplaySource {

    @NonNull
    List<FundsTransactionProjectionFact> loadFacts(@NonNull FundsTransactionProjectionReplayRange range);
}
