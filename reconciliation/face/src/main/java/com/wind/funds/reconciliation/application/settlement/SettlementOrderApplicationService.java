package com.wind.funds.reconciliation.application.settlement;

import com.wind.funds.reconciliation.model.dto.SettlementOrderDTO;
import com.wind.funds.reconciliation.model.request.ApproveSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CancelSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.CreateSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.LockSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.ReturnSettlementOrderToDraftRequest;
import com.wind.funds.reconciliation.model.request.ReleaseSettlementOrderRequest;
import com.wind.funds.reconciliation.model.request.SubmitSettlementOrderRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 结算单公共应用服务。
 *
 * <p>当前只支持已确认清算本金的中间户锁定；外部出款不属于本服务。</p>
 *
 * @author wuxp
 * @since 2026-07-30
 */
@NullMarked
public interface SettlementOrderApplicationService {

    /**
     * 创建结算单草稿。
     *
     * @param request  创建请求
     * @param operator 操作者
     * @return 结算单事实
     */
    SettlementOrderDTO createOrder(CreateSettlementOrderRequest request, WindOperator operator);

    /**
     * 提交结算单复核。
     *
     * @param request  提交请求
     * @param operator 操作者
     * @return 更新后的结算单事实
     */
    SettlementOrderDTO submitOrder(SubmitSettlementOrderRequest request, WindOperator operator);

    /**
     * 将待处理结算单退回草稿。
     *
     * @param request  退回请求
     * @param operator 操作者
     * @return 更新后的结算单事实
     */
    SettlementOrderDTO returnToDraft(ReturnSettlementOrderToDraftRequest request, WindOperator operator);

    /**
     * 审批结算单。
     *
     * @param request  审批请求
     * @param operator 操作者
     * @return 更新后的结算单事实
     */
    SettlementOrderDTO approveOrder(ApproveSettlementOrderRequest request, WindOperator operator);

    /**
     * 取消尚未进入不可撤销阶段的结算单。
     *
     * @param request  取消请求
     * @param operator 操作者
     * @return 更新后的结算单事实
     */
    SettlementOrderDTO cancelOrder(CancelSettlementOrderRequest request, WindOperator operator);

    /**
     * 锁定已审批结算单对应的资金。
     *
     * @param request  锁定请求
     * @param operator 操作者
     * @return 更新后的结算单事实
     */
    SettlementOrderDTO lockOrder(LockSettlementOrderRequest request, WindOperator operator);

    /**
     * 在事务内重查 Gate 和释放授权后，将结算锁定资金安全释放到冻结余额。
     *
     * @param request  释放请求及来源闭合证据
     * @param operator 操作者
     * @return 更新后的结算单事实
     */
    SettlementOrderDTO releaseOrder(ReleaseSettlementOrderRequest request, WindOperator operator);

    /**
     * 查询租户内指定结算单。
     *
     * @param tenantId         租户 ID
     * @param settlementOrderSn 结算单流水号
     * @return 结算单事实
     */
    SettlementOrderDTO getOrder(Long tenantId, String settlementOrderSn);
}
