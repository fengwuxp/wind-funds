package com.wind.funds.wallet.application.instrument.impl;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.FundsAccountCapabilityApplicationService;
import com.wind.funds.wallet.application.funding.FundingResponsibilityResolutionApplicationService;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentCapabilityApplicationService;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentPreTransactionSnapshotApplicationService;
import com.wind.funds.wallet.model.dto.FundingResponsibilityDecisionDTO;
import com.wind.funds.wallet.model.dto.FundsAccountCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentPreTransactionSnapshotDTO;
import com.wind.funds.wallet.model.request.ResolveFundingResponsibilityRequest;
import com.wind.funds.wallet.model.request.ResolveFundsAccountCapabilityRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentCapabilityRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentPreTransactionSnapshotRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 支付工具预交易快照应用服务实现。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Slf4j
@Service
@AllArgsConstructor
public class PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl
        implements PaymentInstrumentPreTransactionSnapshotApplicationService {

    private final PaymentInstrumentCapabilityApplicationService paymentInstrumentCapabilityApplicationService;

    private final FundingResponsibilityResolutionApplicationService fundingResponsibilityResolutionApplicationService;

    private final FundsAccountCapabilityApplicationService fundsAccountCapabilityApplicationService;

    @Override
    @Transactional(readOnly = true)
    public @NonNull PaymentInstrumentPreTransactionSnapshotDTO resolvePreTransactionSnapshot(
            @NonNull ResolvePaymentInstrumentPreTransactionSnapshotRequest request) {
        validateRequest(request);
        PaymentInstrumentCapabilityDecisionDTO instrumentDecision = resolvePaymentInstrument(request);
        FundingResponsibilityDecisionDTO fundingDecision = resolveFundingResponsibility(request, instrumentDecision);
        assertBindingMatchesFundingSubject(instrumentDecision, fundingDecision);
        FundsAccountId boundAccountId = resolveBoundAccountId(instrumentDecision);
        FundsAccountId targetAccountId = resolveTargetAccountId(fundingDecision);
        if (!boundAccountId.equals(targetAccountId)) {
            FundsAccountCapabilityDecisionDTO boundAccountDecision =
                    resolveFundsAccountCapability(request, boundAccountId);
            assertAccountCapabilitySupportsAction(request, boundAccountDecision);
        }
        FundsAccountCapabilityDecisionDTO accountDecision = resolveFundsAccountCapability(request, targetAccountId);
        assertAccountCapabilitySupportsAction(request, accountDecision);
        PaymentInstrumentPreTransactionSnapshotDTO snapshot =
                toSnapshot(request, instrumentDecision, fundingDecision, targetAccountId, accountDecision);
        log.info("支付工具预交易快照解析完成，tenantId={}, businessScene={}, businessSn={}, instrumentSn={}, "
                        + "action={}, targetAccountType={}, targetAccountId={}, amount={}, currency={}, ready={}",
                request.getTenantId(), request.getBusinessScene(), request.getBusinessSn(), request.getInstrumentSn(),
                request.getAction(), targetAccountId.type(), targetAccountId.id(), request.getAmount(),
                request.getCurrency(), snapshot.getReady());
        return snapshot;
    }

    private void validateRequest(ResolvePaymentInstrumentPreTransactionSnapshotRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.equals(TenantContextHolder.requireTenantId(), request.getTenantId(),
                "支付工具预交易快照 tenantId 与当前租户不一致");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAction(), "支付工具动作不能为空");
        AssertUtils.notNull(request.getAmount(), "交易金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "交易金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "币种不能为空");
        AssertUtils.notNull(request.getBindingRole(), "支付工具绑定角色不能为空");
        AssertUtils.notNull(request.getRelationType(), "资金责任关系类型不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "业务场景不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "业务流水号不能为空");
    }

    private PaymentInstrumentCapabilityDecisionDTO resolvePaymentInstrument(
            ResolvePaymentInstrumentPreTransactionSnapshotRequest request) {
        return paymentInstrumentCapabilityApplicationService.resolvePaymentInstrumentCapability(
                new ResolvePaymentInstrumentCapabilityRequest()
                        .setTenantId(request.getTenantId())
                        .setInstrumentSn(request.getInstrumentSn())
                        .setAction(request.getAction())
                        .setCurrency(request.getCurrency())
                        .setBindingRole(request.getBindingRole())
                        .setExpectedBindingVersion(request.getExpectedBindingVersion()));
    }

    private FundingResponsibilityDecisionDTO resolveFundingResponsibility(
            ResolvePaymentInstrumentPreTransactionSnapshotRequest request,
            PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        return fundingResponsibilityResolutionApplicationService.resolveFundingResponsibility(
                new ResolveFundingResponsibilityRequest()
                        .setTenantId(request.getTenantId())
                        .setSpendSubjectId(instrumentDecision.getSubjectId())
                        .setSpendSubjectType(instrumentDecision.getSubjectType())
                        .setCurrency(request.getCurrency())
                        .setRelationType(request.getRelationType()));
    }

    private void assertBindingMatchesFundingSubject(PaymentInstrumentCapabilityDecisionDTO instrumentDecision,
                                                    FundingResponsibilityDecisionDTO fundingDecision) {
        AssertUtils.isTrue(Objects.equals(instrumentDecision.getSubjectId(), fundingDecision.getSpendSubjectId())
                        && instrumentDecision.getSubjectType() == fundingDecision.getSpendSubjectType(),
                "支付工具绑定主体与资金责任主体不一致，instrumentSn = {}, bindingSubject = {}:{}, fundingSubject = {}:{}",
                instrumentDecision.getInstrumentSn(),
                instrumentDecision.getSubjectType(),
                instrumentDecision.getSubjectId(),
                fundingDecision.getSpendSubjectType(),
                fundingDecision.getSpendSubjectId());
    }

    private FundsAccountId resolveTargetAccountId(FundingResponsibilityDecisionDTO fundingDecision) {
        AssertUtils.notNull(fundingDecision.getTargetSubjectType(), "资金责任目标主体类型不能为空");
        AssertUtils.hasText(fundingDecision.getTargetSubjectId(), "资金责任目标主体 ID 不能为空");
        AssertUtils.isTrue(fundingDecision.getTargetSubjectType() == FundsSubjectType.FUNDING_ACCOUNT
                        || fundingDecision.getTargetSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "预交易快照资金责任目标只能是资金账户或信用账户，targetSubjectType = {}",
                fundingDecision.getTargetSubjectType());
        return FundsAccountId.immutable(fundingDecision.getTargetSubjectId(), fundingDecision.getTargetSubjectType());
    }

    private FundsAccountId resolveBoundAccountId(PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        AssertUtils.notNull(instrumentDecision.getSubjectType(), "支付工具绑定主体类型不能为空");
        AssertUtils.hasText(instrumentDecision.getSubjectId(), "支付工具绑定主体 ID 不能为空");
        AssertUtils.isTrue(instrumentDecision.getSubjectType() == FundsSubjectType.FUNDING_ACCOUNT
                        || instrumentDecision.getSubjectType() == FundsSubjectType.CREDIT_ACCOUNT,
                "支付工具绑定主体只能是资金账户或信用账户，subjectType = {}",
                instrumentDecision.getSubjectType());
        return FundsAccountId.immutable(instrumentDecision.getSubjectId(), instrumentDecision.getSubjectType());
    }

    private FundsAccountCapabilityDecisionDTO resolveFundsAccountCapability(
            ResolvePaymentInstrumentPreTransactionSnapshotRequest request,
            FundsAccountId targetAccountId) {
        return fundsAccountCapabilityApplicationService.resolveFundsAccountCapability(
                new ResolveFundsAccountCapabilityRequest()
                        .setTenantId(request.getTenantId())
                        .setAccountId(targetAccountId)
                        .setCurrency(request.getCurrency()));
    }

    private void assertAccountCapabilitySupportsAction(ResolvePaymentInstrumentPreTransactionSnapshotRequest request,
                                                       FundsAccountCapabilityDecisionDTO accountDecision) {
        boolean allowed = switch (request.getAction()) {
            case RECEIVE -> Boolean.TRUE.equals(accountDecision.getCanReceive());
            case PAY, AUTHORIZE -> Boolean.TRUE.equals(accountDecision.getCanPay());
            case WITHDRAW -> Boolean.TRUE.equals(accountDecision.getCanWithdraw());
            // 业务决策型退款按到账能力校验；原路径退款仍由 transaction 层按 route snapshot 回放。
            case REFUND -> Boolean.TRUE.equals(accountDecision.getCanReceive());
        };
        AssertUtils.isTrue(allowed,
                "预交易快照账户能力不支持当前动作，accountId = {}, action = {}",
                accountDecision.getAccountId(),
                request.getAction());
    }

    private PaymentInstrumentPreTransactionSnapshotDTO toSnapshot(
            ResolvePaymentInstrumentPreTransactionSnapshotRequest request,
            PaymentInstrumentCapabilityDecisionDTO instrumentDecision,
            FundingResponsibilityDecisionDTO fundingDecision,
            FundsAccountId targetAccountId,
            FundsAccountCapabilityDecisionDTO accountDecision) {
        return new PaymentInstrumentPreTransactionSnapshotDTO()
                .setTenantId(request.getTenantId())
                .setInstrumentSn(request.getInstrumentSn())
                .setAction(request.getAction())
                .setAmount(request.getAmount())
                .setCurrency(request.getCurrency())
                .setBindingRole(request.getBindingRole())
                .setRelationType(request.getRelationType())
                .setBusinessScene(request.getBusinessScene())
                .setBusinessSn(request.getBusinessSn())
                .setReady(Boolean.TRUE)
                .setTargetAccountId(targetAccountId)
                .setPaymentInstrumentCapability(instrumentDecision)
                .setFundingResponsibility(fundingDecision)
                .setFundsAccountCapability(accountDecision);
    }
}
