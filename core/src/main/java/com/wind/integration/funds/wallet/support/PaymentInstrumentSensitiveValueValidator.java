package com.wind.integration.funds.wallet.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    private static final Set<String> SENSITIVE_BINDING_SNAPSHOT_FIELDS = Set.of(
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
        if (!StringUtils.hasText(instrumentNo)) {
            return false;
        }
        String compactInstrumentNo = instrumentNo.replace(" ", "").replace("-", "");
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

    /**
     * 判断绑定快照中是否包含敏感支付工具字段。
     *
     * @param bindingSnapshot 支付工具绑定快照
     * @return true 表示快照字段名形似 CVV、token secret 或密钥
     */
    public static boolean containsSensitiveBindingSnapshotField(@Nullable Map<String, Object> bindingSnapshot) {
        return containsSensitiveField(bindingSnapshot);
    }

    /**
     * 判断扩展上下文 JSON 中是否包含敏感支付工具字段。
     *
     * @param contextVariables 扩展上下文 JSON
     * @return true 表示上下文字段名形似 CVV、token secret 或密钥
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

    /**
     * 判断对象树中是否包含敏感支付工具字段。
     *
     * @param value Map、Iterable 或普通值
     * @return true 表示对象树字段名形似 CVV、token secret 或密钥
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
        return false;
    }

    private static boolean isSensitiveBindingSnapshotField(@Nullable String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.toLowerCase(Locale.ROOT).replaceAll(NON_FIELD_NAME_CHARACTER_PATTERN, "");
        return SENSITIVE_BINDING_SNAPSHOT_FIELDS.contains(normalized);
    }
}
