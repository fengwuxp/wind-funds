package com.wind.funds.route.ref;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 支付工具引用快照。
 */
public interface PaymentInstrumentRefSpec {

    @NonNull
    String getInstrumentId();

    @NonNull
    String getInstrumentType();

    @NonNull
    String getInstrumentNo();

    @NonNull
    String getOwnerId();

    @NonNull
    String getOwnerType();

    @Nullable
    default Long getTenantId() {
        return null;
    }

    @Nullable
    default String getCurrency() {
        return null;
    }

    @Nullable
    default String getStatus() {
        return null;
    }

    @NonNull
    default Map<String, Object> getBindingSnapshot() {
        return Map.of();
    }

    @Nullable
    default String getDescription() {
        return null;
    }
}
