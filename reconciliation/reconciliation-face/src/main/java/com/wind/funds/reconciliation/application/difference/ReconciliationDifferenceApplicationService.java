package com.wind.funds.reconciliation.application.difference;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ReconciliationDifferenceDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationDifferenceRequest;
import com.wind.funds.reconciliation.model.request.LinkReconciliationDifferenceAdjustmentRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationDifferenceRerunRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 对账差错应用服务。
 *
 * <p>职责：登记对账差错、记录处理动作回链、登记重新对账结果，为清算、结算、出款和运营解释提供差错闭环入口。</p>
 *
 * <p>边界：本服务不直接修改交易、账本、余额投影或交易投影；需要资金影响时只能记录已由交易层或账本层完成的白名单处理动作引用。</p>
 */
@NullMarked
public interface ReconciliationDifferenceApplicationService {

    /**
     * 创建对账差错。
     *
     * @param request  对账差错创建请求，差错流水号用于幂等
     * @param operator 操作人，用于审计创建人
     * @return 对账差错结果
     */
    ReconciliationDifferenceDTO createDifference(CreateReconciliationDifferenceRequest request, WindOperator operator);

    /**
     * 回链差错处理动作或调账结果。
     *
     * @param request  处理动作回链请求
     * @param operator 操作人，用于审计处理人
     * @return 对账差错结果
     */
    ReconciliationDifferenceDTO linkAdjustmentResult(LinkReconciliationDifferenceAdjustmentRequest request,
                                                     WindOperator operator);

    /**
     * 记录差错处理后的重新对账结果。
     *
     * @param request  重跑结果请求
     * @param operator 操作人，用于审计关闭人或重跑登记人
     * @return 对账差错结果
     */
    ReconciliationDifferenceDTO recordRerunResult(RecordReconciliationDifferenceRerunRequest request,
                                                  WindOperator operator);
}
