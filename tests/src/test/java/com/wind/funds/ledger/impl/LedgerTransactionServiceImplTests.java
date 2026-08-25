package com.wind.funds.ledger.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.dto.LedgerTransactionPostResult;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerPhaseCode;
import com.wind.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.funds.ledger.enums.LedgerPostingRole;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.ledger.spec.LedgerEntrySpec;
import com.wind.funds.ledger.spec.LedgerPostingPhaseSpec;
import com.wind.funds.ledger.spec.LedgerPostingPlanSpec;
import com.wind.funds.ledger.spec.LedgerTransactionSpec;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 账本交易服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        LedgerTransactionServiceImplTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LedgerTransactionServiceImplTests extends AbstractFundsServiceTest {

    private static final LocalDateTime TRANSACTION_TIME = LocalDateTime.of(2026, 5, 27, 10, 0);

    private static final LocalDateTime NANOS_TRANSACTION_TIME =
            LocalDateTime.of(2026, 5, 27, 10, 0, 0, 987_654_321);

    private static final String LEDGER_TRANSACTION_DIGEST_DOMAIN = "ledger.persisted-transaction.v1";

    private static final String LEDGER_POSTING_PLAN_DIGEST_DOMAIN = "ledger.persisted-plan.v1";

    private static final String LEDGER_ENTRY_DIGEST_DOMAIN = "ledger.persisted-entry.v1";

    private static final String CORE1B_LEDGER_LEGACY_DIGEST =
            "50c63ea9e47976aae601f54b38e0ca92128ed454411ee270621ddf880068a71c";

    private static final Map<String, Object> SENSITIVE_ITERABLE_CONTEXT_VARIABLES =
            Map.of("processorPayload", List.of("trace-ref", "4111111111111111"));

    private static final Map<String, Object> CORE_BENEFIT_CONTEXT_VARIABLES =
            Map.of("benefitPayload", Map.of(
                    "amount", Money.immutable(2000L, CurrencyIsoCode.USD),
                    "fundingNature", "PLATFORM_BORNE"));

    private static final Map<String, Object> CORE_BENEFIT_ITERABLE_CONTEXT_VARIABLES =
            Map.of("benefitDecisionTrace", List.of(Map.of("currentMarketingRule", "latest-rule")));

    @Autowired
    private LedgerTransactionServiceImpl ledgerTransactionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpLedgerTransactionServiceTestData() {
        cleanupLedgerContextFacts();
    }

    @AfterEach
    void tearDownLedgerTransactionServiceTestData() {
        cleanupLedgerContextFacts();
    }

    /**
     * 场景：历史 ledger transaction 已保存 legacy 摘要，当前版本收到相同请求。
     * 输入：先由当前 writer 写入 canonical v1，再把持久化摘要替换为基线旧 writer 的固定 golden。
     * 输出：同请求因 legacy 摘要被拒绝，legacy 摘要和全部账务事实均保持不变。
     * 红线：目标 persisted contract 不得兼容、回填或重写历史摘要。
     */
    @Test
    void testPersistedLegacyLedgerDigestShouldFailClosedWithoutChangingFacts() {
        LedgerTransactionSpec transaction = ledgerTransaction(Map.of(), Map.of(), Map.of());

        ledgerTransactionService.postLedgerTransaction(transaction);

        assertThat(persistedLedgerDigest()).isNotBlank().isNotEqualTo(CORE1B_LEDGER_LEGACY_DIGEST);
        assertThat(countFacts("t_ledger_transaction", "sn")).isEqualTo(1L);
        assertThat(countFacts("t_ledger_posting_plan", "ledger_transaction_sn")).isEqualTo(1L);
        assertThat(countFacts("t_ledger_entry", "ledger_transaction_sn")).isEqualTo(2L);
        assertThat(jdbcTemplate.update("UPDATE t_ledger_transaction SET sha256 = ? WHERE sn = ?",
                CORE1B_LEDGER_LEGACY_DIGEST, "LE_LEDGER_CONTEXT_001")).isEqualTo(1);
        LedgerFactSnapshot persistedLegacyFacts = ledgerFactSnapshot(jdbcTemplate);

        Throwable failure = catchThrowable(() -> ledgerTransactionService.postLedgerTransaction(transaction));
        assertThat(failure).as("ledger legacy digest must be rejected").isNotNull();
        assertThat(failure).hasMessageContaining("摘要不一致");
        assertThat(persistedLedgerDigest()).isEqualTo(CORE1B_LEDGER_LEGACY_DIGEST);
        assertLedgerFactsUnchanged(jdbcTemplate, persistedLegacyFacts);
    }

    /**
     * 场景：请求时间含 nanos，汇率以不同 decimal scale 重放。
     * 输入：真实 writer 写入后，从 transaction/plan/entry 持久化列重建唯一 v1 摘要。
     * 输出：时间统一截断到秒，1 与 1.00000000 同义重放，非等值汇率冲突。
     * 红线：摘要不得依赖写前 nanos、数据库 decimal scale 或 legacy fallback。
     */
    @Test
    void testPersistedDigestShouldRoundTripCanonicalTimeAndDecimalScale() {
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of(), Map.of(), Map.of(), LedgerPostingIntentType.TRANSFER,
                NANOS_TRANSACTION_TIME, BigDecimal.ONE);

        LedgerTransactionPostResult first = ledgerTransactionService.postLedgerTransaction(transaction);
        LedgerFactSnapshot persistedFacts = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionPostResult replay = ledgerTransactionService.postLedgerTransaction(ledgerTransaction(
                Map.of(), Map.of(), Map.of(), LedgerPostingIntentType.TRANSFER,
                NANOS_TRANSACTION_TIME, new BigDecimal("1.00000000")));

        assertThat(replay.getLedgerTransactionId()).isEqualTo(first.getLedgerTransactionId());
        assertThat(replay.isNewlyPosted()).isFalse();
        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(ledgerTransaction(
                Map.of(), Map.of(), Map.of(), LedgerPostingIntentType.TRANSFER,
                NANOS_TRANSACTION_TIME, new BigDecimal("1.01"))))
                .hasMessageContaining("账本交易已存在但摘要不一致");
        assertLedgerFactsUnchanged(jdbcTemplate, persistedFacts);

        Map<String, Object> actual = new TreeMap<>();
        actual.put("transactionTime", persistedTransactionDigestFacts().get("transactionTime"));
        actual.put("entryTimes", persistedLedgerEntryDigestFacts().stream()
                .map(facts -> facts.get("transactionTime"))
                .toList());
        actual.put("digests", persistedDigests());
        LocalDateTime expectedTime = NANOS_TRANSACTION_TIME.truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> expected = new TreeMap<>();
        expected.put("transactionTime", expectedTime);
        expected.put("entryTimes", List.of(expectedTime, expectedTime));
        expected.put("digests", expectedPersistedDigests());

        assertThat(actual)
                .as("persisted ledger digest must round-trip canonical time and decimal")
                .isEqualTo(expected);
    }

    /**
     * 场景：外部 LedgerTransactionSpec 实现绕过默认 DSL，交易级上下文携带敏感字段。
     * 输入：transaction.contextVariables 含 secretKey。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：账务事实交易上下文不得持久化支付工具密钥。
     */
    @Test
    void testPostLedgerTransactionShouldRejectSensitiveTransactionContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of("secretKey", "secret-value"),
                Map.of(),
                Map.of());

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .hasMessageContaining("ledgerTransaction.contextVariables must not contain sensitive fields");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerTransactionSpec 实现绕过默认 DSL，交易级上下文携带外部账户原文字段。
     * 输入：transaction.contextVariables 含 externalAccount.bankAccountNo。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：账务事实交易上下文不得持久化外部账户号。
     */
    @Test
    void testPostLedgerTransactionShouldRejectExternalAccountTransactionContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of("externalAccount", Map.of("bankAccountNo", "123456789012")),
                Map.of(),
                Map.of());

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .hasMessageContaining("ledgerTransaction.contextVariables must not contain sensitive fields");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerTransactionSpec 实现绕过默认 DSL，交易级上下文把原始 PAN 藏入集合值。
     * 输入：transaction.contextVariables 含 processorPayload 列表，其中一项是原始 PAN。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：账务事实交易上下文不得通过嵌套集合保存原始支付工具号。
     */
    @Test
    void testPostLedgerTransactionShouldRejectSensitiveIterableContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                SENSITIVE_ITERABLE_CONTEXT_VARIABLES,
                Map.of(),
                Map.of());

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .hasMessageContaining("ledgerTransaction.contextVariables must not contain sensitive fields");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerPostingPlanSpec 实现绕过默认 DSL，计划级上下文携带外部账户原文字段。
     * 输入：plan.contextVariables 含 iban。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：posting plan 合并上下文不得持久化外部账户原文。
     */
    @Test
    void testPostLedgerTransactionShouldRejectSensitivePostingPlanContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of(),
                Map.of("iban", "DE89370400440532013000"),
                Map.of());

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .hasMessageContaining("ledgerPostingPlan.contextVariables must not contain sensitive fields");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerEntrySpec 实现绕过默认 DSL，分录级上下文携带原始 PAN。
     * 输入：entry.contextVariables 含 pan。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：账本分录上下文不得持久化原始支付工具号。
     */
    @Test
    void testPostLedgerTransactionShouldRejectSensitiveLedgerEntryContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of(),
                Map.of(),
                Map.of("pan", "4111111111111111"));

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .hasMessageContaining("ledgerEntry.contextVariables must not contain sensitive fields");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerTransactionSpec 实现绕过默认 DSL，交易级上下文携带权益金额和资金责任。
     * 输入：transaction.contextVariables 含 amount、fundingNature。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：账务事实交易上下文不得成为权益核心事实的旁路承载。
     */
    @Test
    void testPostLedgerTransactionShouldRejectCoreBenefitTransactionContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                CORE_BENEFIT_CONTEXT_VARIABLES,
                Map.of(),
                Map.of());

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerTransaction.contextVariables must not contain core benefit field");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerPostingPlanSpec 实现绕过默认 DSL，计划级上下文携带实时营销规则。
     * 输入：plan.contextVariables 含 benefitDecisionTrace.currentMarketingRule。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：posting plan 合并上下文不得承载实时权益规则或权益金额。
     */
    @Test
    void testPostLedgerTransactionShouldRejectCoreBenefitPostingPlanContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of(),
                CORE_BENEFIT_ITERABLE_CONTEXT_VARIABLES,
                Map.of());

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerPostingPlan.contextVariables must not contain core benefit field: "
                        + "currentMarketingRule");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部 LedgerEntrySpec 实现绕过默认 DSL，分录级上下文携带权益金额和资金责任。
     * 输入：entry.contextVariables 含 amount、fundingNature。
     * 输出：账本交易写入被拒绝，且 transaction、posting plan、entry 三类账务事实均不落库。
     * 红线：账本分录上下文不得承载权益核心事实。
     */
    @Test
    void testPostLedgerTransactionShouldRejectCoreBenefitLedgerEntryContextWithoutFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        LedgerTransactionSpec transaction = ledgerTransaction(
                Map.of(),
                Map.of(),
                CORE_BENEFIT_CONTEXT_VARIABLES);

        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(transaction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ledgerEntry.contextVariables must not contain core benefit field");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    private LedgerTransactionSpec ledgerTransaction(Map<String, Object> transactionContext,
                                                    Map<String, Object> planContext,
                                                    Map<String, Object> entryContext) {
        return ledgerTransaction(transactionContext, planContext, entryContext, LedgerPostingIntentType.TRANSFER);
    }

    private LedgerTransactionSpec ledgerTransaction(Map<String, Object> transactionContext,
                                                    Map<String, Object> planContext,
                                                    Map<String, Object> entryContext,
                                                    LedgerPostingIntentType intent) {
        return ledgerTransaction(transactionContext, planContext, entryContext, intent,
                TRANSACTION_TIME, BigDecimal.ONE);
    }

    private LedgerTransactionSpec ledgerTransaction(Map<String, Object> transactionContext,
                                                    Map<String, Object> planContext,
                                                    Map<String, Object> entryContext,
                                                    LedgerPostingIntentType intent,
                                                    LocalDateTime transactionTime,
                                                    BigDecimal exchangeRate) {
        return new TestLedgerTransactionSpec(List.of(new TestLedgerPostingPlanSpec(
                "PLAN_LEDGER_CONTEXT_001",
                "LE_LEDGER_CONTEXT_001",
                intent,
                List.of(new TestLedgerPostingPhaseSpec(LedgerPhaseCode.TRANSFER, List.of(
                        entry("source_account", EntrySide.DEBIT, entryContext, transactionTime, exchangeRate),
                        entry("target_account", EntrySide.CREDIT, Map.of(), transactionTime, exchangeRate)))),
                planContext)), transactionTime, transactionContext);
    }

    private String persistedLedgerDigest() {
        return jdbcTemplate.queryForObject(
                "SELECT sha256 FROM t_ledger_transaction WHERE sn = ?",
                String.class,
                "LE_LEDGER_CONTEXT_001");
    }

    private Long countFacts(String tableName, String ledgerTransactionColumn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + ledgerTransactionColumn + " = ?",
                Long.class,
                "LE_LEDGER_CONTEXT_001");
    }

    private LedgerEntrySpec entry(String subjectId,
                                  EntrySide side,
                                  Map<String, Object> contextVariables,
                                  LocalDateTime transactionTime,
                                  BigDecimal exchangeRate) {
        return new TestLedgerEntrySpec(subjectId,
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                "LE_LEDGER_CONTEXT_001",
                side,
                Money.immutable(100L, CurrencyIsoCode.USD),
                transactionTime,
                exchangeRate,
                contextVariables);
    }

    private Map<String, String> persistedDigests() {
        Map<String, String> result = new TreeMap<>();
        result.put("transaction:LE_LEDGER_CONTEXT_001", persistedLedgerDigest());
        persistedPostingPlanDigestFacts().forEach(plan -> {
            String planSn = (String) plan.get("sn");
            result.put("plan:" + planSn, jdbcTemplate.queryForObject(
                    "SELECT sha256 FROM t_ledger_posting_plan WHERE sn = ?", String.class, planSn));
        });
        persistedLedgerEntryDigestFacts().forEach(entry -> {
            String entrySn = (String) entry.get("sn");
            result.put("entry:" + entrySn, jdbcTemplate.queryForObject(
                    "SELECT sha256 FROM t_ledger_entry WHERE sn = ?", String.class, entrySn));
        });
        return result;
    }

    private Map<String, String> expectedPersistedDigests() {
        Map<String, String> result = new TreeMap<>();
        result.put("transaction:LE_LEDGER_CONTEXT_001", FundsStableHashSupport.sha256CanonicalJson(
                LEDGER_TRANSACTION_DIGEST_DOMAIN, persistedLedgerAggregateDigestFacts()));
        persistedPostingPlanDigestFacts().forEach(plan -> result.put("plan:" + plan.get("sn"),
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_POSTING_PLAN_DIGEST_DOMAIN, plan)));
        persistedLedgerEntryDigestFacts().forEach(entry -> result.put("entry:" + entry.get("sn"),
                FundsStableHashSupport.sha256CanonicalJson(LEDGER_ENTRY_DIGEST_DOMAIN, entry)));
        return result;
    }

    private Map<String, Object> persistedLedgerAggregateDigestFacts() {
        Map<String, Object> facts = new TreeMap<>();
        facts.put("transaction", persistedTransactionDigestFacts());
        facts.put("postingPlans", persistedPostingPlanDigestFacts().stream().map(plan -> {
            Map<String, Object> aggregate = new TreeMap<>();
            aggregate.put("plan", plan);
            aggregate.put("entries", persistedLedgerEntryDigestFacts((String) plan.get("sn")));
            return aggregate;
        }).toList());
        return facts;
    }

    private Map<String, Object> persistedTransactionDigestFacts() {
        return jdbcTemplate.queryForObject("""
                        SELECT sn AS "sn", tenant_id AS "tenantId", instruction_type AS "instructionType",
                               event_type AS "eventType", funds_transaction_sn AS "fundsTransactionSn",
                               transaction_type AS "transactionType", business_scene AS "businessScene",
                               business_sn AS "businessSn", amount AS "amount", currency AS "currency",
                               original_amount AS "originalAmount", original_currency AS "originalCurrency",
                               exchange_rate AS "exchangeRate", debit_amount AS "debitAmount",
                               credit_amount AS "creditAmount", transaction_time AS "transactionTime",
                               reference_ledger_transaction_sn AS "referenceLedgerTransactionSn"
                        FROM t_ledger_transaction WHERE sn = ?
                        """, (resultSet, rowNum) -> digestFacts(resultSet), "LE_LEDGER_CONTEXT_001");
    }

    private List<Map<String, Object>> persistedPostingPlanDigestFacts() {
        return jdbcTemplate.query("""
                        SELECT sn AS "sn", tenant_id AS "tenantId",
                               ledger_transaction_sn AS "ledgerTransactionSn",
                               funds_transaction_sn AS "fundsTransactionSn", route_leg_id AS "routeLegId",
                               intent AS "intent", posting_scope AS "postingScope",
                               balance_effect_type AS "balanceEffectType", phase_code AS "phaseCode",
                               amount AS "amount", currency AS "currency", debit_amount AS "debitAmount",
                               credit_amount AS "creditAmount"
                        FROM t_ledger_posting_plan WHERE ledger_transaction_sn = ? ORDER BY sn
                        """, (resultSet, rowNum) -> digestFacts(resultSet), "LE_LEDGER_CONTEXT_001");
    }

    private List<Map<String, Object>> persistedLedgerEntryDigestFacts() {
        return jdbcTemplate.query("""
                        SELECT sn AS "sn", tenant_id AS "tenantId",
                               ledger_transaction_sn AS "ledgerTransactionSn", posting_plan_sn AS "postingPlanSn",
                               funds_transaction_sn AS "fundsTransactionSn", ledger_id AS "ledgerId",
                               period_type AS "periodType", period_id AS "periodId", subject_id AS "subjectId",
                               subject_type AS "subjectType", ledger_subject_code AS "ledgerSubjectCode",
                               ledger_subject_category AS "ledgerSubjectCategory", entry_side AS "entrySide",
                               posting_role AS "postingRole", balance_constraint_type AS "balanceConstraintType",
                               intent AS "intent", posting_scope AS "postingScope",
                               balance_effect_type AS "balanceEffectType", phase_code AS "phaseCode",
                               business_scene AS "businessScene", business_sn AS "businessSn",
                               amount AS "amount", currency AS "currency", original_amount AS "originalAmount",
                               original_currency AS "originalCurrency", exchange_rate AS "exchangeRate",
                               transaction_time AS "transactionTime"
                        FROM t_ledger_entry WHERE ledger_transaction_sn = ? ORDER BY sn
                        """, (resultSet, rowNum) -> digestFacts(resultSet), "LE_LEDGER_CONTEXT_001");
    }

    private List<Map<String, Object>> persistedLedgerEntryDigestFacts(String postingPlanSn) {
        return persistedLedgerEntryDigestFacts().stream()
                .filter(entry -> postingPlanSn.equals(entry.get("postingPlanSn")))
                .toList();
    }

    private Map<String, Object> digestFacts(ResultSet resultSet) throws SQLException {
        Map<String, Object> facts = new TreeMap<>();
        for (int column = 1; column <= resultSet.getMetaData().getColumnCount(); column++) {
            Object value = resultSet.getObject(column);
            if (value instanceof Timestamp timestamp) {
                value = timestamp.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
            } else if (value instanceof LocalDateTime time) {
                value = time.truncatedTo(ChronoUnit.SECONDS);
            }
            facts.put(resultSet.getMetaData().getColumnLabel(column), value);
        }
        return facts;
    }

    private void cleanupLedgerContextFacts() {
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE ledger_transaction_sn = ?", "LE_LEDGER_CONTEXT_001");
        jdbcTemplate.update("DELETE FROM t_ledger_posting_plan WHERE ledger_transaction_sn = ?",
                "LE_LEDGER_CONTEXT_001");
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE sn = ?", "LE_LEDGER_CONTEXT_001");
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
                                             List<LedgerPostingPhaseSpec> postingPhases,
                                             Map<String, Object> contextVariables)
            implements LedgerPostingPlanSpec {

        private TestLedgerPostingPlanSpec {
            postingPhases = List.copyOf(postingPhases);
            contextVariables = Map.copyOf(contextVariables);
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

        @Override
        public Map<String, Object> getContextVariables() {
            return contextVariables;
        }
    }

    private record TestLedgerEntrySpec(String subjectId,
                                       String subjectType,
                                       LedgerSubjectCode ledgerSubjectCode,
                                       LedgerSubjectCategory ledgerSubjectCategory,
                                       String ledgerTransactionSn,
                                       EntrySide entryType,
                                       Money amount,
                                       LocalDateTime transactionTime,
                                       BigDecimal exchangeRate,
                                       Map<String, Object> contextVariables) implements LedgerEntrySpec {

        private TestLedgerEntrySpec {
            contextVariables = Map.copyOf(contextVariables);
        }

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
        public LedgerPostingRole getPostingRole() {
            return LedgerPostingRole.DETAIL;
        }

        @Override
        public String getBusinessScene() {
            return "LEDGER_CONTEXT_REDACTION";
        }

        @Override
        public String getBusinessSn() {
            return "BIZ-LEDGER-CONTEXT-001";
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
            return exchangeRate;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return transactionTime;
        }

        @Override
        public String getDescription() {
            return "ledger context sensitive value contract";
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return contextVariables;
        }
    }

    private record TestLedgerTransactionSpec(List<LedgerPostingPlanSpec> postingPlans,
                                             LocalDateTime transactionTime,
                                             Map<String, Object> contextVariables)
            implements LedgerTransactionSpec {

        private TestLedgerTransactionSpec {
            postingPlans = List.copyOf(postingPlans);
            contextVariables = Map.copyOf(contextVariables);
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }

        @Override
        public String getSn() {
            return "LE_LEDGER_CONTEXT_001";
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
            return "BIZ-LEDGER-CONTEXT-001";
        }

        @Override
        public DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public String getBusinessScene() {
            return "LEDGER_CONTEXT_REDACTION";
        }

        @Override
        public String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return transactionTime;
        }

        @Override
        public String getDescription() {
            return "ledger context sensitive value contract";
        }

        @Override
        public List<LedgerPostingPlanSpec> getPostingPlans() {
            return postingPlans;
        }

        @Override
        public Map<String, Object> getContextVariables() {
            return contextVariables;
        }
    }

    @Configuration
    @Import(LedgerTransactionServiceImpl.class)
    static class Config {
    }
}
