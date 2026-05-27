package com.wind.integration.funds.model.route;

import com.wind.integration.funds.model.FundsContextVariables;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 不可变支付工具引用快照实现。
 */
@Builder
@FieldNameConstants
public record ImmutablePaymentInstrumentRefSpec(String instrumentId,
                                                String instrumentType,
                                                String instrumentNo,
                                                String ownerId,
                                                String ownerType,
                                                @Nullable Long tenantId,
                                                @Nullable String currency,
                                                @Nullable String status,
                                                Map<String, Object> bindingSnapshot,
                                                @Nullable String description)
        implements PaymentInstrumentRefSpec {

    public ImmutablePaymentInstrumentRefSpec {
        if (PaymentInstrumentSensitiveValueValidator.isRawSensitiveInstrumentNo(instrumentNo)) {
            throw new IllegalArgumentException("instrumentNo must be masked or token reference");
        }
        if (PaymentInstrumentSensitiveValueValidator.containsSensitiveBindingSnapshotField(bindingSnapshot)) {
            throw new IllegalArgumentException(
                    "bindingSnapshot must not contain sensitive payment instrument fields");
        }
        bindingSnapshot = FundsContextVariables.immutableCopy(bindingSnapshot);
    }

    @Override
    public @NonNull String getInstrumentId() {
        return instrumentId;
    }

    @Override
    public @NonNull String getInstrumentType() {
        return instrumentType;
    }

    @Override
    public @NonNull String getInstrumentNo() {
        return instrumentNo;
    }

    @Override
    public @NonNull String getOwnerId() {
        return ownerId;
    }

    @Override
    public @NonNull String getOwnerType() {
        return ownerType;
    }

    @Override
    public @Nullable Long getTenantId() {
        return tenantId;
    }

    @Override
    public @Nullable String getCurrency() {
        return currency;
    }

    @Override
    public @Nullable String getStatus() {
        return status;
    }

    @Override
    public @NonNull Map<String, Object> getBindingSnapshot() {
        return bindingSnapshot;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }
}
