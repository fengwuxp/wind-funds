package com.capte.funds.transaction.projection;

import org.jspecify.annotations.NonNull;

/**
 * 交易投影正常发布端口。
 *
 * <p>职责：在资金交易生命周期和账本入账成功后构建或更新交易只读投影。</p>
 *
 * <p>边界：发布失败不得回滚已经成功的交易事实或账本事实；投影补偿、影子重建和正式重放由治理模块承接。</p>
 */
public interface FundsTransactionProjectionPublisher {

    /**
     * 发布交易只读投影。
     *
     * @param context 投影发布上下文
     */
    void publish(@NonNull FundsTransactionProjectionPublishContext context);
}
