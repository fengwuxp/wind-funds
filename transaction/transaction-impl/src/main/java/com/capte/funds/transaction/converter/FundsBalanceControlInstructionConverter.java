package com.capte.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.converter.FundsInstructionFxSupport.ConvertedAmount;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
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
 * 资金余额控制指令转换器。
 */
@Component
public class FundsBalanceControlInstructionConverter {

    private final FundsInstructionFxSupport fxSupport;

    @Autowired
    public FundsBalanceControlInstructionConverter(@NonNull FundsAccountQueryService fundsAccountQueryService,
                                                    @NonNull FxService fxService) {
        this.fxSupport = new FundsInstructionFxSupport(fundsAccountQueryService, fxService);
    }

    public @NonNull FundsInstructionSpec convertToFreezeInstruction(@NonNull FundsBalanceFreezeRequest request,
                                                                    @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.FREEZE)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .businessScene(request.getBusinessScene())
                .businessSn(request.getBusinessSn())
                .eventTime(LocalDateTime.now())
                .description(request.getDescription())
                .operator(operationActor(operator))
                .contextVariables(mergeContext(request.getContextVariables(),
                        Map.of(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId())))
                .build();
    }

    public @NonNull FundsInstructionSpec convertToUnfreezeInstruction(@NonNull FundsBalanceUnfreezeRequest request,
                                                                      @NonNull WindOperator operator) {
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.UNFREEZE)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(request.getReferenceFreezeSn() == null
                        ? null
                        : reference(FundsInstructionReferenceType.FREEZE_ORDER, request.getReferenceFreezeSn()))
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

    public @NonNull FundsInstructionSpec convertToAdjustInstruction(@NonNull FundsBalanceAdjustRequest request,
                                                                    @NonNull WindOperator operator) {
        FundsTransactionEventType eventType = isLimitAdjust(request)
                ? FundsTransactionEventType.LIMIT_ADJUST
                : FundsTransactionEventType.BALANCE_ADJUST;
        ConvertedAmount amount = fxSupport.convert(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
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
                        FundsInstructionContextKeys.INCREASE, request.getIncrease())))
                .build();
    }

    private boolean isLimitAdjust(@NonNull FundsBalanceAdjustRequest request) {
        String type = request.getAccountId().type();
        return FundsSubjectType.CREDIT_ACCOUNT.name().equals(type) || FundsSubjectType.BUDGET_GROUP.name().equals(type);
    }

    private @NonNull FundsInstructionReferenceSpec reference(@NonNull FundsInstructionReferenceType referenceType,
                                                             @Nullable String referenceSn) {
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
