package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionExpireRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 资金授权交易命令服务。
 * <pre>
 * [1] Authorization（授权）
 *      ↓
 *      ├── Declined（失败）
 *      └── Approved（成功）
 *               ↓
 *       ┌───────┴────────┐
 *       ↓                ↓
 * Reversal（撤销）     Settlement（结算）
 * （释放冻结）            ↓
 *                      Capture（扣款）
 *                           ↓
 *               ┌───────────┴───────────┐
 *               ↓                       ↓
 *          Refund（退款）        Chargeback（结算后拒付/争议）
 *          （商户发起）          （持卡人/发卡机构发起）
 * </pre>
 *
 * @author wuxp
 * @date 2026-04-21 09:04
 **/
@NullMarked
public interface FundsAuthorizationTransactionService {

    /**
     * 交易授权
     *
     * @param request  交易授权请求
     * @param operator 操作者
     * @return 授权交易流水号
     */
    String authorize(FundsAuthorizationTransactionAuthorizeRequest request, WindOperator operator);

    /**
     * 账户交易授权撤销
     *
     * @param request  授权交易撤销请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String reversal(FundsAuthorizationTransactionReversalRequest request, WindOperator operator);

    /**
     * 账户交易授权过期释放。
     *
     * <p>用于系统按授权有效期释放剩余授权占用；底层账务路径可复用释放路径，
     * 但事件、终态、原因和审计必须保留过期语义。</p>
     *
     * @param request  授权交易过期释放请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String expire(FundsAuthorizationTransactionExpireRequest request, WindOperator operator);

    /**
     * 交易完成（结算）
     *
     * @param request  交易授权完成请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String settle(FundsAuthorizationTransactionSettleRequest request, WindOperator operator);

    /**
     * 交易完成撤销（退款）
     *
     * @param request  交易完成撤销请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String settleRefund(FundsAuthorizationTransactionRefundRequest request, WindOperator operator);

    /**
     * 授权结算后拒付（争议）。
     * <p>
     * 用于已结算授权交易发生持卡人或发卡机构争议时，按原结算路径回放资金退回；
     * 不用于授权阶段批准失败，授权失败使用 authorize 的 approved=false 表达。
     *
     * @param request  授权结算后拒付/争议请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String chargeback(FundsAuthorizationTransactionChargebackRequest request, WindOperator operator);
}
