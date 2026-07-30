package com.wind.funds.reconciliation.application.settlement;

import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CancelSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReturnSettlementOrderToDraftRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 结算单公共应用服务。
 *
 * <p>当前只支持已确认清算本金的中间户锁定；外部出款不属于本服务。</p>
 */
@NullMarked
public interface SettlementOrderApplicationService {

    SettlementOrderDTO createOrder(CreateSettlementOrderRequest request, WindOperator operator);

    SettlementOrderDTO submitOrder(SubmitSettlementOrderRequest request, WindOperator operator);

    SettlementOrderDTO returnToDraft(ReturnSettlementOrderToDraftRequest request, WindOperator operator);

    SettlementOrderDTO approveOrder(ApproveSettlementOrderRequest request, WindOperator operator);

    SettlementOrderDTO cancelOrder(CancelSettlementOrderRequest request, WindOperator operator);

    SettlementOrderDTO lockOrder(LockSettlementOrderRequest request, WindOperator operator);

    SettlementOrderDTO getOrder(Long tenantId, String settlementOrderSn);
}
