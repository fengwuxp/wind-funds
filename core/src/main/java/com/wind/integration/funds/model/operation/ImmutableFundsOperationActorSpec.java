package com.wind.integration.funds.model.operation;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
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
        contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
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
