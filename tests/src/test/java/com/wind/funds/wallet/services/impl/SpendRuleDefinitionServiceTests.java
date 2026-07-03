package com.wind.funds.wallet.services.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.enums.SpendRuleAssignmentExplanationStatus;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleDefinitionStatus;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.enums.SpendRuleVersionStatus;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentExplanationDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentExplainQuery;
import com.wind.funds.wallet.model.query.SpendRuleAssignmentQuery;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.service.SpendRuleAssignmentService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
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
 * Spend Rule 定义、版本、挂载和挂载解释标准基础服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendRuleDefinitionServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendRuleDefinitionServiceTests extends AbstractFundsServiceTest {

    private static final String RULE_ID = "sr_definition_service_daily_limit";

    private static final String RULE_VERSION = "2026-06-23.1";

    private static final String FUTURE_RULE_VERSION = "2026-06-23.2";

    private static final String EXPIRED_RULE_VERSION = "2026-06-23.3";

    private static final String RULE_SPEC = "{\"window\":\"DAILY\",\"amount\":10000,\"currency\":\"USD\"}";

    private static final String RULE_DIGEST = "sha256:spend-rule-definition-service";

    private static final String FUTURE_RULE_DIGEST = "sha256:spend-rule-definition-service-future";

    private static final String EXPIRED_RULE_DIGEST = "sha256:spend-rule-definition-service-expired";

    private static final String CHANGED_RULE_DIGEST = "sha256:spend-rule-definition-service-changed";

    private static final String ASSIGNMENT_SN = "spend_rule_definition_service_assignment";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_rule_definition_service_card";

    private static final String FUNDING_ACCOUNT_SCOPE_ID = "funding_account_spend_rule_scope_001";

    private static final String CREDIT_ACCOUNT_SCOPE_ID = "credit_account_spend_rule_scope_001";

    private static final String ACCOUNT_HIERARCHY_SCOPE_ID = "employee_cardholder_spend_rule_scope_001";

    private static final String CARD_PRODUCT_SCOPE_ID = "CARD_PRODUCT_TRAVEL";

    private static final String BUSINESS_SCENE = "SPEND_RULE_DEFINITION_SERVICE";

    private static final String BUSINESS_SN = "SPEND_RULE_DEFINITION_SERVICE_001";

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.now().withNano(0).minusDays(1);

    private static final LocalDateTime EFFECTIVE_TO = LocalDateTime.now().withNano(0).plusDays(30);

    @Autowired
    private SpendRuleDefinitionService spendRuleDefinitionService;

    @Autowired
    private SpendRuleAssignmentService spendRuleAssignmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：标准基础服务发布不可变 Spend Rule 版本。
     * 输入：同一版本重复发布同摘要和异摘要。
     * 输出：同摘要幂等返回，异摘要拒绝。
     * 红线：规则版本不可原地覆盖，失败不得创建资金、route、posting 或账本事实。
     */
    @Test
    void testPublishVersionShouldBeIdempotentAndRejectDigestDriftWithoutFundsSideEffect() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleDefinitionDTO definition = spendRuleDefinitionService.getDefinitionById(
                spendRuleDefinitionService.createDefinition(createDefinitionRequest()));
        SpendRuleVersionDTO published =
                spendRuleDefinitionService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));
        SpendRuleVersionDTO replayed =
                spendRuleDefinitionService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));

        assertThat(definition.getStatus()).isEqualTo(SpendRuleDefinitionStatus.ACTIVE);
        assertThat(published.getStatus()).isEqualTo(SpendRuleVersionStatus.PUBLISHED);
        assertThat(replayed.getId()).isEqualTo(published.getId());
        assertThatThrownBy(() -> spendRuleDefinitionService.publishVersion(
                publishVersionRequest(CHANGED_RULE_DIGEST, "{\"window\":\"MONTHLY\"}")))
                .hasMessageContaining("Spend Rule 版本已发布但内容摘要不一致");
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：标准基础服务挂载已发布规则版本。
     * 输入：同一挂载流水重复进入且内容一致。
     * 输出：重复请求返回同一条挂载。
     * 红线：挂载只生成控制事实，不创建决策记录、资金交易或账本事实。
     */
    @Test
    void testAssignVersionShouldReuseIdempotentAssignmentWithoutFundsSideEffect() {
        publishRuleVersions();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleAssignmentDTO first = spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN,
                RULE_VERSION,
                PAYMENT_INSTRUMENT_SN));
        SpendRuleAssignmentDTO replayed = spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN,
                RULE_VERSION,
                PAYMENT_INSTRUMENT_SN));

        assertThat(replayed.getId()).isEqualTo(first.getId());
        assertThat(countAssignments()).isEqualTo(1);
        assertNoDecisionRecord();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：标准基础服务查询当前有效挂载并解释挂载状态。
     * 输入：同一支付工具 scope 下存在当前有效、未来生效和已过期挂载。
     * 输出：有效查询只返回当前挂载，解释分别返回 EFFECTIVE、NOT_YET_EFFECTIVE、EXPIRED。
     * 红线：挂载读服务只读，不记录决策记录、不创建资金交易或账本事实。
     */
    @Test
    void testQueryAndExplainAssignmentShouldBeReadOnlyWithoutFundsSideEffect() {
        publishRuleVersions();
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN,
                RULE_VERSION,
                PAYMENT_INSTRUMENT_SN));
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_future",
                FUTURE_RULE_VERSION,
                PAYMENT_INSTRUMENT_SN)
                .setEffectiveFrom(LocalDateTime.now().plusDays(1))
                .setEffectiveTo(LocalDateTime.now().plusDays(2)));
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_expired",
                EXPIRED_RULE_VERSION,
                PAYMENT_INSTRUMENT_SN)
                .setEffectiveFrom(LocalDateTime.now().minusDays(3))
                .setEffectiveTo(LocalDateTime.now().minusDays(2)));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        List<SpendRuleAssignmentDTO> assignments = spendRuleAssignmentService.queryAssignments(
                new SpendRuleAssignmentQuery()
                        .setTenantId(TENANT_ID)
                        .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                        .setScopeId(PAYMENT_INSTRUMENT_SN)
                        .setEffectiveOnly(Boolean.TRUE));
        SpendRuleAssignmentExplanationDTO effective = explainAssignment(ASSIGNMENT_SN);
        SpendRuleAssignmentExplanationDTO future = explainAssignment(ASSIGNMENT_SN + "_future");
        SpendRuleAssignmentExplanationDTO expired = explainAssignment(ASSIGNMENT_SN + "_expired");

        assertThat(assignments).hasSize(1);
        assertThat(assignments.getFirst().getAssignmentSn()).isEqualTo(ASSIGNMENT_SN);
        assertThat(effective.getExplanationStatus()).isEqualTo(SpendRuleAssignmentExplanationStatus.EFFECTIVE);
        assertThat(future.getExplanationStatus())
                .isEqualTo(SpendRuleAssignmentExplanationStatus.NOT_YET_EFFECTIVE);
        assertThat(expired.getExplanationStatus()).isEqualTo(SpendRuleAssignmentExplanationStatus.EXPIRED);
        assertThat(effective.getEvidenceRefs()).contains(
                "spendRule:" + RULE_ID,
                "spendRuleVersion:" + RULE_ID + "@" + RULE_VERSION,
                "spendRuleAssignment:" + ASSIGNMENT_SN);
        assertNoDecisionRecord();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：Highnote payment card、financial account、authorized user/cardholder、card product 映射到本系统挂载范围。
     * 输入：同一规则版本分别挂载到支付工具、资金账户、信用账户、账户层级和业务场景。
     * 输出：按 scope 精确查询，账户层级解释保留本系统 scope 证据引用。
     * 红线：这些挂载只是控制事实，不得创建决策记录、资金交易或账本事实，也不得把授权使用人当作资金主体。
     */
    @Test
    void testAssignVersionShouldSupportHighnoteScopeMappingsWithoutFundsSideEffect() {
        publishRuleVersions();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_payment_card",
                RULE_VERSION,
                PAYMENT_INSTRUMENT_SN,
                SpendRuleScopeType.PAYMENT_INSTRUMENT));
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_funding_account",
                RULE_VERSION,
                FUNDING_ACCOUNT_SCOPE_ID,
                SpendRuleScopeType.FUNDING_ACCOUNT));
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_credit_account",
                RULE_VERSION,
                CREDIT_ACCOUNT_SCOPE_ID,
                SpendRuleScopeType.CREDIT_ACCOUNT));
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_account_hierarchy",
                RULE_VERSION,
                ACCOUNT_HIERARCHY_SCOPE_ID,
                SpendRuleScopeType.ACCOUNT_HIERARCHY));
        spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_card_product",
                RULE_VERSION,
                CARD_PRODUCT_SCOPE_ID,
                SpendRuleScopeType.BUSINESS_SCENE));

        assertThat(queryAssignmentSns(SpendRuleScopeType.PAYMENT_INSTRUMENT, PAYMENT_INSTRUMENT_SN))
                .containsExactly(ASSIGNMENT_SN + "_payment_card");
        assertThat(queryAssignmentSns(SpendRuleScopeType.FUNDING_ACCOUNT, FUNDING_ACCOUNT_SCOPE_ID))
                .containsExactly(ASSIGNMENT_SN + "_funding_account");
        assertThat(queryAssignmentSns(SpendRuleScopeType.CREDIT_ACCOUNT, CREDIT_ACCOUNT_SCOPE_ID))
                .containsExactly(ASSIGNMENT_SN + "_credit_account");
        assertThat(queryAssignmentSns(SpendRuleScopeType.ACCOUNT_HIERARCHY, ACCOUNT_HIERARCHY_SCOPE_ID))
                .containsExactly(ASSIGNMENT_SN + "_account_hierarchy");
        assertThat(queryAssignmentSns(SpendRuleScopeType.BUSINESS_SCENE, CARD_PRODUCT_SCOPE_ID))
                .containsExactly(ASSIGNMENT_SN + "_card_product");
        assertThat(explainAssignment(ASSIGNMENT_SN + "_account_hierarchy").getEvidenceRefs())
                .contains("spendRuleScope:" + SpendRuleScopeType.ACCOUNT_HIERARCHY + ":"
                        + ACCOUNT_HIERARCHY_SCOPE_ID);
        assertNoDecisionRecord();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：挂载未发布版本或缺少有效窗口。
     * 输入：不存在的版本、缺冲突策略、结束时间早于开始时间。
     * 输出：请求被拒绝。
     * 红线：失败不得留下挂载、决策记录、资金交易或账本事实。
     */
    @Test
    void testAssignVersionShouldRejectInvalidRequestWithoutFundsSideEffect() {
        spendRuleDefinitionService.createDefinition(createDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_missing_version",
                "missing-version",
                PAYMENT_INSTRUMENT_SN)))
                .hasMessageContaining("Spend Rule 版本不存在");
        assertThatThrownBy(() -> spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_no_policy",
                RULE_VERSION,
                PAYMENT_INSTRUMENT_SN)
                .setConflictPolicy(null)))
                .hasMessageContaining("Spend Rule 挂载冲突策略不能为空");
        assertThatThrownBy(() -> spendRuleDefinitionService.assignVersion(assignmentRequest(
                ASSIGNMENT_SN + "_invalid_window",
                RULE_VERSION,
                PAYMENT_INSTRUMENT_SN)
                .setEffectiveFrom(EFFECTIVE_TO)
                .setEffectiveTo(EFFECTIVE_FROM)))
                .hasMessageContaining("Spend Rule 挂载生效结束时间必须晚于开始时间");
        assertThat(countAssignments()).isZero();
        assertNoDecisionRecord();
        assertNoTransactionFacts();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendRuleDefinitionServiceTestData() {
        cleanupSpendRuleDefinitionServiceTestData();
    }

    @AfterEach
    void tearDownSpendRuleDefinitionServiceTestData() {
        cleanupSpendRuleDefinitionServiceTestData();
    }

    private void publishRuleVersions() {
        spendRuleDefinitionService.createDefinition(createDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));
        spendRuleDefinitionService.publishVersion(
                publishVersionRequest(FUTURE_RULE_DIGEST, RULE_SPEC).setRuleVersion(FUTURE_RULE_VERSION));
        spendRuleDefinitionService.publishVersion(
                publishVersionRequest(EXPIRED_RULE_DIGEST, RULE_SPEC).setRuleVersion(EXPIRED_RULE_VERSION));
    }

    private SpendRuleAssignmentExplanationDTO explainAssignment(String assignmentSn) {
        return spendRuleAssignmentService.explainAssignment(
                new SpendRuleAssignmentExplainQuery()
                        .setTenantId(TENANT_ID)
                        .setAssignmentSn(assignmentSn));
    }

    private CreateSpendRuleDefinitionRequest createDefinitionRequest() {
        return new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleName("VCC 单卡日限额")
                .setRuleType(SpendRuleType.AMOUNT_LIMIT)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("用于验证 Spend Rule 定义标准基础服务");
    }

    private PublishSpendRuleVersionRequest publishVersionRequest(String ruleDigest, String ruleSpec) {
        return new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setRuleSpec(ruleSpec)
                .setRuleDigest(ruleDigest)
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:SPEND-RULE-DEFINITION-VERSION-ASSIGNMENT-SERVICE-SPLIT")
                .setDescription("发布 Spend Rule 版本");
    }

    private AssignSpendRuleVersionRequest assignmentRequest(String assignmentSn,
                                                            String ruleVersion,
                                                            String scopeId) {
        return assignmentRequest(assignmentSn, ruleVersion, scopeId, SpendRuleScopeType.PAYMENT_INSTRUMENT);
    }

    private AssignSpendRuleVersionRequest assignmentRequest(String assignmentSn,
                                                            String ruleVersion,
                                                            String scopeId,
                                                            SpendRuleScopeType scopeType) {
        return new AssignSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setAssignmentSn(assignmentSn)
                .setRuleId(RULE_ID)
                .setRuleVersion(ruleVersion)
                .setScopeType(scopeType)
                .setScopeId(scopeId)
                .setPriority(10)
                .setConflictPolicy(SpendRuleConflictPolicy.DENY_OVERRIDES)
                .setEffectiveFrom(EFFECTIVE_FROM)
                .setEffectiveTo(EFFECTIVE_TO)
                .setDescription("挂载 Spend Rule 版本");
    }

    private List<String> queryAssignmentSns(SpendRuleScopeType scopeType, String scopeId) {
        return spendRuleAssignmentService.queryAssignments(new SpendRuleAssignmentQuery()
                        .setTenantId(TENANT_ID)
                        .setScopeType(scopeType)
                        .setScopeId(scopeId)
                        .setEffectiveOnly(Boolean.TRUE))
                .stream()
                .map(SpendRuleAssignmentDTO::getAssignmentSn)
                .toList();
    }

    private void cleanupSpendRuleDefinitionServiceTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
    }

    private int countAssignments() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_rule_assignment
                WHERE tenant_id = ? AND rule_id = ?
                """, Integer.class, TENANT_ID, RULE_ID);
    }

    private void assertNoDecisionRecord() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_rule_decision_record
                WHERE tenant_id = ? AND rule_id = ?
                """, Integer.class, TENANT_ID, RULE_ID);
        assertThat(count).isZero();
    }

    private void assertNoTransactionFacts() {
        assertThat(countRows("t_funds_transaction")).isZero();
        assertThat(countRows("t_funds_transaction_detail")).isZero();
        assertThat(postingPlanCount()).isZero();
        assertThat(countRows("t_ledger_transaction")).isZero();
        assertThat(countRows("t_ledger_entry")).isZero();
    }

    private Integer postingPlanCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, BUSINESS_SN);
    }

    @Configuration
    @Import({
            SpendRuleDefinitionServiceImpl.class,
            SpendRuleVersionServiceImpl.class,
            SpendRuleAssignmentServiceImpl.class
    })
    static class Config {
    }
}
