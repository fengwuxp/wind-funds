package com.wind.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.model.request.FundsBenefitFundingRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitFundingSettleRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/**
 * 权益让利资金交易应用服务。
 *
 * <p>职责：面向交易准入和路由准入，承接业务侧已经决策完成的优惠、代金券、支付立减、平台补贴或商户让利结果，
 * 生成资金底座可理解、可记账、可退款、可对账的权益让利资金交易。</p>
 *
 * <p>边界：本服务和 {@code FundsDirectTransactionService} 处于同一抽象层级；
 * 不计算优惠金额，不判断券是否可用，不维护券包生命周期，不创建独立营销交易状态机。
 * 具体实现必须委派标准交易路由、交易事实和账本分录链路，不得在 application 层直接写 route、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@NullMarked
public interface FundsBenefitFundingApplicationService {

    /**
     * 结算权益让利资金交易。
     *
     * <p>调用方应传入已决策的让利事实：让利方、受益方、让利金额、工具或规则引用、原订单或原交易引用和业务场景。
     * 服务返回资金交易流水号，后续退款、清结算、对账和重放均以该交易流水号作为稳定引用。
     * 这里的结算指权益让利资金影响的确认入账，不替代清结算模块的批次结算。</p>
     *
     * @param request  权益让利资金结算请求
     * @param operator 操作者
     * @return 资金交易流水号
     */
    @NonNull String settle(
            @NonNull FundsBenefitFundingSettleRequest request,
            @NonNull WindOperator operator);

    /**
     * 退回已入账的权益让利资金影响。
     *
     * <p>退款、业务取消、人工纠错或反向冲销都应引用原权益让利资金交易流水号，
     * 并以本次业务流水号保证幂等、回放和审计追踪。</p>
     *
     * @param request  权益让利退款请求
     * @param operator 操作者
     * @return 资金交易流水号
     */
    @NonNull String refund(
            @NonNull FundsBenefitFundingRefundRequest request,
            @NonNull WindOperator operator);

}
