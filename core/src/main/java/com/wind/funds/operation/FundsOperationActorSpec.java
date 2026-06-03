package com.wind.funds.operation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 资金操作参与者快照。
 *
 * <p>只表达资金 DSL 需要固化的操作者事实，避免 wind-funds 依赖具体业务域的 Operator 实现。</p>
 */
public interface FundsOperationActorSpec {

    String OPERATOR_TYPE_SYSTEM = "SYSTEM";

    String OPERATOR_TYPE_RISK_CONTROL = "RISK_CONTROL";

    @NonNull
    Long getOperatorId();

    @NonNull
    String getOperatorType();

    @Nullable
    String getOperatorName();

    @NonNull
    String getAppName();

    @NonNull
    Map<String, Object> getContextVariables();

    default boolean isSystem() {
        String operatorType = getOperatorType();
        return OPERATOR_TYPE_SYSTEM.equals(operatorType) || OPERATOR_TYPE_RISK_CONTROL.equals(operatorType);
    }
}
