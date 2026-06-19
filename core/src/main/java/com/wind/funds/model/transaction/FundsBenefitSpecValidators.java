package com.wind.funds.model.transaction;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.wind.funds.model.FundsContextVariables;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 权益资金上下文校验工具。
 */
public final class FundsBenefitSpecValidators {

    private static final String NON_FIELD_NAME_CHARACTER_PATTERN = "[^a-z0-9]";

    private static final Pattern RAW_FIELD_NAME_PATTERN = Pattern.compile(
            "(?:\"([^\"]+)\"|(?<![A-Za-z0-9_-])([A-Za-z][A-Za-z0-9_ -]*))\\s*:");

    private static final Set<String> RESERVED_INSTRUCTION_CONTEXT_KEYS = Set.of(
            "benefitSnapshot",
            "benefitGroupSn",
            "orderAmount",
            "userPayAmount",
            "merchantReceivableAmount",
            "authorizationExpireTime",
            "amount",
            "ledgerEffect",
            "fundingNature",
            "refundPolicy",
            "partialRefundStrategy",
            "dispositions",
            "refundDisposition",
            "refundableAmount",
            "nonRefundableAmount",
            "userCouponId",
            "lockNo",
            "redemptionNo",
            "releaseNo",
            "returnNo",
            "ruleVersionId",
            "currentMarketingRule",
            "couponEligibility",
            "couponAvailable",
            "recalculatedDiscount",
            "bestCoupon",
            "activityRules",
            "userCouponBag");

    private static final Set<String> MONEY_VALUE_OBJECT_KEYS = Set.of("amount", "currency");

    private FundsBenefitSpecValidators() {
    }

    public static Map<String, Object> immutableInstructionContext(Map<String, Object> contextVariables, String owner) {
        Map<String, Object> copied = FundsContextVariables.immutableCopy(contextVariables);
        rejectReservedInstructionContextKeys(copied, owner);
        return copied;
    }

    public static void rejectInstructionContextVariables(@Nullable String contextVariables, String owner) {
        if (!StringUtils.hasText(contextVariables)) {
            return;
        }
        try {
            rejectReservedInstructionContextKeys(JSON.parse(contextVariables), owner);
        } catch (JSONException ignored) {
            rejectReservedRawInstructionContextKeys(contextVariables, owner);
        }
    }

    private static void rejectReservedInstructionContextKeys(@Nullable Object value, String owner) {
        rejectReservedContextKeys(value, owner, RESERVED_INSTRUCTION_CONTEXT_KEYS);
    }

    private static void rejectReservedContextKeys(@Nullable Object value, String owner, Set<String> reservedKeys) {
        if (value instanceof Map<?, ?> values) {
            if (isMoneyValueObject(values)) {
                return;
            }
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (entry.getKey() instanceof String key && reservedKeys.contains(key)) {
                    throw new IllegalArgumentException(
                            owner + ".contextVariables must not contain core benefit field: " + key);
                }
                rejectReservedContextKeys(entry.getValue(), owner, reservedKeys);
            }
            return;
        }
        if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                rejectReservedContextKeys(item, owner, reservedKeys);
            }
            return;
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                rejectReservedContextKeys(Array.get(value, index), owner, reservedKeys);
            }
        }
    }

    private static boolean isMoneyValueObject(Map<?, ?> values) {
        return values.size() == MONEY_VALUE_OBJECT_KEYS.size()
                && values.keySet().stream()
                .allMatch(key -> key instanceof String stringKey
                        && MONEY_VALUE_OBJECT_KEYS.contains(stringKey));
    }

    private static void rejectReservedRawInstructionContextKeys(String contextVariables, String owner) {
        Matcher matcher = RAW_FIELD_NAME_PATTERN.matcher(contextVariables);
        while (matcher.find()) {
            String fieldName = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            if (isReservedInstructionContextKey(fieldName)) {
                throw new IllegalArgumentException(
                        owner + ".contextVariables must not contain core benefit field: " + fieldName);
            }
        }
    }

    private static boolean isReservedInstructionContextKey(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        for (String reservedKey : RESERVED_INSTRUCTION_CONTEXT_KEYS) {
            if (reservedKey.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "")
                    .equals(normalized)) {
                return true;
            }
        }
        return false;
    }
}
