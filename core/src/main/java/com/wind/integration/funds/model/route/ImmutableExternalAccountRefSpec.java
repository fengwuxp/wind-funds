package com.wind.integration.funds.model.route;

import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 不可变外部账户引用实现。
 */
@Builder
public record ImmutableExternalAccountRefSpec(String externalAccountId,
                                              String externalAccountType,
                                              @Nullable String externalAccountNo,
                                              @Nullable String providerCode,
                                              @Nullable String channelCode,
                                              @Nullable String currency,
                                              @Nullable String countryCode,
                                              @Nullable String description,
                                              Map<String, Object> contextVariables)
        implements ExternalAccountRefSpec {

    public ImmutableExternalAccountRefSpec {
        contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
    }

    @Override
    public @NonNull String getExternalAccountId() {
        return externalAccountId;
    }

    @Override
    public @NonNull String getExternalAccountType() {
        return externalAccountType;
    }

    @Override
    public @Nullable String getExternalAccountNo() {
        return externalAccountNo;
    }

    @Override
    public @Nullable String getProviderCode() {
        return providerCode;
    }

    @Override
    public @Nullable String getChannelCode() {
        return channelCode;
    }

    @Override
    public @Nullable String getCurrency() {
        return currency;
    }

    @Override
    public @Nullable String getCountryCode() {
        return countryCode;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }
}
