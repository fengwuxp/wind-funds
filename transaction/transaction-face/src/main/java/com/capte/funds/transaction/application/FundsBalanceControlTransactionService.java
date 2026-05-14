package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 资金余额控制交易命令服务。
 *
 * @author wuxp
 * @date 2026-04-30 10:25
 **/
@NullMarked
public interface FundsBalanceControlTransactionService {

    /**
     * 冻结账户资金
     *
     * @param request 冻结余额请求
     * @return 冻结记录流水号
     */
    String freeze(FundsBalanceFreezeRequest request, WindOperator operator);

    /**
     * 解冻账户资金
     *
     * @param request 解冻余额请求
     * @return 解冻记录流水号
     */
    String unfreeze(FundsBalanceUnfreezeRequest request, WindOperator operator);


    /**
     * 调整账户资金
     *
     * @param request 调整余额请求
     * @return 调整记录流水号
     */
    String adjust(FundsBalanceAdjustRequest request, WindOperator operator);

}
