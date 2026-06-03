package com.wind.funds.model.route;

import com.wind.funds.model.FundsContextVariables;
import com.wind.funds.route.ref.ExternalAccountRefSpec;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 不可变外部账户引用实现。
 */
@Builder
@FieldNameConstants
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
        if (ExternalAccountSensitiveValueValidator.isRawSensitiveExternalAccountNo(externalAccountNo)) {
            throw new IllegalArgumentException("externalAccountNo must be masked or token reference");
        }
        if (ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables)) {
            throw new IllegalArgumentException("contextVariables must not contain sensitive external account fields");
        }
        contextVariables = FundsContextVariables.immutableCopy(contextVariables);
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
