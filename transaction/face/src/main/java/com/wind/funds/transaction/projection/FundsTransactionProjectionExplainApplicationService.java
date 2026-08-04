package com.wind.funds.transaction.projection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/**
 * 交易投影解释查询应用服务。
 *
 * <p>职责：基于已经落库的资金交易事实、RouteSnapshot 和交易明细，生成用户账单、商户账单、
 * 运营时间线和财务视图可消费的只读解释摘要。</p>
 *
 * <p>边界：本服务只读解释既有事实，不生成 Route，不写资金交易、账本交易、账本分录或余额投影。</p>
 */
@NullMarked
public interface FundsTransactionProjectionExplainApplicationService {

    /**
     * 解释一笔已落库资金交易的投影展示口径。
     *
     * @param query 投影解释查询条件
     * @return 交易投影解释摘要
     */
    @NonNull
    FundsTransactionProjectionExplanation explain(@NonNull FundsTransactionProjectionExplainQuery query);

    @NonNull
    FundsTransactionProjectionScanCursor initializeScanCursor(@NonNull FundsTransactionProjectionScanQuery query);

    @NonNull
    FundsTransactionProjectionScanBatch scan(@NonNull FundsTransactionProjectionScanQuery query);
}
