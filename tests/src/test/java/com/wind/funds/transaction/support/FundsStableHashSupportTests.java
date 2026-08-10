package com.wind.funds.transaction.support;

import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void testSha256JsonShouldKeepLegacyGoldenDigest() {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("amount", 10);
        facts.put("subject", new TreeMap<>(Map.of("type", "USER", "id", "user-1")));

        assertThat(FundsStableHashSupport.sha256Json(facts))
                .isEqualTo("ac05be35455ddb8d7dc6d04ba310c81e6c09ed76b36a651efebd2b0e754db9fc");
    }

    @Test
    void testSha256CanonicalJsonShouldKeepV1GoldenAcrossMapOrderAndNumberScale() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("nullable", null);
        nested.put("active", true);
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("stages", List.of(TestStage.AUTHORIZATION, "CLEARING"));
        first.put("nested", nested);
        first.put("currency", TestCurrency.USD);
        first.put("amount", new BigDecimal("10.00"));
        Map<String, Object> second = Map.of(
                "amount", 10,
                "currency", "USD",
                "nested", nested,
                "stages", List.of("AUTHORIZATION", "CLEARING"));

        String firstDigest = FundsStableHashSupport.sha256CanonicalJson("transaction.request", first);
        String secondDigest = FundsStableHashSupport.sha256CanonicalJson("transaction.request", second);

        assertThat(firstDigest)
                .isEqualTo(secondDigest)
                .isEqualTo("715b8670a4aeaac06164943ec968e994aed696d0a93edbcc5cd532da0eee7c22");
    }

    @Test
    void testSha256CanonicalJsonShouldSeparateDomainAndPreserveListOrder() {
        String transactionDigest = FundsStableHashSupport.sha256CanonicalJson(
                "transaction.request", List.of("DEBIT", "CREDIT"));

        assertThat(FundsStableHashSupport.sha256CanonicalJson(
                "ledger.request", List.of("DEBIT", "CREDIT"))).isNotEqualTo(transactionDigest);
        assertThat(FundsStableHashSupport.sha256CanonicalJson(
                "transaction.request", List.of("CREDIT", "DEBIT"))).isNotEqualTo(transactionDigest);
    }

    @Test
    void testSha256CanonicalJsonShouldEncodeLocalDateTimeAsIsoText() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 17, 10, 0, 0, 123_000_000);

        assertThat(FundsStableHashSupport.sha256CanonicalJson("transaction.request", timestamp))
                .isEqualTo(FundsStableHashSupport.sha256CanonicalJson(
                        "transaction.request", "2026-06-17T10:00:00.123"));
    }

    @Test
    void testSha256CanonicalJsonShouldProjectMoneyToAmountAndCurrency() {
        Money money = Money.immutable(10L, CurrencyIsoCode.USD);

        assertThat(FundsStableHashSupport.sha256CanonicalJson("transaction.request", money))
                .isEqualTo(FundsStableHashSupport.sha256CanonicalJson(
                        "transaction.request", Map.of("amount", 10, "currency", "USD")));
    }

    @Test
    void testSha256CanonicalJsonShouldRejectUnboundedInputs() {
        assertThatThrownBy(() -> FundsStableHashSupport.sha256CanonicalJson(" ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FundsStableHashSupport.sha256CanonicalJson("transaction.request", Set.of("x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FundsStableHashSupport.sha256CanonicalJson(
                "transaction.request", Map.of(1, "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FundsStableHashSupport.sha256CanonicalJson("transaction.request", 1.5D))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testMatchesCanonicalOrLegacyJsonShouldAcceptOnlyKnownDigestVersions() {
        Map<String, Object> facts = Map.of("amount", 10, "currency", "USD");
        String canonical = FundsStableHashSupport.sha256CanonicalJson("transaction.request", facts);
        String legacy = FundsStableHashSupport.sha256Json(facts);

        assertThat(FundsStableHashSupport.matchesCanonicalOrLegacyJson(
                canonical, "transaction.request", facts, facts)).isTrue();
        assertThat(FundsStableHashSupport.matchesCanonicalOrLegacyJson(
                legacy, "transaction.request", facts, facts)).isTrue();
        assertThat(FundsStableHashSupport.matchesCanonicalOrLegacyJson(
                FundsStableHashSupport.sha256("other"), "transaction.request", facts, facts)).isFalse();
    }

    private enum TestCurrency {
        USD
    }

    private enum TestStage {
        AUTHORIZATION
    }
}
