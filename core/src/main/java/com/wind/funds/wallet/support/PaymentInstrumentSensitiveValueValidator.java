package com.wind.funds.wallet.support;

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
 * 支付工具敏感值校验器。
 *
 * <p>用于阻断银行卡号等原始敏感号进入资金 DSL 快照、支付工具存储或日志链路。
 * 允许使用脱敏展示号、token 或外部稳定引用。</p>
 */
public final class PaymentInstrumentSensitiveValueValidator {

    private static final int MIN_RAW_PAN_LENGTH = 12;

    private static final int MAX_RAW_PAN_LENGTH = 19;

    private static final String NON_FIELD_NAME_CHARACTER_PATTERN = "[^a-z0-9]";

    private static final Pattern RAW_FIELD_NAME_PATTERN = Pattern.compile(
            "(?:\"([^\"]+)\"|(?<![A-Za-z0-9_-])([A-Za-z][A-Za-z0-9_ -]*))\\s*:");

    private static final Pattern RAW_PAN_FRAGMENT_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9])([0-9][0-9 -]{10,}[0-9])(?![A-Za-z0-9])");

    private static final Set<String> SENSITIVE_BINDING_SNAPSHOT_FIELDS = Set.of(
            "pan",
            "cardno",
            "cardnumber",
            "fullcardnumber",
            "primaryaccountnumber",
            "paymentaccountnumber",
            "cvv",
            "cvv2",
            "cvc",
            "cvc2",
            "securitycode",
            "cardsecuritycode",
            "tokensecret",
            "secret",
            "secretkey",
            "privatekey");

    private PaymentInstrumentSensitiveValueValidator() {
        throw new AssertionError();
    }

    /**
     * 判断输入是否形似原始 PAN。
     *
     * @param instrumentNo 支付工具展示号、token 或原始号候选
     * @return true 表示输入为 12 到 19 位数字，可带空格或短横线分隔
     */
    public static boolean isRawSensitiveInstrumentNo(@Nullable String instrumentNo) {
        if (instrumentNo == null || instrumentNo.isBlank()) {
            return false;
        }
        String compactInstrumentNo = instrumentNo.replace(" ", "").replace("-", "");
        return isRawPanCandidate(compactInstrumentNo);
    }

    /**
     * 判断绑定快照中是否包含敏感支付工具字段。
     *
     * @param bindingSnapshot 支付工具绑定快照
     * @return true 表示快照字段名形似 CVV、token secret 或密钥，或字段值形似原始 PAN
     */
    public static boolean containsSensitiveBindingSnapshotField(@Nullable Map<String, Object> bindingSnapshot) {
        return containsSensitiveField(bindingSnapshot);
    }

    /**
     * 判断扩展上下文 JSON 中是否包含敏感支付工具字段。
     *
     * @param contextVariables 扩展上下文 JSON
     * @return true 表示上下文字段名形似 CVV、token secret 或密钥，或字段值形似原始 PAN
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

    /**
     * 判断对象树中是否包含敏感支付工具字段。
     *
     * @param value Map、Iterable 或普通值
     * @return true 表示对象树字段名形似 CVV、token secret 或密钥，或字段值形似原始 PAN
     */
    public static boolean containsSensitiveField(@Nullable Object value) {
        if (value instanceof Map<?, ?> values) {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (entry.getKey() instanceof String fieldName && isSensitiveBindingSnapshotField(fieldName)) {
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
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (containsSensitiveField(Array.get(value, i))) {
                    return true;
                }
            }
        }
        if (value instanceof CharSequence text) {
            return isRawPanValue(text.toString());
        }
        if (value instanceof Number number) {
            return isRawPanValue(number.toString());
        }
        return false;
    }

    private static boolean isSensitiveBindingSnapshotField(@Nullable String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        return SENSITIVE_BINDING_SNAPSHOT_FIELDS.contains(normalized);
    }

    private static boolean containsSensitiveRawContextFragment(String contextVariables) {
        return containsSensitiveRawFieldName(contextVariables) || containsRawPanFragment(contextVariables);
    }

    private static boolean containsSensitiveRawFieldName(String contextVariables) {
        Matcher matcher = RAW_FIELD_NAME_PATTERN.matcher(contextVariables);
        while (matcher.find()) {
            String fieldName = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            if (isSensitiveBindingSnapshotField(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsRawPanFragment(String contextVariables) {
        Matcher matcher = RAW_PAN_FRAGMENT_PATTERN.matcher(contextVariables);
        while (matcher.find()) {
            if (isRawPanValue(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRawPanValue(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String compactValue = value.replace(" ", "").replace("-", "");
        return isRawPanCandidate(compactValue) && isLuhnValid(compactValue);
    }

    private static boolean isRawPanCandidate(String compactInstrumentNo) {
        if (compactInstrumentNo.length() < MIN_RAW_PAN_LENGTH
                || compactInstrumentNo.length() > MAX_RAW_PAN_LENGTH) {
            return false;
        }
        for (int i = 0; i < compactInstrumentNo.length(); i++) {
            if (!Character.isDigit(compactInstrumentNo.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLuhnValid(String compactInstrumentNo) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = compactInstrumentNo.length() - 1; i >= 0; i--) {
            int digit = compactInstrumentNo.charAt(i) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
