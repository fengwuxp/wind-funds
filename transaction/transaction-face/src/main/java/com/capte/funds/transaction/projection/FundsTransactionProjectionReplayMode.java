package com.capte.funds.transaction.projection;

/**
 * 交易投影重放模式。
 */
public enum FundsTransactionProjectionReplayMode {

    /**
     * 只生成差异报告，不写正式或影子投影。
     */
    VERIFY_ONLY,

    /**
     * 写影子投影，用于灰度核对。
     */
    REBUILD_SHADOW,

    /**
     * 覆盖正式只读投影。
     */
    REBUILD_APPLY
}
