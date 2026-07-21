package com.wind.funds.reconciliation.application.gate;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 对账差错准入消费应用服务。
 *
 * <p>职责：为清算、结算和出款等消费方提供对账差错状态准入判断，返回阻断或条件放行解释。</p>
 *
 * <p>边界：本服务只读取对账差错事实，不创建清算批次、结算单、出款单，也不写交易、账本、余额投影或交易投影。</p>
 */
@NullMarked
public interface ReconciliationGateApplicationService {

    /**
     * 检查对账差错准入。
     *
     * @param request  准入检查请求
     * @param operator 操作人，用于审计检查人
     * @return 准入决策和解释摘要
     */
    ReconciliationGateDecisionDTO checkGate(CheckReconciliationGateRequest request, WindOperator operator);
}
