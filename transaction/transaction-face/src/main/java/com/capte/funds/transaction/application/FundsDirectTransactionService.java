package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
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
     * @param request  入账交易请求对象
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
     * @param request 入账交易请求对象
     * @return 交易流水号
     */
    String pay(FundsTransactionPayRequest request, WindOperator operator);

    /**
     * 账户退款
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
     * 手续费收取 (单独手续手续费场景)
     *
     * @param request 手续费收取请求
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
