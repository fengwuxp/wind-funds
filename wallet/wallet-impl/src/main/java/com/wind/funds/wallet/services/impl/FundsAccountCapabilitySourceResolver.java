package com.wind.funds.wallet.services.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wind.common.exception.AssertUtils;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

/**
 * 账户能力来源解析器。
 */
final class FundsAccountCapabilitySourceResolver {

    static final String CONTEXT_CAPABILITIES_KEY = "fundsAccountCapabilities";

    static final String SOURCE_LEDGER_PROFILE = "LEDGER_PROFILE";

    static final String SOURCE_CONTEXT_VARIABLES = "CONTEXT_VARIABLES";

    private FundsAccountCapabilitySourceResolver() {
        throw new AssertionError();
    }

    static AccountCapabilityResolution resolve(@NonNull LedgerProfileCode profileCode,
                                               @Nullable String contextVariables) {
        Set<FundsAccountCapability> profileCapabilities = profileCapabilities(profileCode);
        Set<FundsAccountCapability> explicitCapabilities = explicitCapabilities(contextVariables);
        if (explicitCapabilities == null) {
            return new AccountCapabilityResolution(profileCapabilities, SOURCE_LEDGER_PROFILE);
        }
        AssertUtils.isTrue(profileCapabilities.containsAll(explicitCapabilities),
                "账户显式能力不能超出 profile 安全能力，profileCode = {}, capabilities = {}",
                profileCode,
                explicitCapabilities);
        return new AccountCapabilityResolution(Set.copyOf(explicitCapabilities), SOURCE_CONTEXT_VARIABLES);
    }

    private static Set<FundsAccountCapability> profileCapabilities(LedgerProfileCode profileCode) {
        return switch (profileCode) {
            case FUNDING_BASIC, FUNDING_MERCHANT, FUNDING_PLATFORM -> Set.of(FundsAccountCapability.RECEIVE,
                    FundsAccountCapability.PAY,
                    FundsAccountCapability.WITHDRAW);
            case CREDIT_BASIC -> Set.of(FundsAccountCapability.PAY);
            case BUDGET_BASIC -> Set.of();
        };
    }

    @Nullable
    private static Set<FundsAccountCapability> explicitCapabilities(@Nullable String contextVariables) {
        if (!StringUtils.hasText(contextVariables)) {
            return null;
        }
        if (!contextVariables.contains(CONTEXT_CAPABILITIES_KEY)) {
            return null;
        }
        JSONObject values = JSON.parseObject(contextVariables);
        if (!values.containsKey(CONTEXT_CAPABILITIES_KEY)) {
            return null;
        }
        JSONArray capabilities = values.getJSONArray(CONTEXT_CAPABILITIES_KEY);
        AssertUtils.notNull(capabilities, "账户显式能力必须使用数组字段 {}", CONTEXT_CAPABILITIES_KEY);
        EnumSet<FundsAccountCapability> result = EnumSet.noneOf(FundsAccountCapability.class);
        for (Object item : capabilities) {
            AssertUtils.isTrue(item instanceof String, "账户显式能力值必须是文本，value = {}", item);
            result.add(FundsAccountCapability.valueOf((String) item));
        }
        return result.isEmpty() ? Set.of() : result;
    }

    record AccountCapabilityResolution(
            Set<FundsAccountCapability> capabilities,
            String source
    ) {
    }
}
