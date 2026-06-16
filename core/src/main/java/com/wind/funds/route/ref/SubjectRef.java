package com.wind.funds.route.ref;

import com.wind.funds.route.enums.FundsSubjectType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Route 层统一主体引用。
 */
public interface SubjectRef {

    @Nullable
    Long getTenantId();

    @NonNull
    String getSubjectId();

    @NonNull
    FundsSubjectType getSubjectType();

    @Nullable
    default String getSubjectName() {
        return null;
    }

    @Nullable
    default String getCurrency() {
        return null;
    }

    @Nullable
    default String getLedgerProfileCode() {
        return null;
    }
}
