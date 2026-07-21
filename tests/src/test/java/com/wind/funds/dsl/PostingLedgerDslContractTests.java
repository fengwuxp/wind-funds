package com.wind.funds.dsl;

import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.spec.ledger.LedgerEntrySpec;
import com.wind.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsInstructionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.ledger.posting.LedgerTransactionSpecFactory.DefaultLedgerTransactionSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Posting/Ledger DSL 契约测试。
 */
class PostingLedgerDslContractTests {

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 5, 20, 10, 0);

    private static final String SPEND_CONTROL_SCOPE_SUBJECT_TYPE = "SPEND_CONTROL_SCOPE";

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
     * 场景：账户层级由资金交易的 RouteSnapshot 负责冻结和回放。
     * 预期：LedgerEntry 只保存账务事实，不重复暴露账户层级快照或不可解析的快照引用。
     * 红线：不能把路由决策证据复制进分录契约，形成两个可能漂移的历史事实源。
     */
    @Test
    void testLedgerEntryShouldNotOwnRouteHierarchySnapshot() {
        assertThat(LedgerEntrySpec.class.getMethods())
                .extracting(Method::getName)
                .doesNotContain("getAccountHierarchySnapshot", "getHierarchySnapshotRef");
    }

    /**
     * 场景：外部模块自行实现账务分录契约。
     * 预期：postingRole 必须由实现方显式声明，不允许默认 DETAIL。
     * 红线：多级账户父级控制写、转移写和汇总视图不能因遗漏字段被误记为明细入账。
     */
    @Test
    void testLedgerEntryPostingRoleShouldRequireExplicitImplementation() throws NoSuchMethodException {
        Method postingRoleMethod = LedgerEntrySpec.class.getMethod("getPostingRole");

        assertThat(isDefaultMethod(postingRoleMethod)).isFalse();
    }

    /**
     * 场景：父级账户或汇总视图产生 AGGREGATE_VIEW 观察分录。
     * 预期：汇总视图只能用于查询/投影，不得作为真实账本分录进入可入账计划。
     * 红线：多级账户不能通过父级汇总视图重复入账，制造虚假借贷平衡。
     */
    @Test
    void testPostingPlanShouldRejectAggregateViewEntries() {
        LedgerPostingPlanSpec aggregateViewPlan = postingPlan("PLAN-AGGREGATE-VIEW",
                entry(EntrySide.DEBIT, 100L, LedgerPostingRole.AGGREGATE_VIEW),
                entry(EntrySide.CREDIT, 100L, LedgerPostingRole.AGGREGATE_VIEW));

        assertThat(aggregateViewPlan.isBalanced()).isFalse();
    }

    /**
     * 场景：支出控制范围兼容主体被误写入 PostingPlan。
     * 预期：借贷金额相等也不能被视为可入账计划。
     * 红线：支出控制范围不能绕过交易路由进入账本分录。
     */
    @Test
    void testPostingPlanShouldRejectSpendControlScopeEntrySubject() {
        LedgerPostingPlanSpec spendControlScopePlan = postingPlan("PLAN-BUDGET-GROUP",
                entry("BG-CONTROL-DEBIT",
                        SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.MEMO,
                        "LE-DSL-001",
                        EntrySide.DEBIT,
                        100L),
                entry("BG-CONTROL-CREDIT",
                        SPEND_CONTROL_SCOPE_SUBJECT_TYPE,
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.MEMO,
                        "LE-DSL-001",
                        EntrySide.CREDIT,
                        100L));

        assertThat(spendControlScopePlan.isBalanced()).isFalse();
    }

    /**
     * 场景：PostingPlan 中出现跨币种账务分录。
     * 预期：借贷两侧同金额但币种不同不能被判定为平衡；同侧跨币种累计必须显式拒绝。
     * 红线：错币种账务计划不得进入可入账状态，也不能通过金额数值相等掩盖币种不一致。
     */
    @Test
    void testPostingPlanShouldRejectCrossCurrencyEntries() {
        LedgerPostingPlanSpec crossCurrencySides = postingPlan("PLAN-CROSS-CURRENCY-SIDES",
                entry(EntrySide.DEBIT, 100L, CurrencyIsoCode.USD),
                entry(EntrySide.CREDIT, 100L, CurrencyIsoCode.CNY));
        LedgerPostingPlanSpec crossCurrencyDebitAggregation = postingPlan("PLAN-CROSS-CURRENCY-DEBIT",
                entry("FA-DSL-DEBIT-USD",
                        FundsSubjectType.FUNDING_ACCOUNT.name(),
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.ASSET,
                        "LE-DSL-001",
                        EntrySide.DEBIT,
                        Money.immutable(60L, CurrencyIsoCode.USD)),
                entry("FA-DSL-DEBIT-CNY",
                        FundsSubjectType.FUNDING_ACCOUNT.name(),
                        LedgerSubjectCode.AVAILABLE,
                        LedgerSubjectCategory.ASSET,
                        "LE-DSL-001",
                        EntrySide.DEBIT,
                        Money.immutable(40L, CurrencyIsoCode.CNY)),
                entry(EntrySide.CREDIT, 100L, CurrencyIsoCode.USD));

        assertThat(crossCurrencySides.isBalanced()).isFalse();
        assertThatThrownBy(crossCurrencyDebitAggregation::isBalanced)
                .hasMessageContaining("currency mismatch");
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

    /**
     * 场景：外部调用方直接构造 LedgerTransactionSpec，随后继续改写原始嵌套上下文。
     * 预期：已构造的账本交易事实保持稳定，不被追加的支付工具原文字段污染。
     * 红线：账本交易事实不能因浅拷贝让 PAN、密钥或外部账户原文进入落库链路。
     */
    @Test
    void testLedgerTransactionShouldDefensivelyCopyNestedContextVariables() {
        Map<String, Object> processorPayload = new HashMap<>();
        processorPayload.put("traceId", "LEDGER-TX-CONTEXT-001");
        LedgerTransactionSpec transaction = DefaultLedgerTransactionSpec.builder()
                .sn("LE-DSL-CONTEXT-001")
                .tenantId(1L)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .fundsTransactionSn("FUNDS-TX-CONTEXT-001")
                .eventType(FundsTransactionEventType.TRANSFER)
                .transactionType(DefaultFundsTransactionType.TRANSFER)
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(100L, CurrencyIsoCode.USD))
                .exchangeRate(BigDecimal.ONE)
                .businessSn("BIZ-POSTING-DSL-001")
                .businessScene("POSTING_LEDGER_DSL")
                .transactionTime(TRANSACTION_TIME)
                .postingPlans(List.of(postingPlan("PLAN-CONTEXT-BALANCED",
                        entry(EntrySide.DEBIT, 100L),
                        entry(EntrySide.CREDIT, 100L))))
                .contextVariables(Map.of("processorPayload", processorPayload))
                .build();

        processorPayload.put("pan", "PAN_AFTER_LEDGER_TRANSACTION_SHOULD_NOT_LEAK");

        Object payloadValue = transaction.getContextVariables().get("processorPayload");
        assertThat(payloadValue).isInstanceOf(Map.class);
        Map<?, ?> payload = (Map<?, ?>) payloadValue;
        assertThat(payload.get("traceId")).isEqualTo("LEDGER-TX-CONTEXT-001");
        assertThat(payload.containsKey("pan")).isFalse();
    }

    private LedgerPostingPlanSpec postingPlan(String planId, LedgerEntrySpec... entries) {
        return new TestLedgerPostingPlanSpec(planId,
                "LE-DSL-001",
                LedgerPostingIntentType.TRANSFER,
                List.of(new TestLedgerPostingPhaseSpec(LedgerPhaseCode.TRANSFER, List.of(entries))));
    }

    private LedgerEntrySpec entry(EntrySide side, long amount) {
        return entry(side, amount, CurrencyIsoCode.USD);
    }

    private LedgerEntrySpec entry(EntrySide side, long amount, LedgerPostingRole postingRole) {
        return entry("FA-DSL-" + side.name(),
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                "LE-DSL-001",
                side,
                Money.immutable(amount, CurrencyIsoCode.USD),
                postingRole);
    }

    private LedgerEntrySpec entry(EntrySide side, long amount, CurrencyIsoCode currency) {
        return entry("FA-DSL-" + side.name(),
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                "LE-DSL-001",
                side,
                Money.immutable(amount, currency));
    }

    private LedgerEntrySpec entry(String subjectId,
                                  String subjectType,
                                  LedgerSubjectCode ledgerSubjectCode,
                                  LedgerSubjectCategory ledgerSubjectCategory,
                                  String ledgerTransactionSn,
                                  EntrySide side,
                                  long amount) {
        return entry(subjectId,
                subjectType,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                ledgerTransactionSn,
                side,
                Money.immutable(amount, CurrencyIsoCode.USD));
    }

    private LedgerEntrySpec entry(String subjectId,
                                  String subjectType,
                                  LedgerSubjectCode ledgerSubjectCode,
                                  LedgerSubjectCategory ledgerSubjectCategory,
                                  String ledgerTransactionSn,
                                  EntrySide side,
                                  Money amount) {
        return entry(subjectId,
                subjectType,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                ledgerTransactionSn,
                side,
                amount,
                LedgerPostingRole.DETAIL);
    }

    private LedgerEntrySpec entry(String subjectId,
                                  String subjectType,
                                  LedgerSubjectCode ledgerSubjectCode,
                                  LedgerSubjectCategory ledgerSubjectCategory,
                                  String ledgerTransactionSn,
                                  EntrySide side,
                                  Money amount,
                                  LedgerPostingRole postingRole) {
        return new TestLedgerEntrySpec(subjectId,
                subjectType,
                ledgerSubjectCode,
                ledgerSubjectCategory,
                ledgerTransactionSn,
                side,
                amount,
                postingRole);
    }

    private LedgerTransactionSpec ledgerTransaction(List<LedgerPostingPlanSpec> postingPlans) {
        return new TestLedgerTransactionSpec(postingPlans);
    }

    private boolean isDefaultMethod(Method method) {
        return !Modifier.isAbstract(method.getModifiers());
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
                                       Money amount,
                                       LedgerPostingRole postingRole) implements LedgerEntrySpec {

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
        public LedgerPostingRole getPostingRole() {
            return postingRole;
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
