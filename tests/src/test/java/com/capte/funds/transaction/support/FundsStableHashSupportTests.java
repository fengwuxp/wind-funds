package com.capte.funds.transaction.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金稳定摘要支撑测试。
 */
class FundsStableHashSupportTests {

    @Test
    void testStableHashMapShouldSortKeysAndRemoveVolatileFieldsRecursively() {
        Map<String, Object> stable = FundsStableHashSupport.stableHashMap(Map.of(
                "traceId", "TRACE-1",
                "b", Map.of(
                        "subjectName", "Alice",
                        "z", 2,
                        "a", 1),
                "a", List.of(
                        Map.of("trace_id", "TRACE-2", "k", "v"),
                        Map.of("traceID", "TRACE-3", "n", 7))));

        assertThat(stable.keySet()).containsExactly("a", "b");
        assertThat(stable)
                .containsEntry("a", List.of(
                        Map.of("k", "v"),
                        Map.of("n", 7)))
                .containsEntry("b", Map.of("a", 1, "z", 2));
    }

    @Test
    void testSha256JsonShouldBeStableForDifferentMapInsertionOrders() {
        String first = FundsStableHashSupport.sha256Json(FundsStableHashSupport.stableHashMap(Map.of(
                "description", "first description",
                "amount", 10,
                "subject", Map.of("id", "user-1", "type", "USER"))));
        String second = FundsStableHashSupport.sha256Json(FundsStableHashSupport.stableHashMap(Map.of(
                "subject", Map.of("type", "USER", "id", "user-1"),
                "amount", 10,
                "description", "second description")));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }
}
