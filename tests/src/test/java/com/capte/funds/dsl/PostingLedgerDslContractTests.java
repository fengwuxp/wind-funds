package com.capte.funds.dsl;

import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Posting/Ledger DSL 契约测试。
 */
class PostingLedgerDslContractTests {

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 5, 20, 10, 0);

    /**
     * 场景：一个 route leg 或控制意图生成一组借贷分录。
     * 预期：PostingPlan 只有在分录非空、金额为正且借贷相等时才算平衡。
     * 红线：空分录、0 金额、负金额或借贷不平不能被误认为可入账计划。
     */
    @Test
    void testPostingPlanShouldRequirePositiveBalancedEntries() {
        LedgerPostingPlanSpec balancedPlan = postingPlan("PLAN-BALANCED",
                entry(EntrySide.DEBIT, 100L),
                entry(EntrySide.CREDIT, 100L));
        LedgerPostingPlanSpec emptyPlan = postingPlan("PLAN-EMPTY");
        LedgerPostingPlanSpec zeroAmountPlan = postingPlan("PLAN-ZERO",
                entry(EntrySide.DEBIT, 0L),
                entry(EntrySide.CREDIT, 0L));
        LedgerPostingPlanSpec negativeAmountPlan = postingPlan("PLAN-NEGATIVE",
                entry(EntrySide.DEBIT, -100L),
                entry(EntrySide.CREDIT, -100L));
        LedgerPostingPlanSpec unbalancedPlan = postingPlan("PLAN-UNBALANCED",
                entry(EntrySide.DEBIT, 100L),
                entry(EntrySide.CREDIT, 80L));

        assertThat(balancedPlan.isBalanced()).isTrue();
        assertThat(emptyPlan.isBalanced()).isFalse();
        assertThat(zeroAmountPlan.isBalanced()).isFalse();
        assertThat(negativeAmountPlan.isBalanced()).isFalse();
        assertThat(unbalancedPlan.isBalanced()).isFalse();
    }

    /**
     * 场景：LedgerEntry 作为最小不可变账务事实进入 PostingPlan。
     * 预期：分录必须具备有效主体、交易流水和正金额。
     * 红线：空白业务标识和非正金额不能靠借贷合计相等伪装成可入账事实。
     */
    @Test
    void testPostingPlanShouldRequireUsableEntryValues() {
        LedgerPostingPlanSpec completePlan = postingPlan("PLAN-COMPLETE",
                entry(EntrySide.DEBIT, 100L),
                entry(EntrySide.CREDIT, 100L));
        LedgerPostingPlanSpec blankSubject = postingPlan("PLAN-BLANK-SUBJECT",
                entry(" ",
                        FundsSubjectType.FUNDING_ACCOUNT.name(),
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.ASSET,
                        "LE-DSL-001",
                        EntrySide.DEBIT,
                        100L),
                entry(EntrySide.CREDIT, 100L));
        LedgerPostingPlanSpec blankSubjectType = postingPlan("PLAN-BLANK-SUBJECT-TYPE",
                entry("FA-DSL-DEBIT",
                        " ",
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.ASSET,
                        "LE-DSL-001",
                        EntrySide.DEBIT,
                        100L),
                entry(EntrySide.CREDIT, 100L));
        LedgerPostingPlanSpec blankLedgerTransactionSn = postingPlan("PLAN-BLANK-TX",
                entry("FA-DSL-DEBIT",
                        FundsSubjectType.FUNDING_ACCOUNT.name(),
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.ASSET,
                        " ",
                        EntrySide.DEBIT,
                        100L),
                entry(EntrySide.CREDIT, 100L));

        assertThat(completePlan.isBalanced()).isTrue();
        assertThat(blankSubject.isBalanced()).isFalse();
        assertThat(blankSubjectType.isBalanced()).isFalse();
        assertThat(blankLedgerTransactionSn.isBalanced()).isFalse();
    }

    /**
     * 场景：整笔账本交易由多个 PostingPlan 组成。
     * 预期：LedgerTransaction 必须同时满足每个计划独立平衡和整笔交易借贷平衡。
     * 红线：多个不平衡计划互相抵消后，不得把整笔交易误判为平衡。
     */
    @Test
    void testLedgerTransactionShouldRequireEveryPostingPlanBalancedIndependently() {
        LedgerPostingPlanSpec debitHeavy = postingPlan("PLAN-DEBIT-HEAVY",
                entry(EntrySide.DEBIT, 100L),
                entry(EntrySide.CREDIT, 80L));
        LedgerPostingPlanSpec creditHeavy = postingPlan("PLAN-CREDIT-HEAVY",
                entry(EntrySide.DEBIT, 80L),
                entry(EntrySide.CREDIT, 100L));
        LedgerTransactionSpec offsetButInvalid = ledgerTransaction(List.of(debitHeavy, creditHeavy));
        LedgerTransactionSpec emptyTransaction = ledgerTransaction(List.of());
        LedgerTransactionSpec balancedTransaction = ledgerTransaction(List.of(postingPlan("PLAN-BALANCED",
                entry(EntrySide.DEBIT, 100L),
                entry(EntrySide.CREDIT, 100L))));

        assertThat(offsetButInvalid.getTotalDebitAmount()).isEqualTo(offsetButInvalid.getTotalCreditAmount());
        assertThat(offsetButInvalid.isBalanced()).isFalse();
        assertThat(emptyTransaction.isBalanced()).isFalse();
        assertThat(balancedTransaction.isBalanced()).isTrue();
    }

    private LedgerPostingPlanSpec postingPlan(String planId, LedgerEntrySpec... entries) {
        return new TestLedgerPostingPlanSpec(planId,
                "LE-DSL-001",
                LedgerPostingIntentType.TRANSFER,
                List.of(new TestLedgerPostingPhaseSpec(LedgerPhaseCode.TRANSFER, List.of(entries))));
    }

    private LedgerEntrySpec entry(EntrySide side, long amount) {
        return entry("FA-DSL-" + side.name(),
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                "LE-DSL-001",
                side,
                amount);
    }

    private LedgerEntrySpec entry(String subjectId,
                                  String subjectType,
                                  LedgerSubjectCode ledgerSubjectCode,
                                  LedgerSubjectCategory ledgerSubjectCategory,
                                  String ledgerTransactionSn,
                                  EntrySide side,
                                  long amount) {
        return new TestLedgerEntrySpec(subjectId,
                subjectType,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                ledgerTransactionSn,
                side,
                Money.immutable(amount, CurrencyIsoCode.USD));
    }

    private LedgerTransactionSpec ledgerTransaction(List<LedgerPostingPlanSpec> postingPlans) {
        return new TestLedgerTransactionSpec(postingPlans);
    }

    private record TestLedgerPostingPhaseSpec(LedgerPhaseCode phaseCode,
                                              List<LedgerEntrySpec> entries) implements LedgerPostingPhaseSpec {

        private TestLedgerPostingPhaseSpec {
            entries = List.copyOf(entries);
        }

        @Override
        public LedgerPhaseCode getPhaseCode() {
            return phaseCode;
        }

        @Override
        public List<LedgerEntrySpec> getEntries() {
            return entries;
        }
    }

    private record TestLedgerPostingPlanSpec(String planId,
                                             String ledgerTransactionSn,
                                             LedgerPostingIntentType intent,
                                             List<LedgerPostingPhaseSpec> postingPhases)
            implements LedgerPostingPlanSpec {

        private TestLedgerPostingPlanSpec {
            postingPhases = List.copyOf(postingPhases);
        }

        @Override
        public String getPlanId() {
            return planId;
        }

        @Override
        public String getLedgerTransactionSn() {
            return ledgerTransactionSn;
        }

        @Override
        public LedgerPostingIntentType getIntent() {
            return intent;
        }

        @Override
        public List<LedgerPostingPhaseSpec> getPostingPhases() {
            return postingPhases;
        }
    }

    private record TestLedgerEntrySpec(String subjectId,
                                       String subjectType,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       LedgerSubjectCategory ledgerSubjectCategory,
                                       String ledgerTransactionSn,
                                       EntrySide entryType,
                                       Money amount) implements LedgerEntrySpec {

        @Override
        public String getSubjectId() {
            return subjectId;
        }

        @Override
        public String getSubjectType() {
            return subjectType;
        }

        @Override
        public LedgerSubjectCode getLedgerSubjectCode() {
            return ledgerSubjectCode;
        }

        @Override
        public LedgerSubjectCategory getLedgerSubjectCategory() {
            return ledgerSubjectCategory;
        }

        @Override
        public String getLedgerTransactionSn() {
            return ledgerTransactionSn;
        }

        @Override
        public EntrySide getEntryType() {
            return entryType;
        }

        @Override
        public String getBusinessScene() {
            return "POSTING_LEDGER_DSL";
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-POSTING-DSL-001";
        }

        @Override
        public Money getAmount() {
            return amount;
        }

        @Override
        public Money getOriginalAmount() {
            return amount;
        }

        @Override
        public BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
        }

        @Override
        public String getDescription() {
            return "posting ledger dsl contract";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    private record TestLedgerTransactionSpec(List<LedgerPostingPlanSpec> postingPlans)
            implements LedgerTransactionSpec {

        private TestLedgerTransactionSpec {
            postingPlans = List.copyOf(postingPlans);
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public String getSn() {
            return "LE-DSL-001";
        }

        @Override
        public FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TRANSFER;
        }

        @Override
        public LedgerTransactionStatus getStatus() {
            return LedgerTransactionStatus.POSTED;
        }

        @Override
        public Money getAmount() {
            return Money.immutable(100L, CurrencyIsoCode.USD);
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-POSTING-DSL-001";
        }

        @Override
        public DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public String getBusinessScene() {
            return "POSTING_LEDGER_DSL";
        }

        @Override
        public String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
        }

        @Override
        public String getDescription() {
            return "posting ledger dsl contract";
        }

        @Override
        public List<LedgerPostingPlanSpec> getPostingPlans() {
            return postingPlans;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }
}
