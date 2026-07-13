package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionRecordDTO;
import com.wind.funds.wallet.model.query.SpendRuleDecisionExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleDecisionRecordQuery;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
import com.wind.funds.wallet.service.SpendRuleDecisionRecordService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
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

import java.time.LocalDateTime;
import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spend Rule 决策记录写入、查询和解释标准基础服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendRuleDecisionRecordServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendRuleDecisionRecordServiceTests extends AbstractFundsServiceTest {

    private static final String RULE_ID = "sr_decision_record_service_daily_limit";

    private static final String RULE_VERSION = "2026-06-23.1";

    private static final String RULE_DIGEST = "sha256:spend-rule-decision-record-service";

    private static final String BINDING_AUDIT_REFERENCE_SN = "grant:spend_rule_decision_record_service_binding";

    private static final String DECISION_SN = "spend_rule_decision_record_service_001";

    private static final String BUSINESS_SCENE = "SPEND_RULE_DECISION_RECORD_SERVICE";

    private static final String BUSINESS_SN = "SPEND_RULE_DECISION_RECORD_SERVICE_001";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_rule_decision_record_service_card";

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.now().withNano(0).minusDays(1);

    private static final LocalDateTime EFFECTIVE_TO = LocalDateTime.now().withNano(0).plusDays(30);

    @Autowired
    private SpendRuleDefinitionService spendRuleDefinitionService;

    @Autowired
    private SpendRuleDecisionRecordService spendRuleDecisionRecordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String spendRuleBindingSn;

    /**
     * 场景：准入链直接通过决策记录标准基础服务固化规则决策。
     * 输入：已发布版本、当前有效挂载和一条拒绝决策。
     * 输出：决策记录可被标准基础服务按业务流水查询并解释。
     * 红线：标准基础服务只固化规则证据，不创建资金交易、route、posting、LedgerEntry 或余额事实。
     */
    @Test
    void testRecordDecisionShouldBeAvailableThroughServiceQueryWithoutFundsSideEffect() {
        prepareRuleVersionAndBinding();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleDecisionRecordDTO decision =
                spendRuleDecisionRecordService.recordDecision(rejectedDecisionRequest());
        List<SpendRuleDecisionRecordDTO> decisions = spendRuleDecisionRecordService.queryDecisions(
                new SpendRuleDecisionRecordQuery()
                        .setTenantId(TENANT_ID)
                        .setBusinessScene(BUSINESS_SCENE)
                        .setBusinessSn(BUSINESS_SN));
        List<SpendRuleDecisionRecordDTO> decisionsWithoutTenantId = spendRuleDecisionRecordService.queryDecisions(
                new SpendRuleDecisionRecordQuery()
                        .setBusinessScene(BUSINESS_SCENE)
                        .setBusinessSn(BUSINESS_SN));
        SpendRuleDecisionExplanationDTO explanation = spendRuleDecisionRecordService.explainDecision(
                new SpendRuleDecisionExplainQuery()
                        .setTenantId(TENANT_ID)
                        .setDecisionSn(DECISION_SN));

        assertThat(decision.getDecisionSn()).isEqualTo(DECISION_SN);
        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decisions).hasSize(1);
        assertThat(decisions.getFirst().getId()).isEqualTo(decision.getId());
        assertThat(decisionsWithoutTenantId).hasSize(1);
        assertThat(decisionsWithoutTenantId.getFirst().getId()).isEqualTo(decision.getId());
        assertThat(explanation.getDecision().getId()).isEqualTo(decision.getId());
        assertThat(explanation.getDecision().getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(explanation.getDecisionSummary()).contains("拒绝", "超过单卡日限额");
        assertThat(explanation.getEvidenceRefs()).contains(
                "spendRule:" + RULE_ID,
                "spendRuleVersion:" + RULE_ID + "@" + RULE_VERSION,
                "spendRuleBinding:" + spendRuleBindingSn,
                "spendRuleDecision:" + DECISION_SN,
                "paymentInstrument:" + PAYMENT_INSTRUMENT_SN);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一决策流水重复进入标准基础服务。
     * 输入：两次完全相同的决策记录请求。
     * 输出：返回同一条决策记录。
     * 红线：幂等回放不得新增第二条决策记录，也不得创建资金或账务事实。
     */
    @Test
    void testRecordDecisionShouldReuseIdempotentDecisionRecordWithoutFundsSideEffect() {
        prepareRuleVersionAndBinding();
        SpendRuleDecisionRecordDTO first = spendRuleDecisionRecordService.recordDecision(rejectedDecisionRequest());
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleDecisionRecordDTO replayed = spendRuleDecisionRecordService.recordDecision(rejectedDecisionRequest());

        assertThat(replayed.getId()).isEqualTo(first.getId());
        assertThat(countDecisionRecords()).isEqualTo(1);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方误用标准基础服务做租户级决策记录扫描。
     * 输入：只传 tenantId。
     * 输出：查询被拒绝。
     * 红线：生产读服务不得提供无业务范围的全租户决策记录扫描入口。
     */
    @Test
    void testQueryDecisionsShouldRejectTenantOnlyScan() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDecisionRecordService.queryDecisions(
                new SpendRuleDecisionRecordQuery().setTenantId(TENANT_ID)))
                .hasMessageContaining("至少提供一个 Spend Rule 决策查询条件");

        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：决策记录查询依赖 MyBatis-Flex 租户隔离，不要求调用方显式传 tenantId。
     * 输入：公共 Query 模型。
     * 输出：tenantId 作为可选过滤条件，不带必填校验注解。
     * 红线：查询模型不得和服务层已支持的无 tenantId 查询契约冲突。
     */
    @Test
    void testDecisionRecordQueryShouldNotRequireTenantId() throws NoSuchFieldException {
        assertThat(SpendRuleDecisionRecordQuery.class.getDeclaredField("tenantId")
                .isAnnotationPresent(NotNull.class)).isFalse();
    }

    @BeforeEach
    void setUpDecisionRecordServiceTestData() {
        cleanupDecisionRecordServiceTestData();
    }

    @AfterEach
    void tearDownDecisionRecordServiceTestData() {
        cleanupDecisionRecordServiceTestData();
    }

    private void prepareRuleVersionAndBinding() {
        spendRuleDefinitionService.createDefinition(createDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishVersionRequest());
        spendRuleBindingSn = spendRuleDefinitionService.createSpendRuleBinding(bindingRequest()).getSn();
    }

    private CreateSpendRuleDefinitionRequest createDefinitionRequest() {
        return new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleName("VCC 单卡日限额")
                .setRuleType(SpendRuleType.AMOUNT_LIMIT)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("用于验证 Spend Rule 决策记录标准基础服务");
    }

    private PublishSpendRuleVersionRequest publishVersionRequest() {
        return new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setRuleSpec("{\"window\":\"DAILY\",\"amount\":10000,\"currency\":\"USD\"}")
                .setRuleDigest(RULE_DIGEST)
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:SPEND-RULE-COMPAT-FACADE-DELEGATION")
                .setDescription("发布 Spend Rule 版本");
    }

    private CreateSpendRuleBindingRequest bindingRequest() {
        return new CreateSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setScopeId(PAYMENT_INSTRUMENT_SN)
                .setPriority(10)
                .setConflictPolicy(SpendRuleConflictPolicy.DENY_OVERRIDES)
                .setEffectiveFrom(EFFECTIVE_FROM)
                .setEffectiveTo(EFFECTIVE_TO)
                .setAuditReferenceSn(BINDING_AUDIT_REFERENCE_SN)
                .setDescription("挂载 Spend Rule 版本");
    }

    private RecordSpendRuleDecisionRecordRequest rejectedDecisionRequest() {
        return new RecordSpendRuleDecisionRecordRequest()
                .setTenantId(TENANT_ID)
                .setDecisionSn(DECISION_SN)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setSpendRuleBindingSn(spendRuleBindingSn)
                .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setScopeId(PAYMENT_INSTRUMENT_SN)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(10001L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setDecisionResult(SpendControlDecisionResult.REJECTED)
                .setRejectReason("超过单卡日限额")
                .setDecisionDigest("sha256:decision-log-service-rejected");
    }

    private void cleanupDecisionRecordServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_binding WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
    }

    private void assertNoTransactionFacts(String businessSn) {
        assertThat(countRows("t_funds_transaction", businessSn)).isZero();
        assertThat(countRows("t_funds_transaction_detail", businessSn)).isZero();
        assertThat(postingPlanCount(businessSn)).isZero();
        assertThat(countRows("t_ledger_transaction", businessSn)).isZero();
        assertThat(countRows("t_ledger_entry", businessSn)).isZero();
    }

    private Integer postingPlanCount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, businessSn);
    }

    private int countRows(String tableName, String businessSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, businessSn);
    }

    private int countDecisionRecords() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_rule_decision_record
                WHERE tenant_id = ? AND decision_sn = ?
                """, Integer.class, TENANT_ID, DECISION_SN);
    }

    @Configuration
    @Import({
            SpendRuleDefinitionServiceImpl.class,
            SpendRuleVersionServiceImpl.class,
            SpendRuleBindingServiceImpl.class,
            SpendRuleDecisionRecordServiceImpl.class
    })
    static class Config {
    }
}
