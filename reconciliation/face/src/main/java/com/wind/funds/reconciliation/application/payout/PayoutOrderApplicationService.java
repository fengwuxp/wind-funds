package com.wind.funds.reconciliation.application.payout;

import com.wind.funds.reconciliation.model.dto.PayoutOrderDTO;
import com.wind.funds.reconciliation.model.request.CreatePayoutOrderRequest;
import com.wind.funds.reconciliation.model.request.HandlePayoutReceiptRequest;
import com.wind.funds.reconciliation.model.request.SubmitPayoutOrderRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 出款单公共应用服务。
 *
 * <p>只维护出款事实和资金结果，不执行任何外部通道调用。</p>
 */
@NullMarked
public interface PayoutOrderApplicationService {

    PayoutOrderDTO createOrder(CreatePayoutOrderRequest request, WindOperator operator);

    PayoutOrderDTO submitOrder(SubmitPayoutOrderRequest request, WindOperator operator);

    PayoutOrderDTO handleReceipt(HandlePayoutReceiptRequest request, WindOperator operator);

    PayoutOrderDTO getOrder(Long tenantId, String payoutOrderSn);
}
