package com.wind.funds.wallet.application.spend;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.application.spend.SpendControlTransactionConsumptionRequest;
import com.wind.funds.transaction.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.BudgetControlLimitAdjustmentApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendRuleEvaluationApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.BudgetControlLimitAdjustmentResultDTO;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.SpendControlAdmissionDecisionDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.dto.SpendRuleEvaluationDecisionDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.request.AdjustBudgetControlLimitRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.EvaluateSpendRuleRequest;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
import com.wind.funds.wallet.model.request.ResolveSpendControlAdmissionRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.service.SpendRuleDecisionRecordService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlMovementServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDecisionRecordServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDefinitionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleVersionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * wallet spend controls 接入验收流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        WalletSpendControlsAcceptanceFlowTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WalletSpendControlsAcceptanceFlowTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "wallet_spend_controls_credit";

    private static final String PAYMENT_INSTRUMENT_SN = "wallet_spend_controls_card";

    private static final String OWNER_ID = "wallet_spend_controls_owner";

    private static final String CHANNEL_CODE = "wallet_spend_controls_channel";

    private static final String BUSINESS_SCENE = "WALLET_SPEND_CONTROLS_ACCEPTANCE";

    private static final String BUSINESS_SN = "WALLET_SPEND_CONTROLS_ACCEPTANCE_001";

    private static final String REFUND_BUSINESS_SN = "WALLET_SPEND_CONTROLS_ACCEPTANCE_REFUND_001";

    private static final String SPEND_RULE_ID = "sr_wallet_spend_controls_monthly_amount";

    private static final String SPEND_RULE_VERSION = "2026-07-02.1";

    private static final String SPEND_RULE_BINDING_AUDIT_REFERENCE_SN = "grant:wallet_spend_controls_binding";

    private static final String SPEND_RULE_DIGEST = "sha256:wallet-spend-controls-rule";

    private static final String SPEND_DECISION_SN = "decision_wallet_spend_controls_001";

    private static final String CONTROL_SCOPE_ID = "wallet_spend_controls_scope";

    private static final String PERIOD_ID = "2026-07";

    private static final String LIMIT_MOVEMENT_SN = "wallet_spend_controls_limit_001";

    private static final String RESERVED_MOVEMENT_SN = "wallet_spend_controls_reserved_001";

    private static final String CONSUMED_MOVEMENT_SN = "wallet_spend_controls_consumed_001";

    private static final String REFUND_MOVEMENT_SN = "wallet_spend_controls_refund_001";

    private static final String FUNDS_TRANSACTION_SN = "wallet_spend_controls_pay_tx_001";

    private static final String REFUND_TRANSACTION_SN = "wallet_spend_controls_refund_tx_001";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendRuleDefinitionService spendRuleDefinitionService;

    @Autowired
    private SpendRuleEvaluationApplicationService spendRuleEvaluationApplicationService;

    @Autowired
    private SpendControlAdmissionApplicationService spendControlAdmissionApplicationService;

    @Autowired
    private SpendRuleDecisionRecordService spendRuleDecisionRecordService;

    @Autowired
    private BudgetControlLimitAdjustmentApplicationService budgetControlLimitAdjustmentApplicationService;

    @Autowired
    private SpendControlMovementService spendControlMovementService;

    @Autowired
    private SpendControlTransactionConsumptionApplicationServiceImpl
            spendControlTransactionConsumptionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String spendRuleBindingSn;

    /**
     * 场景：企业钱包用 Spend Rule 控制员工卡月度授权额度。
     * 输入：已发布的月度金额规则、支付工具、信用账户、资金责任关系和当期额度。
     * 输出：评估通过，准入固化决策证据，预留额度，交易成功后消费额度，退款后补偿额度。
     * 红线：Spend Controls 只写规则决策和控制额度事实，不能创建或修改资金交易、route、posting、LedgerEntry 或账本余额事实。
     */
    @Test
    void testWalletSpendControlsShouldSupportMonthlyAuthorizationLifecycleWithoutLedgerSideEffect() {
        prepareAcceptanceData();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        BudgetControlLimitAdjustmentResultDTO limit = budgetControlLimitAdjustmentApplicationService.adjustLimit(
                adjustLimitRequest(), WindOperatorFactory.system());
        assertThat(limit.getProjection().getAvailableControlAmount()).isEqualTo(100L);

        SpendRuleEvaluationDecisionDTO evaluation = spendRuleEvaluationApplicationService.evaluate(
                evaluateRequest(60L));
        assertThat(evaluation.getDecisionResult()).isEqualTo(SpendControlDecisionResult.PASSED);
        recordDecision(evaluation);

        SpendControlAdmissionDecisionDTO admission = spendControlAdmissionApplicationService
                .resolveSpendControlAdmission(admissionRequest(evaluation));
        assertThat(admission.getAdmitted()).isTrue();
        assertThat(admission.getTargetAccountId()).isEqualTo(targetAccountId());
        assertThat(admission.getControlScopeId()).isEqualTo(CONTROL_SCOPE_ID);

        SpendControlMovementDTO reserved = spendControlMovementService.recordMovement(
                reserveRequest(admission));
        assertThat(reserved.getMovementType()).isEqualTo(SpendControlMovementType.RESERVED);

        insertFundsTransaction(FUNDS_TRANSACTION_SN, BUSINESS_SN, DefaultFundsTransactionType.PAY,
                FundsTransactionState.CLOSED, 60L, null);
        SpendControlMovementDTO consumed = spendControlTransactionConsumptionApplicationService.consume(
                transactionConsumptionRequest(CONSUMED_MOVEMENT_SN, FUNDS_TRANSACTION_SN,
                        "sha256:wallet-spend-controls-consumed", 60L));
        assertThat(consumed.getMovementType()).isEqualTo(SpendControlMovementType.CONSUMED);

        BudgetControlProjectionDTO afterConsume = spendControlMovementService.getBudgetControlProjection(
                projectionQuery(PERIOD_ID));
        assertThat(afterConsume.getLimitAmount()).isEqualTo(100L);
        assertThat(afterConsume.getReservedAmount()).isEqualTo(60L);
        assertThat(afterConsume.getConsumedAmount()).isEqualTo(60L);
        assertThat(afterConsume.getRemainingControlAmount()).isZero();
        assertThat(afterConsume.getAvailableControlAmount()).isEqualTo(40L);

        insertFundsTransaction(REFUND_TRANSACTION_SN, REFUND_BUSINESS_SN, DefaultFundsTransactionType.REFUND,
                FundsTransactionState.CLOSED, 20L, FUNDS_TRANSACTION_SN);
        SpendControlMovementDTO refund = spendControlTransactionConsumptionApplicationService.refund(
                transactionConsumptionRequest(REFUND_MOVEMENT_SN, REFUND_TRANSACTION_SN,
                        "sha256:wallet-spend-controls-refund", 20L)
                        .setReasonCode("PRODUCT_POLICY_REFUND_RESTORE")
                        .setOperatorId("spend-control-refund-service")
                        .setAuditReferenceSn("audit:wallet-spend-controls-refund"));
        assertThat(refund.getMovementType()).isEqualTo(SpendControlMovementType.REFUND_COMPENSATED);

        BudgetControlProjectionDTO afterRefund = spendControlMovementService.getBudgetControlProjection(
                projectionQuery(PERIOD_ID));
        assertThat(afterRefund.getLimitAmount()).isEqualTo(100L);
        assertThat(afterRefund.getReservedAmount()).isEqualTo(60L);
        assertThat(afterRefund.getConsumedAmount()).isEqualTo(40L);
        assertThat(afterRefund.getRemainingControlAmount()).isZero();
        assertThat(afterRefund.getAvailableControlAmount()).isEqualTo(60L);

        BudgetControlProjectionDTO emptyHistoricalPeriod = spendControlMovementService.getBudgetControlProjection(
                projectionQuery("2026-06"));
        assertThat(emptyHistoricalPeriod.getLimitAmount()).isZero();
        assertThat(emptyHistoricalPeriod.getAvailableControlAmount()).isZero();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpWalletSpendControlsAcceptanceData() {
        cleanupWalletSpendControlsAcceptanceData();
    }

    @AfterEach
    void tearDownWalletSpendControlsAcceptanceData() {
        cleanupWalletSpendControlsAcceptanceData();
    }

    private void prepareAcceptanceData() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest());
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        spendRuleDefinitionService.createDefinition(createSpendRuleDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishSpendRuleVersionRequest());
        spendRuleBindingSn = spendRuleDefinitionService.createSpendRuleBinding(createSpendRuleBindingRequest()).getSn();
    }

    private AdjustBudgetControlLimitRequest adjustLimitRequest() {
        return new AdjustBudgetControlLimitRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(LIMIT_MOVEMENT_SN)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN + "_LIMIT")
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(PERIOD_ID)
                .setTargetAccountId(targetAccountId())
                .setAmount(100L)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setIncrease(Boolean.TRUE)
                .setReasonCode("MONTHLY_SPEND_RULE_LIMIT")
                .setAuditReferenceSn("approval:wallet-spend-controls-monthly")
                .setMovementDigest("sha256:wallet-spend-controls-limit");
    }

    private EvaluateSpendRuleRequest evaluateRequest(long amount) {
        return new EvaluateSpendRuleRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleVersion(SPEND_RULE_VERSION)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(amount)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(PERIOD_ID)
                .setTargetAccountId(targetAccountId());
    }

    private ResolveSpendControlAdmissionRequest admissionRequest(SpendRuleEvaluationDecisionDTO evaluation) {
        return new ResolveSpendControlAdmissionRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(evaluation.getAmount())
                .setCurrency(evaluation.getCurrency())
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setExpectedBindingVersion(1)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(PERIOD_ID);
    }

    private void recordDecision(SpendRuleEvaluationDecisionDTO evaluation) {
        spendRuleDecisionRecordService.recordDecision(new RecordSpendRuleDecisionRecordRequest()
                .setTenantId(TENANT_ID)
                .setDecisionSn(SPEND_DECISION_SN)
                .setRuleId(evaluation.getRuleId())
                .setRuleVersion(evaluation.getRuleVersion())
                .setSpendRuleBindingSn(spendRuleBindingSn)
                .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setScopeId(PAYMENT_INSTRUMENT_SN)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setInstrumentBindingVersion(1)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(PERIOD_ID)
                .setTargetAccountId(targetAccountId())
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(evaluation.getAmount())
                .setCurrency(evaluation.getCurrency())
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setDecisionResult(evaluation.getDecisionResult())
                .setRejectReason(evaluation.getRejectReason())
                .setDecisionDigest(evaluation.getDecisionDigest()));
    }

    private RecordSpendControlMovementRequest reserveRequest(SpendControlAdmissionDecisionDTO admission) {
        return new RecordSpendControlMovementRequest()
                .setTenantId(admission.getTenantId())
                .setMovementSn(RESERVED_MOVEMENT_SN)
                .setMovementType(SpendControlMovementType.RESERVED)
                .setBusinessScene(admission.getBusinessScene())
                .setBusinessSn(admission.getBusinessSn())
                .setInstrumentSn(admission.getInstrumentSn())
                .setAction(admission.getAction())
                .setTargetAccountId(admission.getTargetAccountId())
                .setAmount(admission.getAmount())
                .setCurrency(admission.getCurrency())
                .setSpendRuleId(admission.getSpendRuleId())
                .setSpendRuleVersion(admission.getSpendRuleVersion())
                .setSpendDecisionSn(admission.getSpendDecisionSn())
                .setSpendDecisionResult(admission.getSpendDecisionResult())
                .setSpendDecisionDigest(admission.getSpendDecisionDigest())
                .setControlScopeId(admission.getControlScopeId())
                .setPeriodId(PERIOD_ID)
                .setMovementDigest("sha256:wallet-spend-controls-reserved");
    }

    private SpendControlTransactionConsumptionRequest transactionConsumptionRequest(String movementSn,
                                                                                   String transactionSn,
                                                                                   String movementDigest,
                                                                                   long amount) {
        return new SpendControlTransactionConsumptionRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(movementSn)
                .setOriginalMovementSn(RESERVED_MOVEMENT_SN)
                .setTransactionSn(transactionSn)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(BUSINESS_SN)
                .setTargetAccountId(targetAccountId())
                .setAmount(amount)
                .setCurrency(CurrencyIsoCode.USD)
                .setMovementDigest(movementDigest);
    }

    private BudgetControlProjectionQuery projectionQuery(String periodId) {
        return new BudgetControlProjectionQuery()
                .setTenantId(TENANT_ID)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(periodId)
                .setCurrency(CurrencyIsoCode.USD)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setTargetAccountId(targetAccountId());
    }

    private CreateSpendRuleDefinitionRequest createSpendRuleDefinitionRequest() {
        return new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleName("Wallet Spend Controls Monthly Amount")
                .setRuleType(SpendRuleType.AMOUNT_LIMIT)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("企业钱包员工卡月度授权额度控制规则");
    }

    private PublishSpendRuleVersionRequest publishSpendRuleVersionRequest() {
        return new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleVersion(SPEND_RULE_VERSION)
                .setRuleSpec("""
                        {"counterSpec":{"windowMode":"CALENDAR_MONTH","aggregationBasis":"AUTHORIZED_AMOUNT"},"limitSpec":{"amountLimit":{"amount":100,"currency":"USD"}}}
                        """)
                .setRuleDigest(SPEND_RULE_DIGEST)
                .setOperatorId("codex")
                .setAuditReferenceSn("goal:wallet-spend-controls-acceptance")
                .setDescription("发布企业钱包员工卡月度授权额度规则版本");
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
                .setEffectiveFrom(LocalDateTime.now().withNano(0).minusDays(1))
                .setEffectiveTo(LocalDateTime.now().withNano(0).plusDays(30))
                .setAuditReferenceSn(SPEND_RULE_BINDING_AUDIT_REFERENCE_SN)
                .setDescription("挂载到企业钱包员工卡");
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
                .setState(FundsAccountState.ACTIVE);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest() {
        return new CreatePaymentInstrumentRequest()
                .setSn(PAYMENT_INSTRUMENT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setFlowDirection(PaymentInstrumentFlowDirection.OUTBOUND)
                .setInstrumentNo("****7788")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_wallet_spend_controls_7788")
                .setCurrency(CurrencyIsoCode.USD)
                .setState(FundsAccountState.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setBindingRole(PaymentInstrumentBindingRole.PAYMENT_SUBJECT)
                .setSubjectId(CREDIT_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE);
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

    private FundsAccountId targetAccountId() {
        return FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private void insertFundsTransaction(String transactionSn,
                                        String businessSn,
                                        DefaultFundsTransactionType transactionType,
                                        FundsTransactionState state,
                                        Long amount,
                                        String referenceTransactionSn) {
        jdbcTemplate.update("""
                        INSERT INTO t_funds_transaction (
                            sn, tenant_id, transaction_mode, transaction_type, business_scene, business_sn,
                            reference_transaction_sn, status, amount, currency, authorized_amount, reversed_amount, completed_amount,
                            refunded_amount, declined_amount, fee_amount, version
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, 0, 0, 0, 0)
                        """,
                transactionSn,
                TENANT_ID,
                FundsTransactionMode.DIRECT.name(),
                transactionType.name(),
                BUSINESS_SCENE,
                businessSn,
                referenceTransactionSn,
                state.name(),
                amount,
                CurrencyIsoCode.USD.name(),
                amount);
    }

    private void cleanupWalletSpendControlsAcceptanceData() {
        jdbcTemplate.update("DELETE FROM t_spend_control_movement WHERE tenant_id = ? AND spend_rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_decision_record WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_binding WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_version WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_spend_rule_definition WHERE tenant_id = ? AND rule_id = ?",
                TENANT_ID, SPEND_RULE_ID);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE tenant_id = ? AND sn LIKE 'wallet_spend_controls_%'",
                TENANT_ID);
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

    @Configuration
    @Import({
            LedgerServiceImpl.class,
            LedgerProfileCatalog.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
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
            SpendControlMovementServiceImpl.class,
            SpendRuleEvaluationApplicationServiceImpl.class,
            BudgetControlLimitAdjustmentApplicationServiceImpl.class,
            SpendControlTransactionConsumptionApplicationServiceImpl.class,
            DefaultFundsTransactionQueryService.class,
            DefaultFundsAccountQueryServiceImpl.class
    })
    static class Config {
    }
}
