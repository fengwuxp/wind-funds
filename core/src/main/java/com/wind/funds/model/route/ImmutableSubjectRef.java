package com.wind.funds.model.route;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.ref.SubjectRef;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 不可变主体引用实现。
 */
@Builder
@FieldNameConstants
public record ImmutableSubjectRef(@Nullable Long tenantId,
                                  String subjectId,
                                  FundsSubjectType subjectType,
                                  @Nullable String subjectName,
                                  @Nullable String currency,
                                  @Nullable String ledgerProfileCode,
                                  @Nullable String description) implements SubjectRef {

    @Override
    public @NonNull String getSubjectId() {
        return subjectId;
    }

    @Override
    public @NonNull FundsSubjectType getSubjectType() {
        return subjectType;
    }


    @Override
    public @Nullable Long getTenantId() {
        return tenantId;
    }

    @Override
    public @Nullable String getSubjectName() {
        return subjectName;
    }

    @Override
    public @Nullable String getCurrency() {
        return currency;
    }

    @Override
    public @Nullable String getLedgerProfileCode() {
        return ledgerProfileCode;
    }

    @Override
    public @Nullable String getDescription() {
        return description;
    }

}
