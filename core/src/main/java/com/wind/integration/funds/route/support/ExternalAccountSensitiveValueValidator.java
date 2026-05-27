package com.wind.integration.funds.route.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 外部账户敏感值校验器。
 *
 * <p>用于阻断银行账户原文、IBAN 等敏感字段进入 route 快照、日志或报表链路。
 * 允许使用脱敏展示号、token 或外部稳定引用。</p>
 */
public final class ExternalAccountSensitiveValueValidator {

    private static final int MIN_RAW_EXTERNAL_ACCOUNT_LENGTH = 8;

    private static final int MAX_RAW_EXTERNAL_ACCOUNT_LENGTH = 34;

    private static final int MIN_RAW_IBAN_LENGTH = 15;

    private static final int IBAN_COUNTRY_CODE_LENGTH = 2;

    private static final int IBAN_CHECK_DIGIT_LENGTH = 2;

    private static final int IBAN_MODULUS = 97;

    private static final int IBAN_EXPECTED_REMAINDER = 1;

    private static final int DECIMAL_RADIX = 10;

    private static final String NON_FIELD_NAME_CHARACTER_PATTERN = "[^a-z0-9]";

    private static final Set<String> SENSITIVE_CONTEXT_FIELDS = Set.of(
            "accountnumber",
            "accountno",
            "bankaccountnumber",
            "bankaccountno",
            "externalaccountnumber",
            "externalaccountno",
            "routingnumber",
            "routingno",
            "iban");

    private ExternalAccountSensitiveValueValidator() {
        throw new AssertionError();
    }

    /**
     * 判断输入是否形似原始外部账户号。
     *
     * @param externalAccountNo 外部账户展示号、token 或原始号候选
     * @return true 表示输入为 8 到 34 位数字或形似 IBAN，可带空格或短横线分隔
     */
    public static boolean isRawSensitiveExternalAccountNo(@Nullable String externalAccountNo) {
        if (!StringUtils.hasText(externalAccountNo)) {
            return false;
        }
        String compactAccountNo = externalAccountNo.replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT);
        return isRawNumericExternalAccountNo(compactAccountNo) || isValidIbanExternalAccountNo(compactAccountNo);
    }

    private static boolean isRawNumericExternalAccountNo(String compactAccountNo) {
        if (compactAccountNo.length() < MIN_RAW_EXTERNAL_ACCOUNT_LENGTH
                || compactAccountNo.length() > MAX_RAW_EXTERNAL_ACCOUNT_LENGTH) {
            return false;
        }
        for (int i = 0; i < compactAccountNo.length(); i++) {
            if (!Character.isDigit(compactAccountNo.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRawIbanExternalAccountNo(String compactAccountNo) {
        if (compactAccountNo.length() < MIN_RAW_IBAN_LENGTH
                || compactAccountNo.length() > MAX_RAW_EXTERNAL_ACCOUNT_LENGTH) {
            return false;
        }
        for (int i = 0; i < compactAccountNo.length(); i++) {
            char character = compactAccountNo.charAt(i);
            if (i < IBAN_COUNTRY_CODE_LENGTH && !Character.isUpperCase(character)) {
                return false;
            }
            if (i >= IBAN_COUNTRY_CODE_LENGTH
                    && i < IBAN_COUNTRY_CODE_LENGTH + IBAN_CHECK_DIGIT_LENGTH
                    && !Character.isDigit(character)) {
                return false;
            }
            if (i >= IBAN_COUNTRY_CODE_LENGTH + IBAN_CHECK_DIGIT_LENGTH
                    && !Character.isLetterOrDigit(character)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断外部账户上下文中是否包含敏感字段。
     *
     * @param contextVariables 外部账户上下文
     * @return true 表示上下文字段名形似账户号、routing number 或 IBAN，或字段值形似 IBAN
     */
    public static boolean containsSensitiveContextField(@Nullable Map<String, Object> contextVariables) {
        return containsSensitiveField(contextVariables);
    }

    /**
     * 判断外部账户上下文 JSON 中是否包含敏感字段。
     *
     * @param contextVariables 外部账户上下文 JSON
     * @return true 表示上下文字段名形似账户号、routing number 或 IBAN，或字段值形似 IBAN
     */
    public static boolean containsSensitiveContextVariables(@Nullable String contextVariables) {
        if (!StringUtils.hasText(contextVariables)) {
            return false;
        }
        try {
            return containsSensitiveField(JSON.parse(contextVariables));
        } catch (JSONException ignored) {
            return false;
        }
    }

    private static boolean containsSensitiveField(@Nullable Object value) {
        if (value instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (entry.getKey() instanceof String fieldName && isSensitiveContextField(fieldName)) {
                    return true;
                }
                if (containsSensitiveField(entry.getValue())) {
                    return true;
                }
            }
        }
        if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                if (containsSensitiveField(item)) {
                    return true;
                }
            }
        }
        if (value instanceof CharSequence text) {
            return isRawIbanValue(text.toString());
        }
        return false;
    }

    private static boolean isSensitiveContextField(@Nullable String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        return SENSITIVE_CONTEXT_FIELDS.contains(normalized);
    }

    private static boolean isRawIbanValue(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String compactValue = value.replace(" ", "").replace("-", "").toUpperCase(Locale.ROOT);
        return isValidIbanExternalAccountNo(compactValue);
    }

    private static boolean isValidIbanExternalAccountNo(String compactAccountNo) {
        return isRawIbanExternalAccountNo(compactAccountNo) && ibanMod97(compactAccountNo) == IBAN_EXPECTED_REMAINDER;
    }

    private static int ibanMod97(String compactAccountNo) {
        String rearranged = compactAccountNo.substring(IBAN_COUNTRY_CODE_LENGTH + IBAN_CHECK_DIGIT_LENGTH)
                + compactAccountNo.substring(0, IBAN_COUNTRY_CODE_LENGTH + IBAN_CHECK_DIGIT_LENGTH);
        int remainder = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            char character = rearranged.charAt(i);
            if (Character.isDigit(character)) {
                remainder = nextRemainder(remainder, character - '0');
            } else {
                int letterValue = character - 'A' + DECIMAL_RADIX;
                remainder = nextRemainder(remainder, letterValue / DECIMAL_RADIX);
                remainder = nextRemainder(remainder, letterValue % DECIMAL_RADIX);
            }
        }
        return remainder;
    }

    private static int nextRemainder(int remainder, int digit) {
        return (remainder * DECIMAL_RADIX + digit) % IBAN_MODULUS;
    }
}
