package com.capte.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsInstructionFxSupport.ConvertedAmount;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.fx.FxService;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资金授权交易指令转换器。
 */
@Component
public class FundsAuthorizationInstructionConverter {

    private final FundsInstructionFxSupport fxSupport;

    @Autowired
    public FundsAuthorizationInstructionConverter(@NonNull FundsAccountQueryService fundsAccountQueryService,
                                                   @NonNull FxService fxService) {
        this.fxSupport = new FundsInstructionFxSupport(fundsAccountQueryService, fxService);
    }

    public @NonNull FundsInstructionSpec convertToAuthorizeInstruction(
            @NonNull FundsAuthorizationTransactionAuthorizeRequest request,
            @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
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
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTHORIZE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getAuthorizedTime()))
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), context))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToReversalInstruction(
            @NonNull FundsAuthorizationTransactionReversalRequest request,
            @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.REVERSAL)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(reference(FundsInstructionReferenceType.AUTHORIZATION,
                        request.getAuthorizationTransactionSn()))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getReversalTime()))
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                        request.getAuthorizationTransactionSn())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToSettleInstruction(
            @NonNull FundsAuthorizationTransactionSettleRequest request,
            @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.SETTLE)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(reference(FundsInstructionReferenceType.AUTHORIZATION,
                        request.getAuthorizationTransactionSn()))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getSettleTime()))
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                        request.getAuthorizationTransactionSn())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToSettleRefundInstruction(
            @NonNull FundsAuthorizationTransactionRefundRequest request,
            @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.AUTH_REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(reference(FundsInstructionReferenceType.AUTHORIZATION,
                        request.getAuthorizationTransactionSn()))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getRefundTime()))
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                        request.getAuthorizationTransactionSn())))
                .build();
    }

    /**
     * 转换授权结算后的拒付/争议指令，不表示授权阶段批准失败。
     */
    public @NonNull FundsInstructionSpec convertToChargebackInstruction(
            @NonNull FundsAuthorizationTransactionChargebackRequest request,
            @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.AUTHORIZATION_TRANSACTION)
                .eventType(FundsTransactionEventType.CHARGEBACK)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(reference(FundsInstructionReferenceType.AUTHORIZATION,
                        request.getAuthorizationTransactionSn()))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(eventTime(request.getChargebackTime()))
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.AUTHORIZATION_TRANSACTION_SN,
                        request.getAuthorizationTransactionSn())))
                .build();
    }

    private @NonNull LocalDateTime eventTime(@Nullable LocalDateTime eventTime) {
        return eventTime == null ? LocalDateTime.now() : eventTime;
    }

    private @NonNull FundsInstructionReferenceSpec reference(@NonNull FundsInstructionReferenceType referenceType,
                                                             @NonNull String referenceSn) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(referenceType)
                .referenceSn(referenceSn)
                .contextVariables(Map.of())
                .build();
    }

    private @NonNull Map<String, Object> mergeContext(@Nullable WritableContextVariables contextVariables,
                                                      @NonNull Map<String, Object> extraContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (contextVariables != null && contextVariables.getContextVariables() != null) {
            result.putAll(contextVariables.getContextVariables());
        }
        result.putAll(extraContext);
        return Map.copyOf(result);
    }

    private @NonNull FundsOperationActorSpec operationActor(@NonNull WindOperator operator) {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(operator.getOperatorId())
                .operatorType(operator.getOperatorType().name())
                .operatorName(operator.getOperatorName())
                .appName(operator.getAppName())
                .contextVariables(operator.getContextVariables())
                .build();
    }
}
