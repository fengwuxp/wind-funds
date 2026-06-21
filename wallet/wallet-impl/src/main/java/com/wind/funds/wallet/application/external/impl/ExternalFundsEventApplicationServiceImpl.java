package com.wind.funds.wallet.application.external.impl;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.external.ExternalFundsEventApplicationService;
import com.wind.funds.wallet.model.request.ConsumeExternalFundsEventRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 外部资金事件消费应用服务实现。
 *
 * <p>当前实现只提供外部事件进入资金域的服务层契约和前置护栏。真实消费链路必须在后续 Grant 中显式接入交易内核、
 * 账本过账、对账差异和审计闭环。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
public class ExternalFundsEventApplicationServiceImpl implements ExternalFundsEventApplicationService {

    private static final Set<String> POSTABLE_ACCOUNT_SUBJECT_TYPES = Set.of(
            FundsSubjectType.FUNDING_ACCOUNT.name(),
            FundsSubjectType.CREDIT_ACCOUNT.name());

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String consume(@NonNull ConsumeExternalFundsEventRequest request,
                                   @NonNull WindOperator operator) {
        validateConsumeRequest(request);
        throw new UnsupportedOperationException("外部资金事件消费尚未接入归一资金事实内核");
    }

    private void validateConsumeRequest(ConsumeExternalFundsEventRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(ThreadContextTenantIdHolder.requireTenantId(), request.getTenantId(),
                "外部资金事件 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getExternalEventSn(), "外部资金事件流水不能为空");
        AssertUtils.hasText(request.getExternalEventType(), "外部资金事件类型不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "外部资金事件目标账户不能为空");
        assertPostableTargetAccount(request.getTargetAccountId());
        AssertUtils.notNull(request.getAmount(), "外部资金事件金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "外部资金事件金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "外部资金事件币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "外部资金事件业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "外部资金事件业务流水号不能为空");
    }

    private void assertPostableTargetAccount(FundsAccountId targetAccountId) {
        AssertUtils.hasText(targetAccountId.id(), "外部资金事件目标账户 ID 不能为空");
        AssertUtils.hasText(targetAccountId.type(), "外部资金事件目标账户类型不能为空");
        AssertUtils.isTrue(POSTABLE_ACCOUNT_SUBJECT_TYPES.contains(targetAccountId.type()),
                "外部资金事件目标账户必须是资金账户或信用账户");
    }
}
