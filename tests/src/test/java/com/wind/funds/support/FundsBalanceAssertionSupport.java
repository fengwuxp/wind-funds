package com.wind.funds.support;

import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.ledger.LedgerBalanceBucket;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金余额和账务计划断言支撑。
 */
public final class FundsBalanceAssertionSupport {

    private static final String LEDGER_TABLE = "t_ledger";

    private static final String LEDGER_TRANSACTION_TABLE = "t_ledger_transaction";

    private static final String LEDGER_POSTING_PLAN_TABLE = "t_ledger_posting_plan";

    private static final String LEDGER_ENTRY_TABLE = "t_ledger_entry";

    private FundsBalanceAssertionSupport() {
        throw new AssertionError();
    }

    public static void assertBucket(FundsSubjectBalanceDTO balance,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    long amount,
                                    CurrencyIsoCode currency) {
        assertThat(balance).as("subject balance").isNotNull();
        assertThat(balance.getSubjectRef()).as("subject ref").isNotNull();
        assertThat(balance.isInitialized()).as("subject %s initialized", balance.getSubjectRef()).isTrue();
        assertThat(balance.getCurrency()).as("balance currency").isEqualTo(currency);
        assertThat(balance.getBalanceBuckets()).as("balance buckets").containsKey(ledgerSubjectCode);
        assertThat(balance.getBalanceBuckets().get(ledgerSubjectCode).balance())
                .as("%s %s bucket balance", balance.getSubjectRef(), ledgerSubjectCode)
                .isEqualTo(Money.immutable(amount, currency));
    }

    public static void assertSubjectBalanceNotInitialized(FundsSubjectBalanceDTO balance) {
        assertThat(balance).as("subject balance").isNotNull();
        assertThat(balance.getSubjectRef()).as("subject ref").isNotNull();
        assertThat(balance.isInitialized()).as("subject %s initialized", balance.getSubjectRef()).isFalse();
        assertThat(balance.getBalanceBuckets()).as("balance buckets").isEmpty();
    }

    public static BalanceSnapshot snapshot(FundsSubjectBalanceDTO... balances) {
        return snapshot(List.of(balances));
    }

    public static BalanceSnapshot snapshot(Collection<FundsSubjectBalanceDTO> balances) {
        Map<BalanceKey, Money> values = new LinkedHashMap<>();
        for (FundsSubjectBalanceDTO balance : balances) {
            assertThat(balance).as("subject balance").isNotNull();
            FundsAccountId subjectRef = balance.getSubjectRef();
            assertThat(subjectRef).as("subject ref").isNotNull();
            Map<LedgerSubjectCode, LedgerBalanceBucket> buckets = balance.getBalanceBuckets();
            if (buckets == null || buckets.isEmpty()) {
                continue;
            }
            for (Map.Entry<LedgerSubjectCode, LedgerBalanceBucket> entry : buckets.entrySet()) {
                LedgerBalanceBucket bucket = entry.getValue();
                assertThat(bucket).as("balance bucket %s", entry.getKey()).isNotNull();
                Money bucketBalance = bucket.balance();
                assertThat(bucketBalance).as("bucket %s balance", entry.getKey()).isNotNull();
                CurrencyIsoCode currency = balance.getCurrency() == null
                        ? bucketBalance.getCurrency()
                        : balance.getCurrency();
                AccountBalancePeriodType periodType = bucket.periodType();
                String periodId = bucket.periodId();
                assertThat(periodType).as("bucket %s period type", entry.getKey()).isNotNull();
                assertThat(periodId).as("bucket %s period id", entry.getKey()).isNotBlank();
                BalanceKey key = BalanceKey.of(subjectRef, entry.getKey(), currency, periodType, periodId);
                assertThat(values.put(key, bucketBalance)).as("duplicate balance key %s", key).isNull();
            }
        }
        return new BalanceSnapshot(values);
    }

    public static LedgerFactSnapshot ledgerFactSnapshot(JdbcTemplate jdbcTemplate) {
        return new LedgerFactSnapshot(
                queryRows(jdbcTemplate, LEDGER_TABLE),
                queryRows(jdbcTemplate, LEDGER_TRANSACTION_TABLE),
                queryRows(jdbcTemplate, LEDGER_POSTING_PLAN_TABLE),
                queryRows(jdbcTemplate, LEDGER_ENTRY_TABLE));
    }

    public static void assertLedgerFactsUnchanged(JdbcTemplate jdbcTemplate, LedgerFactSnapshot expected) {
        assertThat(ledgerFactSnapshot(jdbcTemplate)).isEqualTo(expected);
    }

    public static void assertLedgerTransactionFactsUnchanged(JdbcTemplate jdbcTemplate,
                                                             LedgerFactSnapshot expected) {
        LedgerFactSnapshot actual = ledgerFactSnapshot(jdbcTemplate);
        assertThat(actual.transactions()).as("ledger transactions").isEqualTo(expected.transactions());
        assertThat(actual.postingPlans()).as("ledger posting plans").isEqualTo(expected.postingPlans());
        assertThat(actual.entries()).as("ledger entries").isEqualTo(expected.entries());
    }

    private static List<Map<String, Object>> queryRows(JdbcTemplate jdbcTemplate, String tableName) {
        return immutableRows(jdbcTemplate.queryForList("SELECT * FROM " + tableName + " ORDER BY id ASC"));
    }

