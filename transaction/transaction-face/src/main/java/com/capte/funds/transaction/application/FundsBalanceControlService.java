package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import org.jspecify.annotations.NullMarked;

/**
 * 资金余额控制命令服务。
 *
 * <p>冻结、解冻和余额调整属于余额控制命令，不等同于标准资金交易主事实。
 * 冻结事实载体由冻结单承担，后续扣划、追偿、退款或调账必须作为独立资金事实引用原冻结单。
 *
 * @author wuxp
 * @date 2026-05-15
 **/
@NullMarked
public interface FundsBalanceControlService {

    /**
     * 冻结账户资金
     *
     * @param request  冻结余额请求
     * @param operator 操作人
     * @return 冻结记录流水号
     */
    String freeze(FundsBalanceFreezeRequest request, WindOperator operator);

    /**
     * 解冻账户资金
     *
     * @param request  解冻余额请求
     * @param operator 操作人
     * @return 解冻记录流水号
     */
    String unfreeze(FundsBalanceUnfreezeRequest request, WindOperator operator);

    /**
     * 调整账户资金
     *
     * @param request  调整余额请求
     * @param operator 操作人
     * @return 调整记录流水号
     */
    String adjust(FundsBalanceAdjustRequest request, WindOperator operator);
}
