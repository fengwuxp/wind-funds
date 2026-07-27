package com.wind.funds.reconciliation.application.difference;

import com.wind.integration.operator.WindOperator;
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
 * <p>边界：本服务不判断上层业务准入，也不直接修改交易、账本、余额投影或交易投影；需要资金影响时，
 * 只能登记上层业务通过既有资金能力完成的受控动作及其结果事实引用。</p>
 */
@NullMarked
public interface ReconciliationDifferenceApplicationService {

    /**
     * 创建对账差错。
     *
     * @param request  对账差错创建请求，已持久化的逐笔匹配结果流水号用于业务幂等
     * @param operator 记录人；权限与操作审计由 Web/宿主层负责
     * @return 对账差错结果
     */
    ReconciliationDifferenceDTO createDifference(CreateReconciliationDifferenceRequest request, WindOperator operator);

    /**
     * 回链差错处理动作或调账结果。
     *
     * <p>新增动作必须在以当前差错批次为父的后继重跑批次创建前回链；已有后继批次时调用失败，
     * 防止使用动作发生前生成的结果关闭差错。当前动作的相同请求重放仍幂等返回。</p>
     *
     * @param request  处理动作回链请求
     * @param operator 记录人；权限与操作审计由 Web/宿主层负责
     * @return 对账差错结果
     */
    ReconciliationDifferenceDTO linkAdjustmentResult(LinkReconciliationDifferenceAdjustmentRequest request,
                                                     WindOperator operator);

    /**
     * 将差错处理结果绑定到已固化的重新对账运行结果。
     *
     * @param request  差错与运行结果绑定请求；对平结论和证据由服务读取持久化事实派生
     * @param operator 重跑结果记录人；权限与操作审计由 Web/宿主层负责
     * @return 对账差错结果
     */
    ReconciliationDifferenceDTO recordRerunResult(RecordReconciliationDifferenceRerunRequest request,
                                                  WindOperator operator);
}
