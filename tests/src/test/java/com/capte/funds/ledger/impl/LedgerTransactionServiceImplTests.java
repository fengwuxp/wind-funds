package com.capte.funds.ledger.impl;

import com.capte.funds.AbstractFundsServiceTest;
import com.capte.funds.ledger.dto.LedgerTransactionPostResult;
import com.capte.funds.ledger.request.UpdateLedgerTransactionRequest;
import com.capte.funds.ledger.service.LedgerTransactionService;
import com.capte.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
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

import static com.capte.funds.support.FundsBalanceAssertionSupport.assertLedgerTransactionFactsUnchanged;
import static com.capte.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
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

    @Autowired
    private LedgerTransactionService ledgerTransactionService;

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
     * 场景：已入账账本交易通过更新接口补充交易上下文，更新请求携带敏感字段。
     * 输入：UpdateLedgerTransactionRequest.contextVariable 含 secretKey。
     * 输出：账本交易更新被拒绝，已入账 transaction、posting plan、entry 三类账务事实保持不变。
     * 红线：账务事实更新入口不得成为敏感上下文写入旁路。
     */
    @Test
    void testUpdateLedgerTransactionShouldRejectSensitiveContextWithoutMutatingFacts() {
        LedgerTransactionPostResult postResult = ledgerTransactionService.postLedgerTransaction(ledgerTransaction(
                Map.of("traceId", "TRACE-LEDGER-CONTEXT-001"),
                Map.of("routeTraceId", "ROUTE-TRACE-001"),
                Map.of("entryTraceId", "ENTRY-TRACE-001")));
        assertThat(postResult.isNewlyPosted()).isTrue();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> ledgerTransactionService.updateLedgerTransaction(new UpdateLedgerTransactionRequest()
                .setId(postResult.getLedgerTransactionId())
                .setContextVariable(Map.of("secretKey", "secret-value"))))
                .hasMessageContaining("ledgerTransaction.contextVariables must not contain sensitive fields");

        assertLedgerTransactionFactsUnchanged(jdbcTemplate, before);
    }

    private LedgerTransactionSpec ledgerTransaction(Map<String, Object> transactionContext,
                                                    Map<String, Object> planContext,
                                                    Map<String, Object> entryContext) {
        return new TestLedgerTransactionSpec(List.of(new TestLedgerPostingPlanSpec(
                "PLAN_LEDGER_CONTEXT_001",
                "LE_LEDGER_CONTEXT_001",
                LedgerPostingIntentType.TRANSFER,
                List.of(new TestLedgerPostingPhaseSpec(LedgerPhaseCode.TRANSFER, List.of(
                        entry("source_account", EntrySide.DEBIT, entryContext),
                        entry("target_account", EntrySide.CREDIT, Map.of())))),
                planContext)), transactionContext);
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
        public LedgerTransactionStatus getStatus() {
            return LedgerTransactionStatus.POSTED;
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
