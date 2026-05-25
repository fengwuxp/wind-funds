package com.capte.funds.wallet.services.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.funds.wallet.dal.entities.FundingAccount;
import com.capte.funds.wallet.dal.entities.table.FundingAccountNameRefs;
import com.capte.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

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
                .and(ref.accountRoleCode.eq(role))
                .and(ref.platform.eq(Boolean.TRUE));
        List<FundingAccount> results = fundingAccountMapper.selectListByQuery(wrapper);
        AssertUtils.notEmpty(results,
                "平台资金账户不存在，tenantId = {}, currency = {}, role = {}", tenantId, currency, role);
        AssertUtils.isTrue(results.size() == 1,
                "平台资金账户配置不唯一，tenantId = {}, currency = {}, role = {}, count = {}",
                tenantId, currency, role, results.size());
        FundingAccount result = results.getFirst();
        AssertUtils.isTrue(Boolean.TRUE.equals(result.getPlatform()) && result.getStatus() == FundsAccountStatus.ACTIVE,
                "平台资金账户状态不可用，tenantId = {}, currency = {}, role = {}, accountId = {}, platform = {}, status = {}",
                tenantId, currency, role, result.getSn(), result.getPlatform(), result.getStatus());
        AssertUtils.isTrue(result.getCurrency() == currency,
                "平台资金账户币种不匹配，tenantId = {}, expectedCurrency = {}, actualCurrency = {}, role = {}, accountId = {}",
                tenantId, currency, result.getCurrency(), role, result.getSn());
        return FundsAccountId.immutable(result.getSn(), result.getAccountType());
    }
}
