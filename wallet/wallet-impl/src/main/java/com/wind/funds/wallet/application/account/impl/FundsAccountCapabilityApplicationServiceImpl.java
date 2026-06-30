package com.wind.funds.wallet.application.account.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.application.account.FundsAccountCapabilityApplicationService;
import com.wind.funds.wallet.model.dto.FundsAccountCapabilityDecisionDTO;
import com.wind.funds.wallet.model.request.ResolveFundsAccountCapabilityRequest;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 资金账户能力准入应用服务实现。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Service
@AllArgsConstructor
public class FundsAccountCapabilityApplicationServiceImpl implements FundsAccountCapabilityApplicationService {

    private final FundsAccountQueryService fundsAccountQueryService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull FundsAccountCapabilityDecisionDTO resolveFundsAccountCapability(
            @NonNull ResolveFundsAccountCapabilityRequest request) {
        validateRequest(request);
        FundsAccount account = fundsAccountQueryService.getAccount(request.getAccountId());
        AssertUtils.isTrue(Objects.equals(account.getTenantId(), request.getTenantId()),
                "资金账户租户不匹配，accountId = {}，tenantId = {}",
                request.getAccountId(),
                request.getTenantId());
        AssertUtils.isTrue(account.getCurrency() == request.getCurrency(),
                "资金账户币种与请求币种不一致，accountId = {}，currency = {}",
                request.getAccountId(),
                request.getCurrency());
        return toDecision(account);
    }

    private void validateRequest(ResolveFundsAccountCapabilityRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "资金账户能力准入 tenantId 与当前租户不一致");
        AssertUtils.notNull(request.getAccountId(), "资金账户标识不能为空");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
    }

    private FundsAccountCapabilityDecisionDTO toDecision(FundsAccount account) {
        return new FundsAccountCapabilityDecisionDTO()
                .setTenantId(account.getTenantId())
                .setAccountId(account.getAccountId())
                .setCurrency(account.getCurrency())
                .setStatus(account.getStatus())
                .setCapabilities(account.getCapabilities())
                .setCanReceive(account.canReceive())
                .setCanPay(account.canPay())
                .setCanWithdraw(account.canWithdraw())
                .setCapabilitySource(account.getCapabilitySource());
    }
}
