package com.wind.funds.transaction.support;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.spec.FundsInstructionFieldKeys;
import com.wind.funds.transaction.spec.FundsInstructionSpec;
import com.wind.funds.wallet.FundsAccountId;
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
        if (isTypedInstructionField(key)) {
            return castValue(key, typedInstructionValue(instruction, key), type);
        }
        Object value = instruction.getContextVariables().get(key);
        return castValue(key, value, type);
    }

    private static <T> @Nullable T castValue(@NonNull String key,
                                             @Nullable Object value,
                                             @NonNull Class<T> type) {
        if (value == null) {
            return null;
        }
        AssertUtils.isTrue(type.isInstance(value),
                "资金指令上下文类型错误，key = {}, expectedType = {}, actualType = {}",
                key, type.getName(), value.getClass().getName());
        return type.cast(value);
    }

    private static boolean isTypedInstructionField(@NonNull String key) {
        return FundsInstructionFieldKeys.ACCOUNT_ID.equals(key)
                || FundsInstructionFieldKeys.PAYER_ACCOUNT_ID.equals(key)
                || FundsInstructionFieldKeys.PAYEE_ACCOUNT_ID.equals(key)
                || FundsInstructionFieldKeys.PAYER_ID.equals(key)
                || FundsInstructionFieldKeys.PAYEE_ID.equals(key)
                || FundsInstructionFieldKeys.PAYER_LEDGER_SUBJECT_CODE.equals(key)
                || FundsInstructionFieldKeys.PAYEE_LEDGER_SUBJECT_CODE.equals(key)
                || FundsInstructionFieldKeys.LINKED_FUNDING_ACCOUNT_ID.equals(key)
                || FundsInstructionFieldKeys.LEDGER_PERIOD_TYPE.equals(key)
                || FundsInstructionFieldKeys.LEDGER_PERIOD_ID.equals(key);
    }

    private static @Nullable Object typedInstructionValue(@NonNull FundsInstructionSpec instruction,
                                                         @NonNull String key) {
        return switch (key) {
            case FundsInstructionFieldKeys.ACCOUNT_ID -> instruction.getAccountId();
            case FundsInstructionFieldKeys.PAYER_ACCOUNT_ID -> instruction.getPayerAccountId();
            case FundsInstructionFieldKeys.PAYEE_ACCOUNT_ID -> instruction.getPayeeAccountId();
            case FundsInstructionFieldKeys.PAYER_ID -> instruction.getPayerId();
            case FundsInstructionFieldKeys.PAYEE_ID -> instruction.getPayeeId();
            case FundsInstructionFieldKeys.PAYER_LEDGER_SUBJECT_CODE -> instruction.getPayerLedgerSubjectCode();
            case FundsInstructionFieldKeys.PAYEE_LEDGER_SUBJECT_CODE -> instruction.getPayeeLedgerSubjectCode();
            case FundsInstructionFieldKeys.LINKED_FUNDING_ACCOUNT_ID -> instruction.getLinkedFundingAccountId();
            case FundsInstructionFieldKeys.LEDGER_PERIOD_TYPE -> instruction.getLedgerPeriodType();
            case FundsInstructionFieldKeys.LEDGER_PERIOD_ID -> instruction.getLedgerPeriodId();
            default -> null;
        };
    }
}
