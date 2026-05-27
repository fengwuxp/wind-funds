package com.capte.funds.transaction.converter;

import com.wind.common.exception.AssertUtils;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 资金指令扩展上下文校验器。
 */
final class FundsInstructionContextValidator {

    private static final String SENSITIVE_CONTEXT_MESSAGE =
            "contextVariables must not contain sensitive funds transaction fields";

    private FundsInstructionContextValidator() {
        throw new AssertionError();
    }

    static void assertNoSensitiveContextVariables(@Nullable WritableContextVariables contextVariables) {
        Map<String, Object> variables = contextVariables == null ? null : contextVariables.getContextVariables();
        AssertUtils.isFalse(variables != null
                        && (PaymentInstrumentSensitiveValueValidator.containsSensitiveField(variables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(variables)),
                SENSITIVE_CONTEXT_MESSAGE);
    }
}
