package com.wind.integration.funds.model.route;

import com.wind.common.exception.AssertUtils;
import com.wind.integration.funds.model.FundsContextVariables;
import com.wind.integration.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.integration.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;

import java.util.Map;

/**
 * Route DSL 扩展上下文校验器。
 */
final class RouteContextVariablesValidator {

    private RouteContextVariablesValidator() {
    }

    static Map<String, Object> immutableContext(Map<String, Object> contextVariables, String owner) {
        AssertUtils.isFalse(PaymentInstrumentSensitiveValueValidator.containsSensitiveField(contextVariables)
                        || ExternalAccountSensitiveValueValidator.containsSensitiveContextField(contextVariables),
                owner + ".contextVariables must not contain sensitive fields");
        return FundsContextVariables.immutableCopy(contextVariables);
    }
}
