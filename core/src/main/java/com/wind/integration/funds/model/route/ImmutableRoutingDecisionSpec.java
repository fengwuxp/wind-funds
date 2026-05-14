package com.wind.integration.funds.model.route;

import com.wind.integration.funds.route.spec.FundingAllocationDecisionSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 不可变路径决策说明实现。
 */
@Builder
public record ImmutableRoutingDecisionSpec(@Nullable String policyCode,
                                           List<String> matchedRules,
                                           @Nullable String selectedProcessor,
                                           @Nullable String selectedCashFundingAccount,
                                           @Nullable String selectedPlatformAccount,
                                           List<FundingAllocationDecisionSpec> fundingAllocations,
                                           @Nullable String decisionReason,
                                           Map<String, Object> contextVariables)
        implements RoutingDecisionSpec {

    public ImmutableRoutingDecisionSpec {
        matchedRules = List.copyOf(matchedRules == null ? List.of() : matchedRules);
        fundingAllocations = List.copyOf(fundingAllocations == null ? List.of() : fundingAllocations);
        contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
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
    public @NonNull List<FundingAllocationDecisionSpec> getFundingAllocations() {
        return fundingAllocations;
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
