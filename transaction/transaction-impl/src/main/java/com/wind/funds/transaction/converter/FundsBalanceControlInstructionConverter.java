package com.wind.funds.transaction.converter;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.transaction.converter.FundsInstructionAmountSupport.ConvertedAmount;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.spec.SourceObjectType;
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

    private static final String EXTERNAL_BALANCE_ANOMALY_SOURCE_SN_REQUIRED_MESSAGE =
            "外部余额异常纠偏缺少来源单号";

    private static final String EXTERNAL_BALANCE_ANOMALY_REASON_CODE_REQUIRED_MESSAGE =
            "外部余额异常纠偏缺少原因码";

    private static final String EXTERNAL_BALANCE_ANOMALY_FINAL_EVENT_REQUIRED_MESSAGE =
            "外部余额异常纠偏缺少外部终局事件引用";

    private static final String EXTERNAL_BALANCE_ANOMALY_SNAPSHOT_REQUIRED_MESSAGE =
            "外部余额异常纠偏缺少外部余额快照引用";

    private static final String EXTERNAL_BALANCE_ANOMALY_RECONCILIATION_REQUIRED_MESSAGE =
            "外部余额异常纠偏缺少对账差错引用";

    private static final String EXTERNAL_BALANCE_ANOMALY_RESPONSIBILITY_REQUIRED_MESSAGE =
            "外部余额异常纠偏缺少责任归属引用";

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
        requireExternalBalanceAnomalyContext(request);
    }

    private void requireExternalBalanceAnomalyContext(@NonNull FundsBalanceAdjustRequest request) {
        if (request.getSourceType() != SourceObjectType.EXTERNAL_BALANCE_ANOMALY) {
            return;
        }
        AssertUtils.hasText(request.getSourceSn(), EXTERNAL_BALANCE_ANOMALY_SOURCE_SN_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getReasonCode(), EXTERNAL_BALANCE_ANOMALY_REASON_CODE_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getExternalFinalEventRef(),
                EXTERNAL_BALANCE_ANOMALY_FINAL_EVENT_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getExternalBalanceSnapshotRef(),
                EXTERNAL_BALANCE_ANOMALY_SNAPSHOT_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getReconciliationExceptionRef(),
                EXTERNAL_BALANCE_ANOMALY_RECONCILIATION_REQUIRED_MESSAGE);
        AssertUtils.hasText(request.getResponsibilityRef(),
                EXTERNAL_BALANCE_ANOMALY_RESPONSIBILITY_REQUIRED_MESSAGE);
    }

    private @NonNull Map<String, Object> adjustContext(@NonNull FundsBalanceAdjustRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(FundsInstructionContextKeys.ACCOUNT_ID, request.getAccountId());
        result.put(FundsInstructionContextKeys.INCREASE, request.getIncrease());
        result.put(FundsInstructionContextKeys.ADJUST_REASON, request.getAdjustReason());
        result.put(FundsInstructionContextKeys.ADJUST_EVIDENCE_REF, request.getAdjustEvidenceRef());
        result.put(FundsInstructionContextKeys.APPROVAL_REF, request.getApprovalRef());
        putIfPresent(result, FundsInstructionContextKeys.SOURCE_TYPE, request.getSourceType() == null
                ? null : request.getSourceType().name());
        putIfPresent(result, FundsInstructionContextKeys.SOURCE_SN, request.getSourceSn());
        putIfPresent(result, FundsInstructionContextKeys.REASON_CODE, request.getReasonCode());
        putIfPresent(result, FundsInstructionContextKeys.EXTERNAL_INSTITUTION_REF,
                request.getExternalInstitutionRef());
        putIfPresent(result, FundsInstructionContextKeys.EXTERNAL_ACCOUNT_REF, request.getExternalAccountRef());
        putIfPresent(result, FundsInstructionContextKeys.EXTERNAL_FINAL_EVENT_REF,
                request.getExternalFinalEventRef());
        putIfPresent(result, FundsInstructionContextKeys.EXTERNAL_BALANCE_SNAPSHOT_REF,
                request.getExternalBalanceSnapshotRef());
        putIfPresent(result, FundsInstructionContextKeys.RESPONSIBILITY_REF, request.getResponsibilityRef());
        if (request.getReconciliationExceptionRef() != null) {
            result.put(FundsInstructionContextKeys.RECONCILIATION_EXCEPTION_REF,
                    request.getReconciliationExceptionRef());
        }
        putIfPresent(result, FundsInstructionContextKeys.RECONCILIATION_RERUN_REF,
                request.getReconciliationRerunRef());
        putIfPresent(result, FundsInstructionContextKeys.ALLOW_NEGATIVE_BALANCE,
                request.getAllowNegativeBalance());
        putIfPresent(result, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_POLICY_CODE,
                request.getNegativeAvailablePolicyCode());
        putIfPresent(result, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_RISK_STATUS,
                request.getNegativeAvailableRiskStatus());
        putIfPresent(result, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_SINGLE_LIMIT,
                request.getNegativeAvailableSingleLimit());
        putIfPresent(result, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_CUMULATIVE_LIMIT,
                request.getNegativeAvailableCumulativeLimit());
        putIfPresent(result, FundsInstructionContextKeys.NEGATIVE_AVAILABLE_AGING_STARTED_AT,
                request.getNegativeAvailableAgingStartedAt());
        return result;
    }

    private void putIfPresent(@NonNull Map<String, Object> target,
                              @NonNull String key,
                              @Nullable Object value) {
        if (value != null) {
            target.put(key, value);
        }
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
