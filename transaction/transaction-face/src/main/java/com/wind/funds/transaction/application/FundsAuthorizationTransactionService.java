package com.wind.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
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
 * Reversal（撤销）     Completion（完成）
 * （释放授权）            ↓
 *                      Consume（消费成立）
 *                           ↓
 *               ↓
 *          Refund（退款 / 争议裁决资金结果）
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
     * 授权交易完成
     *
     * @param request  交易授权完成请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String complete(FundsAuthorizationTransactionCompleteRequest request, WindOperator operator);

    /**
     * 已完成交易退款
     *
     * @param request  授权链退款请求
     * @param operator 操作者
     * @return 交易流水号
     */
    String refund(FundsAuthorizationTransactionRefundRequest request, WindOperator operator);

}
