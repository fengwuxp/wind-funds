package com.wind.funds.wallet.application.instrument.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.wallet.FundsAccount;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.application.funding.FundingResponsibilityResolutionApplicationService;
import com.wind.funds.wallet.application.instrument.AuthorizationAdmissionApplicationService;
import com.wind.funds.wallet.application.instrument.PaymentInstrumentCapabilityApplicationService;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.FundingResponsibilityDecisionDTO;
import com.wind.funds.wallet.model.dto.PaymentInstrumentCapabilityDecisionDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.ResolveFundingResponsibilityRequest;
import com.wind.funds.wallet.model.request.ResolvePaymentInstrumentCapabilityRequest;
import com.wind.transaction.core.Money;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 支付工具授权准入应用服务实现。
 *
 * @author Codex
 * @date 2026-06-18
 */
@Service
@AllArgsConstructor
public class AuthorizationAdmissionApplicationServiceImpl implements AuthorizationAdmissionApplicationService {

    private final PaymentInstrumentCapabilityApplicationService paymentInstrumentCapabilityApplicationService;

    private final FundingResponsibilityResolutionApplicationService fundingResponsibilityResolutionApplicationService;

    private final FundsAccountQueryService fundsAccountQueryService;

    private final FundsAuthorizationTransactionService authorizationTransactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public @NonNull String authorizeByPaymentInstrument(@NonNull AuthorizeByPaymentInstrumentRequest request,
                                                        @NonNull WindOperator operator) {
        validateRequest(request);
        PaymentInstrumentCapabilityDecisionDTO instrumentDecision = resolvePaymentInstrument(request);
        FundingResponsibilityDecisionDTO fundingDecision = resolveFundingResponsibility(request, instrumentDecision);
        assertBindingMatchesFundingSubject(instrumentDecision, fundingDecision);
        FundsAccountId accountId = resolveTargetAccountId(fundingDecision);
        assertAccountCanAuthorize(request, accountId);
        return authorizationTransactionService.authorize(convertToAuthorizeRequest(request, accountId), operator);
    }

    private void validateRequest(AuthorizeByPaymentInstrumentRequest request) {
        AssertUtils.notNull(request.getTenantId(), "租户 ID 不能为空");
        AssertUtils.hasText(request.getInstrumentSn(), "支付工具号不能为空");
        AssertUtils.notNull(request.getAmount(), "授权金额不能为空");
        AssertUtils.isTrue(request.getAmount() > 0L, "授权金额必须大于 0");
        AssertUtils.notNull(request.getCurrency(), "授权币种不能为空");
        AssertUtils.hasText(request.getBusinessSn(), "授权业务流水号不能为空");
        AssertUtils.hasText(request.getBusinessScene(), "授权业务场景不能为空");
        AssertUtils.notNull(request.getApproved(), "授权是否通过不能为空");
        if (Boolean.FALSE.equals(request.getApproved())) {
            AssertUtils.hasText(request.getDeclineReason(), "授权拒绝原因不能为空");
        }
    }

    private PaymentInstrumentCapabilityDecisionDTO resolvePaymentInstrument(AuthorizeByPaymentInstrumentRequest request) {
        return paymentInstrumentCapabilityApplicationService.resolvePaymentInstrumentCapability(
                new ResolvePaymentInstrumentCapabilityRequest()
                        .setTenantId(request.getTenantId())
                        .setInstrumentSn(request.getInstrumentSn())
                        .setAction(PaymentInstrumentAction.AUTHORIZE)
                        .setCurrency(request.getCurrency())
                        .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                        .setExpectedBindingVersion(request.getExpectedBindingVersion()));
    }

    private FundingResponsibilityDecisionDTO resolveFundingResponsibility(
            AuthorizeByPaymentInstrumentRequest request,
            PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        return fundingResponsibilityResolutionApplicationService.resolveFundingResponsibility(
                new ResolveFundingResponsibilityRequest()
                        .setTenantId(request.getTenantId())
                        .setSpendSubjectId(instrumentDecision.getSubjectId())
                        .setSpendSubjectType(instrumentDecision.getSubjectType())
                        .setCurrency(request.getCurrency())
                        .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE));
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
                "授权准入资金责任目标只能是资金账户或信用账户，targetSubjectType = {}",
                fundingDecision.getTargetSubjectType());
        return FundsAccountId.immutable(fundingDecision.getTargetSubjectId(), fundingDecision.getTargetSubjectType());
    }

    private void assertAccountCanAuthorize(AuthorizeByPaymentInstrumentRequest request, FundsAccountId accountId) {
        FundsAccount account = fundsAccountQueryService.getAccount(accountId);
        AssertUtils.isTrue(Objects.equals(account.getTenantId(), request.getTenantId()),
                "授权准入账户租户不匹配，accountId = {}", accountId);
        AssertUtils.isTrue(account.getCurrency() == request.getCurrency(),
                "授权准入账户币种不匹配，accountId = {}, currency = {}", accountId, request.getCurrency());
        AssertUtils.isTrue(account.canPay(),
                "授权准入账户不具备付款能力，accountId = {}", accountId);
    }

    private FundsAuthorizationTransactionAuthorizeRequest convertToAuthorizeRequest(
            AuthorizeByPaymentInstrumentRequest request,
            FundsAccountId accountId) {
        return new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(accountId)
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(request.getAmount(),
                        request.getCurrency())))
                .setBusinessSn(request.getBusinessSn())
                .setBusinessScene(request.getBusinessScene())
                .setApproved(request.getApproved())
                .setAuthorizedTime(request.getAuthorizedTime())
                .setTransactionCountry(request.getTransactionCountry())
                .setDeclineReason(request.getDeclineReason())
                .setDescription(request.getDescription());
    }
}
