package com.wind.funds.transaction.projection;

import lombok.Builder;

/**
 * 交易投影扫描的稳定双高水位游标。
 */
@Builder
public record FundsTransactionProjectionScanCursor(long lastTransactionId,
                                                   long transactionUpperBoundId,
                                                   long lastFrozenOrderId,
                                                   long frozenOrderUpperBoundId) {

    public static FundsTransactionProjectionScanCursor initial(long transactionUpperBoundId,
                                                               long frozenOrderUpperBoundId) {
        return new FundsTransactionProjectionScanCursor(0L, transactionUpperBoundId, 0L,
                frozenOrderUpperBoundId);
    }

    public String checkpointValue() {
        return "%d:%d:%d:%d".formatted(lastTransactionId, transactionUpperBoundId,
                lastFrozenOrderId, frozenOrderUpperBoundId);
    }

    public static FundsTransactionProjectionScanCursor parse(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 4) {
            throw new IllegalArgumentException("交易投影扫描 checkpoint 格式不正确");
        }
        return new FundsTransactionProjectionScanCursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]),
                Long.parseLong(parts[2]), Long.parseLong(parts[3]));
    }
}
