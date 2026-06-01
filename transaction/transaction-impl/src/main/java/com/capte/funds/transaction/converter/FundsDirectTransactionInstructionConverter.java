package com.capte.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.capte.funds.transaction.converter.FundsInstructionAmountSupport.ConvertedAmount;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
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

    private final FundsInstructionAmountSupport amountSupport;

    @Autowired
    public FundsDirectTransactionInstructionConverter(@NonNull PlatformFundingAccountService platformFundingAccountService,
                                                     @NonNull FundsAccountQueryService fundsAccountQueryService) {
        this.platformFundingAccountService = platformFundingAccountService;
        this.amountSupport = new FundsInstructionAmountSupport(fundsAccountQueryService);
    }

    public @NonNull FundsInstructionSpec convertToTopupInstruction(@NonNull FundsTransactionTopupRequest request,
                                                                   @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "直接充值入账账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "直接充值入账账户不能是外部账户");
        AssertUtils.notNull(request.getChannel(), "直接充值资金通道不能为空");
        AssertUtils.notNull(request.getChannelTransactionSn(), "直接充值通道交易流水不能为空");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(), request.getAccountId());
        requirePlatformAccount(amount.amount().getCurrency(), PlatformFundingAccountRole.CASH_MAPPING);
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        extraContext.put(FundsInstructionContextKeys.CHANNEL_CODE, request.getChannel().name());
        extraContext.put(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID, request.getChannelTransactionSn());
        putFeeSpec(extraContext, request.getFeeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
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
        AssertUtils.notNull(request.getPayerAccountId(), "系统内转账付款账户不能为空");
        AssertUtils.notNull(request.getPayeeAccountId(), "系统内转账收款账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getPayerAccountId()),
                "系统内转账付款账户不能是外部账户");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getPayeeAccountId()),
                "系统内转账收款账户不能是外部账户");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(), request.getPayerAccountId());
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.PAYER_ACCOUNT_ID, request.getPayerAccountId());
        extraContext.put(FundsInstructionContextKeys.PAYEE_ACCOUNT_ID, request.getPayeeAccountId());
        putFeeSpec(extraContext, request.getFeeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
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
                .contextVariables(mergeContext(request.getContextVariables(), extraContext))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToPayInstruction(@NonNull FundsTransactionPayRequest request,
                                                                 @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "直接付款账户不能为空");
        AssertUtils.notNull(request.getPayeeId(), "直接付款收款主体不能为空");
        AssertUtils.notNull(request.getPayeeLedgerCode(), "直接付款收款账目不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "直接付款账户不能是外部账户");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getPayeeId()),
                "直接付款收款主体不能是外部账户");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(), request.getAccountId());
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        extraContext.put(FundsInstructionContextKeys.PAYEE_ID, request.getPayeeId());
        extraContext.put(FundsInstructionContextKeys.PAYEE_LEDGER_SUBJECT_CODE, request.getPayeeLedgerCode());
        putFeeSpec(extraContext, request.getFeeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
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
                .contextVariables(mergeContext(request.getContextVariables(), extraContext))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToRefundInstruction(@NonNull FundsTransactionRefundRequest request,
                                                                    @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "直接退款到账账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "直接退款到账账户不能是外部账户");
        AssertUtils.notNull(request.getPayerId(), "直接退款出资主体不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getPayerId()),
                "直接退款出资主体不能是外部账户");
        AssertUtils.notNull(request.getPayerLedgerCode(), "直接退款出资账目不能为空");
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.PAYER_ID, request.getPayerId());
        extraContext.put(FundsInstructionContextKeys.PAYER_LEDGER_SUBJECT_CODE, request.getPayerLedgerCode());
        extraContext.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        putFeeSpec(extraContext, request.getFeeSpec());
        if (request.getChannel() != null) {
            extraContext.put(FundsInstructionContextKeys.CHANNEL_CODE, request.getChannel().name());
        }
        if (request.getChannelTransactionSn() != null) {
            extraContext.put(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID, request.getChannelTransactionSn());
        }
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
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
        AssertUtils.notNull(request.getAccountId(), "提现账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "提现账户不能是外部账户");
        AssertUtils.hasText(request.getReferenceFreezeSn(), "提现冻结流水号不能为空");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(), request.getAccountId());
        requirePlatformAccount(amount.amount().getCurrency(), PlatformFundingAccountRole.CASH_MAPPING);
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        extraContext.put(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, request.getReferenceFreezeSn());
        putFeeSpec(extraContext, request.getFeeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
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
                .contextVariables(mergeContext(request.getContextVariables(), extraContext))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToFeeInstruction(@NonNull FundsTransactionFeeRequest request,
                                                                 @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "手续费支出账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "手续费支出账户不能是外部账户");
        AssertUtils.notNull(request.getFeeType(), "手续费类型不能为空");
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
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

    public @NonNull FundsInstructionSpec convertToFeeRefundInstruction(@NonNull FundsTransactionFeeRefundRequest request,
                                                                       @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getFeeSourceTransactionSn(), "手续费退回原费用交易流水不能为空");
        AssertUtils.notNull(request.getAccountId(), "手续费退回到账账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "手续费退回到账账户不能是外部账户");
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.FEE_REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(reference(FundsInstructionReferenceType.FEE, request.getFeeSourceTransactionSn(), null))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId())))
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

    private void putFeeSpec(@NonNull Map<String, Object> context, @Nullable FeeSpec feeSpec) {
        if (feeSpec != null) {
            context.put(FundsInstructionContextKeys.FEE_SPEC, feeSpec);
        }
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
