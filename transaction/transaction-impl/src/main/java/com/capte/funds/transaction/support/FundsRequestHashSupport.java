package com.capte.funds.transaction.support;

import com.alibaba.fastjson2.JSON;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 资金请求幂等摘要支撑。
 *
 * <p>用于构造稳定的请求摘要输入：Map key 排序、递归处理嵌套 Map/List，并排除描述、trace 等易变字段。
 * 调用方仍负责选择哪些业务字段进入摘要。</p>
 */
public final class FundsRequestHashSupport {

    private static final String SHA_256_ALGORITHM = "SHA-256";

    private static final Set<String> VOLATILE_REQUEST_HASH_FIELDS = Set.of(
            "description",
            "subjectName",
            "traceId",
            "traceID",
            "trace_id");

    private FundsRequestHashSupport() {
    }

    /**
     * 对对象的稳定 JSON 文本计算 SHA-256 摘要。
     *
     * @param value 摘要输入对象
     * @return 64 位十六进制 SHA-256 摘要
     */
    public static @NonNull String sha256Json(@NonNull Object value) {
        return sha256(JSON.toJSONString(value));
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
                    .map(FundsRequestHashSupport::stableHashValue)
                    .toList();
        }
        return value;
    }
}
