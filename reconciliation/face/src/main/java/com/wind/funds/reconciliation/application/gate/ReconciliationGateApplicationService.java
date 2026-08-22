package com.wind.funds.reconciliation.application.gate;

import com.wind.integration.operator.WindOperator;
import com.wind.funds.reconciliation.model.dto.ReconciliationGateDecisionDTO;
import com.wind.funds.reconciliation.model.request.CheckReconciliationGateRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationGateRequirementRequest;
import com.wind.funds.reconciliation.model.value.GateRequirementRef;
import org.jspecify.annotations.NullMarked;

/**
 * 对账差错准入消费应用服务。
 *
 * <p>职责：为清算、结算和出款等消费方提供对账差错状态准入判断，返回通过或阻断解释。</p>

 * <p>当前边界：已处理差错经当前批次重跑对平后按普通通过返回，并单独报告历史差错数量；
 * 带审批、阈值和到期重查的条件放行尚未实现。</p>
 *
 * <p>资金安全边界：检查结果是当前事务内的时点判断，不是可缓存、可转交的授权凭证。最终资金命令必须在自己的
 * 数据库事务中调用本服务，并让对象血缘头和当前批次锁持续到资金命令完成；无法保持同一事务时必须在最终写入前重新检查。</p>
 *
 * <p>边界：本服务只读取对账差错事实，不创建清算批次、结算单、出款单，也不写交易、账本、余额投影或交易投影。</p>
 */
@NullMarked
public interface ReconciliationGateApplicationService {

    /**
     * 发布一份不可变的必需对账对版本。
     *
     * @param request 要求事实和期望的当前版本
     * @param operator 发布者
     * @return 稳定的要求引用
     */
    GateRequirementRef recordGateRequirement(RecordReconciliationGateRequirementRequest request,
                                             WindOperator operator);

    /**
     * 检查对账差错准入。
     *
     * <p>调用方必须已经开启最终资金命令事务；事务外调用失败，不会为本次检查单独创建短事务。</p>
     *
     * @param request  准入检查请求
     * @param operator 检查人；检查结果会回显该标识，访问与操作审计由 Web/宿主层统一记录
     * @return 准入决策和解释摘要
     */
    ReconciliationGateDecisionDTO checkGate(CheckReconciliationGateRequest request, WindOperator operator);

    /**
     * 查询对账 Gate 的时点解释快照。
     *
     * <p>本方法不锁定对象血缘头和当前批次，仅用于报表、运营查询和问题解释；返回结果不得缓存、转交或作为
     * 最终资金命令的授权凭证。最终清算、结算或出款仍必须在自己的数据库事务中调用 {@link #checkGate}。</p>
     *
     * @param request  Gate 查询请求
     * @param operator 查询人；查询审计由 Web/宿主层统一记录
     * @return 当前读取时点的 Gate 决策和解释摘要
     */
    ReconciliationGateDecisionDTO inspectGate(CheckReconciliationGateRequest request, WindOperator operator);
}
