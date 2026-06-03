package com.wind.funds.model.route;

import com.wind.common.exception.AssertUtils;
import com.wind.funds.model.transaction.FundsBenefitSpecValidators;
import com.wind.funds.route.support.ExternalAccountSensitiveValueValidator;
import com.wind.funds.wallet.support.PaymentInstrumentSensitiveValueValidator;

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
        return FundsBenefitSpecValidators.immutableInstructionContext(contextVariables, owner);
    }
}
