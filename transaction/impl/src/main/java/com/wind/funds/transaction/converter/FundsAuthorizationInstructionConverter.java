package com.wind.funds.transaction.converter;

import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperator;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.converter.FundsInstructionAmountSupport.ConvertedAmount;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionCompleteRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.transaction.instruction.ImmutableFundsInstructionSpec;
import com.wind.funds.transaction.spec.FundsInstructionReferenceSpec;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资金授权交易指令转换器。
 */
@Component
public class FundsAuthorizationInstructionConverter {

    private static final String TRUSTED_FORCE_COMPLETION_POLICY_CODE = "B4_FORCE_COMPLETION_OPS";

    private static final long TRUSTED_FORCE_COMPLETION_LIMIT_AMOUNT = 60L;

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

    private final FundsInstructionAmountSupport amountSupport;

    @Autowired
    public FundsAuthorizationInstructionConverter(@NonNull FundsAccountQueryService fundsAccountQueryService) {
        this.amountSupport = new FundsInstructionAmountSupport(fundsAccountQueryService);
    }

    public @NonNull FundsInstructionSpec convertToAuthorizeInstruction(
            @NonNull FundsAuthorizationTransactionAuthorizeRequest request,
            @NonNull WindOperator operator) {
        assertNotSpendControlScope(request.getAccountId(), "授权交易账户不能是支出控制范围");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(FundsInstructionContextKeys.APPROVED, request.getApproved());
        if (request.getDeclineReason() != null) {
            context.put(FundsInstructionContextKeys.DECLINE_REASON, request.getDeclineReason());
        }
        if (request.getMerchantInfo() != null) {
            context.put(FundsInstructionContextKeys.MERCHANT_INFO, request.getMerchantInfo());
        }
        if (request.getTransactionCountry() != null) {
            context.put(FundsInstructionContextKeys.TRANSACTION_COUNTRY, request.getTransactionCountry());
        }
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTHORIZE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .linkedFundingAccountId(request.getLinkedFundingAccountId())
                .ledgerPeriodType(request.getLedgerPeriodType())
                .ledgerPeriodId(request.getLedgerPeriodId())
                .instrumentRef(request.getPaymentInstrumentRef())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getAuthorizedTime()))
                .description(request.getDescription())
                .operator(operator)
                .contextVariables(mergeContext(request.getContextVariables(), context))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToReversalInstruction(
            @NonNull FundsAuthorizationTransactionReversalRequest request,
            @NonNull WindOperator operator,
            @NonNull String referenceLedgerTransactionSn) {
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.REVERSAL)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(authorizationReference(request.getAuthorizationTransactionSn(),
                        referenceLedgerTransactionSn))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getReversalTime()))
                .description(request.getDescription())
                .operator(operator)
                .accountId(request.getAccountId())
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                        request.getAuthorizationTransactionSn())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToCompleteInstruction(
            @NonNull FundsAuthorizationTransactionCompleteRequest request,
            @NonNull WindOperator operator,
            @Nullable String referenceLedgerTransactionSn) {
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        Map<String, Object> context = new LinkedHashMap<>();
        FundsInstructionReferenceSpec reference = null;
        if (request.isForceCompletion()) {
            validateForceCompletionRequest(request, amount);
            context.put(FundsInstructionContextKeys.COMPLETION_MODE,
                    FundsAuthorizationTransactionCompleteRequest.COMPLETION_MODE_FORCE);
            context.put(FundsInstructionContextKeys.FORCE_COMPLETION_POLICY_CODE,
                    request.getForceCompletionPolicyCode());
            context.put(FundsInstructionContextKeys.FORCE_COMPLETION_LIMIT_AMOUNT,
                    request.getForceCompletionLimitAmount());
            context.put(FundsInstructionContextKeys.FORCE_COMPLETION_REASON, request.getForceCompletionReason());
            context.put(FundsInstructionContextKeys.EXTERNAL_ORIGINAL_FACT_REF,
                    request.getExternalOriginalFactRef());
            context.put(FundsInstructionContextKeys.FORCE_COMPLETION_VOUCHER_REF,
                    request.getForceCompletionVoucherRef());
        } else {
            AssertUtils.hasText(request.getAuthorizationTransactionSn(),
                    "authorizationTransactionSn must not be blank");
            AssertUtils.hasText(referenceLedgerTransactionSn, "授权完成原账本交易流水不能为空");
            reference = authorizationReference(request.getAuthorizationTransactionSn(),
                    referenceLedgerTransactionSn);
            context.put(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                    request.getAuthorizationTransactionSn());
        }
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.COMPLETE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .reference(reference)
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getCompletedTime()))
                .description(request.getDescription())
                .operator(operator)
                .contextVariables(mergeContext(request.getContextVariables(), context))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToRefundInstruction(
            @NonNull FundsAuthorizationTransactionRefundRequest request,
            @NonNull WindOperator operator,
            @Nullable String referenceLedgerTransactionSn) {
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        Map<String, Object> context = new LinkedHashMap<>();
        FundsInstructionReferenceSpec reference;
        if (request.isNoAuthRefund()) {
            validateNoAuthRefundRequest(request);
            reference = noAuthRefundReference(request);
            context.put(FundsInstructionContextKeys.REFUND_MODE,
                    FundsInstructionContextKeys.REFUND_MODE_NO_AUTH);
            context.put(FundsInstructionContextKeys.EXTERNAL_REFERENCE_SN,
                    request.getExternalReferenceSn());
            context.put(FundsInstructionContextKeys.REFUND_REASON, request.getRefundReason());
        } else {
            AssertUtils.hasText(request.getAuthorizationTransactionSn(),
                    "authorizationTransactionSn must not be blank");
            AssertUtils.hasText(referenceLedgerTransactionSn, "授权退款原账本交易流水不能为空");
            reference = authorizationReference(request.getAuthorizationTransactionSn(),
                    referenceLedgerTransactionSn);
            context.put(FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                    request.getAuthorizationTransactionSn());
            putDisputeRefundContext(request, context);
        }
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TenantContextHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTH_REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .reference(reference)
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getRefundTime()))
                .description(request.getDescription())
                .operator(operator)
                .contextVariables(mergeContext(request.getContextVariables(), context))
                .build();
    }

    private @NonNull LocalDateTime eventTime(@Nullable LocalDateTime eventTime) {
        return eventTime == null ? LocalDateTime.now() : eventTime;
    }

    private void validateForceCompletionRequest(@NonNull FundsAuthorizationTransactionCompleteRequest request,
                                                @NonNull ConvertedAmount amount) {
        AssertUtils.isFalse(StringUtils.hasText(request.getAuthorizationTransactionSn()),
                "force completion must not carry authorizationTransactionSn");
        AssertUtils.hasText(request.getForceCompletionPolicyCode(), "forceCompletionPolicyCode must not be blank");
        AssertUtils.isTrue(TRUSTED_FORCE_COMPLETION_POLICY_CODE.equals(request.getForceCompletionPolicyCode()),
                "forceCompletionPolicyCode must be trusted");
        AssertUtils.notNull(request.getForceCompletionLimitAmount(), "forceCompletionLimitAmount must not be null");
        AssertUtils.isTrue(request.getForceCompletionLimitAmount() > 0,
                "forceCompletionLimitAmount must be greater than 0");
        AssertUtils.isTrue(request.getForceCompletionLimitAmount() == TRUSTED_FORCE_COMPLETION_LIMIT_AMOUNT,
                "forceCompletionLimitAmount must match trusted policy limit");
        AssertUtils.isTrue(amount.amount().getAmount() <= request.getForceCompletionLimitAmount(),
                "forceCompletionLimitAmount must be greater than or equal to transaction amount");
        AssertUtils.hasText(request.getForceCompletionReason(), "forceCompletionReason must not be blank");
        AssertUtils.hasText(request.getExternalOriginalFactRef(), "externalOriginalFactRef must not be blank");
        AssertUtils.hasText(request.getForceCompletionVoucherRef(), "forceCompletionVoucherRef must not be blank");
    }

    private void validateNoAuthRefundRequest(@NonNull FundsAuthorizationTransactionRefundRequest request) {
        AssertUtils.isFalse(StringUtils.hasText(request.getAuthorizationTransactionSn()),
                "no-auth refund must not carry authorizationTransactionSn");
        AssertUtils.isFalse(request.isDisputeRefund(),
                "no-auth refund must not carry dispute fields");
        AssertUtils.hasText(request.getExternalReferenceSn(), "externalReferenceSn must not be blank");
        AssertUtils.hasText(request.getRefundReason(), "refundReason must not be blank");
    }

    private void putDisputeRefundContext(@NonNull FundsAuthorizationTransactionRefundRequest request,
                                         @NonNull Map<String, Object> context) {
        if (!request.isDisputeRefund()) {
            return;
        }
        AssertUtils.hasText(request.getDisputeMode(), "disputeMode must not be blank");
        AssertUtils.hasText(request.getDisputeReason(), "disputeReason must not be blank");
        AssertUtils.hasText(request.getDisputeVoucherRef(), "disputeVoucherRef must not be blank");
        AssertUtils.hasText(request.getExternalDisputeRef(), "externalDisputeRef must not be blank");
        context.put(FundsInstructionContextKeys.REFUND_MODE,
                FundsInstructionContextKeys.REFUND_MODE_DISPUTE);
        context.put(FundsInstructionContextKeys.DISPUTE_MODE, request.getDisputeMode());
        context.put(FundsInstructionContextKeys.DISPUTE_REASON, request.getDisputeReason());
        context.put(FundsInstructionContextKeys.DISPUTE_VOUCHER_REF, request.getDisputeVoucherRef());
        context.put(FundsInstructionContextKeys.EXTERNAL_DISPUTE_REF, request.getExternalDisputeRef());
    }

    private @NonNull FundsInstructionReferenceSpec authorizationReference(
            @NonNull String authorizationTransactionSn,
            @NonNull String referenceLedgerTransactionSn) {
        return reference(FundsInstructionReferenceType.AUTHORIZATION, authorizationTransactionSn,
                referenceLedgerTransactionSn);
    }

    private @NonNull FundsInstructionReferenceSpec noAuthRefundReference(
            @NonNull FundsAuthorizationTransactionRefundRequest request) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.EXTERNAL_TRANSACTION)
                .externalTransactionId(request.getExternalReferenceSn())
                .contextVariables(Map.of())
                .build();
    }

    private @NonNull FundsInstructionReferenceSpec reference(@NonNull FundsInstructionReferenceType referenceType,
                                                             @NonNull String referenceSn,
                                                             @Nullable String referenceLedgerTransactionSn) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(referenceType)
                .referenceSn(referenceSn)
                .referenceLedgerTransactionSn(referenceLedgerTransactionSn)
                .contextVariables(Map.of())
                .build();
    }

    private @NonNull Map<String, Object> mergeContext(@Nullable ReadonlyContextVariables contextVariables,
                                                      @NonNull Map<String, Object> extraContext) {
        FundsInstructionContextValidator.assertNoSensitiveContextVariables(contextVariables);
        Map<String, Object> result = new LinkedHashMap<>();
        if (contextVariables != null && contextVariables.getContextVariables() != null) {
            result.putAll(contextVariables.getContextVariables());
        }
        result.putAll(extraContext);
        return Map.copyOf(result);
    }

    private void assertNotSpendControlScope(@NonNull FundsAccountId accountId, @NonNull String message) {
        AssertUtils.isFalse(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE.equals(accountId.type()), message);
    }

}
