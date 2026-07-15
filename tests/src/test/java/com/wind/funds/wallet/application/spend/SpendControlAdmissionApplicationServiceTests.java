package com.wind.funds.wallet.application.spend;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingConcurrencyGuard;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDefinitionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDecisionRecordServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleVersionServiceImpl;
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

import java.time.LocalDateTime;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 支出控制准入应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        SpendControlAdmissionApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpendControlAdmissionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "spend_control_credit_account";

    private static final String PAYMENT_INSTRUMENT_SN = "spend_control_payment_card";

    private static final String PAYMENT_BINDING_SN = "spend_control_payment_binding";

    private static final String OWNER_ID = "spend_control_owner";

    private static final String CHANNEL_CODE = "spend_control_channel";

    private static final String BUSINESS_SCENE = "SPEND_CONTROL_ADMISSION";

    private static final String BUSINESS_SN = "SPEND_CONTROL_ADMISSION_001";

    private static final String TENANT_MISMATCH_BUSINESS_SN = "SPEND_CONTROL_ADMISSION_TENANT_MISMATCH";

    private static final String SPEND_RULE_ID = "sr_vcc_daily_limit";

    private static final String SPEND_RULE_VERSION = "2026-06-19.1";

    private static final String SPEND_RULE_BINDING_AUDIT_REFERENCE_SN = "grant:spend_control_rule_binding";

    private static final String SPEND_RULE_DIGEST = "sha256:spend-control-rule-version";

    private static final String SPEND_DECISION_SN = "decision_spend_control_001";

    private static final String SPEND_DECISION_DIGEST = "sha256:spend-control-admission";

    private static final String MULTI_RULE_FINAL_DECISION_SN = "decision_spend_control_multi_rule_final_001";

    private static final String MULTI_RULE_FINAL_DECISION_DIGEST = "sha256:multi-rule-final-deny-overrides";

    private static final LocalDateTime SPEND_RULE_EFFECTIVE_FROM = LocalDateTime.now().withNano(0).minusDays(1);

    private static final LocalDateTime SPEND_RULE_EFFECTIVE_TO = LocalDateTime.now().withNano(0).plusDays(30);

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendRuleDefinitionService spendRuleDefinitionService;

    @Autowired
    private SpendControlAdmissionApplicationService spendControlAdmissionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String spendRuleBindingSn;

    /**
     * 场景：支付工具预交易快照通过后，Spend Rule 决策也通过。
     * 输入：支付工具、资金责任和账户能力均可用，并带有规则 ID、版本、决策流水和摘要。
     * 输出：返回可审计支出控制准入快照。
     * 红线：准入快照只读，不得创建交易、route、posting、LedgerEntry 或余额投影事实。
     */
    @Test
    void testResolveSpendControlAdmissionShouldPassWithDecisionEvidenceWithoutFundsSideEffect() {
        prepareSpendControlAdmissionData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlAdmissionDecisionDTO decision =
                spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                        admissionRequest().setSpendDecisionResult(SpendControlDecisionResult.PASSED));

        assertThat(decision.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(decision.getBusinessScene()).isEqualTo(BUSINESS_SCENE);
        assertThat(decision.getBusinessSn()).isEqualTo(BUSINESS_SN);
        assertThat(decision.getInstrumentSn()).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(decision.getAction()).isEqualTo(PaymentInstrumentAction.AUTHORIZE);
        assertThat(decision.getAmount()).isEqualTo(60L);
        assertThat(decision.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
        assertThat(decision.getAdmitted()).isTrue();
        assertThat(decision.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(decision.getSpendRuleId()).isEqualTo(SPEND_RULE_ID);
        assertThat(decision.getSpendRuleVersion()).isEqualTo(SPEND_RULE_VERSION);
        assertThat(decision.getSpendRuleBindingSn()).isEqualTo(spendRuleBindingSn);
        assertThat(decision.getSpendRuleScopeType()).isEqualTo(SpendRuleScopeType.PAYMENT_INSTRUMENT);
        assertThat(decision.getSpendRuleScopeId()).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(decision.getSpendDecisionSn()).isEqualTo(SPEND_DECISION_SN);
        assertThat(decision.getSpendDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        assertThat(decision.getSpendDecisionDigest()).isEqualTo(SPEND_DECISION_DIGEST);
        assertThat(decision.getSpendDecisionRecordId()).isNotNull();
        assertSpendRuleDecisionRecord(SpendControlDecisionResult.PASSED, null);
        assertThat(decision.getPreTransactionSnapshot().getReady()).isTrue();
        assertThat(decision.getPreTransactionSnapshot().getFundsAccountCapability().getCapabilities())
                .containsExactly(FundsAccountCapability.PAY);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具预交易快照通过，但上游或外部 Spend Rule 决策拒绝。
     * 输入：支付工具和账户能力可用，上游最终规则决策结果为拒绝，并带有拒绝原因。
     * 输出：返回 admitted=false 的准入决策，不抛出系统异常。
     * 红线：业务拒绝不得创建交易、route、posting、LedgerEntry 或余额投影事实。
     */
    @Test
    void testResolveSpendControlAdmissionShouldRejectWithDecisionEvidenceWithoutFundsSideEffect() {
        prepareSpendControlAdmissionData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlAdmissionDecisionDTO decision =
                spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                        admissionRequest()
                                .setSpendDecisionResult(SpendControlDecisionResult.REJECTED)
                                .setRejectReason("超过单卡单日授权限额"));

        assertThat(decision.getAdmitted()).isFalse();
        assertThat(decision.getSpendDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo("超过单卡单日授权限额");
        assertThat(decision.getSpendDecisionRecordId()).isNotNull();
        assertSpendRuleDecisionRecord(SpendControlDecisionResult.REJECTED, "超过单卡单日授权限额");
        assertThat(decision.getTargetAccountId())
                .isEqualTo(FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT));
        assertThat(decision.getPreTransactionSnapshot().getReady()).isTrue();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：上游已完成多条 Spend Rule 的冲突裁决，本服务只消费最终裁决证据。
     * 输入：单笔限额通过、MCC 黑名单拒绝后，上游按 DENY_OVERRIDES 给出最终拒绝流水和摘要。
     * 输出：准入固化最终决策流水、结果、摘要和拒绝原因，不要求传入每条 evaluatedRules 明细。
     * 红线：wallet 不重算多规则、不展开明细落库，不创建交易、route、posting、LedgerEntry 或账本余额事实。
     */
    @Test
    void testResolveSpendControlAdmissionShouldConsumeUpstreamMultiRuleFinalDecisionDigest() {
        prepareSpendControlAdmissionData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        String finalRejectReason = "MCC 黑名单命中，DENY_OVERRIDES 最终拒绝";

        SpendControlAdmissionDecisionDTO decision =
                spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                        admissionRequest()
                                .setSpendDecisionSn(MULTI_RULE_FINAL_DECISION_SN)
                                .setSpendDecisionDigest(MULTI_RULE_FINAL_DECISION_DIGEST)
                                .setSpendDecisionResult(SpendControlDecisionResult.REJECTED)
                                .setRejectReason(finalRejectReason));

        assertThat(decision.getAdmitted()).isFalse();
        assertThat(decision.getSpendDecisionSn()).isEqualTo(MULTI_RULE_FINAL_DECISION_SN);
        assertThat(decision.getSpendDecisionDigest()).isEqualTo(MULTI_RULE_FINAL_DECISION_DIGEST);
        assertThat(decision.getSpendDecisionResult()).isEqualTo(SpendControlDecisionResult.REJECTED);
        assertThat(decision.getRejectReason()).isEqualTo(finalRejectReason);
        assertThat(decision.getSpendDecisionRecordId()).isNotNull();
        assertSpendRuleDecisionRecord(MULTI_RULE_FINAL_DECISION_SN, MULTI_RULE_FINAL_DECISION_DIGEST,
                SpendControlDecisionResult.REJECTED, finalRejectReason);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：上游或外部 Spend Rule 决策通过，但 wallet 资金责任基础事实缺失。
     * 输入：携带 PASSED 决策证据，支付工具绑定和账户存在，但资金责任关系不存在。
     * 输出：准入停在预交易快照阶段，不固化 Spend Rule 决策记录。
     * 红线：外部 approve 不代表资金可用，不能绕过支付工具、账户能力和资金责任校验。
     */
    @Test
    void testResolveSpendControlAdmissionShouldRejectExternalApproveWhenFundingResponsibilityMissingWithoutSideEffect() {
        prepareSpendControlAdmissionData();
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID,
                CREDIT_ACCOUNT_SN);
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest().setSpendDecisionResult(SpendControlDecisionResult.PASSED)))
                .hasMessageContaining("资金责任关系不存在");

        assertThat(decisionRecordCount()).isZero();
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方缺少 Spend Rule 决策证据。
     * 输入：没有规则版本。
     * 输出：准入阶段失败。
     * 红线：证据不完整不得创建交易、route、posting、LedgerEntry 或余额投影事实。
     */
    @Test
    void testResolveSpendControlAdmissionShouldRejectMissingDecisionEvidenceWithoutFundsSideEffect() {
        prepareSpendControlAdmissionData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest()
                        .setSpendRuleVersion(null)
                        .setSpendDecisionResult(SpendControlDecisionResult.PASSED)))
                .hasMessageContaining("Spend Rule 版本不能为空");

        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：Spend Rule 准入请求租户与当前线程租户不一致。
     * 输入：当前线程租户为 1，请求 tenantId 为 2。
     * 输出：应用层入口直接拒绝，不写决策记录、资金交易或账本事实。
     * 红线：控制准入写决策记录前必须守住租户边界。
     */
    @Test
    void testResolveSpendControlAdmissionShouldRejectTenantMismatchWithoutSideEffect() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest()
                        .setTenantId(TENANT_ID + 1)
                        .setBusinessSn(TENANT_MISMATCH_BUSINESS_SN)
                        .setSpendDecisionResult(SpendControlDecisionResult.PASSED)))
                .hasMessageContaining("支出控制准入 tenantId 与当前租户不一致");

        assertThat(decisionRecordCount()).isZero();
        assertNoTransactionFacts(TENANT_MISMATCH_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：调用方重复提交相同 Spend Rule 决策流水和摘要。
     * 输入：同一个规则决策流水号重复准入。
     * 输出：复用同一条决策记录，准入结果幂等。
     * 红线：重复准入不得创建第二条决策记录、交易事实或账本事实。
     */
    @Test
    void testResolveSpendControlAdmissionShouldReuseSameDecisionRecordForIdempotentDecisionEvidence() {
        prepareSpendControlAdmissionData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        SpendControlAdmissionDecisionDTO firstDecision =
                spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                        admissionRequest().setSpendDecisionResult(SpendControlDecisionResult.PASSED));
        SpendControlAdmissionDecisionDTO replayedDecision =
                spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                        admissionRequest().setSpendDecisionResult(SpendControlDecisionResult.PASSED));

        assertThat(replayedDecision.getSpendDecisionRecordId()).isEqualTo(firstDecision.getSpendDecisionRecordId());
        assertThat(decisionRecordCount()).isEqualTo(1);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一 Spend Rule 决策流水被不同决策摘要复用。
     * 输入：第一次通过，第二次使用相同决策流水但摘要不同。
     * 输出：准入阶段拒绝。
     * 红线：摘要冲突不得创建交易事实或账本事实。
     */
    @Test
    void testResolveSpendControlAdmissionShouldRejectDecisionDigestConflictWithoutFundsSideEffect() {
        prepareSpendControlAdmissionData();
        spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest().setSpendDecisionResult(SpendControlDecisionResult.PASSED));
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> spendControlAdmissionApplicationService.resolveSpendControlAdmission(
                admissionRequest()
                        .setSpendDecisionResult(SpendControlDecisionResult.PASSED)
                        .setSpendDecisionDigest("sha256:changed-spend-control-admission")))
                .hasMessageContaining("Spend Rule 决策流水已存在但内容不一致");

        assertThat(decisionRecordCount()).isEqualTo(1);
        assertNoTransactionFacts(BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpSpendControlAdmissionTestData() {
        cleanupSpendControlAdmissionTestData();
    }

    @AfterEach
    void tearDownSpendControlAdmissionTestData() {
        cleanupSpendControlAdmissionTestData();
    }

    private void prepareSpendControlAdmissionData() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        spendRuleDefinitionService.createDefinition(createSpendRuleDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishSpendRuleVersionRequest());
        spendRuleBindingSn = spendRuleDefinitionService.createSpendRuleBinding(createSpendRuleBindingRequest()).getSn();
    }

    private void cleanupSpendControlAdmissionTestData() {
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_binding WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID,
                CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn = ?",
                PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn = ?", PAYMENT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
    }

    private ResolveSpendControlAdmissionRequest admissionRequest() {
        return new ResolveSpendControlAdmissionRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(1)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendRuleBindingSn(spendRuleBindingSn)
                .setSpendRuleScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setSpendRuleScopeId(PAYMENT_INSTRUMENT_SN)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setSpendDecisionDigest(SPEND_DECISION_DIGEST);
    }

    private CreateSpendRuleDefinitionRequest createSpendRuleDefinitionRequest() {
        return new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleName("Spend Control Admission Rule")
                .setRuleType(SpendRuleType.AMOUNT_LIMIT)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("支出控制准入决策消费测试规则");
    }

    private PublishSpendRuleVersionRequest publishSpendRuleVersionRequest() {
        return new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleVersion(SPEND_RULE_VERSION)
                .setRuleSpec("{\"dslCaseId\":\"DSL-SPEND-RULE-DECISION-CONSUME-001\"}")
                .setRuleDigest(SPEND_RULE_DIGEST)
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:SPEND-RULE-DECISION-CONSUME")
                .setDescription("发布支出控制准入消费规则版本");
    }

    private CreateSpendRuleBindingRequest createSpendRuleBindingRequest() {
        return new CreateSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleVersion(SPEND_RULE_VERSION)
                .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setScopeId(PAYMENT_INSTRUMENT_SN)
                .setPriority(10)
                .setConflictPolicy(SpendRuleConflictPolicy.DENY_OVERRIDES)
                .setEffectiveFrom(SPEND_RULE_EFFECTIVE_FROM)
                .setEffectiveTo(SPEND_RULE_EFFECTIVE_TO)
                .setAuditReferenceSn(SPEND_RULE_BINDING_AUDIT_REFERENCE_SN)
                .setDescription("挂载到支付工具准入 scope");
    }

    private CreateCreditAccountRequest createCreditAccountRequest() {
        return new CreateCreditAccountRequest()
                .setSn(CREDIT_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(CreditFundsAccountType.SHARED_CARD.name())
                .setCurrency(CurrencyIsoCode.USD)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setLedgerProfileCode(LedgerProfileCode.CREDIT_BASIC)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest() {
        return new CreatePaymentInstrumentRequest()
                .setSn(PAYMENT_INSTRUMENT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setFlowDirection(PaymentInstrumentFlowDirection.OUTBOUND)
                .setInstrumentNo("****2468")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_spend_control_2468")
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(PAYMENT_BINDING_SN)
                .setRequestSn(PAYMENT_BINDING_SN + "_create")
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setSubjectId(CREDIT_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest() {
        return new CreateSpendSubjectFundingRelationRequest()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(CREDIT_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE);
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

    private void assertSpendRuleDecisionRecord(SpendControlDecisionResult decisionResult, String rejectReason) {
        assertSpendRuleDecisionRecord(SPEND_DECISION_SN, SPEND_DECISION_DIGEST, decisionResult, rejectReason);
    }

    private void assertSpendRuleDecisionRecord(String decisionSn,
                                               String decisionDigest,
                                               SpendControlDecisionResult decisionResult,
                                               String rejectReason) {
        assertThat(jdbcTemplate.queryForObject("""
                        SELECT COUNT(*) FROM t_spend_rule_decision_record
                        WHERE tenant_id = ? AND decision_sn = ? AND rule_id = ? AND rule_version = ?
                          AND spend_rule_binding_sn = ? AND scope_type = ? AND scope_id = ?
                          AND instrument_sn = ? AND business_scene = ? AND business_sn = ?
                          AND decision_result = ? AND decision_digest = ?
                """,
                Integer.class,
                TENANT_ID,
                decisionSn,
                SPEND_RULE_ID,
                SPEND_RULE_VERSION,
                spendRuleBindingSn,
                SpendRuleScopeType.PAYMENT_INSTRUMENT.name(),
                PAYMENT_INSTRUMENT_SN,
                PAYMENT_INSTRUMENT_SN,
                BUSINESS_SCENE,
                BUSINESS_SN,
                decisionResult.name(),
                decisionDigest)).isEqualTo(1);
        String actualRejectReason = jdbcTemplate.queryForObject("""
                        SELECT reject_reason FROM t_spend_rule_decision_record
                        WHERE tenant_id = ? AND decision_sn = ?
                        """,
                String.class,
                TENANT_ID,
                decisionSn);
        assertThat(actualRejectReason).isEqualTo(rejectReason);
    }

    private Integer decisionRecordCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_spend_rule_decision_record
                WHERE tenant_id = ? AND decision_sn = ?
                """, Integer.class, TENANT_ID, SPEND_DECISION_SN);
    }

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            PaymentInstrumentBindingConcurrencyGuard.class,
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class,
            FundingResponsibilityResolutionApplicationServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl.class,
            SpendRuleDefinitionServiceImpl.class,
            SpendRuleVersionServiceImpl.class,
            SpendRuleBindingServiceImpl.class,
            SpendRuleDecisionRecordServiceImpl.class,
            SpendControlAdmissionApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}
