package com.capte.funds.support;

import com.capte.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.integration.funds.ledger.LedgerBalanceBucket;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;

import java.util.Collection;
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
                BalanceKey key = BalanceKey.of(subjectRef, entry.getKey(), currency);
                assertThat(values.put(key, bucketBalance)).as("duplicate balance key %s", key).isNull();
            }
        }
        return new BalanceSnapshot(values);
    }

    public static void assertOnlyBalanceDeltas(BalanceSnapshot before,
                                               BalanceSnapshot after,
                                               ExpectedBalanceDelta... expectedDeltas) {
        Map<BalanceKey, Long> expectedDeltaMap = new LinkedHashMap<>();
        for (ExpectedBalanceDelta expectedDelta : expectedDeltas) {
            BalanceKey key = BalanceKey.of(expectedDelta.subjectRef(), expectedDelta.ledgerSubjectCode(),
                    expectedDelta.currency());
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
        return new ExpectedBalanceDelta(subjectRef, ledgerSubjectCode, amountDelta, currency);
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
                             CurrencyIsoCode currency) {

        public static BalanceKey of(FundsAccountId subjectRef,
                                    LedgerSubjectCode ledgerSubjectCode,
                                    CurrencyIsoCode currency) {
            assertThat(subjectRef).as("subject ref").isNotNull();
            assertThat(currency).as("currency").isNotNull();
            return new BalanceKey(subjectRef.id(), subjectRef.type(), ledgerSubjectCode, currency);
        }
    }

    public record ExpectedBalanceDelta(FundsAccountId subjectRef,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       long amountDelta,
                                       CurrencyIsoCode currency) {
    }
}
