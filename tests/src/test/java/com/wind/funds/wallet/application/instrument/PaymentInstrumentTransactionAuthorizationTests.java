package com.wind.funds.wallet.application.instrument;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.route.DefaultRouteReplayService;
import com.wind.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.wind.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.wind.funds.route.CompositeRouteResolver;
import com.wind.funds.route.DefaultRouteSnapshotFactory;
import com.wind.funds.route.RefundRouteAdmission;
import com.wind.funds.route.RouteFeeChargeAppender;
import com.wind.funds.route.RouteAccountHierarchySnapshotAppender;
import com.wind.funds.route.TransferFundsInstructionRouteResolver;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsAuthorizationTransactionService;
import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.model.request.TransactionAmount;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainQuery;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.transaction.projection.impl.DefaultFundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.transaction.application.instrument.impl.PaymentInstrumentAuthorizationProcessor;
import com.wind.funds.transaction.application.instrument.impl.PaymentInstrumentTransactionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.SpendControlTransactionConsumptionApplicationService;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.BudgetControlProjectionDTO;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.dto.SpendControlMovementDTO;
import com.wind.funds.wallet.model.query.BudgetControlProjectionQuery;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CompleteAuthorizationByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.model.request.RecordSpendControlMovementRequest;
import com.wind.funds.wallet.model.request.RecordSpendRuleDecisionRecordRequest;
import com.wind.funds.wallet.model.request.ReverseAuthorizationByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.query.SpendControlMovementQuery;
import com.wind.funds.wallet.model.request.SuspendSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.SpendControlTransactionConsumptionRequest;
import com.wind.funds.wallet.model.request.UnbindPaymentInstrumentBindingRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendControlMovementService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.service.SpendRuleDecisionRecordService;
import com.wind.funds.wallet.service.SpendRuleBindingService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.AccountHierarchyRelationServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerQueryService;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlMovementServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDefinitionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDecisionRecordServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleVersionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
import com.wind.transaction.core.Money;
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
import java.util.List;
import java.util.Map;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 授权支付工具准入应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PaymentInstrumentTransactionAuthorizationTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentInstrumentTransactionAuthorizationTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "auth_admission_credit";

    private static final String PARENT_FUNDING_ACCOUNT_SN = "auth_admission_parent_funding";

    private static final String PREPAID_FUNDING_ACCOUNT_SN = "auth_admission_prepaid_funding";

    private static final String PLATFORM_SETTLEMENT_ACCOUNT_SN = "auth_adm_settle";

    private static final String PAYMENT_INSTRUMENT_SN = "pi_auth_admission_card";

    private static final String RECEIVE_INSTRUMENT_SN = "pi_auth_admission_receive";

    private static final String OWNER_ID = "owner_auth_admission";

    private static final String CHANNEL_CODE = "issuer_processor";

    private static final String BUSINESS_SCENE = "AUTHORIZATION_ADMISSION";

    private static final String AUTHORIZE_BUSINESS_SN = "AUTH_ADMISSION_AUTHORIZE";

    private static final String BALANCE_ADJUST_BUSINESS_SN = "AUTH_ADMISSION_LIMIT";

    private static final String DIRECTION_FAIL_BUSINESS_SN = "AUTH_ADMISSION_DIRECTION_FAIL";

    private static final String DECLINE_BUSINESS_SN = "AUTH_ADMISSION_DECLINE";

    private static final String SPEND_REJECT_BUSINESS_SN = "AUTH_ADMISSION_SPEND_REJECT";

    private static final String TENANT_MISMATCH_BUSINESS_SN = "AUTH_ADMISSION_TENANT_MISMATCH";

    private static final String SPEND_RULE_ID = "sr_auth_admission_daily_limit";

    private static final String SPEND_RULE_VERSION = "2026-06-21.1";

    private static final LocalDateTime SPEND_RULE_EFFECTIVE_FROM = LocalDateTime.now().withNano(0).minusDays(1);

    private static final LocalDateTime SPEND_RULE_EFFECTIVE_TO = LocalDateTime.now().withNano(0).plusDays(30);

    private static final String SPEND_RULE_BINDING_AUDIT_REFERENCE_SN = "grant:auth_admission_spend_rule_binding";

    private static final String SPEND_RULE_DIGEST = "sha256:auth-admission-spend-rule";

    private static final String SPEND_DECISION_SN = "decision_auth_admission_001";

    private static final String SPEND_DECISION_DIGEST = "sha256:auth-admission-spend-reject";

    private static final String SPEND_PASS_DECISION_SN = "decision_auth_admission_pass_001";

    private static final String SPEND_PASS_DECISION_DIGEST = "sha256:auth-admission-spend-pass";

    private static final String CONTROL_SCOPE_ID = "budget_auth_admission";

    private static final String CONTROL_PERIOD_ID = "2026-07";

    private static final String CONTROL_LIMIT_MOVEMENT_SN = "auth_admission_control_limit";

    private static final String COMPLETION_BUSINESS_SCENE = "AUTHORIZATION_COMPLETION";

    private static final String COMPLETION_BUSINESS_SN = "AUTH_ADMISSION_COMPLETE";

    private static final String REVERSAL_BUSINESS_SCENE = "AUTHORIZATION_REVERSAL";

    private static final String REVERSAL_BUSINESS_SN = "AUTH_ADMISSION_REVERSAL";

    private static final String REFUND_BUSINESS_SCENE = "AUTHORIZATION_REFUND";

    private static final String REFUND_BUSINESS_SN = "AUTH_ADMISSION_REFUND";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private SpendRuleDefinitionService spendRuleDefinitionService;

    @Autowired
    private SpendRuleDecisionRecordService spendRuleDecisionRecordService;

    @Autowired
    private SpendRuleBindingService spendRuleBindingService;

    @Autowired
    private SpendControlMovementService spendControlMovementService;

    @Autowired
    private FundsBalanceControlService balanceControlService;

    @Autowired
    private FundsAuthorizationTransactionService authorizationTransactionService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private PaymentInstrumentTransactionApplicationService paymentInstrumentTransactionApplicationService;

    @Autowired
    private SpendControlTransactionConsumptionApplicationService spendControlTransactionConsumptionApplicationService;

    @Autowired
    private FundsTransactionProjectionExplainApplicationService projectionExplainApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String spendRuleBindingSn;

    /**
     * 场景：VCC/卡支付工具入口完成授权准入后，委派账户主体型授权交易内核。
     * 输入：支付工具绑定信用账户，资金责任解析到同一信用账户，账户有 100 可用额度，授权 60。
     * 输出：返回授权交易号，信用账户 AVAILABLE 减 60、AUTHORIZATION 增 60，并产生可追溯交易和账本事实。
     * 红线：wallet 应用入口不直接写账，最终账务事实必须由标准授权交易路由和 ledger posting 链路产生。
     */
    @Test
    void testAuthorizeByInstrumentShouldResolveAdmissionAndDelegateAuthorizationKernel() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        FundsSubjectBalanceDTO beforeAuthorize = balance(creditAccount);
        assertBucket(beforeAuthorize, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(beforeAuthorize, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);

        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN)
                        .setControlScopeId("budget_without_applicable_rule"),
                WindOperatorFactory.system());

        assertThat(authorizationSn).isNotBlank();
        FundsSubjectBalanceDTO afterAuthorize = balance(creditAccount);
        assertBucket(afterAuthorize, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(afterAuthorize, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertThat(fundsTransactionStatus(AUTHORIZE_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.OPEN.name());
        assertThat(fundsTransactionDetailStatuses(AUTHORIZE_BUSINESS_SN))
                .containsExactly(FundsTransactionDetailStatus.SUCCEEDED.name());
        assertThat(ledgerTransactionEvents(AUTHORIZE_BUSINESS_SN))
                .containsExactly(FundsTransactionEventType.AUTHORIZE.name());
        assertThat(ledgerEntrySubjects(AUTHORIZE_BUSINESS_SN)).containsOnly(CREDIT_ACCOUNT_SN);
        assertThat(ledgerEntrySubjectCodes(AUTHORIZE_BUSINESS_SN))
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE.name(),
                        LedgerSubjectCode.AUTHORIZATION.name());
        assertThat(postingPlanCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
        assertThat(ledgerEntryCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(2);
        assertThat(routeSnapshotJson(AUTHORIZE_BUSINESS_SN)).isNotBlank();
        assertThat(routeLegCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
        assertAuthorizationInstrumentSnapshot(AUTHORIZE_BUSINESS_SN);
        assertAuthorizationAdmissionContextSnapshot(AUTHORIZE_BUSINESS_SN);
        assertThat(JSON.parseObject(transactionContextVariablesJson(AUTHORIZE_BUSINESS_SN))
                .getJSONObject("spendRuleDecision")
                .getString("controlScopeId"))
                .isEqualTo("budget_without_applicable_rule");
        assertAuthorizationProjectionInstrumentExplanation(authorizationSn);
    }

    /**
     * 场景：共享 VCC 绑定信用账户，但真实资金责任落在父资金账户。
     * 输入：支付工具绑定信用账户，资金责任解析到父 FundingAccount，两边各有 100 可用余额，授权 60。
     * 输出：信用账户和父资金账户同时 AVAILABLE 减 60、AUTHORIZATION 增 60，并共享同一授权交易事实。
     * 红线：共享卡授权不得只占用信用额度而跳过父资金账户，否则父账户最终可能透支。
     */
    @Test
    void testAuthorizeSharedCardShouldHoldCreditAndParentFundingAccount() {
        FundsAccountId creditAccount = creditAccountId();
        FundsAccountId parentFundingAccount = parentFundingAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        fundingAccountService.createFundingAccount(createParentFundingAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createParentFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        initializeAvailableBalance(parentFundingAccount, 100L);
        FundsSubjectBalanceDTO beforeCreditAuthorize = balance(creditAccount);
        FundsSubjectBalanceDTO beforeParentAuthorize = balance(parentFundingAccount);
        assertBucket(beforeCreditAuthorize, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(beforeCreditAuthorize, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertBucket(beforeParentAuthorize, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(beforeParentAuthorize, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);

        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(authorizationSn).isNotBlank();
        FundsSubjectBalanceDTO afterCreditAuthorize = balance(creditAccount);
        FundsSubjectBalanceDTO afterParentAuthorize = balance(parentFundingAccount);
        assertBucket(afterCreditAuthorize, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(afterCreditAuthorize, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(afterParentAuthorize, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(afterParentAuthorize, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertThat(fundsTransactionStatus(AUTHORIZE_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.OPEN.name());
        assertThat(fundsTransactionDetailStatuses(AUTHORIZE_BUSINESS_SN))
                .containsExactly(FundsTransactionDetailStatus.SUCCEEDED.name(),
                        FundsTransactionDetailStatus.SUCCEEDED.name());
        assertThat(ledgerEntrySubjects(AUTHORIZE_BUSINESS_SN))
                .containsExactlyInAnyOrder(CREDIT_ACCOUNT_SN, CREDIT_ACCOUNT_SN,
                        PARENT_FUNDING_ACCOUNT_SN, PARENT_FUNDING_ACCOUNT_SN);
        assertThat(ledgerEntrySubjectCodes(AUTHORIZE_BUSINESS_SN))
                .containsExactlyInAnyOrder(LedgerSubjectCode.AVAILABLE.name(),
                        LedgerSubjectCode.AUTHORIZATION.name(),
                        LedgerSubjectCode.AVAILABLE.name(),
                        LedgerSubjectCode.AUTHORIZATION.name());
        assertThat(postingPlanCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(2);
        assertThat(ledgerEntryCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(4);
        assertThat(routeLegCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(2);
        assertAuthorizationAdmissionContextSnapshot(AUTHORIZE_BUSINESS_SN, parentFundingAccount);
        assertAuthorizationProjectionInstrumentExplanation(authorizationSn);
    }

    /**
     * 场景：预付 VCC 绑定资金子账户，资金责任关系保留父资金账户来源。
     * 输入：支付工具绑定预付资金子账户，资金责任解析到父 FundingAccount，两边各有 100 可用余额，授权 60。
     * 输出：只占用预付资金子账户，父资金账户余额不变，授权路由只有一个资金主体。
     * 红线：预付资金子账户本身承担真实资金，父账户不得被自动追加成第二个授权占用主体。
     */
    @Test
    void testAuthorizePrepaidCardShouldOnlyHoldBoundFundingAccount() {
        FundsAccountId prepaidFundingAccount = FundsAccountId.immutable(PREPAID_FUNDING_ACCOUNT_SN,
                FundsSubjectType.FUNDING_ACCOUNT);
        FundsAccountId parentFundingAccount = parentFundingAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        fundingAccountService.createFundingAccount(createParentFundingAccountRequest());
        fundingAccountService.createFundingAccount(createPrepaidFundingAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createPrepaidBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createPrepaidFundingRelationRequest());
        initializeAvailableBalance(prepaidFundingAccount, 100L);
        initializeAvailableBalance(parentFundingAccount, 100L);

        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(authorizationSn).isNotBlank();
        FundsSubjectBalanceDTO prepaidBalance = balance(prepaidFundingAccount);
        FundsSubjectBalanceDTO parentBalance = balance(parentFundingAccount);
        assertBucket(prepaidBalance, LedgerSubjectCode.AVAILABLE, 40L, CURRENCY);
        assertBucket(prepaidBalance, LedgerSubjectCode.AUTHORIZATION, 60L, CURRENCY);
        assertBucket(parentBalance, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(parentBalance, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertThat(ledgerEntrySubjects(AUTHORIZE_BUSINESS_SN))
                .containsOnly(PREPAID_FUNDING_ACCOUNT_SN);
        assertThat(postingPlanCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
        assertThat(ledgerEntryCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(2);
        assertThat(routeLegCount(AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
        assertAuthorizationAdmissionContextSnapshot(AUTHORIZE_BUSINESS_SN, parentFundingAccount);
    }

    /**
     * 场景：支付工具授权入口携带 Spend Rule 决策证据，规则准入通过后进入授权内核。
     * 输入：支付工具、资金责任、账户能力和 Spend Rule 决策均通过。
     * 输出：交易投影解释可以展示规则、版本、挂载、scope、决策流水、结果和决策记录引用。
     * 红线：规则解释只读，不执行规则脚本，不读取规则原文，不新增资金或账本事实。
     */
    @Test
    void testAuthorizeByInstrumentShouldExposeSpendRuleDecisionInProjectionExplanation() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        prepareSpendRuleDecisionData();
        recordAuthorizationDecision(SPEND_PASS_DECISION_SN,
                AUTHORIZE_BUSINESS_SN,
                SpendControlDecisionResult.PASSED,
                null,
                SPEND_PASS_DECISION_DIGEST);
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        prepareBudgetControlLimit(100L);

        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(authorizationSn).isNotBlank();
        SpendControlMovementDTO reservation = findControlReservation(authorizationSn);
        assertThat(reservation.getMovementType()).isEqualTo(SpendControlMovementType.RESERVED);
        assertThat(reservation.getMovementSn()).isEqualTo(controlReservationMovementSn(AUTHORIZE_BUSINESS_SN));
        assertThat(reservation.getAmount()).isEqualTo(60L);
        assertThat(reservation.getControlScopeId()).isEqualTo(CONTROL_SCOPE_ID);
        assertThat(reservation.getPeriodId()).isEqualTo(CONTROL_PERIOD_ID);
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getLimitAmount()).isEqualTo(100L);
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(60L);
        assertThat(projection.getAvailableControlAmount()).isEqualTo(40L);
        var beforeExplainFacts = ledgerFactSnapshot(jdbcTemplate);
        assertAuthorizationProjectionSpendRuleExplanation(authorizationSn);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeExplainFacts);
    }

    /**
     * 场景：已固化的授权决策被复用到另一个预算周期。
     * 输入：决策记录绑定当期窗口，授权请求仅替换 periodId。
     * 输出：交易入口在准入阶段 fail-closed。
     * 红线：跨周期 decisionRef 不得创建交易、路由、记账计划或账本事实。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectDecisionReusedAcrossPeriodWithoutFundsFacts() {
        preparePassedAuthorizationData();
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN)
                        .setPeriodId("2026-08"),
                WindOperatorFactory.system()))
                .hasMessageContaining("控制窗口不一致");

        assertNoFundsOrLedgerFacts(AUTHORIZE_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具授权生成控制预留后，可信部分完成同步消费对应控制占用。
     * 输入：原授权 60，本次完成 40，完成动作使用独立稳定 businessSn，并重放同一完成请求。
     * 输出：授权聚合保持 OPEN，资金累计完成和控制 CONSUMED 均为 40，控制流水只生成一次。
     * 红线：资金完成与控制消费必须同事务，重放不得重复完成或重复消费，也不得按当前绑定重新选路。
     */
    @Test
    void testPartialCompletionByInstrumentShouldConsumeLinkedControlReservationOnce() {
        preparePassedAuthorizationData();
        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system());
        SpendControlMovementDTO reservation = findControlReservation(authorizationSn);

        String completionSn = paymentInstrumentTransactionApplicationService.completeAuthorizationByInstrument(
                completeAuthorizationRequest(authorizationSn, 40L),
                WindOperatorFactory.system());
        String replayedCompletionSn = paymentInstrumentTransactionApplicationService.completeAuthorizationByInstrument(
                completeAuthorizationRequest(authorizationSn, 40L),
                WindOperatorFactory.system());

        assertThat(completionSn).isEqualTo(authorizationSn);
        assertThat(replayedCompletionSn).isEqualTo(completionSn);
        assertThat(fundsTransactionStatus(AUTHORIZE_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.OPEN.name());
        assertThat(fundsTransactionAmounts(AUTHORIZE_BUSINESS_SN)).containsExactly(60L, 0L, 40L, 0L);
        assertThat(ledgerTransactionEvents(COMPLETION_BUSINESS_SCENE, COMPLETION_BUSINESS_SN))
                .containsExactly(FundsTransactionEventType.COMPLETE.name());

        List<SpendControlMovementDTO> consumedMovements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(TENANT_ID)
                        .setOriginalMovementSn(reservation.getMovementSn())
                        .setMovementType(SpendControlMovementType.CONSUMED));

        assertThat(consumedMovements).hasSize(1);
        SpendControlMovementDTO consumed = consumedMovements.getFirst();
        assertThat(consumed.getMovementType()).isEqualTo(SpendControlMovementType.CONSUMED);
        assertThat(consumed.getOriginalMovementSn()).isEqualTo(reservation.getMovementSn());
        assertThat(consumed.getTransactionSn()).isEqualTo(completionSn);
        assertThat(consumed.getAmount()).isEqualTo(40L);
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getConsumedAmount()).isEqualTo(40L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(20L);
        assertThat(projection.getAvailableControlAmount()).isEqualTo(40L);
    }

    private CompleteAuthorizationByPaymentInstrumentRequest completeAuthorizationRequest(String authorizationSn,
                                                                                          long amount) {
        return new CompleteAuthorizationByPaymentInstrumentRequest()
                .setTenantId(TENANT_ID)
                .setAuthorizationTransactionSn(authorizationSn)
                .setAmount(amount)
                .setCurrency(CURRENCY)
                .setBusinessScene(COMPLETION_BUSINESS_SCENE)
                .setBusinessSn(COMPLETION_BUSINESS_SN)
                .setDescription("trusted payment instrument authorization completion");
    }

    /**
     * 场景：VCC 授权完成命中已存在但摘要冲突的控制消费幂等键。
     * 输入：授权 60 后预置一条同完成幂等键、不同摘要的控制消费，再请求可信完成 20。
     * 输出：统一完成入口失败，资金累计完成和完成账务事实整体回滚，既有控制事实保持不变。
     * 红线：控制消费失败不得留下资金已完成、预算仍预留的半完成状态。
     */
    @Test
    void testCompleteAuthorizationByInstrumentShouldRollbackFundsCompletionWhenControlConsumptionConflicts() {
        preparePassedAuthorizationData();
        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system());
        SpendControlMovementDTO reservation = findControlReservation(authorizationSn);
        String consumeMovementSn = controlConsumeMovementSn(authorizationSn);
        spendControlMovementService.recordMovement(conflictingControlMovementRequest(
                consumeMovementSn, SpendControlMovementType.CONSUMED, reservation, authorizationSn,
                "sha256:conflicting-existing-control-consumption"));
        var beforeCompletion = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.completeAuthorizationByInstrument(
                completeAuthorizationRequest(authorizationSn, 20L), WindOperatorFactory.system()))
                .hasMessageContaining("控制额度变动流水已存在但摘要不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, beforeCompletion);
        assertThat(fundsTransactionAmounts(AUTHORIZE_BUSINESS_SN)).containsExactly(60L, 0L, 0L, 0L);
        assertThat(ledgerTransactionEvents(COMPLETION_BUSINESS_SCENE, COMPLETION_BUSINESS_SN)).isEmpty();
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getConsumedAmount()).isEqualTo(1L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(59L);
    }

    /**
     * 场景：VCC 授权生成控制预留后，可信撤销释放部分资金占用和控制占用。
     * 输入：原授权 60，本次可信撤销 20，控制释放仍回链原授权和原控制预留。
     * 输出：授权聚合保持 OPEN，资金累计撤销和控制 RELEASED 均为 20，剩余控制占用为 40。
     * 红线：没有新增可信撤销金额时，不得仅凭超时或本地判断继续释放控制占用。
     */
    @Test
    void testTrustedAuthorizationReversalShouldReleaseLinkedControlReservation() {
        preparePassedAuthorizationData();
        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system());
        SpendControlMovementDTO reservation = findControlReservation(authorizationSn);

        String reversalSn = paymentInstrumentTransactionApplicationService.reverseAuthorizationByInstrument(
                reverseAuthorizationRequest(authorizationSn, 20L),
                WindOperatorFactory.system());
        String replayedReversalSn = paymentInstrumentTransactionApplicationService.reverseAuthorizationByInstrument(
                reverseAuthorizationRequest(authorizationSn, 20L),
                WindOperatorFactory.system());

        assertThat(reversalSn).isEqualTo(authorizationSn);
        assertThat(replayedReversalSn).isEqualTo(reversalSn);
        assertThat(fundsTransactionStatus(AUTHORIZE_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.OPEN.name());
        assertThat(ledgerTransactionEvents(REVERSAL_BUSINESS_SCENE, REVERSAL_BUSINESS_SN))
                .containsExactly(FundsTransactionEventType.REVERSAL.name());

        List<SpendControlMovementDTO> releasedMovements = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(TENANT_ID)
                        .setOriginalMovementSn(reservation.getMovementSn())
                        .setMovementType(SpendControlMovementType.RELEASED));

        assertThat(releasedMovements).hasSize(1);
        SpendControlMovementDTO released = releasedMovements.getFirst();
        assertThat(released.getMovementType()).isEqualTo(SpendControlMovementType.RELEASED);
        assertThat(released.getOriginalMovementSn()).isEqualTo(reservation.getMovementSn());
        assertThat(released.getTransactionSn()).isEqualTo(reversalSn);
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getReservedAmount()).isEqualTo(60L);
        assertThat(projection.getReleasedAmount()).isEqualTo(20L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(40L);
        assertThat(projection.getAvailableControlAmount()).isEqualTo(60L);

        assertThatThrownBy(() -> spendControlTransactionConsumptionApplicationService.release(
                controlMovementRequest("auth_admission_control_untrusted_release", reservation, reversalSn, 1L,
                        "sha256:auth-admission-control-untrusted-release")))
                .hasMessageContaining("控制释放累计金额超过资金交易可信撤销金额");
        assertThat(spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                .setTenantId(TENANT_ID)
                .setMovementSn("auth_admission_control_untrusted_release"))).isEmpty();
        BudgetControlProjectionDTO afterRejectedRelease = budgetControlProjection();
        assertThat(afterRejectedRelease.getReleasedAmount()).isEqualTo(20L);
        assertThat(afterRejectedRelease.getRemainingControlAmount()).isEqualTo(40L);
    }

    /**
     * 场景：VCC 授权撤销命中已存在但摘要冲突的控制释放幂等键。
     * 输入：授权 60 后预置一条同撤销幂等键、不同摘要的控制释放，再请求可信撤销 20。
     * 输出：统一撤销入口失败，资金累计撤销和撤销账务事实整体回滚，既有控制事实保持不变。
     * 红线：控制释放失败不得留下资金已撤销、预算仍占用的半完成状态。
     */
    @Test
    void testReverseAuthorizationByInstrumentShouldRollbackFundsReversalWhenControlReleaseConflicts() {
        preparePassedAuthorizationData();
        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system());
        SpendControlMovementDTO reservation = findControlReservation(authorizationSn);
        String releaseMovementSn = controlReleaseMovementSn(authorizationSn);
        spendControlMovementService.recordMovement(conflictingControlMovementRequest(
                releaseMovementSn, SpendControlMovementType.RELEASED, reservation, authorizationSn,
                "sha256:conflicting-existing-control-release"));
        var beforeReversal = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.reverseAuthorizationByInstrument(
                reverseAuthorizationRequest(authorizationSn, 20L), WindOperatorFactory.system()))
                .hasMessageContaining("控制额度变动流水已存在但摘要不一致");

        assertLedgerFactsUnchanged(jdbcTemplate, beforeReversal);
        assertThat(fundsTransactionAmounts(AUTHORIZE_BUSINESS_SN)).containsExactly(60L, 0L, 0L, 0L);
        assertThat(ledgerTransactionEvents(REVERSAL_BUSINESS_SCENE, REVERSAL_BUSINESS_SN)).isEmpty();
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getReleasedAmount()).isEqualTo(1L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(59L);
    }

    /**
     * 场景：VCC 授权后，当前支付工具从原信用账户改绑到另一资金账户，再收到可信完成和撤销。
     * 输入：原授权 60，改绑后分别完成 20、撤销 20，当前绑定主体已不再是原授权主体。
     * 输出：完成和撤销账务分录仍只落在原授权快照中的信用账户，控制消费和释放各为 20。
     * 红线：授权后绑定变化不得改写历史路由，否则会把同一授权聚合的后续账务事实记到不同主体。
     */
    @Test
    void testAuthorizationCompletionAndReversalShouldUseHistoricalSubjectAfterInstrumentRebind() {
        preparePassedAuthorizationData();
        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system());
        rebindPaymentInstrumentToParentFundingAccount();

        paymentInstrumentTransactionApplicationService.completeAuthorizationByInstrument(
                completeAuthorizationRequest(authorizationSn, 20L), WindOperatorFactory.system());
        paymentInstrumentTransactionApplicationService.reverseAuthorizationByInstrument(
                reverseAuthorizationRequest(authorizationSn, 20L), WindOperatorFactory.system());

        assertThat(fundsTransactionAmounts(AUTHORIZE_BUSINESS_SN)).containsExactly(60L, 20L, 20L, 0L);
        assertThat(ledgerEntrySubjects(COMPLETION_BUSINESS_SCENE, COMPLETION_BUSINESS_SN))
                .containsOnly(CREDIT_ACCOUNT_SN);
        assertThat(ledgerEntrySubjects(REVERSAL_BUSINESS_SCENE, REVERSAL_BUSINESS_SN))
                .containsOnly(CREDIT_ACCOUNT_SN);
        FundsSubjectBalanceDTO reboundAccountBalance = balance(parentFundingAccountId());
        assertBucket(reboundAccountBalance, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(reboundAccountBalance, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getConsumedAmount()).isEqualTo(20L);
        assertThat(projection.getReleasedAmount()).isEqualTo(20L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(20L);
    }

    /**
     * 场景：VCC 授权完成后支付工具改绑，再通过账户主体型 canonical 服务退回原交易本金。
     * 输入：原授权 60、完成 40、改绑到另一资金账户后退款 30。
     * 输出：退款仍只回放原信用账户和原 RouteSnapshot，资金累计退款为 30，控制消费不自动补偿。
     * 红线：退款不得按当前支付工具绑定重新选路，也不得把资金退款自动解释为周期控制额度恢复。
     */
    @Test
    void testAuthorizationRefundShouldUseHistoricalSubjectWithoutAutomaticControlCompensationAfterRebind() {
        preparePassedAuthorizationData();
        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system());
        paymentInstrumentTransactionApplicationService.completeAuthorizationByInstrument(
                completeAuthorizationRequest(authorizationSn, 40L), WindOperatorFactory.system());
        rebindPaymentInstrumentToParentFundingAccount();

        String refundSn = authorizationTransactionService.refund(
                refundAuthorizationRequest(authorizationSn, 30L), WindOperatorFactory.system());

        assertThat(refundSn).isEqualTo(authorizationSn);
        assertThat(fundsRefundedAmount(AUTHORIZE_BUSINESS_SN)).isEqualTo(30L);
        assertThat(ledgerTransactionEvents(REFUND_BUSINESS_SCENE, REFUND_BUSINESS_SN))
                .containsExactly(FundsTransactionEventType.AUTH_REFUND.name());
        assertThat(ledgerEntrySubjects(REFUND_BUSINESS_SCENE, REFUND_BUSINESS_SN))
                .containsOnly(CREDIT_ACCOUNT_SN);
        FundsSubjectBalanceDTO originalAccountBalance = balance(creditAccountId());
        assertBucket(originalAccountBalance, LedgerSubjectCode.AVAILABLE, 70L, CURRENCY);
        assertBucket(originalAccountBalance, LedgerSubjectCode.AUTHORIZATION, 20L, CURRENCY);
        FundsSubjectBalanceDTO reboundAccountBalance = balance(parentFundingAccountId());
        assertBucket(reboundAccountBalance, LedgerSubjectCode.AVAILABLE, 0L, CURRENCY);
        assertBucket(reboundAccountBalance, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getConsumedAmount()).isEqualTo(40L);
        assertThat(projection.getRemainingControlAmount()).isEqualTo(20L);
        assertThat(projection.getAvailableControlAmount()).isEqualTo(40L);
        assertThat(spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                .setTenantId(TENANT_ID)
                .setMovementType(SpendControlMovementType.REFUND_COMPENSATED))).isEmpty();
    }

    private FundsAuthorizationTransactionRefundRequest refundAuthorizationRequest(String authorizationSn,
                                                                                    long amount) {
        return new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(creditAccountId())
                .setTransactionAmount(TransactionAmount.sameCurrency(Money.immutable(amount, CURRENCY)))
                .setAuthorizationTransactionSn(authorizationSn)
                .setBusinessScene(REFUND_BUSINESS_SCENE)
                .setBusinessSn(REFUND_BUSINESS_SN)
                .setDescription("payment instrument authorization principal refund");
    }

    private ReverseAuthorizationByPaymentInstrumentRequest reverseAuthorizationRequest(String authorizationSn,
                                                                                         long amount) {
        return new ReverseAuthorizationByPaymentInstrumentRequest()
                .setTenantId(TENANT_ID)
                .setAuthorizationTransactionSn(authorizationSn)
                .setAmount(amount)
                .setCurrency(CURRENCY)
                .setBusinessScene(REVERSAL_BUSINESS_SCENE)
                .setBusinessSn(REVERSAL_BUSINESS_SN)
                .setDescription("trusted payment instrument authorization reversal");
    }

    private void rebindPaymentInstrumentToParentFundingAccount() {
        fundingAccountService.createFundingAccount(createParentFundingAccountRequest());
        String bindingSn = jdbcTemplate.queryForObject("""
                SELECT sn FROM t_payment_instrument_binding
                WHERE tenant_id = ? AND instrument_sn = ? AND binding_role = ?
                """, String.class, TENANT_ID, PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name());
        paymentInstrumentService.unbindPaymentInstrumentBinding(new UnbindPaymentInstrumentBindingRequest()
                .setTenantId(TENANT_ID)
                .setBindingSn(bindingSn)
                .setOperatorId("codex")
                .setChangeReason("rebind after authorization"));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest()
                .setSubjectId(PARENT_FUNDING_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setOperatorId("codex")
                .setChangeReason("rebind after authorization"));
        assertThat(jdbcTemplate.queryForObject("""
                SELECT subject_id FROM t_payment_instrument_binding
                WHERE tenant_id = ? AND instrument_sn = ? AND binding_role = ?
                """, String.class, TENANT_ID, PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name())).isEqualTo(PARENT_FUNDING_ACCOUNT_SN);
    }

    private RecordSpendControlMovementRequest conflictingControlMovementRequest(String movementSn,
                                                                                 SpendControlMovementType movementType,
                                                                                 SpendControlMovementDTO reservation,
                                                                                 String authorizationSn,
                                                                                 String movementDigest) {
        return new RecordSpendControlMovementRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(movementSn)
                .setMovementType(movementType)
                .setBusinessScene(reservation.getBusinessScene())
                .setBusinessSn(reservation.getBusinessSn())
                .setOriginalMovementSn(reservation.getMovementSn())
                .setTransactionSn(authorizationSn)
                .setInstrumentSn(reservation.getInstrumentSn())
                .setAction(reservation.getAction())
                .setTargetAccountId(reservation.getTargetAccountId())
                .setAmount(1L)
                .setCurrency(reservation.getCurrency())
                .setSpendRuleId(reservation.getSpendRuleId())
                .setSpendRuleVersion(reservation.getSpendRuleVersion())
                .setSpendDecisionSn(reservation.getSpendDecisionSn())
                .setSpendDecisionResult(reservation.getSpendDecisionResult())
                .setSpendDecisionDigest(reservation.getSpendDecisionDigest())
                .setControlScopeId(reservation.getControlScopeId())
                .setPeriodId(reservation.getPeriodId())
                .setMovementDigest(movementDigest);
    }

    private String controlConsumeMovementSn(String authorizationSn) {
        return FundsStableHashSupport.sha256("SPEND_CONTROL_AUTHORIZATION_CONSUME|"
                + TENANT_ID + "|" + authorizationSn + "|" + COMPLETION_BUSINESS_SCENE + "|" + COMPLETION_BUSINESS_SN);
    }

    private String controlReservationMovementSn(String businessSn) {
        return "SCR" + FundsStableHashSupport.sha256("SPEND_CONTROL_AUTHORIZATION_RESERVE|"
                + TENANT_ID + "|" + BUSINESS_SCENE + "|" + businessSn).substring(0, 61);
    }

    private String controlReleaseMovementSn(String authorizationSn) {
        return FundsStableHashSupport.sha256("SPEND_CONTROL_AUTHORIZATION_RELEASE|"
                + TENANT_ID + "|" + authorizationSn + "|" + REVERSAL_BUSINESS_SCENE + "|" + REVERSAL_BUSINESS_SN);
    }

    private SpendControlTransactionConsumptionRequest controlMovementRequest(String movementSn,
                                                                              SpendControlMovementDTO reservation,
                                                                              String transactionSn,
                                                                              long amount,
                                                                              String movementDigest) {
        return new SpendControlTransactionConsumptionRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(movementSn)
                .setOriginalMovementSn(reservation.getMovementSn())
                .setTransactionSn(transactionSn)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(AUTHORIZE_BUSINESS_SN)
                .setTargetAccountId(creditAccountId())
                .setAmount(amount)
                .setCurrency(CURRENCY)
                .setMovementDigest(movementDigest);
    }

    /**
     * 场景：Spend Rule 已通过，但控制周期可用预算不足。
     * 输入：资金账户可授权 60，预算控制周期仅剩 50。
     * 输出：授权请求失败，预算预留和资金授权事实均不落库。
     * 红线：预算预留失败不得留下交易、route、posting、LedgerEntry 或授权余额占用。
     */
    @Test
    void testAuthorizeByInstrumentShouldRollbackFundsFactsWhenControlReservationFails() {
        FundsAccountId creditAccount = preparePassedAuthorizationData(50L);
        var beforeAuthorize = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN),
                WindOperatorFactory.system()))
                .hasMessageContaining("控制占用金额超过可用控制额度");

        assertNoFundsOrLedgerFacts(AUTHORIZE_BUSINESS_SN);
        assertThat(spendControlMovementService.queryMovements(new SpendControlMovementQuery()
                .setTenantId(TENANT_ID)
                .setMovementType(SpendControlMovementType.RESERVED)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(AUTHORIZE_BUSINESS_SN))).isEmpty();
        BudgetControlProjectionDTO projection = budgetControlProjection();
        assertThat(projection.getLimitAmount()).isEqualTo(50L);
        assertThat(projection.getReservedAmount()).isZero();
        assertThat(projection.getAvailableControlAmount()).isEqualTo(50L);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeAuthorize);
        FundsSubjectBalanceDTO balance = balance(creditAccount);
        assertBucket(balance, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(balance, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
    }

    /**
     * 场景：已成功授权后，原 Spend Rule 挂载被暂停，调用方重放完全相同的业务请求。
     * 输入：首次授权已固化准入和资金事实，随后暂停原规则挂载，再提交相同 businessSn 和 decisionSn。
     * 输出：返回原授权交易号，不重新按当前挂载拒绝，也不重复生成资金或账务事实。
     * 红线：当前规则状态变化不得破坏已经成立的资金请求幂等重放。
     */
    @Test
    void testAuthorizeByInstrumentShouldReplayEstablishedAuthorizationAfterBindingSuspended() {
        FundsAccountId creditAccount = preparePassedAuthorizationData();
        AuthorizeByPaymentInstrumentRequest request =
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN);
        String firstAuthorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request, WindOperatorFactory.system());
        spendRuleBindingService.suspendSpendRuleBinding(new SuspendSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(spendRuleBindingSn));
        FundsSubjectBalanceDTO balanceAfterFirstAuthorization = balance(creditAccount);
        var factsAfterFirstAuthorization = ledgerFactSnapshot(jdbcTemplate);

        String replayedAuthorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request, WindOperatorFactory.system());

        assertThat(replayedAuthorizationSn).isEqualTo(firstAuthorizationSn);
        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);
        assertThat(countRows("t_funds_transaction", AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：未命中 Spend Rule 的授权携带控制周期，随后先原样重放、再切换周期重放。
     * 输入：首次和第二次均为 scope + 2026-07，第三次沿用业务键但改为 2026-08。
     * 输出：完全相同重放返回原交易号，跨周期重放被拒绝且不改变既有资金与账务事实。
     * 红线：NO_APPLICABLE_RULE 也必须固化周期，不能误拒绝原样重放或放过跨周期重放。
     */
    @Test
    void testAuthorizeByInstrumentShouldReplayNoApplicableRuleOnlyWithinEstablishedPeriod() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        AuthorizeByPaymentInstrumentRequest request = authorizeRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN)
                .setControlScopeId("budget_without_applicable_rule")
                .setPeriodId(CONTROL_PERIOD_ID);

        String firstAuthorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request, WindOperatorFactory.system());
        FundsSubjectBalanceDTO balanceAfterFirstAuthorization = balance(creditAccount);
        var factsAfterFirstAuthorization = ledgerFactSnapshot(jdbcTemplate);

        assertThat(paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request, WindOperatorFactory.system())).isEqualTo(firstAuthorizationSn);
        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request.setPeriodId("2026-08"), WindOperatorFactory.system()))
                .hasMessageContaining("已成立授权 Spend Rule 证据不一致");
        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);
        assertThat(countRows("t_funds_transaction", AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：已成功授权后，调用方沿用业务幂等键和决策引用但切换预算周期重放。
     * 输入：首次授权绑定 2026-07，重放仅把 periodId 改为 2026-08。
     * 输出：幂等身份校验拒绝，原交易、余额和账务事实保持不变。
     * 红线：已成立授权的预算周期必须精确重放，不能复用到另一个控制窗口。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectEstablishedReplayAcrossPeriod() {
        FundsAccountId creditAccount = preparePassedAuthorizationData();
        AuthorizeByPaymentInstrumentRequest request =
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN);
        paymentInstrumentTransactionApplicationService.authorizeByInstrument(request, WindOperatorFactory.system());
        FundsSubjectBalanceDTO balanceAfterFirstAuthorization = balance(creditAccount);
        var factsAfterFirstAuthorization = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request.setPeriodId("2026-08"), WindOperatorFactory.system()))
                .hasMessageContaining("已成立授权 Spend Rule 证据不一致");

        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);
        assertThat(countRows("t_funds_transaction", AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：已成功授权且原 Spend Rule 挂载已暂停，调用方用同一业务流水篡改金额重试。
     * 输入：沿用原 businessSn 和 decisionSn，但把授权金额从 60 改为 61。
     * 输出：幂等身份校验拒绝，原交易、余额和账务事实保持不变。
     * 红线：跳过当前挂载重算只适用于完全相同的已成立请求，不能把业务键变成参数绕过入口。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectChangedReplayAfterBindingSuspended() {
        FundsAccountId creditAccount = preparePassedAuthorizationData();
        AuthorizeByPaymentInstrumentRequest request =
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN);
        paymentInstrumentTransactionApplicationService.authorizeByInstrument(request, WindOperatorFactory.system());
        spendRuleBindingService.suspendSpendRuleBinding(new SuspendSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(spendRuleBindingSn));
        FundsSubjectBalanceDTO balanceAfterFirstAuthorization = balance(creditAccount);
        var factsAfterFirstAuthorization = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request.setAmount(61L), WindOperatorFactory.system()))
                .hasMessageContaining("已成立授权请求参数不一致");

        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);
        assertThat(countRows("t_funds_transaction", AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：已成功授权且原 Spend Rule 挂载已暂停，调用方重放时省略原 decisionRef。
     * 输入：沿用原业务流水、金额和支付工具，但删除首次授权使用的 decisionSn。
     * 输出：幂等身份校验拒绝，原交易、余额和账务事实保持不变。
     * 红线：已成立授权的规则证据必须精确重放，不能把可回读引用降级为可选字段。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectReplayWithoutEstablishedDecisionReference() {
        FundsAccountId creditAccount = preparePassedAuthorizationData();
        AuthorizeByPaymentInstrumentRequest request =
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN);
        paymentInstrumentTransactionApplicationService.authorizeByInstrument(request, WindOperatorFactory.system());
        spendRuleBindingService.suspendSpendRuleBinding(new SuspendSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(spendRuleBindingSn));
        FundsSubjectBalanceDTO balanceAfterFirstAuthorization = balance(creditAccount);
        var factsAfterFirstAuthorization = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request.setSpendDecisionSn(null), WindOperatorFactory.system()))
                .hasMessageContaining("已成立授权 Spend Rule 证据不一致");

        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);
        assertThat(countRows("t_funds_transaction", AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：已成功授权且原 Spend Rule 挂载已暂停，调用方用错误的期望工具绑定版本重放。
     * 输入：沿用原业务流水和决策引用，但把 expectedBindingVersion 从空改为 2。
     * 输出：幂等身份校验拒绝，原交易、余额和账务事实保持不变。
     * 红线：early replay 不能绕过支付工具绑定版本的乐观并发守卫。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectReplayWithMismatchedExpectedBindingVersion() {
        FundsAccountId creditAccount = preparePassedAuthorizationData();
        AuthorizeByPaymentInstrumentRequest request =
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN);
        paymentInstrumentTransactionApplicationService.authorizeByInstrument(request, WindOperatorFactory.system());
        spendRuleBindingService.suspendSpendRuleBinding(new SuspendSpendRuleBindingRequest()
                .setTenantId(TENANT_ID)
                .setSn(spendRuleBindingSn));
        FundsSubjectBalanceDTO balanceAfterFirstAuthorization = balance(creditAccount);
        var factsAfterFirstAuthorization = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request.setExpectedBindingVersion(2), WindOperatorFactory.system()))
                .hasMessageContaining("已成立授权请求参数不一致");

        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstAuthorization);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstAuthorization);
        assertThat(countRows("t_funds_transaction", AUTHORIZE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：支付工具资金流向不支持授权。
     * 输入：RECEIVE-only 工具发起授权准入。
     * 输出：准入阶段拒绝，业务流水下没有资金交易、账本交易、posting plan 或分录。
     * 红线：工具准入失败不得进入交易内核，不得留下半成功资金事实。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectDirectionMismatchWithoutFundsFacts() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(RECEIVE_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.INBOUND));
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(DIRECTION_FAIL_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system()))
                .hasMessageContaining("支付工具资金流向不支持当前动作");

        assertNoFundsOrLedgerFacts(DIRECTION_FAIL_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：授权入口请求租户与当前线程租户不一致。
     * 输入：当前线程租户为 1，请求 tenantId 为 2。
     * 输出：应用层入口直接拒绝，业务流水下没有资金交易、账本交易、posting plan 或分录。
     * 红线：wallet 应用层作为上游入口不能把跨租户请求下推到交易和账本内核。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectTenantMismatchWithoutFundsFacts() {
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(TENANT_MISMATCH_BUSINESS_SN, PAYMENT_INSTRUMENT_SN).setTenantId(TENANT_ID + 1),
                WindOperatorFactory.system()))
                .hasMessageContaining("支付工具授权 tenantId 与当前租户不一致");

        assertNoFundsOrLedgerFacts(TENANT_MISMATCH_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具入口透传授权拒绝结果。
     * 输入：支付工具绑定信用账户，资金责任解析到同一信用账户，账户有 100 可用额度，授权结果 approved=false。
     * 输出：记录授权拒绝交易事实和拒绝明细，余额不变，没有 ledger transaction、posting plan 或 LedgerEntry。
     * 红线：工具入口的授权拒绝不是资金冻结失败，也不是结算后的拒付/争议，不得产生账务副作用。
     */
    @Test
    void testAuthorizeByInstrumentShouldRecordDeclinedAuthorizationWithoutLedgerPosting() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        FundsSubjectBalanceDTO beforeDecline = balance(creditAccount);
        assertBucket(beforeDecline, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(beforeDecline, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        var beforeDeclineFacts = ledgerFactSnapshot(jdbcTemplate);

        String authorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeDeclineRequest(DECLINE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(authorizationSn).isNotBlank();
        FundsSubjectBalanceDTO afterDecline = balance(creditAccount);
        assertBucket(afterDecline, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(afterDecline, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeDeclineFacts);
        assertThat(fundsTransactionStatus(DECLINE_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.REJECTED.name());
        assertThat(fundsTransactionDetailStatuses(DECLINE_BUSINESS_SN))
                .containsExactly(FundsTransactionDetailStatus.REJECTED.name());
        assertThat(fundsTransactionAmounts(DECLINE_BUSINESS_SN))
                .containsExactly(0L, 0L, 0L, 0L);
        assertThat(routeSnapshotJson(DECLINE_BUSINESS_SN)).isNotBlank();
        assertThat(routeLegCount(DECLINE_BUSINESS_SN)).isZero();
        assertThat(ledgerTransactionCount(DECLINE_BUSINESS_SN)).isZero();
        assertThat(postingPlanCount(DECLINE_BUSINESS_SN)).isZero();
        assertThat(ledgerEntryCount(DECLINE_BUSINESS_SN)).isZero();
    }

    /**
     * 场景：上游拒绝授权时仍携带仅适用于批准路径的 controlScopeId，并完全相同地重放请求。
     * 输入：首次 approved=false 已固化 REJECTED 交易，随后以相同 businessSn、拒绝原因和 controlScopeId 重试。
     * 输出：返回原授权交易号，交易事实只保留一份，余额和账务事实不变。
     * 红线：批准路径忽略的规则 scope 不能破坏已经成立的拒绝结果幂等性。
     */
    @Test
    void testAuthorizeByInstrumentShouldReplayDeclinedAuthorizationWithIgnoredControlScope() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        AuthorizeByPaymentInstrumentRequest request = authorizeDeclineRequest(
                DECLINE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN).setControlScopeId("ignored_decline_scope");

        String firstAuthorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request, WindOperatorFactory.system());
        FundsSubjectBalanceDTO balanceAfterFirstDecline = balance(creditAccount);
        var factsAfterFirstDecline = ledgerFactSnapshot(jdbcTemplate);

        String replayedAuthorizationSn = paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                request, WindOperatorFactory.system());

        assertThat(replayedAuthorizationSn).isEqualTo(firstAuthorizationSn);
        assertThat(balance(creditAccount)).isEqualTo(balanceAfterFirstDecline);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstDecline);
        assertThat(countRows("t_funds_transaction", DECLINE_BUSINESS_SN)).isEqualTo(1);
    }

    /**
     * 场景：支付工具授权入口携带 Spend Rule 决策证据，且规则决策拒绝。
     * 输入：支付工具、资金责任和账户能力均可用，但 Spend Rule 决策结果为 REJECTED。
     * 输出：授权准入阶段拒绝，不创建资金交易、route、posting plan、账本交易或分录。
     * 红线：Spend Rule 是交易前准入控制，拒绝时不得委派账户主体型授权交易内核。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectSpendRuleDecisionWithoutFundsFacts() {
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        prepareSpendRuleDecisionData();
        recordAuthorizationDecision(SPEND_DECISION_SN,
                SPEND_REJECT_BUSINESS_SN,
                SpendControlDecisionResult.REJECTED,
                "超过单卡单日授权限额",
                SPEND_DECISION_DIGEST);
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeSpendRejectedRequest(), WindOperatorFactory.system()))
                .hasMessageContaining("Spend Rule 准入未通过");

        assertSpendRuleDecisionRecord();
        assertNoFundsOrLedgerFacts(SPEND_REJECT_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：wallet 可解析出有效 Spend Rule 挂载，但授权请求没有 decisionRef。
     * 输入：支付工具、资金责任和账户能力可用，规则挂载有效，授权候选结果为通过。
     * 输出：授权准入 fail-closed，不创建任何资金或账务事实。
     * 红线：存在适用规则时，省略规则字段不能被解释为默认通过。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectMissingDecisionReferenceForApplicableBinding() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        prepareSpendRuleDecisionData();
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(SPEND_REJECT_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperatorFactory.system()))
                .hasMessageContaining("适用 Spend Rule 挂载要求 decisionRef");

        assertNoFundsOrLedgerFacts(SPEND_REJECT_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：租户存在有效支出控制范围挂载，但支付工具授权请求省略 controlScopeId。
     * 输入：支付工具、资金责任和账户能力可用，SPEND_CONTROL_SCOPE 挂载有效，请求未提供控制范围。
     * 输出：支付工具业务入口 fail-closed，不创建资金交易、route、posting plan、账本交易或分录。
     * 红线：调用方省略控制范围不能把已有支出控制策略降级为无适用规则。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectUnresolvedSpendControlScopeWithoutFundsFacts() {
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        spendRuleDefinitionService.createDefinition(createSpendRuleDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishSpendRuleVersionRequest());
        spendRuleDefinitionService.createSpendRuleBinding(createSpendRuleBindingRequest()
                .setScopeType(SpendRuleScopeType.SPEND_CONTROL_SCOPE)
                .setScopeId(CONTROL_SCOPE_ID)
                .setDescription("挂载到支付工具授权控制范围"));
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(SPEND_REJECT_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperatorFactory.system()))
                .hasMessageContaining("SPEND_CONTROL_SCOPE 挂载无法从可信上下文解析");

        assertNoFundsOrLedgerFacts(SPEND_REJECT_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：授权请求只携带裸 PASSED 和格式化摘要，没有可回读 decisionRef。
     * 输入：调用方自报 PASSED 与 sha256 摘要。
     * 输出：授权入口直接拒绝，不创建任何资金或账务事实。
     * 红线：摘要格式合法不代表决策可信，裸结果不得获得授权能力。
     */
    @Test
    void testAuthorizeByInstrumentShouldRejectBarePassedDecisionWithoutReference() {
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.authorizeByInstrument(
                authorizeRequest(SPEND_REJECT_BUSINESS_SN, PAYMENT_INSTRUMENT_SN)
                        .setSpendDecisionResult(SpendControlDecisionResult.PASSED)
                        .setSpendDecisionDigest(SPEND_PASS_DECISION_DIGEST),
                WindOperatorFactory.system()))
                .hasMessageContaining("必须提供可回读的 decisionRef");

        assertNoFundsOrLedgerFacts(SPEND_REJECT_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpAuthorizationAdmissionTestData() {
        cleanupAuthorizationAdmissionTestData();
    }

    @AfterEach
    void tearDownAuthorizationAdmissionTestData() {
        cleanupAuthorizationAdmissionTestData();
    }

    private void cleanupAuthorizationAdmissionTestData() {
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
        jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn IN (
                    SELECT sn FROM t_ledger_transaction
                    WHERE business_sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)
                )
                """, AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                DECLINE_BUSINESS_SN, SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN, COMPLETION_BUSINESS_SN,
                REVERSAL_BUSINESS_SN, REFUND_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN, COMPLETION_BUSINESS_SN, REVERSAL_BUSINESS_SN,
                REFUND_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE business_sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN, COMPLETION_BUSINESS_SN, REVERSAL_BUSINESS_SN,
                REFUND_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE business_sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN, COMPLETION_BUSINESS_SN, REVERSAL_BUSINESS_SN,
                REFUND_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_frozen_order WHERE business_sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN, COMPLETION_BUSINESS_SN, REVERSAL_BUSINESS_SN,
                REFUND_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE business_sn IN (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN, COMPLETION_BUSINESS_SN, REVERSAL_BUSINESS_SN,
                REFUND_BUSINESS_SN);
        jdbcTemplate.update("""
                DELETE FROM t_spend_subject_funding_rel
                WHERE tenant_id = ? AND spend_subject_id IN (?, ?)
                """, TENANT_ID, CREDIT_ACCOUNT_SN, PREPAID_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?, ?)",
                CREDIT_ACCOUNT_SN,
                PREPAID_FUNDING_ACCOUNT_SN,
                PLATFORM_SETTLEMENT_ACCOUNT_SN,
                PARENT_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                PLATFORM_SETTLEMENT_ACCOUNT_SN,
                PARENT_FUNDING_ACCOUNT_SN,
                PREPAID_FUNDING_ACCOUNT_SN);
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

    private CreateFundingAccountRequest createPlatformSettlementAccountRequest() {
        return new CreateFundingAccountRequest()
                .setSn(PLATFORM_SETTLEMENT_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId("platform")
                .setOwnerType(FundsAccountOwnerType.PLATFORM)
                .setAccountType(FundsSubjectType.FUNDING_ACCOUNT.name())
                .setPlatform(Boolean.TRUE)
                .setAccountRoleCode(PlatformFundingAccountRole.SETTLEMENT)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_PLATFORM)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreateFundingAccountRequest createParentFundingAccountRequest() {
        return new CreateFundingAccountRequest()
                .setSn(PARENT_FUNDING_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.USER_WALLET.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreateFundingAccountRequest createPrepaidFundingAccountRequest() {
        return createParentFundingAccountRequest()
                .setSn(PREPAID_FUNDING_ACCOUNT_SN)
                .setAccountType(FundingAccountType.PREPAID_CARD.name());
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentFlowDirection direction) {
        return new CreatePaymentInstrumentRequest()
                .setSn(instrumentSn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setFlowDirection(direction)
                .setInstrumentNo("****2468")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_auth_admission_2468")
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
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

    private CreatePaymentInstrumentBindingRequest createPrepaidBindingRequest() {
        return createBindingRequest()
                .setSubjectId(PREPAID_FUNDING_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT);
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

    private CreateSpendSubjectFundingRelationRequest createParentFundingRelationRequest() {
        return createFundingRelationRequest()
                .setTargetSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setTargetSubjectId(PARENT_FUNDING_ACCOUNT_SN);
    }

    private CreateSpendSubjectFundingRelationRequest createPrepaidFundingRelationRequest() {
        return createParentFundingRelationRequest()
                .setSpendSubjectId(PREPAID_FUNDING_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.FUNDING_ACCOUNT);
    }

    private void prepareSpendRuleDecisionData() {
        spendRuleDefinitionService.createDefinition(createSpendRuleDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishSpendRuleVersionRequest());
        spendRuleBindingSn = spendRuleDefinitionService.createSpendRuleBinding(createSpendRuleBindingRequest()).getSn();
    }

    private FundsAccountId preparePassedAuthorizationData() {
        return preparePassedAuthorizationData(100L);
    }

    private FundsAccountId preparePassedAuthorizationData(long controlLimit) {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        prepareSpendRuleDecisionData();
        recordAuthorizationDecision(SPEND_PASS_DECISION_SN,
                AUTHORIZE_BUSINESS_SN,
                SpendControlDecisionResult.PASSED,
                null,
                SPEND_PASS_DECISION_DIGEST);
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        prepareBudgetControlLimit(controlLimit);
        return creditAccount;
    }

    private void prepareBudgetControlLimit(long amount) {
        spendControlMovementService.recordMovement(new RecordSpendControlMovementRequest()
                .setTenantId(TENANT_ID)
                .setMovementSn(CONTROL_LIMIT_MOVEMENT_SN)
                .setMovementType(SpendControlMovementType.LIMIT_INCREASED)
                .setBusinessScene("AUTHORIZATION_ADMISSION_CONTROL_LIMIT")
                .setBusinessSn(CONTROL_LIMIT_MOVEMENT_SN)
                .setTargetAccountId(creditAccountId())
                .setAmount(amount)
                .setCurrency(CURRENCY)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(CONTROL_PERIOD_ID)
                .setReasonCode("RULE_LIMIT_INITIALIZED")
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:AUTHORIZATION_ADMISSION_CONTROL_LIMIT")
                .setMovementDigest("sha256:auth-admission-control-limit-" + amount));
    }

    private SpendControlMovementDTO findControlReservation(String authorizationSn) {
        List<SpendControlMovementDTO> reservations = spendControlMovementService.queryMovements(
                new SpendControlMovementQuery()
                        .setTenantId(TENANT_ID)
                        .setMovementType(SpendControlMovementType.RESERVED)
                        .setTransactionSn(authorizationSn));
        assertThat(reservations).hasSize(1);
        return reservations.getFirst();
    }

    private BudgetControlProjectionDTO budgetControlProjection() {
        return spendControlMovementService.getBudgetControlProjection(new BudgetControlProjectionQuery()
                .setTenantId(TENANT_ID)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(CONTROL_PERIOD_ID)
                .setCurrency(CURRENCY)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setTargetAccountId(creditAccountId()));
    }

    private CreateSpendRuleDefinitionRequest createSpendRuleDefinitionRequest() {
        return new CreateSpendRuleDefinitionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleName("Authorization Admission Spend Rule")
                .setRuleType(SpendRuleType.AMOUNT_LIMIT)
                .setRuleDomain(SpendRuleDomain.PAYMENT_INSTRUMENT)
                .setDescription("支付工具授权准入决策消费测试规则");
    }

    private PublishSpendRuleVersionRequest publishSpendRuleVersionRequest() {
        return new PublishSpendRuleVersionRequest()
                .setTenantId(TENANT_ID)
                .setRuleId(SPEND_RULE_ID)
                .setRuleVersion(SPEND_RULE_VERSION)
                .setRuleSpec("{\"dslCaseId\":\"DSL-SPEND-RULE-DECISION-CONSUME-AUTH-001\"}")
                .setRuleDigest(SPEND_RULE_DIGEST)
                .setOperatorId("codex")
                .setAuditReferenceSn("grant:SPEND-RULE-DECISION-CONSUME")
                .setDescription("发布支付工具授权准入规则版本");
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
                .setDescription("挂载到支付工具授权准入 scope");
    }

    private AuthorizeByPaymentInstrumentRequest authorizeRequest(String businessSn, String instrumentSn) {
        return new AuthorizeByPaymentInstrumentRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(instrumentSn)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setApproved(Boolean.TRUE)
                .setExpectedBindingVersion(1)
                .setDescription("authorization admission flow");
    }

    private AuthorizeByPaymentInstrumentRequest authorizeDeclineRequest(String businessSn, String instrumentSn) {
        return authorizeRequest(businessSn, instrumentSn)
                .setApproved(Boolean.FALSE)
                .setDeclineReason("RISK_DECLINED");
    }

    private AuthorizeByPaymentInstrumentRequest authorizeSpendRejectedRequest() {
        return authorizeRequest(SPEND_REJECT_BUSINESS_SN, PAYMENT_INSTRUMENT_SN)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(CONTROL_PERIOD_ID);
    }

    private AuthorizeByPaymentInstrumentRequest authorizeSpendPassedRequest(String businessSn, String instrumentSn) {
        return authorizeRequest(businessSn, instrumentSn)
                .setSpendDecisionSn(SPEND_PASS_DECISION_SN)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(CONTROL_PERIOD_ID);
    }

    private void recordAuthorizationDecision(String decisionSn,
                                             String businessSn,
                                             SpendControlDecisionResult decisionResult,
                                             String rejectReason,
                                             String decisionDigest) {
        spendRuleDecisionRecordService.recordDecision(new RecordSpendRuleDecisionRecordRequest()
                .setTenantId(TENANT_ID)
                .setDecisionSn(decisionSn)
                .setRuleId(SPEND_RULE_ID)
                .setRuleVersion(SPEND_RULE_VERSION)
                .setSpendRuleBindingSn(spendRuleBindingSn)
                .setScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setScopeId(PAYMENT_INSTRUMENT_SN)
                .setInstrumentSn(PAYMENT_INSTRUMENT_SN)
                .setInstrumentBindingVersion(1)
                .setControlScopeId(CONTROL_SCOPE_ID)
                .setPeriodId(CONTROL_PERIOD_ID)
                .setTargetAccountId(creditAccountId())
                .setAction(PaymentInstrumentAction.AUTHORIZE)
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setDecisionResult(decisionResult)
                .setRejectReason(rejectReason)
                .setDecisionDigest(decisionDigest));
    }

    private FundsAccountId creditAccountId() {
        return FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private FundsAccountId parentFundingAccountId() {
        return FundsAccountId.immutable(PARENT_FUNDING_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private void adjustBalance(FundsAccountId accountId, long amount, String businessSn) {
        balanceControlService.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(amount, CURRENCY))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("AUTH_ADMISSION_LIMIT_ADJUST")
                .setBusinessSn(businessSn)
                .setAdjustReason("authorization admission test limit")
                .setAdjustEvidenceRef("EVIDENCE_" + businessSn)
                .setApprovalRef("APPROVAL_" + businessSn)
                .setDescription("authorization admission limit"), WindOperatorFactory.system());
    }

    private void initializeAvailableBalance(FundsAccountId accountId, long amount) {
        jdbcTemplate.update("""
                UPDATE t_ledger
                SET credit_amount = ?
                WHERE tenant_id = ?
                  AND subject_id = ?
                  AND subject_type = ?
                  AND ledger_subject_code = ?
                """, amount, TENANT_ID, accountId.id(), accountId.type(), LedgerSubjectCode.AVAILABLE.name());
    }

    private FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
        return balanceQueryService.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(accountId))
                .setCurrency(CURRENCY));
    }

    private String fundsTransactionStatus(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT status FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private List<String> fundsTransactionDetailStatuses(String businessSn) {
        return jdbcTemplate.queryForList("""
                SELECT status FROM t_funds_transaction_detail
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private List<String> ledgerTransactionEvents(String businessSn) {
        return ledgerTransactionEvents(BUSINESS_SCENE, businessSn);
    }

    private List<String> ledgerTransactionEvents(String businessScene, String businessSn) {
        return jdbcTemplate.queryForList("""
                SELECT event_type FROM t_ledger_transaction
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, businessScene, businessSn);
    }

    private List<Long> fundsTransactionAmounts(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT authorized_amount, reversed_amount, completed_amount, declined_amount
                FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, (rs, rowNum) -> List.of(
                rs.getLong("authorized_amount"),
                rs.getLong("reversed_amount"),
                rs.getLong("completed_amount"),
                rs.getLong("declined_amount")), BUSINESS_SCENE, businessSn);
    }

    private Long fundsRefundedAmount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT refunded_amount FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, Long.class, BUSINESS_SCENE, businessSn);
    }

    private List<String> ledgerEntrySubjects(String businessSn) {
        return ledgerEntrySubjects(BUSINESS_SCENE, businessSn);
    }

    private List<String> ledgerEntrySubjects(String businessScene, String businessSn) {
        return jdbcTemplate.queryForList("""
                SELECT subject_id FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, businessScene, businessSn);
    }

    private List<String> ledgerEntrySubjectCodes(String businessSn) {
        return jdbcTemplate.queryForList("""
                SELECT ledger_subject_code FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private Integer postingPlanCount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_posting_plan p
                JOIN t_ledger_transaction t ON p.ledger_transaction_sn = t.sn
                WHERE t.business_scene = ? AND t.business_sn = ?
                """, Integer.class, BUSINESS_SCENE, businessSn);
    }

    private Integer ledgerTransactionCount(String businessSn) {
        return countRows("t_ledger_transaction", businessSn);
    }

    private Integer ledgerEntryCount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                """, Integer.class, BUSINESS_SCENE, businessSn);
    }

    private String routeSnapshotJson(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT route_snapshot FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private int routeLegCount(String businessSn) {
        return JSON.parseObject(routeSnapshotJson(businessSn))
                .getJSONArray("legs")
                .size();
    }

    private void assertAuthorizationInstrumentSnapshot(String businessSn) {
        JSONObject paymentInstrumentRef = JSON.parseObject(routeSnapshotJson(businessSn))
                .getJSONObject("paymentInstrumentRef");
        assertThat(paymentInstrumentRef).isNotNull().isNotEmpty();
        assertThat(paymentInstrumentRef.getString("instrumentId")).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(paymentInstrumentRef.getString("instrumentType")).isEqualTo("CARD");
        assertThat(paymentInstrumentRef.getString("currency")).isEqualTo(CurrencyIsoCode.USD.name());
        assertThat(paymentInstrumentRef.getString("status")).isEqualTo(FundsAccountStatus.ACTIVE.name());

        JSONObject bindingSnapshot = paymentInstrumentRef.getJSONObject("bindingSnapshot");
        assertThat(bindingSnapshot).isNotNull().isNotEmpty();
        String bindingSn = bindingSnapshot.getString("bindingSn");
        assertThat(bindingSn).isNotBlank();
        assertThat(bindingSnapshot.getInteger("bindingVersion")).isEqualTo(1);
        assertThat(bindingSnapshot.getString("bindingRole"))
                .isEqualTo(PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name());
        assertThat(bindingSnapshot.getString("subjectType")).isEqualTo(FundsSubjectType.CREDIT_ACCOUNT.name());
        assertThat(bindingSnapshot.getString("subjectId")).isEqualTo(CREDIT_ACCOUNT_SN);
        assertThat(bindingSnapshot.getString("admissionAction")).isEqualTo("AUTHORIZE");
        assertThat(bindingSnapshot.getString("admissionDecision")).isEqualTo("APPROVED");
    }

    private void assertAuthorizationAdmissionContextSnapshot(String businessSn) {
        assertAuthorizationAdmissionContextSnapshot(businessSn, creditAccountId());
    }

    private void assertAuthorizationAdmissionContextSnapshot(String businessSn, FundsAccountId targetAccountId) {
        JSONObject contextVariables = JSON.parseObject(transactionContextVariablesJson(businessSn));
        String bindingSn = JSON.parseObject(routeSnapshotJson(businessSn))
                .getJSONObject("paymentInstrumentRef")
                .getJSONObject("bindingSnapshot")
                .getString("bindingSn");

        assertThat(contextVariables).isNotNull().isNotEmpty();
        assertThat(contextVariables.getString("instrumentSn")).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(contextVariables.getString("instrumentAction")).isEqualTo("AUTHORIZE");
        assertThat(contextVariables.getString("instrumentBindingRole"))
                .isEqualTo(PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name());
        assertThat(contextVariables.getString("instrumentBindingSn")).isEqualTo(bindingSn);
        assertThat(contextVariables.getInteger("instrumentBindingVersion")).isEqualTo(1);
        assertThat(contextVariables.getString("fundingRelationSn")).isNotBlank();
        assertThat(contextVariables.getString("fundingRelationType"))
                .isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE.name());
        assertThat(contextVariables.getString("targetAccountId")).isEqualTo(targetAccountId.id());
        assertThat(contextVariables.getString("targetAccountType")).isEqualTo(targetAccountId.type());
        JSONObject spendRuleDecision = contextVariables.getJSONObject("spendRuleDecision");
        assertThat(spendRuleDecision).isNotNull();
        assertThat(spendRuleDecision.getString("decisionResult"))
                .isEqualTo(SpendControlDecisionResult.NO_APPLICABLE_RULE.name());
        assertThat(spendRuleDecision).doesNotContainKeys("decisionRecordId", "ruleId", "decisionSn", "decisionDigest");
    }

    private String transactionContextVariablesJson(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT context_variables FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private void assertAuthorizationProjectionInstrumentExplanation(String authorizationSn) {
        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(authorizationSn)
                        .build());

        assertThat(explanation.factStatus()).isEqualTo("HELD");
        assertThat(explanation.displayStatus()).isEqualTo("AUTHORIZED_HOLD");
        assertThat(explanation.operationStatus()).isEqualTo("WAITING_CAPTURE_OR_RELEASE");
        assertThat(explanation.nextAction()).isEqualTo("WAIT_FOR_CAPTURE_OR_RELEASE");
        assertThat(explanation.evidenceRefs())
                .contains("paymentInstrument:" + PAYMENT_INSTRUMENT_SN)
                .anySatisfy(evidenceRef -> assertThat(evidenceRef)
                        .startsWith("paymentInstrumentBinding:PIB")
                        .endsWith(":v1"));
        assertThat(explanation.payload()).containsKey("paymentInstrumentRef");
        @SuppressWarnings("unchecked")
        Map<String, Object> paymentInstrumentRef = (Map<String, Object>) explanation.payload()
                .get("paymentInstrumentRef");
        assertThat(paymentInstrumentRef)
                .containsEntry("instrumentId", PAYMENT_INSTRUMENT_SN)
                .containsEntry("instrumentType", "CARD")
                .containsEntry("instrumentNo", "****2468")
                .containsEntry("currency", CurrencyIsoCode.USD.name())
                .containsEntry("status", FundsAccountStatus.ACTIVE.name());
        assertThat(paymentInstrumentRef)
                .doesNotContainKey("externalInstrumentId");
        assertThat(paymentInstrumentRef.toString()).doesNotContain("tok_auth_admission_2468");
        @SuppressWarnings("unchecked")
        Map<String, Object> bindingSnapshot = (Map<String, Object>) paymentInstrumentRef.get("bindingSnapshot");
        assertThat(bindingSnapshot)
                .containsEntry("bindingVersion", 1)
                .containsEntry("bindingRole", PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name())
                .containsEntry("subjectType", FundsSubjectType.CREDIT_ACCOUNT.name())
                .containsEntry("subjectId", CREDIT_ACCOUNT_SN)
                .containsEntry("admissionAction", "AUTHORIZE")
                .containsEntry("admissionDecision", "APPROVED");
        assertThat(bindingSnapshot.get("bindingSn")).asString().startsWith("PIB");
    }

    private void assertAuthorizationProjectionSpendRuleExplanation(String authorizationSn) {
        FundsTransactionProjectionExplanation explanation = projectionExplainApplicationService.explain(
                FundsTransactionProjectionExplainQuery.builder()
                        .fundsTransactionSn(authorizationSn)
                        .build());

        assertThat(explanation.evidenceRefs())
                .contains("spendRule:" + SPEND_RULE_ID,
                        "spendRuleVersion:" + SPEND_RULE_ID + ":" + SPEND_RULE_VERSION,
                        "spendRuleBinding:" + spendRuleBindingSn,
                        "spendRuleDecision:" + SPEND_PASS_DECISION_SN);
        assertThat(explanation.payload()).containsKey("spendRuleDecision");
        @SuppressWarnings("unchecked")
        Map<String, Object> spendRuleDecision = (Map<String, Object>) explanation.payload()
                .get("spendRuleDecision");
        assertThat(spendRuleDecision)
                .containsEntry("ruleId", SPEND_RULE_ID)
                .containsEntry("ruleVersion", SPEND_RULE_VERSION)
                .containsEntry("spendRuleBindingSn", spendRuleBindingSn)
                .containsEntry("scopeType", SpendRuleScopeType.PAYMENT_INSTRUMENT.name())
                .containsEntry("scopeId", PAYMENT_INSTRUMENT_SN)
                .containsEntry("decisionSn", SPEND_PASS_DECISION_SN)
                .containsEntry("decisionResult", SpendControlDecisionResult.PASSED.name())
                .containsEntry("decisionDigest", SPEND_PASS_DECISION_DIGEST)
                .containsEntry("controlScopeId", CONTROL_SCOPE_ID)
                .containsEntry("periodId", CONTROL_PERIOD_ID);
        assertThat(spendRuleDecision).containsKeys("decisionRecordId", "controlReservationSn");
        assertThat(spendRuleDecision.get("controlReservationSn")).asString().startsWith("SCR");
        assertThat(explanation.payload().toString())
                .doesNotContain("dslCaseId")
                .doesNotContain("script");
    }

    private void assertSpendRuleDecisionRecord() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_spend_rule_decision_record
                WHERE tenant_id = ?
                  AND decision_sn = ?
                  AND rule_id = ?
                  AND rule_version = ?
                  AND spend_rule_binding_sn = ?
                  AND scope_type = ?
                  AND scope_id = ?
                  AND instrument_sn = ?
                  AND business_scene = ?
                  AND business_sn = ?
                  AND decision_result = ?
                  AND decision_digest = ?
                """, Integer.class, TENANT_ID, SPEND_DECISION_SN, SPEND_RULE_ID, SPEND_RULE_VERSION,
                spendRuleBindingSn, SpendRuleScopeType.PAYMENT_INSTRUMENT.name(), PAYMENT_INSTRUMENT_SN,
                PAYMENT_INSTRUMENT_SN, BUSINESS_SCENE, SPEND_REJECT_BUSINESS_SN,
                SpendControlDecisionResult.REJECTED.name(), SPEND_DECISION_DIGEST)).isEqualTo(1);
    }

    private void assertNoFundsOrLedgerFacts(String businessSn) {
        assertThat(countRows("t_funds_transaction", businessSn)).isZero();
        assertThat(countRows("t_funds_transaction_detail", businessSn)).isZero();
        assertThat(countRows("t_ledger_transaction", businessSn)).isZero();
        assertThat(countRows("t_ledger_entry", businessSn)).isZero();
        assertThat(postingPlanCount(businessSn)).isZero();
    }

    private int countRows(String tableName, String businessSn) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                Integer.class, BUSINESS_SCENE, businessSn);
    }

    @Configuration
    @Import({
            FundsDirectTransactionInstructionConverter.class,
            FundsBalanceControlInstructionConverter.class,
            FundsAuthorizationInstructionConverter.class,
            RouteParticipantFactory.class,
            RouteSubjectSupport.class,
            PlatformAccountRouteSupport.class,
            DefaultRouteReplayService.class,
            TransferFundsInstructionRouteResolver.class,
            BalanceControlFundsInstructionRouteResolver.class,
            AuthorizationFundsInstructionRouteResolver.class,
            CompositeRouteResolver.class,
            RefundRouteAdmission.class,
            RouteFeeChargeAppender.class,
            RouteAccountHierarchySnapshotAppender.class,
            DefaultRouteSnapshotFactory.class,
            DefaultLedgerPostingAssembler.class,
            DefaultRoutedFundsInstructionOrchestrator.class,
            FundsTransactionCommandServiceImpl.class,
            LedgerServiceImpl.class,
            LedgerTransactionServiceImpl.class,
            LedgerBalanceProjectionServiceImpl.class,
            DefaultLedgerTransactionPostingServiceImpl.class,
            DefaultFundsInstructionLifecycleSaver.class,
            DefaultFundsFrozenOrderLifecycleSaver.class,
            DelegatingFundsInstructionLifecycleRecorder.class,
            DefaultFundsTransactionQueryService.class,
            DefaultLedgerQueryService.class,
            DefaultFundsTransactionProjectionExplainApplicationService.class,
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            AccountHierarchyRelationServiceImpl.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentBindingServiceImpl.class,
            PaymentInstrumentBindingHistoryServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class,
            FundingResponsibilityResolutionApplicationServiceImpl.class,
            FundsAccountCapabilityApplicationServiceImpl.class,
            PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl.class,
            SpendControlAdmissionApplicationServiceImpl.class,
            SpendRuleDefinitionServiceImpl.class,
            SpendRuleVersionServiceImpl.class,
            SpendRuleBindingServiceImpl.class,
            SpendRuleDecisionRecordServiceImpl.class,
            SpendControlMovementServiceImpl.class,
            SpendControlTransactionConsumptionApplicationServiceImpl.class,
            PaymentInstrumentAuthorizationProcessor.class,
            PaymentInstrumentTransactionApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {
    }
}
