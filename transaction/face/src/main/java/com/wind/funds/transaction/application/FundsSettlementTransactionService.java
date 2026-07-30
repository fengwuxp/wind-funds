package com.wind.funds.transaction.application;

import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 结算单编排使用的内部资金原语。
 *
 * <p>固定将同一资金账户的 {@code AVAILABLE} 转为 {@code SETTLEMENT}，宿主不得直接调用。</p>
 */
@NullMarked
public interface FundsSettlementTransactionService {

    String lock(FundsSettlementLockRequest request, WindOperator operator);
}
