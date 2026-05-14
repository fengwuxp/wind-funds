package com.capte.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.services.PlatformFundingAccountService;
import com.capte.funds.transaction.converter.FundsInstructionFxSupport.ConvertedAmount;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.integration.funds.fx.FxService;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资金直接交易指令转换器。
 */
@Component
public class FundsDirectTransactionInstructionConverter {

    private static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";

    private final PlatformFundingAccountService platformFundingAccountService;

    private final FundsInstructionFxSupport fxSupport;

    @Autowired
    public FundsDirectTransactionInstructionConverter(@NonNull PlatformFundingAccountService platformFundingAccountService,
                                                 @NonNull FundsAccountQueryService fundsAccountQueryService,
                                                 @NonNull FxService fxService) {
        this.platformFundingAccountService = platformFundingAccountService;
        this.fxSupport = new FundsInstructionFxSupport(fundsAccountQueryService, fxService);
    }

    public @NonNull FundsInstructionSpec convertToTopupInstruction(@NonNull FundsTransactionTopupRequest request,
                                                                   @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        requirePlatformAccount(amount.amount().getCurrency(), PlatformFundingAccountRole.RESERVE_FUND);
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        extraContext.put(FundsInstructionContextKeys.CHANNEL_CODE, request.getChannel().name());
        extraContext.put(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID, request.getChannelTransactionSn());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.TRANSFER)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TOPUP)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .externalAccountRef(externalAccountRef(request.getFundsSourceAccountId(), request.getChannel().name(),
                        request.getChannelTransactionSn(), request.getDescription()))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), extraContext))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToTransferInstruction(@NonNull FundsTransactionTransferRequest request,
                                                                      @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getPayerAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.TRANSFER)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.PAYER_ACCOUNT_ID, request.getPayerAccountId(),
                        FundsInstructionContextKeys.PAYEE_ACCOUNT_ID, request.getPayeeAccountId())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToPayInstruction(@NonNull FundsTransactionPayRequest request,
                                                                 @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.TRANSFER)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.PAYEE_ID, request.getPayeeId(),
                        FundsInstructionContextKeys.PAYEE_LEDGER_SUBJECT_CODE, request.getPayeeLedgerCode())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToRefundInstruction(@NonNull FundsTransactionRefundRequest request,
                                                                    @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.PAYER_ID, request.getPayerId());
        extraContext.put(FundsInstructionContextKeys.PAYER_LEDGER_SUBJECT_CODE, request.getPayerLedgerCode());
        extraContext.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        if (request.getChannel() != null) {
            extraContext.put(FundsInstructionContextKeys.CHANNEL_CODE, request.getChannel().name());
        }
        if (request.getChannelTransactionSn() != null) {
            extraContext.put(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID, request.getChannelTransactionSn());
        }
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.TRANSFER)
                .eventType(FundsTransactionEventType.REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(request.getChannelTransactionSn() == null
                        ? null
                        : reference(FundsInstructionReferenceType.EXTERNAL_TRANSACTION, null,
                        request.getChannelTransactionSn()))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), extraContext))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToWithdrawInstruction(@NonNull FundsTransactionWithdrawRequest request,
                                                                      @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        requirePlatformAccount(amount.amount().getCurrency(), PlatformFundingAccountRole.RESERVE_FUND);
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.TRANSFER)
                .eventType(FundsTransactionEventType.WITHDRAW)
                .transactionType(DefaultFundsTransactionType.WITHDRAW)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .externalAccountRef(externalAccountRef(request.getPayeeId(), null, null, request.getDescription()))
                .reference(reference(FundsInstructionReferenceType.FREEZE_ORDER, request.getReferenceFreezeSn(), null))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.REFERENCE_FREEZE_SN, request.getReferenceFreezeSn())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToFeeInstruction(@NonNull FundsTransactionFeeRequest request,
                                                                 @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.TRANSFER)
                .eventType(FundsTransactionEventType.FEE_CHARGE)
                .transactionType(DefaultFundsTransactionType.FEE)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId(),
                        FundsInstructionContextKeys.FEE_TYPE, request.getFeeType())))
                .build();
    }

    private @NonNull FundsAccountId requirePlatformAccount(@NonNull CurrencyIsoCode currency,
                                                           @NonNull PlatformFundingAccountRole role) {
        return platformFundingAccountService.requireAccountId(currency, role);
    }

    private @NonNull ExternalAccountRefSpec externalAccountRef(@NonNull FundsAccountId externalAccountId,
                                                               @Nullable String channelCode,
                                                               @Nullable String externalTransactionId,
                                                               @Nullable String description) {
        Map<String, Object> contextVariables = externalTransactionId == null
                ? Map.of()
                : Map.of(EXTERNAL_TRANSACTION_ID, externalTransactionId);
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(externalAccountId.id())
                .externalAccountType(externalAccountId.type())
                .externalAccountNo(externalAccountId.id())
                .channelCode(channelCode)
                .description(description)
                .contextVariables(contextVariables)
                .build();
    }

    private @NonNull FundsInstructionReferenceSpec reference(@NonNull FundsInstructionReferenceType referenceType,
                                                             @Nullable String referenceSn,
                                                             @Nullable String externalTransactionId) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(referenceType)
                .referenceSn(referenceSn)
                .externalTransactionId(externalTransactionId)
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
