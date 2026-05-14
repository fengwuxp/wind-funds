package com.wind.integration.funds.model.transaction;

import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 不可变资金指令引用实现。
 */
@Builder
public record ImmutableFundsInstructionReferenceSpec(FundsInstructionReferenceType referenceType,
                                                     @Nullable String referenceSn,
                                                     @Nullable String referenceBusinessSn,
                                                     @Nullable String referenceLedgerTransactionSn,
                                                     @Nullable String externalTransactionId,
                                                     @Nullable String authCode,
                                                     Map<String, Object> contextVariables)
        implements FundsInstructionReferenceSpec {

    public ImmutableFundsInstructionReferenceSpec {
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
