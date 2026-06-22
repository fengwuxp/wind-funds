package com.wind.funds.wallet.application.spend;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.application.spend.impl.SpendRuleDefinitionApplicationServiceImpl;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.enums.SpendRuleDefinitionStatus;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.enums.SpendRuleVersionStatus;
import com.wind.funds.wallet.model.dto.SpendRuleAssignmentDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDecisionLogDTO;
import com.wind.funds.wallet.model.dto.SpendRuleDefinitionDTO;
import com.wind.funds.wallet.model.dto.SpendRuleVersionDTO;
import com.wind.funds.wallet.model.request.AssignSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionLogRequest;
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

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spend Rule 定义、版本、挂载和决策日志应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendRuleDefinitionApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendRuleDefinitionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String RULE_ID = "sr_definition_contract_daily_limit";

    private static final String RULE_VERSION = "2026-06-22.1";

    private static final String RULE_SPEC = """
            {"window":"DAILY","amount":10000,"currency":"USD","scope":"CARD"}
            """;

    private static final String RULE_DIGEST = "sha256:spend-rule-definition-contract";

    private static final String CHANGED_RULE_DIGEST = "sha256:changed-spend-rule-definition-contract";

    private static final String ASSIGNMENT_SN = "spend_rule_assignment_definition_contract_001";

    private static final String DECISION_SN = "spend_rule_decision_definition_contract_001";

    private static final String BUSINESS_SCENE = "SPEND_RULE_DEFINITION_CONTRACT";

    private static final String BUSINESS_SN = "SPEND_RULE_DEFINITION_CONTRACT_001";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_rule_definition_card";

    private static final String BUDGET_GROUP_SN = "spend_rule_definition_budget";

    @Autowired
    private SpendRuleDefinitionApplicationService spendRuleDefinitionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：规则定义创建后发布一个不可变版本。
     * 输入：同一规则版本第二次发布时更换规则摘要。
     * 输出：重复同摘要请求幂等返回，换摘要请求被拒绝。
     * 红线：已发布版本不得原地改写，避免历史交易按新规则被重新解释。
     */
    @Test
    void testPublishedSpendRuleVersionShouldRejectInPlaceOverwrite() {
        SpendRuleDefinitionDTO definition =
                spendRuleDefinitionApplicationService.createDefinition(createDefinitionRequest());
        SpendRuleVersionDTO published =
                spendRuleDefinitionApplicationService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));

        SpendRuleVersionDTO replayed =
                spendRuleDefinitionApplicationService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));

        assertThat(definition.getRuleId()).isEqualTo(RULE_ID);
        assertThat(definition.getStatus()).isEqualTo(SpendRuleDefinitionStatus.ACTIVE);
        assertThat(published.getRuleId()).isEqualTo(RULE_ID);
        assertThat(published.getRuleVersion()).isEqualTo(RULE_VERSION);
        assertThat(published.getStatus()).isEqualTo(SpendRuleVersionStatus.PUBLISHED);
        assertThat(replayed.getId()).isEqualTo(published.getId());
        assertThatThrownBy(() -> spendRuleDefinitionApplicationService.publishVersion(
                publishVersionRequest(CHANGED_RULE_DIGEST, "{\"window\":\"MONTHLY\"}")))
                .hasMessageContaining("Spend Rule 版本已发布但内容摘要不一致");
    }

    /**
     * 场景：规则版本挂载到支付工具和预算控制范围。
     * 输入：支付工具 scope 与预算组 scope。
     * 输出：只产生规则挂载记录。
     * 红线：挂载对象只是控制范围，不能输出预算组、支付工具或 Spend Rule 作为资金责任主体。
     */
    @Test
    void testAssignSpendRuleVersionShouldOnlyRecordControlScopeWithoutFundsSideEffect() {
        publishRuleVersion();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleAssignmentDTO instrumentAssignment = spendRuleDefinitionApplicationService.assignVersion(
                assignmentRequest(ASSIGNMENT_SN, SpendRuleScopeType.PAYMENT_INSTRUMENT, PAYMENT_INSTRUMENT_SN));
        SpendRuleAssignmentDTO budgetAssignment = spendRuleDefinitionApplicationService.assignVersion(
                assignmentRequest(ASSIGNMENT_SN + "_budget", SpendRuleScopeType.BUDGET_GROUP, BUDGET_GROUP_SN));

        assertThat(instrumentAssignment.getAssignmentSn()).isEqualTo(ASSIGNMENT_SN);
        assertThat(instrumentAssignment.getScopeType()).isEqualTo(SpendRuleScopeType.PAYMENT_INSTRUMENT);
        assertThat(instrumentAssignment.getStatus()).isEqualTo(SpendRuleAssignmentStatus.ACTIVE);
        assertThat(budgetAssignment.getScopeType()).isEqualTo(SpendRuleScopeType.BUDGET_GROUP);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：Spend Rule 决策拒绝支付工具交易。
     * 输入：已发布规则、已挂载规则和拒绝决策日志。
     * 输出：记录拒绝决策日志。
     * 红线：拒绝决策不得生成 route、posting、LedgerEntry、资金交易或账本余额副作用。
     */
    @Test
    void testRecordRejectedSpendRuleDecisionShouldNotCreateFundsFacts() {
        publishRuleVersion();
        spendRuleDefinitionApplicationService.assignVersion(
                assignmentRequest(ASSIGNMENT_SN, SpendRuleScopeType.PAYMENT_INSTRUMENT, PAYMENT_INSTRUMENT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendRuleDecisionLogDTO decision = spendRuleDefinitionApplicationService.recordDecision(
                rejectedDecisionRequest());

        assertThat(decision.getDecisionSn()).isEqualTo(DECISION_SN);
        assertThat(decision.getDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("超过单卡日限额");
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具范围的 Spend Rule 决策日志需要用于交易投影和对账解释。
     * 输入：控制范围是支付工具，但支付工具号缺失或与控制范围不一致。
     * 输出：请求被拒绝。
     * 红线：不得留下 scope 与支付工具引用不一致的决策证据，失败不得产生资金事实。
     */
    @Test
    void testPaymentInstrumentDecisionShouldRequireMatchedInstrumentReference() {
        publishRuleVersion();
        spendRuleDefinitionApplicationService.assignVersion(
                assignmentRequest(ASSIGNMENT_SN, SpendRuleScopeType.PAYMENT_INSTRUMENT, PAYMENT_INSTRUMENT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDefinitionApplicationService.recordDecision(
                rejectedDecisionRequest().setInstrumentSn(null)))
                .hasMessageContaining("支付工具范围的 Spend Rule 决策必须携带支付工具号");
        assertThatThrownBy(() -> spendRuleDefinitionApplicationService.recordDecision(
                rejectedDecisionRequest().setInstrumentSn(PAYMENT_INSTRUMENT_SN + "_other")))
                .hasMessageContaining("Spend Rule 决策支付工具号与控制范围不一致");
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：Spend Rule 决策结果为通过。
     * 输入：通过型决策仍携带拒绝原因。
     * 输出：请求被拒绝。
     * 红线：通过和拒绝原因不能同时出现，避免投影解释、客服审计和对账报告出现矛盾证据。
     */
    @Test
    void testPassedSpendRuleDecisionShouldRejectRejectReason() {
        publishRuleVersion();
        spendRuleDefinitionApplicationService.assignVersion(
                assignmentRequest(ASSIGNMENT_SN, SpendRuleScopeType.PAYMENT_INSTRUMENT, PAYMENT_INSTRUMENT_SN));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendRuleDefinitionApplicationService.recordDecision(
                passedDecisionRequest().setRejectReason("误携带拒绝原因")))
                .hasMessageContaining("非拒绝 Spend Rule 决策不能携带拒绝原因");
        assertNoSpendRuleDecisionLog(DECISION_SN + "_passed");
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendRuleDefinitionTestData() {
        cleanupSpendRuleDefinitionTestData();
    }

    @AfterEach
    void tearDownSpendRuleDefinitionTestData() {
        cleanupSpendRuleDefinitionTestData();
    }

    private void publishRuleVersion() {
        spendRuleDefinitionApplicationService.createDefinition(createDefinitionRequest());
        spendRuleDefinitionApplicationService.publishVersion(publishVersionRequest(RULE_DIGEST, RULE_SPEC));
    }

    private CreateSpendRuleDefinitionRequest createDefinitionRequest() {
        return new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleName("VCC 单卡日限额")
                .setRuleType(SpendRuleType.AMOUNT_LIMIT)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("用于验证 Spend Rule 定义契约");
    }

    private PublishSpendRuleVersionRequest publishVersionRequest(String ruleDigest, String ruleSpec) {
        return new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setRuleSpec(ruleSpec)
                .setRuleDigest(ruleDigest)
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:GSD2-B5-SPEND-RULE-DEFINITION-CONTRACT-001")
                .setDescription("发布 Spend Rule 版本");
    }

    private AssignSpendRuleVersionRequest assignmentRequest(String assignmentSn,
                                                            SpendRuleScopeType scopeType,
                                                            String scopeId) {
        return new AssignSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setAssignmentSn(assignmentSn)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setScopeType(scopeType)
                .setScopeId(scopeId)
                .setPriority(10)
                .setDescription("挂载 Spend Rule 版本");
    }

    private RecordSpendRuleDecisionLogRequest rejectedDecisionRequest() {
        return new RecordSpendRuleDecisionLogRequest()
                .setTenantId(TENANT_ID)
                .setDecisionSn(DECISION_SN)
                .setRuleId(RULE_ID)
                .setRuleVersion(RULE_VERSION)
                .setAssignmentSn(ASSIGNMENT_SN)
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
                .setDecisionDigest("sha256:spend-rule-rejected-decision");
    }

    private RecordSpendRuleDecisionLogRequest passedDecisionRequest() {
        return rejectedDecisionRequest()
                .setDecisionSn(DECISION_SN + "_passed")
                .setAmount(9000L)
                .setDecisionResult(SpendControlDecisionResult.PASSED)
                .setRejectReason(null)
                .setDecisionDigest("sha256:spend-rule-passed-decision");
    }

    private void cleanupSpendRuleDefinitionTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_log WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_assignment WHERE tenant_id = ? AND rule_id = ?",
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

    private void assertNoSpendRuleDecisionLog(String decisionSn) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_spend_rule_decision_log WHERE tenant_id = ? AND decision_sn = ?",
                Integer.class,
                TENANT_ID,
                decisionSn);
        assertThat(count).isZero();
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

    @Configuration
    @Import({
            SpendRuleDefinitionApplicationServiceImpl.class
    })
    static class Config {
    }
}
