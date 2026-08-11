package com.wind.funds.transaction.application.external.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.ledger.LedgerPostingRejectedException;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.application.ExternalFundsEventApplicationService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.application.support.ExternalFundsRailResolver;
import com.wind.funds.transaction.application.support.ExternalFundsRailResolver.ExternalFundsRailDecision;
import com.wind.funds.transaction.model.request.ConsumeExternalFundsEventRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 外部资金事件消费应用服务实现。
 *
 * <p>当前实现只接入已确认外部入金事件到标准充值内核。扣款、退票、撤销、差异单和事件消费者仍必须由后续
 * Grant 单独证明。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
@Service
@AllArgsConstructor
public class ExternalFundsEventApplicationServiceImpl implements ExternalFundsEventApplicationService {

    private static final String EXTERNAL_SOURCE_ACCOUNT_ID = "external_funds_event_source";

    private final FundsDirectTransactionService directTransactionService;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = LedgerPostingRejectedException.class)
    public @NonNull String consume(@NonNull ConsumeExternalFundsEventRequest request,
                                   @NonNull WindOperator operator) {
        validateConsumeRequest(request);
        ExternalFundsRailDecision railDecision =
                ExternalFundsRailResolver.requireConfirmedCreditRailDecision(request.getExternalEventType());
        return directTransactionService.topup(toTopupRequest(request, railDecision), operator);
    }

    private void validateConsumeRequest(ConsumeExternalFundsEventRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "外部资金事件 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getExternalEventSn(), "外部资金事件流水不能为空");
        AssertUtils.hasText(request.getExternalSourceCode(), "外部资金事实来源编码不能为空");
        AssertUtils.hasText(request.getExternalFundsFactSn(), "外部资金事实流水不能为空");
        AssertUtils.hasText(request.getExternalEventType(), "外部资金事件类型不能为空");
        AssertUtils.notNull(request.getTargetAccountId(), "外部资金事件目标账户不能为空");
        assertFundingAccountTarget(request.getTargetAccountId());
        AssertUtils.notNull(request.getAmount(), "外部资金事件金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "外部资金事件金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "外部资金事件币种不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "外部资金事件业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "外部资金事件业务流水号不能为空");
    }

    private void assertFundingAccountTarget(FundsAccountId targetAccountId) {
        AssertUtils.hasText(targetAccountId.id(), "外部资金事件目标账户 ID 不能为空");
        AssertUtils.hasText(targetAccountId.type(), "外部资金事件目标账户类型不能为空");
        AssertUtils.isTrue(FundsSubjectType.FUNDING_ACCOUNT.name().equals(targetAccountId.type()),
                "外部资金入金事件目标账户必须是资金账户");
    }

    private FundsTransactionTopupRequest toTopupRequest(ConsumeExternalFundsEventRequest request,
                                                        ExternalFundsRailDecision railDecision) {
        return new FundsTransactionTopupRequest()
                .setAccountId(request.getTargetAccountId())
                .setFundsSourceAccountId(FundsAccountId.immutable(EXTERNAL_SOURCE_ACCOUNT_ID,
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(railDecision.transactionChannel())
                .setExternalRailCode(railDecision.externalRailCode())
                .setChannelTransactionSn(request.getExternalEventSn())
                .setExternalSourceCode(request.getExternalSourceCode())
                .setExternalFundsFactSn(request.getExternalFundsFactSn())
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(request.getAmount(),
                        request.getCurrency())))
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setContextVariables(ReadonlyContextVariables.of(externalEventContext(request, railDecision)))
                .setDescription(request.getDescription());
    }

    private Map<String, Object> externalEventContext(ConsumeExternalFundsEventRequest request,
                                                     ExternalFundsRailDecision railDecision) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("externalEventSn", request.getExternalEventSn());
        result.put("externalEventType", request.getExternalEventType());
        result.put("externalRailCode", railDecision.externalRailCode());
        result.put("transactionChannel", railDecision.transactionChannel().name());
        putIfPresent(result, "originalTransactionSn", request.getOriginalTransactionSn());
        putIfPresent(result, "reconciliationDifferenceSn", request.getReconciliationDifferenceSn());
        return Map.copyOf(result);
    }

    private void putIfPresent(Map<String, Object> values, String key, String value) {
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }
}
