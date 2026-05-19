package com.capte.funds.transaction.projection;

/**
 * 交易投影重放检查点类型。
 */
public enum FundsTransactionProjectionCheckpointType {

    TRANSACTION_PROJECTION,

    BALANCE_WATERMARK,

    ARCHIVE_MANIFEST,

    REPORT_METRIC
}
