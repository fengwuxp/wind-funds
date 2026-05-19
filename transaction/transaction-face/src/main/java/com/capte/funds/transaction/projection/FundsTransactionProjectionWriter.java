package com.capte.funds.transaction.projection;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放写入端口，是重放服务对投影视图执行比对和写入的端口。
 *
 * <p>职责：比较现有投影与重建行差异，并按模式写入影子投影或正式只读投影。</p>
 *
 * <p>能力：支持 verify-only 差异核对、shadow 灰度核对和 apply 正式投影刷新。</p>
 *
 * <p>边界：端口只能处理投影视图数据，不得写交易事实、账本分录、余额桶或其他业务域数据。</p>
 */
public interface FundsTransactionProjectionWriter {

    /**
     * 比较现有投影视图与本次重建结果之间的差异。
     *
     * @param viewDomain 投影视图域，例如用户账单、商户流水或运营视图
     * @param rebuiltRows 根据来源事实重建出的候选投影行
     * @return 字段级差异列表；完全一致时返回空列表
     */
    @NonNull
    List<FundsTransactionProjectionDifference> compare(@NonNull String viewDomain,
                                                       @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

    /**
     * 写入影子投影，用于灰度核对、人工复核或正式覆盖前的结果预览。
     *
     * @param taskSn 本次重放任务号，用于区分不同批次的影子结果
     * @param rebuiltRows 根据来源事实重建出的候选投影行
     */
    void upsertShadow(@NonNull String taskSn, @NonNull List<FundsTransactionProjectionRow> rebuiltRows);

    /**
     * 写入正式只读投影，用于在确认重建结果后刷新线上投影视图。
     *
     * @param taskSn 本次重放任务号，用于审计追踪投影刷新来源
     * @param rebuiltRows 根据来源事实重建出的正式投影行
     */
    void upsertOfficial(@NonNull String taskSn, @NonNull List<FundsTransactionProjectionRow> rebuiltRows);
}
