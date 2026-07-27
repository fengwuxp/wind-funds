package com.wind.funds.reconciliation.service;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ClearingSettlementGateResultDTO;
import com.wind.funds.reconciliation.model.request.CheckClearingSettlementGateRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 清算 / 结算对账准入消费服务。
 *
 * <p>职责：面向清算编排、结算编排、运营后台或自动任务，只读查看目标清算或结算对象的对账 gate 时点状态。</p>
 *
 * <p>边界：本服务只返回时点解释，不能作为最终放行凭证。最终资金命令必须在自己的事务内调用
 * {@code ReconciliationGateApplicationService.checkGate} 完成权威复核。</p>
 */
@NullMarked
public interface ClearingSettlementGateConsumerService {

    /**
     * 查看清算或结算对象的对账准入时点状态。
     *
     * @param request  清算 / 结算对象级准入检查请求，必须包含租户、对象类型、对象流水和运行结果流水
     * @param operator 检查人；访问与操作审计由 Web/宿主层统一记录
     * @return 清算 / 结算准入时点检查结果；永不返回 null，不能作为最终放行凭证
     */
    ClearingSettlementGateResultDTO inspectGate(CheckClearingSettlementGateRequest request, WindOperator operator);
}
