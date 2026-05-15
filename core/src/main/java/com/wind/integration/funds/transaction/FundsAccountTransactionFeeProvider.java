package com.wind.integration.funds.transaction;

import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 资金账户交易手续费提供者
 *
 * @author wuxp
 * @date 2026-04-21 15:10
 **/
@NullMarked
public interface FundsAccountTransactionFeeProvider {

    /**
     * 提供资金账户交易手续费
     *
     * @param accountId     账户ID
     * @param businessScene 业务场景
     * @return 资金账户交易手续费
     */
    @Nullable
    FeeSpec apply(FundsAccountId accountId, String businessScene);

    /**
     * 是否支持该账户
     *
     * @param accountId 账户ID
     * @return true:支持
     */
    boolean supports(FundsAccountId accountId);
}

