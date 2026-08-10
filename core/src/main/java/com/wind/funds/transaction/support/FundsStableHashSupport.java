package com.wind.funds.transaction.support;

import com.wind.jackson.WindJson;
import com.wind.transaction.core.Money;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 资金稳定摘要支撑。
 *
 * <p>用于构造稳定摘要输入：Map key 排序、递归处理嵌套 Map/List，并排除描述、trace 等易变字段。
 * 调用方仍负责选择哪些业务字段进入摘要。</p>
 */
public final class FundsStableHashSupport {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    private static final String CANONICAL_DIGEST_VERSION = "v1";

    private static final String HEX_DIGITS = "0123456789abcdef";

    private static final Set<String> VOLATILE_REQUEST_HASH_FIELDS = Set.of(
            "description",
            "subjectName",
            "traceId",
            "traceID",
            "trace_id");

    private FundsStableHashSupport() {
    }

    /**
     * 对对象的稳定 JSON 文本计算 SHA-256 摘要。
     *
     * @param value 摘要输入对象
     * @return 64 位十六进制 SHA-256 摘要
     */
    public static @NonNull String sha256Json(@NonNull Object value) {
        return sha256(WindJson.toJsonString(value));
    }

    /**
     * 对有界业务事实计算 canonical v1 SHA-256 摘要。
     *
     * <p>摘要前像固定包含 domain、payload 和 version。payload 只接受无环的 String-key Map、List、
     * String、Boolean、整数、BigDecimal、Money、LocalDateTime、enum 和 null；Map key 按自然顺序排列，
     * BigDecimal 去除无意义尾零，Money 固定投影 amount/currency，LocalDateTime 使用 ISO 文本。</p>
     *
     * @param domain 摘要业务域，不得为空白
     * @param value  摘要输入事实
     * @return 64 位十六进制 SHA-256 摘要
     */
    public static @NonNull String sha256CanonicalJson(@NonNull String domain, @Nullable Object value) {
        if (domain.isBlank()) {
            throw new IllegalArgumentException("Canonical digest domain must not be blank");
        }
        StringBuilder canonical = new StringBuilder();
        canonical.append("{\"domain\":");
        appendCanonicalString(canonical, domain);
        canonical.append(",\"payload\":");
        appendCanonicalValue(canonical, value);
        canonical.append(",\"version\":\"").append(CANONICAL_DIGEST_VERSION).append("\"}");
        return sha256(canonical.toString());
    }

    /**
     * 判断已持久化摘要是否匹配 canonical v1 或 legacy JSON 摘要。
     *
     * @param storedDigest   已持久化摘要
     * @param domain         canonical v1 业务域
     * @param canonicalValue canonical v1 输入事实
     * @param legacyValue    legacy JSON 输入事实
     * @return 是否命中任一已知版本
     */
    public static boolean matchesCanonicalOrLegacyJson(@Nullable String storedDigest,
                                                       @NonNull String domain,
                                                       @Nullable Object canonicalValue,
                                                       @NonNull Object legacyValue) {
        return sha256CanonicalJson(domain, canonicalValue).equals(storedDigest)
                || sha256Json(legacyValue).equals(storedDigest);
    }

    /**
     * 对文本计算 SHA-256 摘要。
     *
     * @param text 摘要输入文本
     * @return 64 位十六进制 SHA-256 摘要
     */
    public static @NonNull String sha256(@NonNull String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", exception);
        }
    }

    /**
     * 返回稳定排序且过滤易变字段后的 Map。
     *
     * @param values 原始请求摘要字段
     * @return 可用于 JSON 序列化和摘要计算的稳定 Map
     */
    public static @NonNull Map<String, Object> stableHashMap(@Nullable Map<?, ?> values) {
        Map<String, Object> result = new TreeMap<>();
        if (values == null || values.isEmpty()) {
            return result;
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (VOLATILE_REQUEST_HASH_FIELDS.contains(key)) {
                continue;
            }
            result.put(key, stableHashValue(entry.getValue()));
        }
        return result;
    }

    private static Object stableHashValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return stableHashMap(map);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(FundsStableHashSupport::stableHashValue)
                    .toList();
        }
        return value;
    }

    private static void appendCanonicalValue(StringBuilder target, @Nullable Object value) {
        if (value == null) {
            target.append("null");
        } else if (value instanceof String text) {
            appendCanonicalString(target, text);
        } else if (value instanceof Boolean bool) {
            target.append(bool);
        } else if (value instanceof Enum<?> enumValue) {
            appendCanonicalString(target, enumValue.name());
        } else if (value instanceof LocalDateTime dateTime) {
            appendCanonicalString(target, dateTime.toString());
        } else if (value instanceof Money money) {
            target.append("{\"amount\":");
            appendCanonicalValue(target, money.getAmount());
            target.append(",\"currency\":");
            appendCanonicalString(target, money.getCurrency().name());
            target.append('}');
        } else if (value instanceof BigDecimal decimal) {
            target.append(decimal.stripTrailingZeros().toPlainString());
        } else if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long || value instanceof BigInteger) {
            target.append(value);
        } else if (value instanceof Map<?, ?> map) {
            appendCanonicalMap(target, map);
        } else if (value instanceof List<?> list) {
            appendCanonicalList(target, list);
        } else {
            throw new IllegalArgumentException("Unsupported canonical digest value type: "
                    + value.getClass().getName());
        }
    }

    private static void appendCanonicalMap(StringBuilder target, Map<?, ?> values) {
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Canonical digest Map keys must be String");
            }
            sorted.put(key, entry.getValue());
        }
        target.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (!first) {
                target.append(',');
            }
            first = false;
            appendCanonicalString(target, entry.getKey());
            target.append(':');
            appendCanonicalValue(target, entry.getValue());
        }
        target.append('}');
    }

    private static void appendCanonicalList(StringBuilder target, List<?> values) {
        target.append('[');
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                target.append(',');
            }
            appendCanonicalValue(target, values.get(index));
        }
        target.append(']');
    }

    private static void appendCanonicalString(StringBuilder target, String value) {
        target.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> target.append("\\\"");
                case '\\' -> target.append("\\\\");
                case '\b' -> target.append("\\b");
                case '\f' -> target.append("\\f");
                case '\n' -> target.append("\\n");
                case '\r' -> target.append("\\r");
                case '\t' -> target.append("\\t");
                default -> appendCanonicalCharacter(target, value, index, character);
            }
            if (Character.isHighSurrogate(character)) {
                index++;
            }
        }
        target.append('"');
    }

    private static void appendCanonicalCharacter(StringBuilder target, String value, int index, char character) {
        if (character < 0x20) {
            target.append("\\u00")
                    .append(HEX_DIGITS.charAt(character >>> 4))
                    .append(HEX_DIGITS.charAt(character & 0x0f));
            return;
        }
        if (Character.isHighSurrogate(character)) {
            if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                throw new IllegalArgumentException("Canonical digest String contains an unpaired surrogate");
            }
            target.append(character).append(value.charAt(index + 1));
            return;
        }
        if (Character.isLowSurrogate(character)) {
            throw new IllegalArgumentException("Canonical digest String contains an unpaired surrogate");
        }
        target.append(character);
    }
}
