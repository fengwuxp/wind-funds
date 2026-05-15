package com.capte.funds.wallet.service;

import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;

/**
 * 平台资金账户查询服务。
 *
 * <p>职责：按租户、币种和平台账户角色解析平台 FundingAccount。</p>
 *
 * <p>边界：只做平台账户解析，不创建平台账户，不在交易路径中自动补账本。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
public interface PlatformFundingAccountService {

    /**
     * 按当前线程租户查询平台资金账户，未初始化时抛出异常。
     *
     * <p>能力范围：从 ThreadContextTenantIdHolder 获取租户，并解析对应平台账户。</p>
     *
     * @param currency 币种
     * @param role 平台账户角色
     * @return 平台账户标识
     */
    @NonNull
    FundsAccountId requireAccountId(@NonNull CurrencyIsoCode currency, @NonNull PlatformFundingAccountRole role);

    /**
     * 按指定租户查询平台资金账户，未初始化时抛出异常。
     *
     * <p>能力范围：解析指定租户的平台账户；平台账户未配置或不唯一时应失败。</p>
     *
     * @param tenantId 租户 ID
     * @param currency 币种
     * @param role 平台账户角色
     * @return 平台账户标识
     */
    @NonNull FundsAccountId requireAccountId(@NonNull Long tenantId,
                                             @NonNull CurrencyIsoCode currency,
                                             @NonNull PlatformFundingAccountRole role);
}
