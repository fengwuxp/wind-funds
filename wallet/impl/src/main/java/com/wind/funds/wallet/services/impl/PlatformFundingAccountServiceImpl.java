package com.wind.funds.wallet.services.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.model.dto.FundingAccountDTO;
import com.wind.funds.wallet.model.query.FundingAccountQuery;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.PlatformFundingAccountService;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.FundsAccountId;
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

    private final FundingAccountService fundingAccountService;

    @Override
    public @NonNull FundsAccountId requireAccountId(@NonNull CurrencyIsoCode currency,
                                                    @NonNull PlatformFundingAccountRole role) {
        return requireAccountId(TenantContextHolder.requireTenantId(), currency, role);
    }

    @Override
    public @NonNull FundsAccountId requireAccountId(@NonNull Long tenantId,
                                                    @NonNull CurrencyIsoCode currency,
                                                    @NonNull PlatformFundingAccountRole role) {
        List<FundingAccountDTO> results = fundingAccountService.queryFundingAccounts(new FundingAccountQuery()
                        .setTenantId(tenantId)
                        .setCurrency(currency)
                        .setPlatform(Boolean.TRUE)
                        .setAccountRoleCode(role),
                DefaultPageQueryOptions.defaults(10)).getRecords();
        AssertUtils.notEmpty(results,
                "平台资金账户不存在，tenantId = {}, currency = {}, role = {}", tenantId, currency, role);
        AssertUtils.isTrue(results.size() == 1,
                "平台资金账户配置不唯一，tenantId = {}, currency = {}, role = {}, count = {}",
                tenantId, currency, role, results.size());
        FundingAccountDTO result = results.getFirst();
        AssertUtils.isTrue(Boolean.TRUE.equals(result.getPlatform()) && result.getStatus() == FundsAccountStatus.ACTIVE,
                "平台资金账户状态不可用，tenantId = {}, currency = {}, role = {}, accountId = {}, platform = {}, status = {}",
                tenantId, currency, role, result.getSn(), result.getPlatform(), result.getStatus());
        AssertUtils.isTrue(result.getCurrency() == currency,
                "平台资金账户币种不匹配，tenantId = {}, expectedCurrency = {}, actualCurrency = {}, role = {}, accountId = {}",
                tenantId, currency, result.getCurrency(), role, result.getSn());
        return FundsAccountId.immutable(result.getSn(), FundsSubjectType.FUNDING_ACCOUNT);
    }
}
