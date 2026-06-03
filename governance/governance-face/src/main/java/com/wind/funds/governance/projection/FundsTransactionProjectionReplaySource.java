package com.wind.funds.governance.projection;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 交易投影重放事实来源端口，是重放服务读取来源事实的只读端口。
 *
 * <p>职责：按照有界重放范围加载能够重建交易投影的来源事实。</p>
 *
 * <p>能力：实现侧可以从交易主表、交易明细、route snapshot 或测试夹具中提供统一事实列表。</p>
 *
 * <p>边界：端口实现必须保持只读，不得在读取事实时写交易、写账本、改余额、推进清结算或修正投影。</p>
 */
public interface FundsTransactionProjectionReplaySource {

    /**
     * 按指定范围加载交易投影重放所需的来源事实。
     *
     * @param range 有界重放范围，必须由调用方在服务层校验为单笔、主体、时间窗口或批次范围
     * @return 可用于重建投影视图的只读事实列表；没有匹配事实时返回空列表
     */
    @NonNull
    List<FundsTransactionProjectionFact> loadFacts(@NonNull FundsTransactionProjectionReplayRange range);
}
