package com.wind.funds.wallet;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import org.jspecify.annotations.NonNull;

/**
 * 资金账户查询服务
 *
 * @author wuxp
 * @date 2026-04-16 15:49
 **/
public interface FundsAccountQueryService {

    /**
     * 获取一个资金账户
     *
     * @param tenantId  租户 ID
     * @param accountId 账户标识
     * @return 资金账户
     */
    @NonNull
    FundsAccount getAccount(@NonNull Long tenantId, @NonNull FundsAccountId accountId);

    /**
     * 获取资金账户实际使用的账本 Profile。
     *
     * @param tenantId  租户 ID
     * @param accountId 账户标识
     * @return 账本 Profile
     */
    @NonNull
    LedgerProfileCode getLedgerProfileCode(@NonNull Long tenantId, @NonNull FundsAccountId accountId);

    /**
     * 获取账户余额
     *
     * @param tenantId  租户 ID
     * @param accountId 账户标识
     * @return 账户余额
     */
    @NonNull
    FundsAccountBalanceView getBalance(@NonNull Long tenantId, @NonNull FundsAccountId accountId);

    /**
     * 是否支持该资金账户
     *
     * @param tenantId  租户 ID
     * @param accountId 账户标识
     * @return 是否支持
     */
    boolean supports(@NonNull Long tenantId, @NonNull FundsAccountId accountId);
}
