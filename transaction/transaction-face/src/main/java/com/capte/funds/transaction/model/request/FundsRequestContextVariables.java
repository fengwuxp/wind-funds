package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.model.FundsContextVariables;
import org.jspecify.annotations.Nullable;

/**
 * 交易请求上下文快照工具。
 */
final class FundsRequestContextVariables {

    private FundsRequestContextVariables() {
        throw new AssertionError();
    }

    static @Nullable WritableContextVariables snapshot(@Nullable WritableContextVariables contextVariables) {
        if (contextVariables == null) {
            return null;
        }
        return WritableContextVariables.of(FundsContextVariables.immutableCopy(contextVariables.getContextVariables()));
    }
}
