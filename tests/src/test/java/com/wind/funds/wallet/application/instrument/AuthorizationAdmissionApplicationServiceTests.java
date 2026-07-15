package com.wind.funds.wallet.application.instrument;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.capte.domain.core.operator.WindOperator;
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
import com.wind.funds.route.TransferFundsInstructionRouteResolver;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainQuery;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.transaction.projection.impl.DefaultFundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.transaction.application.instrument.impl.AuthorizationAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.request.CreateSpendRuleBindingRequest;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendRuleDefinitionRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.model.request.PublishSpendRuleVersionRequest;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendRuleDefinitionService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.AccountHierarchyBindingServiceImpl;
import com.wind.funds.wallet.services.impl.AccountHierarchyServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerQueryService;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingConcurrencyGuard;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
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
        AuthorizationAdmissionApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthorizationAdmissionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String CREDIT_ACCOUNT_SN = "auth_admission_credit";

    private static final String PARENT_FUNDING_ACCOUNT_SN = "auth_admission_parent_funding";

    private static final String PLATFORM_SETTLEMENT_ACCOUNT_SN = "auth_adm_settle";

    private static final String PAYMENT_INSTRUMENT_SN = "pi_auth_admission_card";

    private static final String RECEIVE_INSTRUMENT_SN = "pi_auth_admission_receive";

    private static final String PAYMENT_BINDING_SN = "pi_auth_admission_binding";

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
    private FundsBalanceControlService balanceControlService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private AuthorizationAdmissionApplicationService authorizationAdmissionApplicationService;

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

        String authorizationSn = authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperator.system());

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

        String authorizationSn = authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperator.system());

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
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);

        String authorizationSn = authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeSpendPassedRequest(AUTHORIZE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperator.system());

        assertThat(authorizationSn).isNotBlank();
        var beforeExplainFacts = ledgerFactSnapshot(jdbcTemplate);
        assertAuthorizationProjectionSpendRuleExplanation(authorizationSn);
        assertLedgerFactsUnchanged(jdbcTemplate, beforeExplainFacts);
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

        assertThatThrownBy(() -> authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeRequest(DIRECTION_FAIL_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperator.system()))
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

        assertThatThrownBy(() -> authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeRequest(TENANT_MISMATCH_BUSINESS_SN, PAYMENT_INSTRUMENT_SN).setTenantId(TENANT_ID + 1),
                WindOperator.system()))
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

        String authorizationSn = authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeDeclineRequest(DECLINE_BUSINESS_SN, PAYMENT_INSTRUMENT_SN), WindOperator.system());

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
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> authorizationAdmissionApplicationService.authorizeByInstrument(
                authorizeSpendRejectedRequest(), WindOperator.system()))
                .hasMessageContaining("Spend Rule 准入未通过");

        assertSpendRuleDecisionRecord();
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
                    WHERE business_sn IN (?, ?, ?, ?, ?, ?)
                )
                """, AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                DECLINE_BUSINESS_SN, SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_sn IN (?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE business_sn IN (?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE business_sn IN (?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_frozen_order WHERE business_sn IN (?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE business_sn IN (?, ?, ?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN,
                SPEND_REJECT_BUSINESS_SN, TENANT_MISMATCH_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID,
                CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                CREDIT_ACCOUNT_SN,
                PLATFORM_SETTLEMENT_ACCOUNT_SN,
                PARENT_FUNDING_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?)",
                PLATFORM_SETTLEMENT_ACCOUNT_SN,
                PARENT_FUNDING_ACCOUNT_SN);
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

    private CreateSpendSubjectFundingRelationRequest createParentFundingRelationRequest() {
        return createFundingRelationRequest()
                .setTargetSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setTargetSubjectId(PARENT_FUNDING_ACCOUNT_SN);
    }

    private void prepareSpendRuleDecisionData() {
        spendRuleDefinitionService.createDefinition(createSpendRuleDefinitionRequest());
        spendRuleDefinitionService.publishVersion(publishSpendRuleVersionRequest());
        spendRuleBindingSn = spendRuleDefinitionService.createSpendRuleBinding(createSpendRuleBindingRequest()).getSn();
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
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendRuleBindingSn(spendRuleBindingSn)
                .setSpendRuleScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setSpendRuleScopeId(PAYMENT_INSTRUMENT_SN)
                .setSpendDecisionSn(SPEND_DECISION_SN)
                .setSpendDecisionResult(SpendControlDecisionResult.REJECTED)
                .setSpendDecisionDigest(SPEND_DECISION_DIGEST)
                .setControlScopeId("budget_auth_admission")
                .setSpendDecisionRejectReason("超过单卡单日授权限额");
    }

    private AuthorizeByPaymentInstrumentRequest authorizeSpendPassedRequest(String businessSn, String instrumentSn) {
        return authorizeRequest(businessSn, instrumentSn)
                .setSpendRuleId(SPEND_RULE_ID)
                .setSpendRuleVersion(SPEND_RULE_VERSION)
                .setSpendRuleBindingSn(spendRuleBindingSn)
                .setSpendRuleScopeType(SpendRuleScopeType.PAYMENT_INSTRUMENT)
                .setSpendRuleScopeId(PAYMENT_INSTRUMENT_SN)
                .setSpendDecisionSn(SPEND_PASS_DECISION_SN)
                .setSpendDecisionResult(SpendControlDecisionResult.PASSED)
                .setSpendDecisionDigest(SPEND_PASS_DECISION_DIGEST)
                .setControlScopeId("budget_auth_admission");
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
                .setDescription("authorization admission limit"), WindOperator.system());
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
        return jdbcTemplate.queryForList("""
                SELECT event_type FROM t_ledger_transaction
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private List<Long> fundsTransactionAmounts(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT authorized_amount, reversed_amount, settled_amount, declined_amount
                FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, (rs, rowNum) -> List.of(
                rs.getLong("authorized_amount"),
                rs.getLong("reversed_amount"),
                rs.getLong("settled_amount"),
                rs.getLong("declined_amount")), BUSINESS_SCENE, businessSn);
    }

    private List<String> ledgerEntrySubjects(String businessSn) {
        return jdbcTemplate.queryForList("""
                SELECT subject_id FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                ORDER BY id ASC
                """, String.class, BUSINESS_SCENE, businessSn);
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
        assertThat(bindingSnapshot.getString("bindingSn")).isEqualTo(PAYMENT_BINDING_SN);
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

        assertThat(contextVariables).isNotNull().isNotEmpty();
        assertThat(contextVariables.getString("instrumentSn")).isEqualTo(PAYMENT_INSTRUMENT_SN);
        assertThat(contextVariables.getString("instrumentAction")).isEqualTo("AUTHORIZE");
        assertThat(contextVariables.getString("instrumentBindingRole"))
                .isEqualTo(PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name());
        assertThat(contextVariables.getString("instrumentBindingSn")).isEqualTo(PAYMENT_BINDING_SN);
        assertThat(contextVariables.getInteger("instrumentBindingVersion")).isEqualTo(1);
        assertThat(contextVariables.getString("fundingRelationSn")).isNotBlank();
        assertThat(contextVariables.getString("fundingRelationType"))
                .isEqualTo(SpendSubjectFundingRelationType.FUNDING_SOURCE.name());
        assertThat(contextVariables.getString("targetAccountId")).isEqualTo(targetAccountId.id());
        assertThat(contextVariables.getString("targetAccountType")).isEqualTo(targetAccountId.type());
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

        assertThat(explanation.evidenceRefs())
                .contains("paymentInstrument:" + PAYMENT_INSTRUMENT_SN,
                        "paymentInstrumentBinding:" + PAYMENT_BINDING_SN + ":v1");
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
                .containsEntry("bindingSn", PAYMENT_BINDING_SN)
                .containsEntry("bindingVersion", 1)
                .containsEntry("bindingRole", PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name())
                .containsEntry("subjectType", FundsSubjectType.CREDIT_ACCOUNT.name())
                .containsEntry("subjectId", CREDIT_ACCOUNT_SN)
                .containsEntry("admissionAction", "AUTHORIZE")
                .containsEntry("admissionDecision", "APPROVED");
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
                .containsEntry("controlScopeId", "budget_auth_admission");
        assertThat(spendRuleDecision).containsKey("decisionRecordId");
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
            AccountHierarchyBindingServiceImpl.class,
            AccountHierarchyServiceImpl.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            PaymentInstrumentBindingConcurrencyGuard.class,
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
            AuthorizationAdmissionApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {
    }
}
