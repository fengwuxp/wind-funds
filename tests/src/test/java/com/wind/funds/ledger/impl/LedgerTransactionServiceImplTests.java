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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private static final String CORE1B_LEDGER_CANONICAL_DIGEST =
            "f27b0b1798d3c1b06576b4c87301bb037bd87dc30f5cdb69ac827990004c80a6";

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
     * 场景：历史 ledger transaction 已保存 legacy 摘要，当前版本收到相同请求和冲突请求。
     * 输入：先由当前 writer 写入 canonical v1，再把持久化摘要替换为基线旧 writer 的固定 golden。
     * 输出：同请求复用原交易，冲突 intent 被拒绝，legacy 摘要和全部账务事实均保持不变。
     * 红线：兼容读取不得重写历史摘要、重复入账或绕过摘要冲突检查。
     */
    @Test
    void testPersistedLegacyLedgerDigestShouldReplayAndRejectConflictWithoutChangingFacts() {
        LedgerTransactionSpec transaction = ledgerTransaction(Map.of(), Map.of(), Map.of());

        LedgerTransactionPostResult first = ledgerTransactionService.postLedgerTransaction(transaction);

        assertThat(first.isNewlyPosted()).isTrue();
        assertThat(first.getLedgerTransactionId()).isNotNull();
        assertThat(persistedLedgerDigest()).isEqualTo(CORE1B_LEDGER_CANONICAL_DIGEST)
                .isNotEqualTo(CORE1B_LEDGER_LEGACY_DIGEST);
        assertThat(countFacts("t_ledger_transaction", "sn")).isEqualTo(1L);
        assertThat(countFacts("t_ledger_posting_plan", "ledger_transaction_sn")).isEqualTo(1L);
        assertThat(countFacts("t_ledger_entry", "ledger_transaction_sn")).isEqualTo(2L);
        assertThat(jdbcTemplate.update("UPDATE t_ledger_transaction SET sha256 = ? WHERE sn = ?",
                CORE1B_LEDGER_LEGACY_DIGEST, "LE_LEDGER_CONTEXT_001")).isEqualTo(1);
        LedgerFactSnapshot persistedLegacyFacts = ledgerFactSnapshot(jdbcTemplate);

        LedgerTransactionPostResult replay = ledgerTransactionService.postLedgerTransaction(transaction);

        assertThat(replay.getLedgerTransactionId()).isEqualTo(first.getLedgerTransactionId());
        assertThat(replay.isNewlyPosted()).isFalse();
        assertThat(persistedLedgerDigest()).isEqualTo(CORE1B_LEDGER_LEGACY_DIGEST);
        assertLedgerFactsUnchanged(jdbcTemplate, persistedLegacyFacts);

        LedgerTransactionSpec conflicting = ledgerTransaction(
                Map.of(), Map.of(), Map.of(), LedgerPostingIntentType.REFUND);
        assertThatThrownBy(() -> ledgerTransactionService.postLedgerTransaction(conflicting))
                .hasMessageContaining("账本交易已存在但摘要不一致");

        assertThat(persistedLedgerDigest()).isEqualTo(CORE1B_LEDGER_LEGACY_DIGEST);
        assertLedgerFactsUnchanged(jdbcTemplate, persistedLegacyFacts);
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
        return new TestLedgerTransactionSpec(List.of(new TestLedgerPostingPlanSpec(
                "PLAN_LEDGER_CONTEXT_001",
                "LE_LEDGER_CONTEXT_001",
                intent,
                List.of(new TestLedgerPostingPhaseSpec(LedgerPhaseCode.TRANSFER, List.of(
                        entry("source_account", EntrySide.DEBIT, entryContext),
                        entry("target_account", EntrySide.CREDIT, Map.of())))),
                planContext)), transactionContext);
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

    private LedgerEntrySpec entry(String subjectId, EntrySide side, Map<String, Object> contextVariables) {
        return new TestLedgerEntrySpec(subjectId,
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.ASSET,
                "LE_LEDGER_CONTEXT_001",
                side,
                Money.immutable(100L, CurrencyIsoCode.USD),
                contextVariables);
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
            return BigDecimal.ONE;
        }

        @Override
        public LocalDateTime getTransactionTime() {
            return TRANSACTION_TIME;
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
            return TRANSACTION_TIME;
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
