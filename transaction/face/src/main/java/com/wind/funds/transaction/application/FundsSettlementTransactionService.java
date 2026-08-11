package com.wind.funds.transaction.application;

import com.wind.funds.transaction.model.request.FundsSettlementLockRequest;
import com.wind.funds.transaction.model.dto.FundsSettlementReleaseResultDTO;
import com.wind.funds.transaction.model.request.FundsSettlementReleaseRequest;
import com.wind.integration.operator.WindOperator;
import org.jspecify.annotations.NullMarked;

/**
 * 结算单编排使用的内部资金原语。
 *
 * <p>锁定将同一资金账户的 {@code AVAILABLE} 转为 {@code SETTLEMENT}；安全释放将原锁定金额转为
 * {@code FROZEN}。宿主必须经结算应用服务完成 Gate 和释放授权检查，不得直接调用本接口。</p>
 *
 * @author wuxp
 * @since 2026-07-30
 */
@NullMarked
public interface FundsSettlementTransactionService {

    /**
     * 锁定结算资金。
     *
     * @param request  结算资金锁定请求
     * @param operator 操作者
     * @return 锁定资金交易流水号；相同业务身份重试返回已存在事实
     */
    String lock(FundsSettlementLockRequest request, WindOperator operator);

    /**
     * 将原结算锁定金额全额释放到同一账户的冻结余额。
     *
     * @param request  结算资金释放请求
     * @param operator 操作者
     * @return 释放资金交易与承接冻结单引用
     */
    FundsSettlementReleaseResultDTO release(FundsSettlementReleaseRequest request, WindOperator operator);
}
