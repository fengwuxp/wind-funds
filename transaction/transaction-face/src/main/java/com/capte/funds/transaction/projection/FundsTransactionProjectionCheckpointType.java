package com.capte.funds.transaction.projection;

/**
 * 交易投影重放检查点类型，用于识别 checkpoint 所属的事实或投影域。
 *
 * <p>职责：防止调用方把余额、归档或报表的处理水位误传给交易投影重放链路。</p>
 *
 * <p>能力：为重放请求校验提供明确枚举，后续也可以承载跨投影治理的类型识别。</p>
 *
 * <p>边界：交易投影重放当前只接受 {@link #TRANSACTION_PROJECTION}，其他类型用于显式拒绝错域复用。</p>
 */
public enum FundsTransactionProjectionCheckpointType {

    TRANSACTION_PROJECTION,

    BALANCE_WATERMARK,

    ARCHIVE_MANIFEST,

    REPORT_METRIC
}
