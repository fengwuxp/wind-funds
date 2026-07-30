package com.wind.funds.transaction.application;

import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 清算确认内部资金原语。
 *
 * <p>本服务只承接清算批次编排已经完成最终复核、正在原子确认的批次，将同一资金账户的
 * {@code CLEARING} 转为 {@code AVAILABLE}。调用方不能选择账目或资金路径。</p>
 *
 * <p>本接口因 transaction 与 reconciliation 的模块依赖方向而位于 transaction-face，
 * 不是宿主直接调用的清算业务入口。宿主应通过
 * {@code ClearingBatchApplicationService#confirmBatch} 发起清算确认。</p>
 */
@NullMarked
public interface FundsClearingTransactionService {

    /**
     * 为正在原子确认的清算批次生成清算资金事实。
     *
     * @param request  清算确认请求
     * @param operator 操作人
     * @return 资金交易流水号
     */
    String confirm(FundsClearingConfirmRequest request, WindOperator operator);
}
