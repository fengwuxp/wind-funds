package com.capte.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.converter.FundsInstructionAmountSupport.ConvertedAmount;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.funds.spec.transaction.FundsInstructionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.FundsAccountQueryService;
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

    private static final String ADJUST_REASON_REQUIRED_MESSAGE = "余额调账缺少调账原因";

    private static final String ADJUST_EVIDENCE_REF_REQUIRED_MESSAGE = "余额调账缺少调账凭证";

    private static final String ADJUST_APPROVAL_REF_REQUIRED_MESSAGE = "余额调账缺少审批引用";

    private static final String UNFREEZE_REFERENCE_REQUIRED_MESSAGE = "余额解冻缺少原冻结单引用";

    private final FundsInstructionAmountSupport amountSupport;

    @Autowired
    public FundsBalanceControlInstructionConverter(@NonNull FundsAccountQueryService fundsAccountQueryService) {
        this.amountSupport = new FundsInstructionAmountSupport(fundsAccountQueryService);
    }

    public @NonNull FundsInstructionSpec convertToFreezeInstruction(@NonNull FundsBalanceFreezeRequest request,
                                                                    @NonNull WindOperator operator) {
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
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
        requireUnfreezeReference(request);
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(ThreadContextTenantIdHolder.requireTenantId())
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.UNFREEZE)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .amount(amount.amount())
                .originalAmount(amount.originalAmount())
                .exchangeRate(amount.exchangeRate())
                .reference(reference(FundsInstructionReferenceType.FREEZE_ORDER, request.getReferenceFreezeSn()))
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

    private void requireUnfreezeReference(@NonNull FundsBalanceUnfreezeRequest request) {
        AssertUtils.hasText(request.getReferenceFreezeSn(), UNFREEZE_REFERENCE_REQUIRED_MESSAGE);
    }

    public @NonNull FundsInstructionSpec convertToAdjustInstruction(@NonNull FundsBalanceAdjustRequest request,
                                                                    @NonNull WindOperator operator) {
        requireAdjustAuditContext(request);
        FundsTransactionEventType eventType = isLimitAdjust(request)
                ? FundsTransactionEventType.LIMIT_ADJUST
                : FundsTransactionEventType.BALANCE_ADJUST;
        ConvertedAmount amount = amountSupport.sameCurrency(request.getAmount(), request.getAccountId());
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
                .contextVariables(mergeContext(request.getContextVariables(), adjustContext(request)))
                .build();
    }

    private void requireAdjustAuditContext(@NonNull FundsBalanceAdjustRequest request) {
        AssertUtils.hasText(request.getAdjustReason(), ADJUST_REASON_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getAdjustEvidenceRef(), ADJUST_EVIDENCE_REF_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getApprovalRef(), ADJUST_APPROVAL_REF_REQUIRED_MESSAGE);
    }

    private @NonNull Map<String, Object> adjustContext(@NonNull FundsBalanceAdjustRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        result.put(FundsInstructionContextKeys.INCREASE, request.getIncrease());
        result.put(FundsInstructionContextKeys.ADJUST_REASON, request.getAdjustReason());
        result.put(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF, request.getAdjustEvidenceRef());
        result.put(FundsInstructionContextKeys.APPROVAL_REF, request.getApprovalRef());
        if (request.getReconciliationExceptionRef() != null) {
            result.put(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF,
                    request.getReconciliationExceptionRef());
        }
        return result;
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
