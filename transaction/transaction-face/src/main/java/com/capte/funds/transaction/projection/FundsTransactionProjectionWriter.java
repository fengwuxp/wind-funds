package com.capte.funds.transaction.projection;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放写入端口。
 */
public interface FundsTransactionProjectionWriter {

    @NonNull
    List<FundsTransactionProjectionDifference> compare(@NonNull String viewDomain,
                                                       @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

    void upsertShadow(@NonNull String taskSn, @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

    void upsertOfficial(@NonNull String taskSn, @NonNull List<FundsTransactionProjectionRow> rebuiltRows);
}
