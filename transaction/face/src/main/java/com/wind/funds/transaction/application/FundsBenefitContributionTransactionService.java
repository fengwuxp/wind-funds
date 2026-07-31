package com.wind.funds.transaction.application;

import com.wind.integration.operator.WindOperator;
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
 * <p>营销或让利账户只是一种可入账资金账户或账户 profile，
 * 用于表达平台、商户、合作方等账务主体承担了让利成本。
 * 它不是营销活动、券实例、权益规则、支付工具或业务参与方身份本身。</p>
 *
 * <p>边界：本服务和 {@code FundsDirectTransactionService} 处于同一抽象层级；
 * 只回答谁承担成本、成本落到哪个可记账承接主体、金额是多少。
 * 不计算优惠金额，不判断券是否可用，不维护券包生命周期，不保存券、活动或规则来源归因，
 * 不创建独立营销交易状态机，不处理返利、佣金、分润、储值负债释放或用户余额入账。
 * 单个平台营销账户只适合平台自有资金补贴，不能把商户或合作方出资合并进去；
 * 多个出资方共同让利时必须按出资方分别入账。
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
     * 成本承担主体可以是平台营销资金账户、商户让利责任账户、合作方补贴账户等已解析的可记账主体；
     * 在进入本服务前，调用方必须把营销活动、券、规则或业务主体解析为资金底座可识别的账户引用。
     * 让利承接账务主体是本次让利出资交易的记账目标方，不等同于营销系统中的用户实体；
     * 调用方还必须显式声明目标账目，平台补足商户使用 {@code CLEARING}，用户或订单归集使用
     * {@code SETTLEMENT}，服务不提供默认值。
     * 多个出资方共同让利时，按出资方拆分多笔结算事实，每笔返回独立资金交易流水号，
     * 不使用批量 API 或全局营销账户聚合出资方责任。
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
     * <p>退款、业务取消、人工纠错或反向冲销都应引用原让利出资记账交易流水号，
     * 并以本次业务流水号保证幂等、回放和审计追踪。
     * 反向处理沿原交易事实冲回，不重新读取当前营销账户绑定、活动规则或优惠决策；
     * 多方出资退款时，上游按原出资交易分别发起冲回。</p>
     *
     * @param request  让利出资退款请求
     * @param operator 操作者
     * @return 资金交易流水号
     */
    @NonNull String refund(
            @NonNull FundsBenefitContributionRefundRequest request,
            @NonNull WindOperator operator);

}
