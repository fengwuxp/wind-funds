package com.wind.funds.route.ref;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 外部对手账户引用快照。
 */
public interface ExternalAccountRefSpec {

    @NonNull
    String getExternalAccountId();

    @NonNull
    String getExternalAccountType();

    @Nullable
    default String getExternalAccountNo() {
        return null;
    }

    @Nullable
    default String getProviderCode() {
        return null;
    }

    @Nullable
    default String getChannelCode() {
        return null;
    }

    @Nullable
    default String getCurrency() {
        return null;
    }

    @Nullable
    default String getCountryCode() {
        return null;
    }

    @Nullable
    default String getDescription() {
        return null;
    }

    @NonNull
    default Map<String, Object> getContextVariables() {
        return Map.of();
    }
}
