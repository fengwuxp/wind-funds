package com.wind.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 资金直接交易命令服务。
 *
 * @author wuxp
 * @date 2026-04-21 08:54
 **/
@NullMarked
public interface FundsDirectTransactionService {

    /**
     * 充值（外部资金进入平台账户）
     *
     * @param request  充值请求对象
     * @param operator 操作人
     * @return 交易流水号
     */
    String topup(FundsTransactionTopupRequest request, WindOperator operator);

    /**
     * 内部账户转账
     *
     * @param request  转账请求对象
     * @param operator 操作人
     * @return 转账交易流水号
     */
    String transfer(FundsTransactionTransferRequest request, WindOperator operator);

    /**
     * 账户付款
     *
     * @param request  付款请求对象
     * @param operator 操作人
     * @return 交易流水号
     */
    String pay(FundsTransactionPayRequest request, WindOperator operator);

    /**
     * 账户退款。
     *
     * <p>{@link FundsTransactionRefundRequest#getReferenceTransactionSn()} 有值时按原交易 route snapshot 回放；
     * 此时原快照是唯一资金路径来源，请求不得再传到账账户、出资账户或出资账目。该字段为空时表示业务方已完成退款决策，
     * 资金底座按请求显式给定的到账账户、出资账户和出资账目执行直接退款，不补默认账目。
     * 本接口不再额外引入退款模式字段；资金底座只校验内部资金主体、账目、余额、状态、幂等和敏感上下文。</p>
     *
     * <p>请求携带 {@link FundsTransactionRefundRequest#getFeeChargeSpec()} 时，表示业务方确认本次退款同时新增收费；
     * 手续费只能从退款路径中唯一真实资金受益 FundingAccount 的 AVAILABLE 扣取，不从 CreditAccount 额度扣取。</p>
     *
     * @param request  退款请求对象
     * @param operator 操作人
     * @return 退款交易流水号
     */
    String refund(FundsTransactionRefundRequest request, WindOperator operator);

    /**
     * 提现，资金从平台流向外部账户，内部提现请使用 {@link #transfer(FundsTransactionTransferRequest, WindOperator)}
     *
     * @param request  提现请求对象
     * @param operator 操作人
     * @return 提现交易流水号
     */
    String withdraw(FundsTransactionWithdrawRequest request, WindOperator operator);

    /**
     * 手续费收取 (单独手续费场景)
     *
     * @param request  手续费收取请求
     * @param operator 操作人
     * @return 交易流水号
     */
    String fee(FundsTransactionFeeRequest request, WindOperator operator);

    /**
     * 手续费退回。
     *
     * @param request  手续费退回请求
     * @param operator 操作人
     * @return 交易流水号
     */
    String refundFee(FundsTransactionFeeRefundRequest request, WindOperator operator);


}
