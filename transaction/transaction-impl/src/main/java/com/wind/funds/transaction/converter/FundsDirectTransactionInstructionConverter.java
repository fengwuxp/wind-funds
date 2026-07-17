package com.wind.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.wallet.service.PlatformFundingAccountService;
import com.wind.funds.transaction.converter.FundsInstructionAmountSupport.ConvertedAmount;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.wind.funds.transaction.model.request.FundsTransactionPayRequest;
import com.wind.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.wind.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.wind.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.route.ImmutableExternalAccountRefSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.wallet.FundsAccountQueryService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 资金直接交易指令转换器。
 */
@Component
public class FundsDirectTransactionInstructionConverter {

    private static final String EXTERNAL_TRANSACTION_ID = "externalTransactionId";

    private static final String SPEND_CONTROL_SCOPE_ACCOUNT_TYPE = SpendRuleScopeType.SPEND_CONTROL_SCOPE.name();

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
        assertNotSpendControlScope(request.getAccountId(), "直接充值入账账户不能是支出控制范围");
        AssertUtils.notNull(request.getChannel(), "直接充值资金通道不能为空");
        AssertUtils.notNull(request.getChannelTransactionSn(), "直接充值通道交易流水不能为空");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        requirePlatformAccount(amount.amount().getCurrency(), PlatformFundingAccountRole.CASH_MAPPING);
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.CHANNEL_CODE, request.getChannel().name());
        extraContext.put(FundsInstructionContextKeys.EXTERNAL_TRANSACTION_ID, request.getChannelTransactionSn());
        putFeeChargeSpec(extraContext, request.getFeeChargeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TOPUP)
                .transactionType(DefaultFundsTransactionType.TOPUP)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .instrumentRef(request.getPaymentInstrumentRef())
                .externalAccountRef(externalAccountRef(request.getFundsSourceAccountId(), request.getChannel().name(),
                        request.getChannelTransactionSn(), request.getChannelId(), request.getDescription()))
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
        assertNotSpendControlScope(request.getPayerAccountId(), "系统内转账付款账户不能是支出控制范围");
        assertNotSpendControlScope(request.getPayeeAccountId(), "系统内转账收款账户不能是支出控制范围");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getPayerAccountId());
        Map<String, Object> extraContext = new LinkedHashMap<>();
        putFeeChargeSpec(extraContext, request.getFeeChargeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .payerAccountId(request.getPayerAccountId())
                .payeeAccountId(request.getPayeeAccountId())
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
        AssertUtils.notNull(request.getPayeeLedgerSubjectCode(), "直接付款收款账目不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "直接付款账户不能是外部账户");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getPayeeId()),
                "直接付款收款主体不能是外部账户");
        assertNotSpendControlScope(request.getAccountId(), "直接付款账户不能是支出控制范围");
        assertNotSpendControlScope(request.getPayeeId(), "直接付款收款主体不能是支出控制范围");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        Map<String, Object> extraContext = new LinkedHashMap<>();
        putFeeChargeSpec(extraContext, request.getFeeChargeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .payeeId(request.getPayeeId())
                .payeeLedgerSubjectCode(request.getPayeeLedgerSubjectCode())
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
        boolean referencedRefund = StringUtils.hasText(request.getReferenceTransactionSn());
        ConvertedAmount amount = referencedRefund
                ? referencedRefundAmount(request)
                : businessConfirmedRefundAmount(request);
        Map<String, Object> extraContext = new LinkedHashMap<>();
        putFeeChargeSpec(extraContext, request.getFeeChargeSpec());
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
                .accountId(request.getAccountId())
                .payerId(request.getPayerId())
                .payerLedgerSubjectCode(request.getPayerLedgerSubjectCode())
                .reference(refundReference(request))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), extraContext))
                .build();
    }

    private @NonNull ConvertedAmount referencedRefundAmount(@NonNull FundsTransactionRefundRequest request) {
        AssertUtils.isNull(request.getAccountId(), "关联退款不得重复传入退款到账账户");
        AssertUtils.isNull(request.getPayerId(), "关联退款不得重复传入退款出资账户");
        AssertUtils.isNull(request.getPayerLedgerSubjectCode(), "关联退款不得重复传入退款出资账目");
        return amountSupport.fromTransactionAmount(request.getTransactionAmount());
    }

    private @NonNull ConvertedAmount businessConfirmedRefundAmount(@NonNull FundsTransactionRefundRequest request) {
        AssertUtils.notNull(request.getAccountId(), "业务确认型直接退款到账账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "业务确认型直接退款到账账户不能是外部账户");
        AssertUtils.notNull(request.getPayerId(), "业务确认型直接退款出资主体不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getPayerId()),
                "业务确认型直接退款出资主体不能是外部账户");
        AssertUtils.notNull(request.getPayerLedgerSubjectCode(), "业务确认型直接退款出资账目不能为空");
        assertNotSpendControlScope(request.getAccountId(), "业务确认型直接退款到账账户不能是支出控制范围");
        assertNotSpendControlScope(request.getPayerId(), "业务确认型直接退款出资主体不能是支出控制范围");
        return amountSupport.fromTransactionAmount(request.getTransactionAmount(), request.getAccountId());
    }

    private @Nullable FundsInstructionReferenceSpec refundReference(@NonNull FundsTransactionRefundRequest request) {
        if (StringUtils.hasText(request.getReferenceTransactionSn())) {
            return reference(FundsInstructionReferenceType.ORIGINAL_TRANSACTION, request.getReferenceTransactionSn(),
                    null);
        }
        if (request.getChannelTransactionSn() == null) {
            return null;
        }
        return reference(FundsInstructionReferenceType.EXTERNAL_TRANSACTION, null, request.getChannelTransactionSn());
    }

    public @NonNull FundsInstructionSpec convertToWithdrawInstruction(@NonNull FundsTransactionWithdrawRequest request,
                                                                      @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getAccountId(), "提现账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "提现账户不能是外部账户");
        AssertUtils.hasText(request.getReferenceFreezeSn(), "提现冻结流水号不能为空");
        assertNotSpendControlScope(request.getAccountId(), "提现账户不能是支出控制范围");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        requirePlatformAccount(amount.amount().getCurrency(), PlatformFundingAccountRole.CASH_MAPPING);
        Map<String, Object> extraContext = new LinkedHashMap<>();
        extraContext.put(FundsInstructionContextKeys.REFERENCE_FREEZE_SN, request.getReferenceFreezeSn());
        putFeeChargeSpec(extraContext, request.getFeeChargeSpec());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.WITHDRAW)
                .transactionType(DefaultFundsTransactionType.WITHDRAW)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .instrumentRef(request.getPaymentInstrumentRef())
                .externalAccountRef(externalAccountRef(request.getPayeeId(), null, null, null,
                        request.getDescription()))
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
        assertNotSpendControlScope(request.getAccountId(), "手续费支出账户不能是支出控制范围");
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.FEE_CHARGE)
                .transactionType(DefaultFundsTransactionType.FEE)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of(
                        FundsInstructionContextKeys.FEE_TYPE, request.getFeeType())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToFeeRefundInstruction(@NonNull FundsTransactionFeeRefundRequest request,
                                                                       @NonNull WindOperator operator) {
        AssertUtils.notNull(request.getFeeSourceTransactionSn(), "手续费退回原费用交易流水不能为空");
        AssertUtils.notNull(request.getAccountId(), "手续费退回到账账户不能为空");
        AssertUtils.isFalse(DefaultFundsAccountType.isExternalAccount(request.getAccountId()),
                "手续费退回到账账户不能是外部账户");
        assertNotSpendControlScope(request.getAccountId(), "手续费退回到账账户不能是支出控制范围");
        ConvertedAmount amount = amountSupport.fromTransactionAmount(request.getTransactionAmount(),
                request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.FEE_REFUND)
                .transactionType(DefaultFundsTransactionType.REFUND)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .accountId(request.getAccountId())
                .reference(reference(FundsInstructionReferenceType.FEE, request.getFeeSourceTransactionSn(), null))
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(), Map.of()))
                .build();
    }

    private @NonNull FundsAccountId requirePlatformAccount(@NonNull CurrencyIsoCode currency,
                                                           @NonNull PlatformFundingAccountRole role) {
        return platformFundingAccountService.requireAccountId(currency, role);
    }

    private @NonNull ExternalAccountRefSpec externalAccountRef(@NonNull FundsAccountId externalAccountId,
                                                               @Nullable String channelCode,
                                                               @Nullable String externalTransactionId,
                                                               @Nullable String providerCode,
                                                               @Nullable String description) {
        Map<String, Object> contextVariables = externalTransactionId == null
                ? Map.of()
                : Map.of(EXTERNAL_TRANSACTION_ID, externalTransactionId);
        return ImmutableExternalAccountRefSpec.builder()
                .externalAccountId(externalAccountId.id())
                .externalAccountType(externalAccountId.type())
                .externalAccountNo(externalAccountId.id())
                .providerCode(providerCode)
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
        FundsInstructionContextValidator.assertNoReservedContextVariables(contextVariables);
        Map<String, Object> result = new LinkedHashMap<>();
        if (contextVariables != null && contextVariables.getContextVariables() != null) {
            result.putAll(contextVariables.getContextVariables());
        }
        result.putAll(extraContext);
        return Map.copyOf(result);
    }

    private void putFeeChargeSpec(@NonNull Map<String, Object> context, @Nullable FeeSpec feeChargeSpec) {
        if (feeChargeSpec != null) {
            context.put(FundsInstructionContextKeys.FEE_CHARGE_SPEC, feeChargeSpec);
        }
    }

    private void assertNotSpendControlScope(@NonNull FundsAccountId accountId, @NonNull String message) {
        AssertUtils.isFalse(SPEND_CONTROL_SCOPE_ACCOUNT_TYPE.equals(accountId.type()), message);
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
