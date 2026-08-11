package com.wind.funds.transaction.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 资金 DSL 扩展上下文快照工具。
 */
public final class FundsContextVariables {

    /** 调账增加方向。 */
    public static final String INCREASE = "increase";

    /** 允许调账后出现负余额。 */
    public static final String ALLOW_NEGATIVE_BALANCE = "allowNegativeBalance";

    private FundsContextVariables() {
    }

    /**
     * 创建资金 DSL 扩展上下文不可变快照，递归复制嵌套 Map、Collection 和数组。
     *
     * @param contextVariables 扩展上下文
     * @return 不可变扩展上下文
     */
    public static Map<String, Object> immutableCopy(@Nullable Map<String, Object> contextVariables) {
        if (contextVariables == null || contextVariables.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>(contextVariables.size());
        for (Map.Entry<String, Object> entry : contextVariables.entrySet()) {
            copied.put(requireContextKey(entry.getKey()), immutableValue(entry.getValue()));
        }
        return Map.copyOf(copied);
    }

    private static Object immutableValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> map) {
            return immutableMap(map);
        }
        if (value instanceof Collection<?> collection) {
            return immutableCollection(collection);
        }
        if (value != null && value.getClass().isArray()) {
            return immutableArray(value);
        }
        return value;
    }

    private static Map<String, Object> immutableMap(Map<?, ?> value) {
        if (value.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> copied = new LinkedHashMap<>(value.size());
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            copied.put(requireContextKey(entry.getKey()), immutableValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copied);
    }

    private static String requireContextKey(@Nullable Object key) {
        if (key instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("contextVariables key must be text");
    }

    private static Collection<Object> immutableCollection(Collection<?> value) {
        if (value.isEmpty()) {
            return List.of();
        }
        if (value instanceof Set<?>) {
            Set<Object> copied = new LinkedHashSet<>(value.size());
            for (Object item : value) {
                copied.add(immutableValue(item));
            }
            return Collections.unmodifiableSet(copied);
        }
        List<Object> copied = new ArrayList<>(value.size());
        for (Object item : value) {
            copied.add(immutableValue(item));
        }
        return Collections.unmodifiableList(copied);
    }

    private static List<Object> immutableArray(Object value) {
        int length = Array.getLength(value);
        if (length == 0) {
            return List.of();
        }
        List<Object> copied = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            copied.add(immutableValue(Array.get(value, index)));
        }
        return Collections.unmodifiableList(copied);
    }
}
