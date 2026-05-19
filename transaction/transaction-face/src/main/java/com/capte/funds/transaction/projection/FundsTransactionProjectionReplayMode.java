package com.capte.funds.transaction.projection;

/**
 * 交易投影重放模式，定义重放任务对投影视图的写入强度。
 *
 * <p>职责：让调用方显式选择只核对、写影子投影或写正式投影。</p>
 *
 * <p>能力：同一套重放链路可以覆盖审计核查、灰度重建和正式修复三类运营动作。</p>
 *
 * <p>边界：模式只控制交易投影写入行为，不代表交易状态推进、账务处理或余额调整。</p>
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
