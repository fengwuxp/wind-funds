package com.wind.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.model.request.FundsBenefitContributionRefundRequest;
import com.wind.funds.transaction.model.request.FundsBenefitContributionSettleRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

/**
 * 让利出资记账交易服务。
 *
 * <p>职责：承接业务侧已经决策完成的优惠券、代金券、支付立减、
 * 平台、商户或合作方给用户让利的出资责任结果，
 * 按出资方逐笔生成标准资金交易和账目影响。</p>
 *
 * <p>边界：本服务和 {@code FundsDirectTransactionService} 处于同一抽象层级；
 * 只回答谁承担成本、成本落到哪个可记账承接主体、金额是多少。
 * 不计算优惠金额，不判断券是否可用，不维护券包生命周期，不保存券、活动或规则来源归因，
 * 不创建独立营销交易状态机，不处理返利、佣金、分润、储值负债释放或用户余额入账。
 * 具体实现必须委派标准交易路由、交易事实和账本分录链路，不得在 application 层直接写 route、posting、LedgerEntry 或余额投影。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@NullMarked
public interface FundsBenefitContributionTransactionService {

    /**
     * 记录一笔让利出资入账交易。
     *
     * <p>调用方应传入已决策的让利结算事实：成本承担主体、让利承接账务主体、让利金额、
     * 原订单或原交易引用和业务场景。
     * 让利承接账务主体是本次让利出资交易的记账目标方，例如用户或订单维度让利归集账目、
     * 商户清结算账户或等价被补足账户，不等同于营销系统中的用户实体。
     * 多个出资方共同让利时，按出资方拆分多笔结算事实，每笔返回独立资金交易流水号，
     * 便于财务核算、多方对账和后续按原出资事实退款。
     * 服务返回资金交易流水号，后续退款、清结算、对账和重放均以该交易流水号作为稳定引用。
     * 这里的结算指权益让利资金影响的确认入账，不替代清结算模块的批次结算。</p>
     *
     * @param request  让利出资记账交易结算请求
     * @param operator 操作者
     * @return 资金交易流水号
     */
    @NonNull String settle(
            @NonNull FundsBenefitContributionSettleRequest request,
            @NonNull WindOperator operator);

    /**
     * 冲回已入账的让利出资交易。
     *
     * <p>退款、业务取消、人工纠错或反向冲销都应引用原权益让利资金交易流水号，
     * 并以本次业务流水号保证幂等、回放和审计追踪。</p>
     *
     * @param request  让利出资退款请求
     * @param operator 操作者
     * @return 资金交易流水号
     */
    @NonNull String refund(
            @NonNull FundsBenefitContributionRefundRequest request,
            @NonNull WindOperator operator);

}
