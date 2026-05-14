package com.capte.funds.transaction.support;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 资金指令上下文读取工具。
 */
public final class FundsInstructionContextReader {

    private FundsInstructionContextReader() {
    }

    public static @NonNull FundsAccountId requireFundsAccountId(@NonNull FundsInstructionSpec instruction,
                                                                @NonNull String key) {
        return requireValue(instruction, key, FundsAccountId.class);
    }

    public static @NonNull LedgerSubjectCode requireLedgerSubjectCode(@NonNull FundsInstructionSpec instruction,
                                                                      @NonNull String key) {
        return requireValue(instruction, key, LedgerSubjectCode.class);
    }

    public static @NonNull String requireString(@NonNull FundsInstructionSpec instruction,
                                                @NonNull String key) {
        return requireValue(instruction, key, String.class);
    }

    public static @NonNull Boolean requireBoolean(@NonNull FundsInstructionSpec instruction,
                                                  @NonNull String key) {
        return requireValue(instruction, key, Boolean.class);
    }

    public static <T> @NonNull T requireValue(@NonNull FundsInstructionSpec instruction,
                                              @NonNull String key,
                                              @NonNull Class<T> type) {
        T result = getValue(instruction, key, type);
        AssertUtils.notNull(result, "资金指令上下文缺失，key = {}", key);
        return result;
    }

    public static <T> @Nullable T getValue(@NonNull FundsInstructionSpec instruction,
                                           @NonNull String key,
                                           @NonNull Class<T> type) {
        Object value = instruction.getContextVariables().get(key);
        if (value == null) {
            return null;
        }
        AssertUtils.isTrue(type.isInstance(value),
                "资金指令上下文类型错误，key = {}, expectedType = {}, actualType = {}",
                key, type.getName(), value.getClass().getName());
        return type.cast(value);
    }
}