    private static List<Map<String, Object>> immutableRows(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> Collections.unmodifiableMap(new LinkedHashMap<>(row)))
                .toList();
    }

    public static void assertOnlyBalanceDeltas(BalanceSnapshot before,
                                               BalanceSnapshot after,
                                               ExpectedBalanceDelta... expectedDeltas) {
        Map<BalanceKey, Long> expectedDeltaMap = new LinkedHashMap<>();
        for (ExpectedBalanceDelta expectedDelta : expectedDeltas) {
            BalanceKey key = BalanceKey.of(expectedDelta.subjectRef(), expectedDelta.ledgerSubjectCode(),
                    expectedDelta.currency(), expectedDelta.periodType(), expectedDelta.periodId());
            assertThat(expectedDeltaMap.put(key, expectedDelta.amountDelta()))
                    .as("duplicate expected balance delta %s", key)
                    .isNull();
        }

        Set<BalanceKey> keys = new LinkedHashSet<>();
        keys.addAll(before.balances().keySet());
        keys.addAll(after.balances().keySet());
        keys.addAll(expectedDeltaMap.keySet());
        for (BalanceKey key : keys) {
            long expectedDelta = expectedDeltaMap.getOrDefault(key, 0L);
            long actualDelta = after.amountOf(key) - before.amountOf(key);
            assertThat(actualDelta).as("balance delta %s", key).isEqualTo(expectedDelta);
        }
    }

    public static ExpectedBalanceDelta delta(FundsAccountId subjectRef,
                                             LedgerSubjectCode ledgerSubjectCode,
                                             long amountDelta,
                                             CurrencyIsoCode currency) {
        return delta(subjectRef, ledgerSubjectCode, amountDelta, currency,
                AccountBalancePeriodType.LIFETIME, AccountBalancePeriodType.LIFETIME.name());
    }

    public static ExpectedBalanceDelta delta(FundsAccountId subjectRef,
                                             LedgerSubjectCode ledgerSubjectCode,
                                             long amountDelta,
                                             CurrencyIsoCode currency,
                                             AccountBalancePeriodType periodType,
                                             String periodId) {
        return new ExpectedBalanceDelta(subjectRef, ledgerSubjectCode, amountDelta, currency, periodType, periodId);
    }

    public static void assertPostingBalanced(LedgerTransactionSpec transaction) {
        assertThat(transaction).as("ledger transaction").isNotNull();
        assertThat(transaction.getPostingPlans()).as("posting plans").isNotEmpty();
        assertThat(transaction.isBalanced()).as("ledger transaction %s balanced", transaction.getSn()).isTrue();
        assertThat(transaction.getTotalDebitAmount())
                .as("transaction debit amount")
                .isEqualTo(transaction.getTotalCreditAmount());
        transaction.getPostingPlans().forEach(FundsBalanceAssertionSupport::assertPostingPlanBalanced);
    }

    public static void assertPostingPlanBalanced(LedgerPostingPlanSpec postingPlan) {
        assertThat(postingPlan).as("posting plan").isNotNull();
        assertThat(postingPlan.getEntries()).as("posting entries").isNotEmpty();
        assertThat(postingPlan.isBalanced()).as("posting plan %s balanced", postingPlan.getPlanId()).isTrue();
        assertThat(postingPlan.getTotalDebitAmount())
                .as("posting plan debit amount")
                .isEqualTo(postingPlan.getTotalCreditAmount());
    }

    public record BalanceSnapshot(Map<BalanceKey, Money> balances) {

        public BalanceSnapshot {
            balances = Map.copyOf(balances);
        }

        public long amountOf(BalanceKey key) {
            Money balance = balances.get(key);
            return balance == null ? 0L : balance.getAmount();
        }
    }

    public record BalanceKey(String subjectId,
                             String subjectType,
                             LedgerSubjectCode ledgerSubjectCode,
                             CurrencyIsoCode currency,
                             AccountBalancePeriodType periodType,
                             String periodId) {

        public static BalanceKey of(FundsAccountId subjectRef,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    CurrencyIsoCode currency,
                                    AccountBalancePeriodType periodType,
                                    String periodId) {
            assertThat(subjectRef).as("subject ref").isNotNull();
            assertThat(currency).as("currency").isNotNull();
            assertThat(periodType).as("period type").isNotNull();
            assertThat(periodId).as("period id").isNotBlank();
            return new BalanceKey(subjectRef.id(), subjectRef.type(), ledgerSubjectCode, currency, periodType, periodId);
        }
    }

    public record ExpectedBalanceDelta(FundsAccountId subjectRef,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       long amountDelta,
                                       CurrencyIsoCode currency,
                                       AccountBalancePeriodType periodType,
                                       String periodId) {
    }

    public record LedgerFactSnapshot(List<Map<String, Object>> ledgers,
                                     List<Map<String, Object>> transactions,
                                     List<Map<String, Object>> postingPlans,
                                     List<Map<String, Object>> entries) {

        public LedgerFactSnapshot {
            ledgers = immutableRows(ledgers);
            transactions = immutableRows(transactions);
            postingPlans = immutableRows(postingPlans);
            entries = immutableRows(entries);
        }
    }
}
