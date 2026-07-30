package com.wind.funds.transaction.application;

import com.wind.funds.transaction.model.request.FundsPayoutRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 清结算出款编排使用的内部资金原语。
 */
@NullMarked
public interface FundsPayoutTransactionService {

    String succeed(FundsPayoutRequest request, WindOperator operator);

    String fail(FundsPayoutRequest request, WindOperator operator);
}
