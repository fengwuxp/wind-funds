package com.wind.integration.funds.model.transaction;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import lombok.Builder;
import lombok.experimental.FieldNameConstants;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 不可变资金指令引用实现。
 */
@Builder
@FieldNameConstants
public record ImmutableFundsInstructionReferenceSpec(FundsInstructionReferenceType referenceType,
                                                     @Nullable String referenceSn,
                                                     @Nullable String referenceBusinessSn,
                                                     @Nullable String referenceLedgerTransactionSn,
                                                     @Nullable String externalTransactionId,
                                                     @Nullable String authCode,
                                                     Map<String, Object> contextVariables)
        implements FundsInstructionReferenceSpec {

    public ImmutableFundsInstructionReferenceSpec {
        AssertUtils.notNull(referenceType, "fundsInstruction.referenceType must not be null");
        if (!StringUtils.hasText(referenceSn)
                && !StringUtils.hasText(referenceBusinessSn)
                && !StringUtils.hasText(referenceLedgerTransactionSn)
                && !StringUtils.hasText(externalTransactionId)
                && !StringUtils.hasText(authCode)) {
            throw new IllegalArgumentException("fundsInstruction.reference identifier is required");
        }
        contextVariables = Map.copyOf(contextVariables == null ? Map.of() : contextVariables);
    }

    @Override
    public @NonNull FundsInstructionReferenceType getReferenceType() {
        return referenceType;
    }

    @Override
    public @NonNull Map<String, Object> getContextVariables() {
        return contextVariables;
    }


    @Override
    public @Nullable String getReferenceSn() {
        return referenceSn;
    }

    @Override
    public @Nullable String getReferenceBusinessSn() {
        return referenceBusinessSn;
    }

    @Override
    public @Nullable String getReferenceLedgerTransactionSn() {
        return referenceLedgerTransactionSn;
    }

    @Override
    public @Nullable String getExternalTransactionId() {
        return externalTransactionId;
    }

    @Override
    public @Nullable String getAuthCode() {
        return authCode;
    }

}
