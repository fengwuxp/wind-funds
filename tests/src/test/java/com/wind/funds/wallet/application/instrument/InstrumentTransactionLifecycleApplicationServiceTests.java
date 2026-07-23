package com.wind.funds.wallet.application.instrument;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wind.integration.operator.WindOperator;
import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.request.CreateLedgerRequest;
import com.wind.funds.ledger.request.UpdateLedgerBalanceRequest;
import com.wind.funds.ledger.service.LedgerService;
import com.wind.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.wind.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.wind.funds.route.CompositeRouteResolver;
import com.wind.funds.route.DefaultRouteReplayService;
import com.wind.funds.route.DefaultRouteSnapshotFactory;
import com.wind.funds.route.RefundRouteAdmission;
import com.wind.funds.route.RouteFeeChargeAppender;
import com.wind.funds.route.RouteAccountHierarchySnapshotAppender;
import com.wind.funds.route.TransferFundsInstructionRouteResolver;
import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.route.support.PlatformAccountRouteSupport;
import com.wind.funds.route.support.RouteParticipantFactory;
import com.wind.funds.route.support.RouteSubjectSupport;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.funds.transaction.application.FundsBalanceControlService;
import com.wind.funds.transaction.application.FundsDirectTransactionService;
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.transaction.application.instrument.impl.InstrumentTransactionLifecycleApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.request.AuthorizeByPaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.request.PayOutByRailRequest;
import com.wind.funds.wallet.model.request.ReceiveByInstrumentRequest;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
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
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 支付工具交易生命周期应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        InstrumentTransactionLifecycleApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InstrumentTransactionLifecycleApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String RECEIVE_ACCOUNT_SN = "inst_lifecycle_recv_acc";

    private static final String CASH_MAPPING_ACCOUNT_SN = "inst_lifecycle_cash_map";

    private static final String PREPAYMENT_ACCOUNT_SN = "inst_lifecycle_prepay";

    private static final String RECEIVE_INSTRUMENT_SN = "pi_lifecycle_va_receive";

    private static final String RECEIVE_INSTRUMENT_NO = "VA-****-2468";

    private static final String PAYMENT_ONLY_INSTRUMENT_SN = "pi_lifecycle_payment_only";

    private static final String PAYOUT_INSTRUMENT_SN = "pi_lifecycle_payout";

    private static final String PAYOUT_INSTRUMENT_NO = "VA-PAYOUT-1357";

    private static final String OWNER_ID = "owner_instrument_lifecycle";

    private static final String CHANNEL_CODE = "bank_rail";

    private static final String CHANNEL_ID = "bank_rail_provider";

    private static final String BUSINESS_SCENE = "INSTRUMENT_RECEIVE";

    private static final String RECEIVE_BUSINESS_SN = "INSTRUMENT_RECEIVE_001";

    private static final String DIRECTION_FAIL_BUSINESS_SN = "INSTRUMENT_RECEIVE_DIRECTION_FAIL";

    private static final String MISSING_BINDING_VERSION_BUSINESS_SN = "INSTRUMENT_RECEIVE_MISSING_BINDING_VERSION";

    private static final String UNSUPPORTED_CHANNEL_BUSINESS_SN = "INSTRUMENT_RECEIVE_UNSUPPORTED_CHANNEL";

    private static final String PAYOUT_BUSINESS_SN = "INSTRUMENT_PAYOUT_RAIL_001";

    private static final String UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN = "INSTRUMENT_PAYOUT_RAIL_UNSUPPORTED";

    private static final String PROCESSING_PAYOUT_BUSINESS_SN = "INSTRUMENT_PAYOUT_RAIL_PROCESSING";

    private static final String PAYOUT_FREEZE_BUSINESS_SN = "INSTRUMENT_PAYOUT_RAIL_FREEZE";

    private static final String AUTHORIZE_BUSINESS_SN = "INSTRUMENT_AUTHORIZE_001";

    private static final String DELEGATED_AUTHORIZATION_SN = "delegated_instrument_authorization_sn";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private FundsBalanceControlService balanceControlService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private InstrumentTransactionLifecycleApplicationService instrumentTransactionLifecycleApplicationService;

    @Autowired
    private RecordingAuthorizationAdmissionApplicationService authorizationAdmissionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：统一支付工具交易生命周期入口承接授权动作。
     * 输入：VCC/卡支付工具授权请求。
     * 输出：生命周期入口返回授权准入专项服务委派结果，并透传原请求。
     * 红线：本入口只做服务层门面收敛，不复制授权准入、route、posting 或 ledger 逻辑。
     */
    @Test
    void testAuthorizeByInstrumentShouldDelegateAuthorizationAdmissionFacade() {
        AuthorizeByPaymentInstrumentRequest request = authorizeRequest();

        String authorizationSn = instrumentTransactionLifecycleApplicationService.authorizeByInstrument(request,
                WindOperatorFactory.system());

        assertThat(authorizationSn).isEqualTo(DELEGATED_AUTHORIZATION_SN);
        assertThat(authorizationAdmissionApplicationService.getInvocationCount()).isOne();
        assertThat(authorizationAdmissionApplicationService.getLastRequest()).isSameAs(request);
    }

    /**
     * 场景：VA/ACH 等收款工具入口完成预交易准入后，委派账户主体型充值交易内核。
     * 输入：收款工具绑定资金账户，资金责任解析到同一资金账户，外部银行账户通过 bank_rail 入金 80。
     * 输出：返回充值交易号，资金账户 AVAILABLE 增加 80，并产生标准 TOPUP 交易、route 和账本事实。
     * 红线：wallet 应用入口负责把业务 rail 解析为交易层渠道，不直接写账；交易内核 canonical 入参仍是已解析的资金账户主体。
     */
    @Test
    void testReceiveByInstrumentShouldResolveTargetAccountAndDelegateTopupKernel() {
        createReceiveScenario();
        FundsAccountId receiveAccount = receiveAccountId();
        FundsSubjectBalanceDTO beforeReceive = balance(receiveAccount);
        assertBucket(beforeReceive, LedgerSubjectCode.AVAILABLE, 0L, CurrencyIsoCode.USD);

        String transactionSn = instrumentTransactionLifecycleApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(transactionSn).isNotBlank();
        FundsSubjectBalanceDTO afterReceive = balance(receiveAccount);
        assertBucket(afterReceive, LedgerSubjectCode.AVAILABLE, 80L, CurrencyIsoCode.USD);
        assertThat(fundsTransactionStatus(RECEIVE_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.CLOSED.name());
        assertThat(fundsTransactionDetailStatuses(RECEIVE_BUSINESS_SN))
                .hasSize(3)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED.name());
        assertThat(ledgerTransactionEvents(RECEIVE_BUSINESS_SN))
                .containsExactly(FundsTransactionEventType.TOPUP.name());
        assertThat(ledgerEntrySubjects(RECEIVE_BUSINESS_SN))
                .contains(RECEIVE_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
        assertThat(ledgerEntrySubjectCodes(RECEIVE_BUSINESS_SN))
                .contains(LedgerSubjectCode.AVAILABLE.name(), LedgerSubjectCode.CASH.name(),
                        LedgerSubjectCode.PREPAYMENT.name());
        assertThat(postingPlanCount(RECEIVE_BUSINESS_SN)).isEqualTo(2);
        assertThat(ledgerEntryCount(RECEIVE_BUSINESS_SN)).isEqualTo(4);
        assertThat(routeLegCount(RECEIVE_BUSINESS_SN)).isEqualTo(2);
        assertReceiveRouteSnapshot(RECEIVE_BUSINESS_SN);
        assertReceiveFactsKeepDirectContextMinimal(RECEIVE_BUSINESS_SN);
    }

    /**
     * 场景：收款入口收到未知 rail / channel 编码。
     * 输入：支付工具、绑定版本和资金责任均有效，但 channelCode 为 mystery_rail。
     * 输出：服务层入口给出可读的渠道拒绝原因，不泄露 Java enum valueOf 实现细节。
     * 红线：非法 rail 不得进入交易内核，不得生成资金交易、route、posting plan、账本交易或分录。
     */
    @Test
    void testReceiveByInstrumentShouldRejectUnsupportedRailCodeWithoutFundsFacts() {
        createReceiveScenario();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        ReceiveByInstrumentRequest request = receiveRequest(UNSUPPORTED_CHANNEL_BUSINESS_SN,
                RECEIVE_INSTRUMENT_SN)
                .setChannelCode("mystery_rail");

        assertThatThrownBy(() -> instrumentTransactionLifecycleApplicationService.receiveByInstrument(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("收款渠道编码不支持")
                .hasMessageContaining("mystery_rail")
                .hasMessageContaining("支持的收款渠道");

        assertNoFundsOrLedgerFacts(UNSUPPORTED_CHANNEL_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：付款-only 工具不能作为收款入口。
     * 输入：PAYMENT-only 工具发起收款。
     * 输出：准入阶段拒绝，不创建资金交易、route、posting plan、账本交易或分录。
     * 红线：支付工具能力不匹配时不得进入充值交易内核，不得留下半成功资金事实。
     */
    @Test
    void testReceiveByInstrumentShouldRejectPaymentOnlyInstrumentWithoutFundsFacts() {
        createReceiveAccount();
        createPaymentOnlyInstrumentScenario();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> instrumentTransactionLifecycleApplicationService.receiveByInstrument(
                receiveRequest(DIRECTION_FAIL_BUSINESS_SN, PAYMENT_ONLY_INSTRUMENT_SN), WindOperatorFactory.system()))
                .hasMessageContaining("支付工具资金流向不支持当前动作");

        assertNoFundsOrLedgerFacts(DIRECTION_FAIL_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：收款工具入口必须携带调用方期望的绑定版本。
     * 输入：VA 收款工具、资金责任和账户能力均有效，但请求未传 expectedBindingVersion。
     * 输出：准入阶段拒绝，不创建资金交易、route、posting plan、账本交易或分录。
     * 红线：收款入口不得在换绑风险下使用“无版本预期”的默认绑定继续入账。
     */
    @Test
    void testReceiveByInstrumentShouldRejectMissingBindingVersionWithoutFundsFacts() {
        createReceiveScenario();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        ReceiveByInstrumentRequest request = receiveRequest(MISSING_BINDING_VERSION_BUSINESS_SN,
                RECEIVE_INSTRUMENT_SN)
                .setExpectedBindingVersion(null);

        assertThatThrownBy(() -> instrumentTransactionLifecycleApplicationService.receiveByInstrument(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("支付工具收款绑定版本不能为空");

        assertNoFundsOrLedgerFacts(MISSING_BINDING_VERSION_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：全球账户出款 rail 入口完成预交易准入后，委派账户主体型提现内核。
     * 输入：出款工具绑定资金账户，资金责任解析到同一资金账户，账户已先入账 80 并冻结 70。
     * 输出：返回提现交易号，资金账户 FROZEN 扣减 70，并产生标准 WITHDRAW 交易、route 和账本事实。
     * 红线：wallet 应用入口不直接写账；提现确认必须引用已冻结资金，不能二次扣减 AVAILABLE。
     */
    @Test
    void testPayOutByRailShouldResolveTargetAccountAndDelegateWithdrawKernel() {
        createReceiveScenario();
        instrumentTransactionLifecycleApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system());
        createPayoutInstrumentScenario();
        FundsAccountId receiveAccount = receiveAccountId();
        String freezeSn = freezeForPayout(receiveAccount);
        FundsSubjectBalanceDTO afterFreeze = balance(receiveAccount);
        assertBucket(afterFreeze, LedgerSubjectCode.AVAILABLE, 10L, CurrencyIsoCode.USD);
        assertBucket(afterFreeze, LedgerSubjectCode.FROZEN, 70L, CurrencyIsoCode.USD);

        String transactionSn = instrumentTransactionLifecycleApplicationService.payOutByRail(
                payoutRequest().setReferenceFreezeSn(freezeSn), WindOperatorFactory.system());

        assertThat(transactionSn).isNotBlank();
        FundsSubjectBalanceDTO afterPayout = balance(receiveAccount);
        assertBucket(afterPayout, LedgerSubjectCode.AVAILABLE, 10L, CurrencyIsoCode.USD);
        assertBucket(afterPayout, LedgerSubjectCode.FROZEN, 0L, CurrencyIsoCode.USD);
        assertThat(fundsTransactionStatus(PAYOUT_BUSINESS_SN)).isEqualTo(FundsTransactionStatus.CLOSED.name());
        assertThat(fundsTransactionDetailStatuses(PAYOUT_BUSINESS_SN))
                .hasSize(3)
                .containsOnly(FundsTransactionDetailStatus.SUCCEEDED.name());
        assertThat(ledgerTransactionEvents(PAYOUT_BUSINESS_SN))
                .containsExactly(FundsTransactionEventType.WITHDRAW.name());
        assertThat(ledgerEntrySubjects(PAYOUT_BUSINESS_SN))
                .contains(RECEIVE_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
        assertThat(ledgerEntrySubjectCodes(PAYOUT_BUSINESS_SN))
                .contains(LedgerSubjectCode.FROZEN.name(), LedgerSubjectCode.CASH.name(),
                        LedgerSubjectCode.PREPAYMENT.name());
        assertThat(postingPlanCount(PAYOUT_BUSINESS_SN)).isEqualTo(2);
        assertThat(ledgerEntryCount(PAYOUT_BUSINESS_SN)).isEqualTo(4);
        assertThat(routeLegCount(PAYOUT_BUSINESS_SN)).isEqualTo(2);
        assertPayoutRouteSnapshot(PAYOUT_BUSINESS_SN);
    }

    /**
     * 场景：全球账户出款 rail 入口收到未知 rail 编码。
     * 输入：请求字段齐全，但 railCode 为 mystery_rail。
     * 输出：服务层入口给出可读拒绝原因，不进入预交易快照和提现内核。
     * 红线：非法 rail 不得生成资金交易、route、posting plan、账本交易或分录。
     */
    @Test
    void testPayOutByRailShouldRejectUnsupportedRailCodeWithoutFundsFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        PayOutByRailRequest request = payoutRequest()
                .setBusinessSn(UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN)
                .setRailCode("mystery_rail")
                .setReferenceFreezeSn("freeze_not_used_for_unknown_rail");

        assertThatThrownBy(() -> instrumentTransactionLifecycleApplicationService.payOutByRail(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("出款 rail 编码不支持")
                .hasMessageContaining("mystery_rail")
                .hasMessageContaining("支持的出款 rail");

        assertNoFundsOrLedgerFacts(UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：全球账户出款 rail 入口收到外部非终态。
     * 输入：外部状态为 PROCESSING，其他请求字段齐全。
     * 输出：服务层入口拒绝，不进入预交易快照和提现内核。
     * 红线：submitted、accepted 或 processing 不能关闭内部冻结资金。
     */
    @Test
    void testPayOutByRailShouldRejectNonFinalExternalStatusWithoutFundsFacts() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        PayOutByRailRequest request = payoutRequest()
                .setBusinessSn(PROCESSING_PAYOUT_BUSINESS_SN)
                .setExternalPayoutStatus("PROCESSING")
                .setReferenceFreezeSn("freeze_not_used_for_processing");

        assertThatThrownBy(() -> instrumentTransactionLifecycleApplicationService.payOutByRail(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("外部出款非终态成功状态不得关闭内部冻结资金");

        assertNoFundsOrLedgerFacts(PROCESSING_PAYOUT_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpInstrumentTransactionLifecycleTestData() {
        authorizationAdmissionApplicationService.reset();
        cleanupInstrumentTransactionLifecycleTestData();
    }

    @AfterEach
    void tearDownInstrumentTransactionLifecycleTestData() {
        cleanupInstrumentTransactionLifecycleTestData();
    }

    private void cleanupInstrumentTransactionLifecycleTestData() {
        jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn IN (
                    SELECT sn FROM t_ledger_transaction
                    WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)
                )
                """, BUSINESS_SCENE, RECEIVE_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN, UNSUPPORTED_CHANNEL_BUSINESS_SN, PAYOUT_BUSINESS_SN,
                UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN, PAYOUT_FREEZE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, PAYOUT_BUSINESS_SN, UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN,
                PAYOUT_FREEZE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_ledger_transaction WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, PAYOUT_BUSINESS_SN, UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN,
                PAYOUT_FREEZE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_funds_transaction_detail WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, PAYOUT_BUSINESS_SN, UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN,
                PAYOUT_FREEZE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_funds_frozen_order WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, PAYOUT_BUSINESS_SN, UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN,
                PAYOUT_FREEZE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_funds_transaction WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN, MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, PAYOUT_BUSINESS_SN, UNSUPPORTED_PAYOUT_RAIL_BUSINESS_SN,
                PAYOUT_FREEZE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID, RECEIVE_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?, ?)",
                RECEIVE_INSTRUMENT_SN, PAYMENT_ONLY_INSTRUMENT_SN, PAYOUT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?, ?)",
                RECEIVE_INSTRUMENT_SN, PAYMENT_ONLY_INSTRUMENT_SN, PAYOUT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?, ?)",
                RECEIVE_INSTRUMENT_SN, PAYMENT_ONLY_INSTRUMENT_SN, PAYOUT_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_ledger WHERE subject_id IN (?, ?, ?)",
                RECEIVE_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_funding_account WHERE sn IN (?, ?, ?)",
                RECEIVE_ACCOUNT_SN, CASH_MAPPING_ACCOUNT_SN, PREPAYMENT_ACCOUNT_SN);
    }

    private void createReceiveScenario() {
        createReceiveAccount();
        createPlatformFundingAccount(CASH_MAPPING_ACCOUNT_SN, PlatformFundingAccountRole.CASH_MAPPING);
        createPlatformFundingAccount(PREPAYMENT_ACCOUNT_SN, PlatformFundingAccountRole.PREPAYMENT);
        createTestLedger(cashMappingAccountId(), LedgerSubjectCode.CASH, 10_000L);
        createTestLedger(prepaymentAccountId(), LedgerSubjectCode.PREPAYMENT, 0L);
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(RECEIVE_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.INBOUND));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest(RECEIVE_INSTRUMENT_SN));
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest());
    }

    private void createPayoutInstrumentScenario() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYOUT_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND, PAYOUT_INSTRUMENT_NO));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest(PAYOUT_INSTRUMENT_SN,
                PaymentInstrumentBindingRole.PAYMENT_SUBJECT));
        fundingRelationService.createSpendSubjectFundingRelation(createFundingRelationRequest(
                SpendSubjectFundingRelationType.FUNDING_SOURCE));
    }

    private void createReceiveAccount() {
        fundingAccountService.createFundingAccount(new CreateFundingAccountRequest()
                .setSn(RECEIVE_ACCOUNT_SN)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setAccountType(FundingAccountType.GLOBAL_ACCOUNT.name())
                .setPlatform(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_BASIC)
                .setStatus(FundsAccountStatus.ACTIVE));
    }

    private void createPaymentOnlyInstrumentScenario() {
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(PAYMENT_ONLY_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.OUTBOUND));
    }

    private void createPlatformFundingAccount(String accountSn, PlatformFundingAccountRole role) {
        FundingAccount account = new FundingAccount();
        account.setTenantId(TENANT_ID);
        account.setSn(accountSn);
        account.setOwnerId("platform");
        account.setOwnerType(FundsAccountOwnerType.PLATFORM);
        account.setAccountType(role.name());
        account.setPlatform(Boolean.TRUE);
        account.setAccountRoleCode(role);
        account.setCurrency(CurrencyIsoCode.USD);
        account.setLedgerProfileCode(role.getLedgerProfileCode());
        account.setLedgerProfileVersion(1);
        account.setStatus(FundsAccountStatus.ACTIVE);
        account.setDescription("instrument lifecycle platform funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void createTestLedger(FundsAccountId accountId,
                                  LedgerSubjectCode subjectCode,
                                  long initialBalance) {
        Long ledgerId = ledgerService.createLedger(new CreateLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(accountId.type())
                .setLedgerProfileCode("TEST")
                .setLedgerProfileVersion(1)
                .setLedgerSubjectCode(subjectCode)
                .setLedgerSubjectCategory(LedgerSubjectCategory.LIABILITY)
                .setNormalBalanceSide(EntrySide.CREDIT)
                .setAllowNegative(Boolean.FALSE)
                .setCurrency(CurrencyIsoCode.USD)
                .setSettlementPolicy("RT")
                .setCutOffTime(LocalTime.MIDNIGHT)
                .setPeriodType(AccountBalancePeriodType.LIFETIME)
                .setPeriodId(AccountBalancePeriodType.LIFETIME.name()));
        if (initialBalance != 0L) {
            ledgerService.updateLedgerBalance(new UpdateLedgerBalanceRequest()
                    .setId(ledgerId)
                    .setCreditAmountDelta(initialBalance > 0L ? initialBalance : null)
                    .setDebitAmountDelta(initialBalance < 0L ? -initialBalance : null));
        }
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentFlowDirection direction) {
        return createPaymentInstrumentRequest(instrumentSn, direction, RECEIVE_INSTRUMENT_NO);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentFlowDirection direction,
                                                                          String instrumentNo) {
        return new CreatePaymentInstrumentRequest()
                .setSn(instrumentSn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType("VA")
                .setFlowDirection(direction)
                .setInstrumentNo(instrumentNo)
                .setChannelCode(CHANNEL_CODE)
                .setExternalInstrumentId(externalInstrumentId(instrumentSn))
                .setCurrency(CurrencyIsoCode.USD)
                .setStatus(FundsAccountStatus.ACTIVE);
    }

    private String externalInstrumentId(String instrumentSn) {
        if (RECEIVE_INSTRUMENT_SN.equals(instrumentSn)) {
            return "va_lifecycle_2468";
        }
        return instrumentSn + "_external";
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest(String instrumentSn) {
        return createBindingRequest(instrumentSn, PaymentInstrumentBindingRole.RECEIVE_SUBJECT);
    }

    private CreatePaymentInstrumentBindingRequest createBindingRequest(String instrumentSn,
                                                                       PaymentInstrumentBindingRole bindingRole) {
        return new CreatePaymentInstrumentBindingRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(instrumentSn)
                .setBindingRole(bindingRole)
                .setSubjectId(RECEIVE_ACCOUNT_SN)
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setPriority(10)
                .setDefaultBinding(Boolean.TRUE);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest() {
        return createFundingRelationRequest(SpendSubjectFundingRelationType.SETTLEMENT_TARGET);
    }

    private CreateSpendSubjectFundingRelationRequest createFundingRelationRequest(
            SpendSubjectFundingRelationType relationType) {
        return new CreateSpendSubjectFundingRelationRequest()
                .setTenantId(TENANT_ID)
                .setSpendSubjectId(RECEIVE_ACCOUNT_SN)
                .setSpendSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setTargetSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setTargetSubjectId(RECEIVE_ACCOUNT_SN)
                .setCurrency(CurrencyIsoCode.USD)
                .setRelationType(relationType);
    }

    private ReceiveByInstrumentRequest receiveRequest(String businessSn, String instrumentSn) {
        return new ReceiveByInstrumentRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(instrumentSn)
                .setAmount(80L)
                .setCurrency(CurrencyIsoCode.USD)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_lifecycle_receive",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannelCode(CHANNEL_CODE)
                .setChannelTransactionSn(businessSn + "_CHANNEL")
                .setChannelId(CHANNEL_ID)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setExpectedBindingVersion(1)
                .setDescription("instrument receive flow");
    }

    private PayOutByRailRequest payoutRequest() {
        return new PayOutByRailRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn(PAYOUT_INSTRUMENT_SN)
                .setPayoutSourceAccountId(receiveAccountId())
                .setPayeeAccountId(payoutPayeeAccountId())
                .setAmount(70L)
                .setCurrency(CurrencyIsoCode.USD)
                .setRailCode("LOCAL")
                .setReceiverReference("receiver_endpoint_001")
                .setExternalPayoutSn("processor_payout_001")
                .setExternalPayoutStatus("PAID")
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(PAYOUT_BUSINESS_SN)
                .setExpectedBindingVersion(1)
                .setDescription("instrument payout rail contract");
    }

    private String freezeForPayout(FundsAccountId accountId) {
        return balanceControlService.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(accountId)
                .setAmount(Money.immutable(70L, CurrencyIsoCode.USD))
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(PAYOUT_FREEZE_BUSINESS_SN)
                .setDescription("instrument payout freeze"), WindOperatorFactory.system());
    }

    private AuthorizeByPaymentInstrumentRequest authorizeRequest() {
        return new AuthorizeByPaymentInstrumentRequest()
                .setTenantId(TENANT_ID)
                .setInstrumentSn("pi_lifecycle_authorize_card")
                .setAmount(60L)
                .setCurrency(CurrencyIsoCode.USD)
                .setBusinessScene("INSTRUMENT_AUTHORIZE")
                .setBusinessSn(AUTHORIZE_BUSINESS_SN)
                .setApproved(Boolean.TRUE)
                .setExpectedBindingVersion(1)
                .setDescription("instrument lifecycle authorization flow");
    }

    private FundsAccountId receiveAccountId() {
        return FundsAccountId.immutable(RECEIVE_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private FundsAccountId cashMappingAccountId() {
        return FundsAccountId.immutable(CASH_MAPPING_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private FundsAccountId prepaymentAccountId() {
        return FundsAccountId.immutable(PREPAYMENT_ACCOUNT_SN, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private FundsAccountId payoutPayeeAccountId() {
        return FundsAccountId.immutable("external_bank_lifecycle_payout", DefaultFundsAccountType.EXTERNAL_BANK);
    }

    private FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
        return balanceQueryService.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(accountId))
                .setCurrency(CurrencyIsoCode.USD));
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

    private Integer ledgerEntryCount(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_ledger_entry
                WHERE business_scene = ? AND business_sn = ?
                """, Integer.class, BUSINESS_SCENE, businessSn);
    }

    private int routeLegCount(String businessSn) {
        return JSON.parseObject(routeSnapshotJson(businessSn))
                .getJSONArray("legs")
                .size();
    }

    private String routeSnapshotJson(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT route_snapshot FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private void assertReceiveRouteSnapshot(String businessSn) {
        JSONObject routeSnapshot = JSON.parseObject(routeSnapshotJson(businessSn));
        JSONObject paymentInstrumentRef = routeSnapshot.getJSONObject("paymentInstrumentRef");
        assertThat(paymentInstrumentRef).isNotNull().isNotEmpty();
        assertThat(paymentInstrumentRef.getString("instrumentId")).isEqualTo(RECEIVE_INSTRUMENT_SN);
        assertThat(paymentInstrumentRef.getString("instrumentType")).isEqualTo("VA");
        assertThat(paymentInstrumentRef.getString("instrumentNo")).isEqualTo(RECEIVE_INSTRUMENT_NO);
        assertThat(paymentInstrumentRef.getString("ownerId")).isEqualTo(OWNER_ID);
        assertThat(paymentInstrumentRef.getString("ownerType")).isEqualTo(FundsAccountOwnerType.USER.name());
        assertThat(paymentInstrumentRef.getString("currency")).isEqualTo(CurrencyIsoCode.USD.name());
        assertThat(paymentInstrumentRef.getString("status")).isEqualTo(FundsAccountStatus.ACTIVE.name());
        assertThat(paymentInstrumentRef.toString()).doesNotContain("va_lifecycle_2468");
        JSONObject bindingSnapshot = paymentInstrumentRef.getJSONObject("bindingSnapshot");
        assertThat(bindingSnapshot).isNotNull().isNotEmpty();
        assertThat(bindingSnapshot.getString("bindingSn")).startsWith("PIB");
        assertThat(bindingSnapshot.getInteger("bindingVersion")).isEqualTo(1);
        assertThat(bindingSnapshot.getString("bindingRole"))
                .isEqualTo(PaymentInstrumentBindingRole.RECEIVE_SUBJECT.name());
        assertThat(bindingSnapshot.getString("subjectType")).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(bindingSnapshot.getString("subjectId")).isEqualTo(RECEIVE_ACCOUNT_SN);
        assertThat(bindingSnapshot.getString("admissionAction")).isEqualTo(PaymentInstrumentAction.RECEIVE.name());
        JSONObject externalAccountRef = routeSnapshot.getJSONObject("externalAccountRef");
        assertThat(externalAccountRef).isNotNull().isNotEmpty();
        assertThat(externalAccountRef.getString("externalAccountId"))
                .isEqualTo("external_bank_lifecycle_receive");
        assertThat(externalAccountRef.getString("externalAccountType"))
                .isEqualTo(DefaultFundsAccountType.EXTERNAL_BANK.name());
        assertThat(externalAccountRef.getString("providerCode")).isEqualTo(CHANNEL_ID);
        assertThat(externalAccountRef.getString("channelCode")).isEqualTo(FundsTransactionChannel.WIRE_TRANSFER.name());
    }

    private void assertReceiveFactsKeepDirectContextMinimal(String businessSn) {
        assertThat(queryContextVariables("t_funds_transaction", businessSn))
                .singleElement()
                .satisfies(context -> assertThat(JSON.parseObject(context)).isEmpty());
        assertThat(queryContextVariables("t_funds_transaction_detail", businessSn))
                .isNotEmpty()
                .allSatisfy(context -> assertThat(JSON.parseObject(context).keySet())
                        .doesNotContain("instrumentSn",
                                "instrumentAction",
                                "instrumentBindingRole",
                                "instrumentBindingSn",
                                "instrumentBindingVersion",
                                "fundingRelationSn",
                                "fundingRelationType",
                                "targetAccountId",
                                "targetAccountType"));
        assertThat(queryContextVariables("t_ledger_transaction", businessSn))
                .singleElement()
                .satisfies(context -> assertThat(JSON.parseObject(context)).isEmpty());
        assertThat(queryContextVariables("t_ledger_entry", businessSn))
                .isNotEmpty()
                .allSatisfy(context -> assertThat(context).doesNotContain("va_lifecycle_2468"));
    }

    private void assertPayoutRouteSnapshot(String businessSn) {
        JSONObject routeSnapshot = JSON.parseObject(routeSnapshotJson(businessSn));
        JSONObject paymentInstrumentRef = routeSnapshot.getJSONObject("paymentInstrumentRef");
        assertThat(paymentInstrumentRef).isNotNull().isNotEmpty();
        assertThat(paymentInstrumentRef.getString("instrumentId")).isEqualTo(PAYOUT_INSTRUMENT_SN);
        assertThat(paymentInstrumentRef.getString("instrumentType")).isEqualTo("VA");
        assertThat(paymentInstrumentRef.getString("instrumentNo")).isEqualTo(PAYOUT_INSTRUMENT_NO);
        assertThat(paymentInstrumentRef.getString("ownerId")).isEqualTo(OWNER_ID);
        assertThat(paymentInstrumentRef.getString("ownerType")).isEqualTo(FundsAccountOwnerType.USER.name());
        assertThat(paymentInstrumentRef.getString("currency")).isEqualTo(CurrencyIsoCode.USD.name());
        assertThat(paymentInstrumentRef.getString("status")).isEqualTo(FundsAccountStatus.ACTIVE.name());
        JSONObject bindingSnapshot = paymentInstrumentRef.getJSONObject("bindingSnapshot");
        assertThat(bindingSnapshot).isNotNull().isNotEmpty();
        assertThat(bindingSnapshot.getString("bindingSn")).startsWith("PIB");
        assertThat(bindingSnapshot.getInteger("bindingVersion")).isEqualTo(1);
        assertThat(bindingSnapshot.getString("bindingRole"))
                .isEqualTo(PaymentInstrumentBindingRole.PAYMENT_SUBJECT.name());
        assertThat(bindingSnapshot.getString("subjectType")).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(bindingSnapshot.getString("subjectId")).isEqualTo(RECEIVE_ACCOUNT_SN);
        assertThat(bindingSnapshot.getString("admissionAction")).isEqualTo(PaymentInstrumentAction.WITHDRAW.name());
        JSONObject externalAccountRef = routeSnapshot.getJSONObject("externalAccountRef");
        assertThat(externalAccountRef).isNotNull().isNotEmpty();
        assertThat(externalAccountRef.getString("externalAccountId"))
                .isEqualTo("external_bank_lifecycle_payout");
        assertThat(externalAccountRef.getString("externalAccountType"))
                .isEqualTo(DefaultFundsAccountType.EXTERNAL_BANK.name());
    }

    private List<String> queryContextVariables(String tableName, String businessSn) {
        return jdbcTemplate.queryForList(
                "SELECT context_variables FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                String.class, BUSINESS_SCENE, businessSn);
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
            InstrumentTransactionLifecycleApplicationServiceImpl.class,
            DefaultFundsAccountQueryServiceImpl.class,
            PlatformFundingAccountServiceImpl.class
    })
    static class Config {

        @Bean
        RecordingAuthorizationAdmissionApplicationService recordingAuthorizationAdmissionApplicationService() {
            return new RecordingAuthorizationAdmissionApplicationService();
        }
    }

    static class RecordingAuthorizationAdmissionApplicationService implements AuthorizationAdmissionApplicationService {

        private int invocationCount;

        private AuthorizeByPaymentInstrumentRequest lastRequest;

        @Override
        public String authorizeByInstrument(AuthorizeByPaymentInstrumentRequest request, WindOperator operator) {
            invocationCount++;
            lastRequest = request;
            return DELEGATED_AUTHORIZATION_SN;
        }

        int getInvocationCount() {
            return invocationCount;
        }

        AuthorizeByPaymentInstrumentRequest getLastRequest() {
            return lastRequest;
        }

        void reset() {
            invocationCount = 0;
            lastRequest = null;
        }
    }
}
