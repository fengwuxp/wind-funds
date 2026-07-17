package com.wind.funds.transaction.converter;

import com.wind.common.exception.AssertUtils;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.transaction.constant.FundsInstructionContextKeys;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * 资金指令扩展上下文校验器。
 */
final class FundsInstructionContextValidator {

    private static final String SENSITIVE_CONTEXT_MESSAGE =
            "contextVariables must not contain sensitive funds transaction fields";

    private static final String RESERVED_CONTEXT_MESSAGE =
            "contextVariables must not contain reserved funds transaction fields";

    private static final Set<String> RESERVED_CONTEXT_FIELDS = Set.of(
            FundsInstructionContextKeys.FEE_CHARGE_SPEC);

    private FundsInstructionContextValidator() {
        throw new AssertionError();
    }

    static void assertNoSensitiveContextVariables(@Nullable ReadonlyContextVariables contextVariables) {
        Map<String, Object> variables = contextVariables == null ? null : contextVariables.getContextVariables();
        AssertUtils.isFalse(variables != null
                        && (PaymentInstrumentSensitiveValueValidator.containsSensitiveField(variables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(variables)),
                SENSITIVE_CONTEXT_MESSAGE);
    }

    static void assertNoReservedContextVariables(@Nullable ReadonlyContextVariables contextVariables) {
        Map<String, Object> variables = contextVariables == null ? null : contextVariables.getContextVariables();
        AssertUtils.isFalse(variables != null && variables.keySet().stream().anyMatch(RESERVED_CONTEXT_FIELDS::contains),
                RESERVED_CONTEXT_MESSAGE + "，reservedFields = {}",
                RESERVED_CONTEXT_FIELDS);
    }
}
