package com.wind.funds.wallet.application.instrument;

import com.alibaba.fastjson2.JSON;
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
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.transaction.ledger.DefaultLedgerPostingAssembler;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.AuthorizationAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.enums.CreditFundsAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentDirection;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateCreditAccountRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.service.CreditAccountService;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.AccountHierarchyServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultLedgerProfileServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultSubjectLedgerInitializer;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
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

import java.util.List;

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

    private static final String PLATFORM_SETTLEMENT_ACCOUNT_SN = "auth_adm_settle";

    private static final String PAYMENT_INSTRUMENT_SN = "pi_auth_admission_card";

    private static final String RECEIVE_INSTRUMENT_SN = "pi_auth_admission_receive";

    private static final String PAYMENT_BINDING_SN = "pi_auth_admission_binding";

    private static final String FUNDING_RELATION_SN = "auth_admission_relation";

    private static final String OWNER_ID = "owner_auth_admission";

    private static final String CHANNEL_CODE = "issuer_processor";

    private static final String BUSINESS_SCENE = "AUTHORIZATION_ADMISSION";

    private static final String AUTHORIZE_BUSINESS_SN = "AUTH_ADMISSION_AUTHORIZE";

    private static final String BALANCE_ADJUST_BUSINESS_SN = "AUTH_ADMISSION_LIMIT";

    private static final String DIRECTION_FAIL_BUSINESS_SN = "AUTH_ADMISSION_DIRECTION_FAIL";

    private static final String DECLINE_BUSINESS_SN = "AUTH_ADMISSION_DECLINE";

    @Autowired
    private CreditAccountService creditAccountService;

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private FundsBalanceControlService balanceControlService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private AuthorizationAdmissionApplicationService authorizationAdmissionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：VCC/卡支付工具入口完成授权准入后，委派账户主体型授权交易内核。
     * 输入：支付工具绑定信用账户，资金责任解析到同一信用账户，账户有 100 可用额度，授权 60。
     * 输出：返回授权交易号，信用账户 AVAILABLE 减 60、AUTHORIZATION 增 60，并产生可追溯交易和账本事实。
     * 红线：wallet 应用入口不直接写账，最终账务事实必须由标准授权交易路由和 ledger posting 链路产生。
     */
    @Test
    void testAuthorizeByPaymentInstrumentShouldResolveAdmissionAndDelegateAuthorizationKernel() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentDirection.PAYMENT));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        FundsSubjectBalanceDTO beforeAuthorize = balance(creditAccount);
        assertBucket(beforeAuthorize, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(beforeAuthorize, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);

        String authorizationSn = authorizationAdmissionApplicationService.authorizeByPaymentInstrument(
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
    }

    /**
     * 场景：支付工具方向不支持授权。
     * 输入：RECEIVE-only 工具发起授权准入。
     * 输出：准入阶段拒绝，业务流水下没有资金交易、账本交易、posting plan 或分录。
     * 红线：工具准入失败不得进入交易内核，不得留下半成功资金事实。
     */
    @Test
    void testAuthorizeByPaymentInstrumentShouldRejectDirectionMismatchWithoutFundsFacts() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(RECEIVE_INSTRUMENT_SN,
                PaymentInstrumentDirection.RECEIVE));
        var before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> authorizationAdmissionApplicationService.authorizeByPaymentInstrument(
                authorizeRequest(DIRECTION_FAIL_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperator.system()))
                .hasMessageContaining("支付工具方向不支持当前动作");

        assertNoFundsOrLedgerFacts(DIRECTION_FAIL_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：支付工具入口透传授权拒绝结果。
     * 输入：支付工具绑定信用账户，资金责任解析到同一信用账户，账户有 100 可用额度，授权结果 approved=false。
     * 输出：记录授权拒绝交易事实和拒绝明细，余额不变，没有 ledger transaction、posting plan 或 LedgerEntry。
     * 红线：工具入口的授权拒绝不是资金冻结失败，也不是结算后的拒付/争议，不得产生账务副作用。
     */
    @Test
    void testAuthorizeByPaymentInstrumentShouldRecordDeclinedAuthorizationWithoutLedgerPosting() {
        FundsAccountId creditAccount = creditAccountId();
        fundingAccountService.createFundingAccount(createPlatformSettlementAccountRequest());
        creditAccountService.createCreditAccount(createCreditAccountRequest());
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_INSTRUMENT_SN,
                PaymentInstrumentDirection.PAYMENT));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest());
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
        adjustBalance(creditAccount, 100L, BALANCE_ADJUST_BUSINESS_SN);
        FundsSubjectBalanceDTO beforeDecline = balance(creditAccount);
        assertBucket(beforeDecline, LedgerSubjectCode.AVAILABLE, 100L, CURRENCY);
        assertBucket(beforeDecline, LedgerSubjectCode.AUTHORIZATION, 0L, CURRENCY);
        var beforeDeclineFacts = ledgerFactSnapshot(jdbcTemplate);

        String authorizationSn = authorizationAdmissionApplicationService.authorizeByPaymentInstrument(
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

    @BeforeEach
    void setUpAuthorizationAdmissionTestData() {
        cleanupAuthorizationAdmissionTestData();
    }

    @AfterEach
    void tearDownAuthorizationAdmissionTestData() {
        cleanupAuthorizationAdmissionTestData();
    }

    private void cleanupAuthorizationAdmissionTestData() {
        jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn IN (
                    SELECT sn FROM t_ledger_transaction
                    WHERE business_sn IN (?, ?, ?, ?)
                )
                """, AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                DECLINE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_sn IN (?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_transaction WHERE business_sn IN (?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction_detail WHERE business_sn IN (?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_frozen_order WHERE business_sn IN (?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE business_sn IN (?, ?, ?, ?)",
                AUTHORIZE_BUSINESS_SN, BALANCE_ADJUST_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, DECLINE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE sn = ?", FUNDING_RELATION_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?)",
                PAYMENT_INSTRUMENT_SN, RECEIVE_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?)",
                CREDIT_ACCOUNT_SN, PLATFORM_SETTLEMENT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_credit_account WHERE sn = ?", CREDIT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn = ?", PLATFORM_SETTLEMENT_ACCOUNT_SN);
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

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentDirection direction) {
        return new CreatePaymentInstrumentRequest()
                .setSn(instrumentSn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("CARD")
                .setInstrumentDirection(direction)
                .setInstrumentNo("****2468")
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId("tok_auth_admission_2468")
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest() {
        return new CreatePaymentInstrumentBindingRequest()
                .setSn(PAYMENT_BINDING_SN)
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
                .setSn(FUNDING_RELATION_SN)
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(CREDIT_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.CREDIT_ACCOUNT)
                .setTargetSubjectId(CREDIT_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(SpendSubjectFundingRelationType.FUNDING_SOURCE)
                .setPriority(10)
                .setDefaultRelation(Boolean.TRUE)
                .setStatus(FundsAccountStatus.ACTIVE);
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

    private FundsAccountId creditAccountId() {
        return FundsAccountId.immutable(CREDIT_ACCOUNT_SN, FundsSubjectType.CREDIT_ACCOUNT);
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
            DefaultLedgerProfileServiceImpl.class,
            DefaultSubjectLedgerInitializer.class,
            AccountHierarchyServiceImpl.class,
            FundingAccountServiceImpl.class,
            CreditAccountServiceImpl.class,
            SpendSubjectFundingRelationServiceImpl.class,
            PaymentInstrumentServiceImpl.class,
            PaymentInstrumentCapabilityApplicationServiceImpl.class,
            FundingResponsibilityResolutionApplicationServiceImpl.class,
            AuthorizationAdmissionApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {
    }
}
