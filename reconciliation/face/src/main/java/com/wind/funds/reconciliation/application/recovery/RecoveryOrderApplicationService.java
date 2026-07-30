package com.wind.funds.reconciliation.application.recovery;

import com.wind.funds.reconciliation.model.dto.RecoveryOrderDTO;
import com.wind.funds.reconciliation.model.request.CreateRecoveryOrderRequest;
import com.wind.funds.reconciliation.model.request.RecordRecoveryResultRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 追偿责任与已完成资金结果登记服务。
 *
 * <p>不选择追偿策略，也不执行退款、拒付、抵扣、催收或资金交易。</p>
 */
@NullMarked
public interface RecoveryOrderApplicationService {

    RecoveryOrderDTO createOrder(CreateRecoveryOrderRequest request, WindOperator operator);

    RecoveryOrderDTO recordResult(RecordRecoveryResultRequest request, WindOperator operator);

    RecoveryOrderDTO getOrder(Long tenantId, String recoveryOrderSn);
}
