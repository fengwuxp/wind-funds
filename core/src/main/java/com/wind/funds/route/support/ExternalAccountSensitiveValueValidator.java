package com.wind.funds.route.support;

import com.wind.jackson.WindJson;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.lang.reflect.Array;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern RAW_FIELD_NAME_PATTERN = Pattern.compile(
            "(?:\"([^\"]+)\"|(?<![A-Za-z0-9_-])([A-Za-z][A-Za-z0-9_ -]*))\\s*:");

    private static final Pattern RAW_IBAN_FRAGMENT_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9])([A-Za-z]{2}[0-9]{2}[A-Za-z0-9 -]{11,30})(?![A-Za-z0-9])");

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

    private static final Pattern INTERNAL_FUNDS_TRANSACTION_SN_PATTERN = Pattern.compile("FT[0-9]{16}");

    private static final Pattern INTERNAL_FREEZE_ORDER_SN_PATTERN = Pattern.compile("FO[0-9]{16}");

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
        if (externalAccountNo == null || externalAccountNo.isBlank()) {
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
        if (contextVariables == null || contextVariables.isBlank()) {
            return false;
        }
        try {
            return containsSensitiveField(WindJson.parseObject(contextVariables, Object.class));
        } catch (JacksonException ignored) {
            return containsSensitiveRawContextFragment(contextVariables);
        }
    }

    private static boolean containsSensitiveField(@Nullable Object value) {
        return containsSensitiveField(value, null);
    }

    private static boolean containsSensitiveField(@Nullable Object value, @Nullable String ownerFieldName) {
        if (value instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (entry.getKey() instanceof String fieldName && isSensitiveContextField(fieldName)) {
                    return true;
                }
                if (containsSensitiveField(entry.getValue(),
                        entry.getKey() instanceof String fieldName ? fieldName : null)) {
                    return true;
                }
            }
        }
        if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                if (containsSensitiveField(item, null)) {
                    return true;
                }
            }
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (containsSensitiveField(Array.get(value, i), null)) {
                    return true;
                }
            }
        }
        if (value instanceof CharSequence text) {
            String textValue = text.toString();
            return !isInternalReferenceContextValue(ownerFieldName, textValue) && isRawIbanValue(textValue);
        }
        return false;
    }

    private static boolean isSensitiveContextField(@Nullable String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        return SENSITIVE_CONTEXT_FIELDS.contains(normalized);
    }

    private static boolean isInternalReferenceContextValue(@Nullable String fieldName, String value) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        return switch (normalized) {
            case "authorizationtransactionsn", "sourcesn" ->
                    INTERNAL_FUNDS_TRANSACTION_SN_PATTERN.matcher(value).matches();
            case "referencefreezesn" -> INTERNAL_FREEZE_ORDER_SN_PATTERN.matcher(value).matches()
                    || INTERNAL_FUNDS_TRANSACTION_SN_PATTERN.matcher(value).matches();
            default -> false;
        };
    }

    private static boolean containsSensitiveRawContextFragment(String contextVariables) {
        return containsSensitiveRawFieldName(contextVariables) || containsRawIbanFragment(contextVariables);
    }

    private static boolean containsSensitiveRawFieldName(String contextVariables) {
        Matcher matcher = RAW_FIELD_NAME_PATTERN.matcher(contextVariables);
        while (matcher.find()) {
            String fieldName = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            if (isSensitiveContextField(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsRawIbanFragment(String contextVariables) {
        Matcher matcher = RAW_IBAN_FRAGMENT_PATTERN.matcher(contextVariables);
        while (matcher.find()) {
            if (isRawIbanValue(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRawIbanValue(String value) {
        if (value == null || value.isBlank()) {
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
