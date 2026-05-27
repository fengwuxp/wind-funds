package com.capte.funds.wallet.services.impl;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;
import org.jspecify.annotations.Nullable;

/**
 * 钱包管理对象扩展上下文校验器。
 */
final class WalletContextVariablesValidator {

    private static final String SENSITIVE_CONTEXT_MESSAGE =
            "contextVariables must not contain sensitive wallet fields";

    private WalletContextVariablesValidator() {
        throw new AssertionError();
    }

    static void assertNoSensitiveContextVariables(@Nullable String contextVariables) {
        AssertUtils.isFalse(
                PaymentInstrumentSensitiveValueValidator.containsSensitiveContextVariables(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextVariables(contextVariables),
                SENSITIVE_CONTEXT_MESSAGE);
    }
}
