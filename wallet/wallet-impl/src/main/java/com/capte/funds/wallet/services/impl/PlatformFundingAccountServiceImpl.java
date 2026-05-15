package com.capte.funds.wallet.services.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * 平台资金账户查询服务实现。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Service
@AllArgsConstructor
public class PlatformFundingAccountServiceImpl implements PlatformFundingAccountService {

    private final FundingAccountMapper fundingAccountMapper;

    @Override
    public @NonNull FundsAccountId requireAccountId(@NonNull CurrencyIsoCode currency,
                                                    @NonNull PlatformFundingAccountRole role) {
        return requireAccountId(ThreadContextTenantIdHolder.requireTenantId(), currency, role);
    }

    @Override
    public @NonNull FundsAccountId requireAccountId(@NonNull Long tenantId,
                                                    @NonNull CurrencyIsoCode currency,
                                                    @NonNull PlatformFundingAccountRole role) {
        FundingAccountNameRefs ref = FundingAccountNameRefs.fundingAccount;
        QueryWrapper wrapper = QueryWrapper.create()
                .from(ref)
                .where(ref.tenantId.eq(tenantId))
                .and(ref.currency.eq(currency))
                .and(ref.accountRoleCode.eq(role));
        FundingAccount result = fundingAccountMapper.selectOneByQuery(wrapper);
        AssertUtils.notNull(result, "平台资金账户不存在，tenantId = {}, currency = {}, role = {}", tenantId, currency, role);
        return FundsAccountId.immutable(result.getSn(), result.getAccountType());
    }
}
