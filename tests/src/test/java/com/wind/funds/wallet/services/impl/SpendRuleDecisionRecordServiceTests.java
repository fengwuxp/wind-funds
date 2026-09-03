package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
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

import java.lang.reflect.Method;
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

    private static final String CONTROL_SCOPE_ID = "spend_rule_decision_record_service_scope";

    private static final String PERIOD_ID = "2026-07";

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
     * 场景：公共调用方按决策事实读取记录。
     * 预期：服务不暴露无租户 raw-id 查询，业务身份由 tenantId + decisionSn 承载。
     * 红线：数据库代理主键不能替代租户对象授权。
     */
    @Test
    void testPublicContractShouldNotExposeRawIdGetter() {
        assertThat(SpendRuleDecisionRecordService.class.getMethods())
                .extracting(Method::getName)
                .doesNotContain("getDecisionRecordById");
    }

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
        assertThatThrownBy(() -> spendRuleDecisionRecordService.queryDecisions(
                new SpendRuleDecisionRecordQuery()
                        .setBusinessScene(BUSINESS_SCENE)
                        .setBusinessSn(BUSINESS_SN)))
                .hasMessageContaining("租户 ID 不能为空");
        SpendRuleDecisionExplanationDTO explanation = spendRuleDecisionRecordService.explainDecision(
                new SpendRuleDecisionExplainQuery()
                        .setTenantId(TENANT_ID)
                        .setDecisionSn(DECISION_SN));

        assertThat(decision.getDecisionSn()).isEqualTo(DECISION_SN);
        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decisions).hasSize(1);
        assertThat(decisions.getFirst().getId()).isEqualTo(decision.getId());
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
     * 场景：可信决策方固化支付工具绑定版本、预算窗口和账户级评估上下文。
     * 输入：同一 decisionSn 先写入完整上下文，再以不同 periodId 重放。
     * 输出：首次写入可回读完整上下文，漂移重放被拒绝。
     * 红线：decisionRef 不得脱离原绑定、控制窗口或账户评估上下文复用。
     */
    @Test
    void testRecordDecisionShouldPersistAdmissionContextAndRejectReplayDrift() {
        prepareRuleVersionAndBinding();
        FundsAccountId targetAccountId = FundsAccountId.immutable(
                "decision_context_credit_account", FundsSubjectType.CREDIT_ACCOUNT);
        RecordSpendRuleDecisionRecordRequest request = rejectedDecisionRequest()
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(PERIOD_ID)
                .setTargetAccountId(targetAccountId);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleDecisionRecordDTO recorded = spendRuleDecisionRecordService.recordDecision(request);

        assertThat(recorded.getInstrumentBindingVersion()).isEqualTo(1);
        assertThat(recorded.getControlScopeId()).isEqualTo(CONTROL_SCOPE_ID);
        assertThat(recorded.getPeriodId()).isEqualTo(PERIOD_ID);
        assertThat(recorded.getTargetAccountId()).isEqualTo(targetAccountId);
        assertThatThrownBy(() -> spendRuleDecisionRecordService.recordDecision(
                rejectedDecisionRequest()
                        .setControlScopeId(CONTROL_SCOPE_ID)
                        .setPeriodId("2026-08")
                        .setTargetAccountId(targetAccountId)))
                .hasMessageContaining("决策流水已存在但内容不一致");
        assertThat(countDecisionRecords()).isEqualTo(1);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：可信决策方只提交预算控制范围，没有周期证据。
     * 输入：controlScopeId 非空、periodId 为空。
     * 输出：写入口 fail-closed，不创建决策记录。
     * 红线：不完整控制窗口不得成为可复用 decisionRef。
     */
    @Test
    void testRecordDecisionShouldRejectPartialControlWindow() {
        prepareRuleVersionAndBinding();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDecisionRecordService.recordDecision(
                rejectedDecisionRequest().setControlScopeId(CONTROL_SCOPE_ID)))
                .hasMessageContaining("控制范围和周期必须同时提供");

        assertThat(countDecisionRecords()).isZero();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：可信决策方把支付工具误作为规则评估目标账户。
     * 输入：targetAccountId.type=PAYMENT_INSTRUMENT。
     * 输出：写入口拒绝，不创建决策记录。
     * 红线：Spend Rule 上下文不能把 PaymentInstrument 变成账务主体。
     */
    @Test
    void testRecordDecisionShouldRejectNonAccountingTarget() {
        prepareRuleVersionAndBinding();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDecisionRecordService.recordDecision(
                rejectedDecisionRequest().setTargetAccountId(FundsAccountId.immutable(
                        PAYMENT_INSTRUMENT_SN, "PAYMENT_INSTRUMENT"))))
                .hasMessageContaining("目标账户只允许资金账户或信用账户");

        assertThat(countDecisionRecords()).isZero();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：历史或异常数据只保留目标账户类型，没有目标账户 ID。
     * 输入：先写入无目标账户决策，再模拟不完整的持久化证据。
     * 输出：读取 fail-closed，不把不完整证据降级为范围级决策。
     * 红线：目标账户证据缺失不得扩大 PASSED 决策的适用范围。
     */
    @Test
    void testFindDecisionRecordShouldRejectPartialPersistedTargetEvidence() {
        prepareRuleVersionAndBinding();
        SpendRuleDecisionRecordDTO recorded =
                spendRuleDecisionRecordService.recordDecision(rejectedDecisionRequest());
        jdbcTemplate.update("""
                UPDATE t_spend_rule_decision_record
                SET target_subject_type = ?
                WHERE id = ?
                """, FundsSubjectType.CREDIT_ACCOUNT.name(), recorded.getId());

        assertThatThrownBy(() -> spendRuleDecisionRecordService.findDecisionRecord(TENANT_ID, DECISION_SN))
                .hasMessageContaining("目标账户证据不完整");
    }

    /**
     * 场景：调用方尝试把无适用规则结果伪装成持久化规则决策。
     * 输入：有效规则和挂载，但 decisionResult=NO_APPLICABLE_RULE。
     * 输出：决策记录写入口拒绝，不创建记录。
     * 红线：NO_APPLICABLE_RULE 只能由 wallet 基于当前 binding 查询显式形成，不能由上游自报。
     */
    @Test
    void testRecordDecisionShouldRejectCallerProvidedNoApplicableRule() {
        prepareRuleVersionAndBinding();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDecisionRecordService.recordDecision(rejectedDecisionRequest()
                .setDecisionResult(SpendControlDecisionResult.NO_APPLICABLE_RULE)
                .setRejectReason(null)))
                .hasMessageContaining("只允许 PASSED 或 REJECTED");

        assertThat(countDecisionRecords()).isZero();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：决策写入请求租户与当前线程租户不一致。
     * 输入：线程租户为 1，请求 tenantId 为 2。
     * 输出：写入口在读取规则或 binding 前拒绝，不创建决策记录。
     * 红线：trusted writer 的部署鉴权不能替代 wallet 服务层租户一致性校验。
     */
    @Test
    void testRecordDecisionShouldRejectTenantMismatch() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDecisionRecordService.recordDecision(
                rejectedDecisionRequest().setTenantId(TENANT_ID + 1)))
                .hasMessageContaining("Spend Rule 决策写入 tenantId 与当前租户不一致");

        assertThat(countDecisionRecords()).isZero();
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
     * 场景：决策记录查询必须由调用方显式提供租户授权。
     * 输入：公共 Query 模型。
     * 输出：tenantId 具有必填校验注解。
     * 红线：查询模型不得允许缺少租户授权的宽查询。
     */
    @Test
    void testDecisionRecordQueryShouldRequireTenantId() throws NoSuchFieldException {
        assertThat(SpendRuleDecisionRecordQuery.class.getDeclaredField("tenantId")
                .isAnnotationPresent(NotNull.class)).isTrue();
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
                .setInstrumentBindingVersion(1)
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
