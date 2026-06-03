package com.wind.funds.model.operation;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.model.transaction.FundsBenefitSpecValidators;
import com.wind.funds.operation.FundsOperationActorSpec;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 不可变资金操作参与者快照。
 */
@Builder
public record ImmutableFundsOperationActorSpec(Long operatorId,
                                               String operatorType,
                                               @Nullable String operatorName,
                                               String appName,
                                               Map<String, Object> contextVariables)
        implements FundsOperationActorSpec {

    public ImmutableFundsOperationActorSpec {
        AssertUtils.notNull(operatorId, "fundsOperationActor.operatorId must not be null");
        AssertUtils.hasText(operatorType, "fundsOperationActor.operatorType must not be blank");
        AssertUtils.hasText(appName, "fundsOperationActor.appName must not be blank");
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables),
                "fundsOperationActor.contextVariables must not contain sensitive fields");
        contextVariables = FundsBenefitSpecValidators.immutableInstructionContext(
                contextVariables, "fundsOperationActor");
    }

    @Override
    public @NonNull Long getOperatorId() {
        return operatorId;
    }

    @Override
    public @NonNull String getOperatorType() {
        return operatorType;
    }

    @Override
    public @Nullable String getOperatorName() {
        return operatorName;
    }

    @Override
    public @NonNull String getAppName() {
        return appName;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
