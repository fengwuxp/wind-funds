package com.wind.integration.funds.route.spec;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 路径决策说明。
 */
public interface RoutingDecisionSpec {

    @Nullable
    default String getPolicyCode() {
        return null;
    }

    @NonNull
    default List<String> getMatchedRules() {
        return List.of();
    }

    @Nullable
    default String getSelectedProcessor() {
        return null;
    }

    @Nullable
    default String getSelectedCashFundingAccount() {
        return null;
    }

    @Nullable
    default String getSelectedPlatformAccount() {
        return null;
    }

    @NonNull
    default List<FundingAllocationDecisionSpec> getFundingAllocations() {
        return List.of();
    }

    @Nullable
    default String getDecisionReason() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}
