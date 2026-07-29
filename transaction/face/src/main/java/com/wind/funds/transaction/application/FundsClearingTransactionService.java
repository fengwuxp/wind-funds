package com.wind.funds.transaction.application;

import com.wind.funds.transaction.model.request.FundsClearingConfirmRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 清算确认资金命令服务。
 *
 * <p>本服务只承接上层已经确认的清算批次，将同一资金账户的 {@code CLEARING} 转为
 * {@code AVAILABLE}。调用方不能选择账目或资金路径。</p>
 */
@NullMarked
public interface FundsClearingTransactionService {

    /**
     * 确认清算批次并生成清算资金事实。
     *
     * @param request  清算确认请求
     * @param operator 操作人
     * @return 资金交易流水号
     */
    String confirm(FundsClearingConfirmRequest request, WindOperator operator);
}
