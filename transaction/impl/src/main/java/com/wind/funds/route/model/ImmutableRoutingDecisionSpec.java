package com.wind.funds.route.model;

import com.wind.funds.route.spec.RoutingDecisionSpec;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 不可变路径决策说明实现。
 */
@Builder
@FieldNameConstants
public record ImmutableRoutingDecisionSpec(@Nullable String policyCode,
                                           List<String> matchedRules,
                                           @Nullable String selectedProcessor,
                                           @Nullable String selectedCashFundingAccount,
                                           @Nullable String selectedPlatformAccount,
                                           @Nullable String decisionReason,
                                           Map<String, Object> contextVariables)
        implements RoutingDecisionSpec {

    public ImmutableRoutingDecisionSpec {
        matchedRules = List.copyOf(matchedRules == null ? List.of() : matchedRules);
        contextVariables = RouteContextVariablesValidator.immutableContext(contextVariables, "routingDecision");
    }

    @Override
    public @Nullable String getPolicyCode() {
        return policyCode;
    }

    @Override
    public @NonNull List<String> getMatchedRules() {
        return matchedRules;
    }

    @Override
    public @Nullable String getSelectedProcessor() {
        return selectedProcessor;
    }

    @Override
    public @Nullable String getSelectedCashFundingAccount() {
        return selectedCashFundingAccount;
    }

    @Override
    public @Nullable String getSelectedPlatformAccount() {
        return selectedPlatformAccount;
    }

    @Override
    public @Nullable String getDecisionReason() {
        return decisionReason;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
