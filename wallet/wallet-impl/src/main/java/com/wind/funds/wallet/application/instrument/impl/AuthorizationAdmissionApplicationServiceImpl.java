package com.wind.funds.wallet.application.instrument.impl;

import com.capte.domain.core.operator.WindOperator;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.model.route.ImmutablePaymentInstrumentRefSpec;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
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

import java.util.LinkedHashMap;
import java.util.Map;
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
        return authorizationTransactionService.authorize(convertToAuthorizeRequest(request,
                accountId,
                instrumentDecision), operator);
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
            FundsAccountId accountId,
            PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
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
                .setPaymentInstrumentRef(paymentInstrumentRef(request, instrumentDecision))
                .setDescription(request.getDescription());
    }

    private PaymentInstrumentRefSpec paymentInstrumentRef(AuthorizeByPaymentInstrumentRequest request,
                                                          PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        assertPaymentInstrumentSnapshotReady(instrumentDecision);
        return ImmutablePaymentInstrumentRefSpec.builder()
                .tenantId(instrumentDecision.getTenantId())
                .instrumentId(instrumentDecision.getInstrumentSn())
                .instrumentType(instrumentDecision.getInstrumentType())
                .instrumentNo(instrumentDecision.getInstrumentNo())
                .ownerId(instrumentDecision.getOwnerId())
                .ownerType(instrumentDecision.getOwnerType().name())
                .currency(instrumentDecision.getCurrency().name())
                .status(instrumentDecision.getStatus().name())
                .bindingSnapshot(bindingSnapshot(request, instrumentDecision))
                .description(instrumentDecision.getDescription())
                .build();
    }

    private void assertPaymentInstrumentSnapshotReady(PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        AssertUtils.hasText(instrumentDecision.getInstrumentSn(), "支付工具快照工具号不能为空");
        AssertUtils.hasText(instrumentDecision.getInstrumentNo(), "支付工具快照展示号不能为空");
        AssertUtils.hasText(instrumentDecision.getOwnerId(), "支付工具快照归属主体 ID 不能为空");
        AssertUtils.notNull(instrumentDecision.getOwnerType(), "支付工具快照归属主体类型不能为空");
        AssertUtils.hasText(instrumentDecision.getInstrumentType(), "支付工具快照类型不能为空");
        AssertUtils.notNull(instrumentDecision.getCurrency(), "支付工具快照币种不能为空");
        AssertUtils.notNull(instrumentDecision.getStatus(), "支付工具快照状态不能为空");
        AssertUtils.hasText(instrumentDecision.getBindingSn(), "支付工具绑定快照绑定号不能为空");
        AssertUtils.notNull(instrumentDecision.getBindingVersion(), "支付工具绑定快照版本不能为空");
        AssertUtils.notNull(instrumentDecision.getBindingRole(), "支付工具绑定快照角色不能为空");
        AssertUtils.notNull(instrumentDecision.getSubjectType(), "支付工具绑定快照主体类型不能为空");
        AssertUtils.hasText(instrumentDecision.getSubjectId(), "支付工具绑定快照主体 ID 不能为空");
    }

    private Map<String, Object> bindingSnapshot(AuthorizeByPaymentInstrumentRequest request,
                                                PaymentInstrumentCapabilityDecisionDTO instrumentDecision) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingSn", instrumentDecision.getBindingSn());
        values.put("bindingVersion", instrumentDecision.getBindingVersion());
        values.put("bindingRole", instrumentDecision.getBindingRole().name());
        values.put("subjectType", instrumentDecision.getSubjectType().name());
        values.put("subjectId", instrumentDecision.getSubjectId());
        values.put("admissionAction", PaymentInstrumentAction.AUTHORIZE.name());
        values.put("admissionDecision", Boolean.TRUE.equals(request.getApproved()) ? "APPROVED" : "DECLINED");
        return values;
    }
}
