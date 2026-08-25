package com.wind.funds.wallet.application.instrument;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.ledger.DefaultLedgerTransactionPostingServiceImpl;
import com.wind.funds.ledger.enums.LedgerPostingAccessType;
import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.ledger.impl.LedgerBalanceProjectionServiceImpl;
import com.wind.funds.ledger.impl.LedgerServiceImpl;
import com.wind.funds.ledger.impl.LedgerTransactionServiceImpl;
import com.wind.funds.ledger.request.InitializeSubjectLedgerRequest;
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
import com.wind.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.funds.transaction.application.instrument.ReceiveByInstrumentRequest;
import com.wind.funds.transaction.application.instrument.impl.PaymentInstrumentAuthorizationProcessor;
import com.wind.funds.transaction.application.instrument.impl.PaymentInstrumentTransactionApplicationServiceImpl;
import com.wind.funds.transaction.application.spend.impl.SpendControlTransactionConsumptionApplicationServiceImpl;
import com.wind.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.wind.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.wind.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.enums.FundsTransactionState;
import com.wind.funds.ledger.posting.DefaultLedgerPostingAssembler;
import com.wind.funds.transaction.services.impl.DefaultFundsFrozenOrderLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsInstructionLifecycleSaver;
import com.wind.funds.transaction.services.impl.DefaultFundsTransactionQueryService;
import com.wind.funds.transaction.services.impl.DelegatingFundsInstructionLifecycleRecorder;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.application.account.impl.FundsAccountCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.funding.impl.FundingResponsibilityResolutionApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentCapabilityApplicationServiceImpl;
import com.wind.funds.wallet.application.instrument.impl.PaymentInstrumentPreTransactionSnapshotApplicationServiceImpl;
import com.wind.funds.wallet.application.spend.impl.SpendControlAdmissionApplicationServiceImpl;
import com.wind.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.funds.wallet.enums.FundingAccountType;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingState;
import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.funds.wallet.dal.entities.FundingAccount;
import com.wind.funds.wallet.dal.mapper.FundingAccountMapper;
import com.wind.funds.wallet.model.dto.FundsSubjectBalanceDTO;
import com.wind.funds.wallet.model.request.CreateFundingAccountRequest;
import com.wind.funds.wallet.model.request.ChangePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentBindingRequest;
import com.wind.funds.wallet.model.request.CreatePaymentInstrumentRequest;
import com.wind.funds.wallet.model.request.CreateSpendSubjectFundingRelationRequest;
import com.wind.funds.wallet.model.query.FundsSubjectBalanceQuery;
import com.wind.funds.wallet.service.FundingAccountService;
import com.wind.funds.wallet.service.FundsSubjectBalanceQueryService;
import com.wind.funds.wallet.service.PaymentInstrumentService;
import com.wind.funds.wallet.service.SpendSubjectFundingRelationService;
import com.wind.funds.wallet.services.impl.AccountHierarchyRelationServiceImpl;
import com.wind.funds.wallet.services.impl.CreditAccountServiceImpl;
import com.wind.funds.wallet.services.impl.DefaultFundsAccountQueryServiceImpl;
import com.wind.funds.ledger.profile.LedgerProfileCatalog;
import com.wind.funds.wallet.services.impl.FundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingHistoryServiceImpl;
import com.wind.funds.wallet.services.impl.PaymentInstrumentBindingServiceImpl;
import com.wind.funds.wallet.services.impl.PlatformFundingAccountServiceImpl;
import com.wind.funds.wallet.services.impl.SpendControlMovementServiceImpl;
import com.wind.funds.wallet.services.impl.SpendSubjectFundingRelationServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleBindingServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDecisionRecordServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleDefinitionServiceImpl;
import com.wind.funds.wallet.services.impl.SpendRuleVersionServiceImpl;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.jackson.WindJson;
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

import tools.jackson.databind.JsonNode;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertBucket;
import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static com.wind.funds.support.LedgerProjectionTestFixture.balanceEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 支付工具交易应用服务收款流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        PaymentInstrumentTransactionApplicationServiceTests.Config.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentInstrumentTransactionApplicationServiceTests extends AbstractFundsServiceTest {

    private static final String RECEIVE_ACCOUNT_SN = "inst_lifecycle_recv_acc";

    private static final String CASH_MAPPING_ACCOUNT_SN = "inst_lifecycle_cash_map";

    private static final String PREPAYMENT_ACCOUNT_SN = "inst_lifecycle_prepay";

    private static final String RECEIVE_INSTRUMENT_SN = "pi_lifecycle_va_receive";

    private static final String RECEIVE_INSTRUMENT_NO = "VA-****-2468";

    private static final String PAYMENT_ONLY_INSTRUMENT_SN = "pi_lifecycle_payment_only";

    private static final String WALLET_INSTRUMENT_SN = "pi_lifecycle_wallet_receive";

    private static final String OWNER_ID = "owner_instrument_lifecycle";

    private static final String PROVIDER_CODE = "bank_rail_provider";

    private static final String EXTERNAL_RAIL_CODE = "ACH";

    private static final String BUSINESS_SCENE = "INSTRUMENT_RECEIVE";

    private static final String RECEIVE_BUSINESS_SN = "INSTRUMENT_RECEIVE_001";

    private static final String RECEIVE_REPLAY_BUSINESS_SN = "INSTRUMENT_RECEIVE_REPLAY_001";

    private static final String EXTERNAL_SOURCE_CODE = "BANK_RAIL_PROVIDER:ACCOUNT_TOKEN_001";

    private static final String EXTERNAL_FUNDS_FACT_SN = "BANK_LEDGER_ENTRY_RECEIVE_001";

    private static final String DIRECTION_FAIL_BUSINESS_SN = "INSTRUMENT_RECEIVE_DIRECTION_FAIL";

    private static final String MISSING_BINDING_VERSION_BUSINESS_SN = "INSTRUMENT_RECEIVE_MISSING_BINDING_VERSION";

    private static final String UNSUPPORTED_CHANNEL_BUSINESS_SN = "INSTRUMENT_RECEIVE_UNSUPPORTED_CHANNEL";

    private static final String MISMATCH_RAIL_BUSINESS_SN = "INSTRUMENT_RECEIVE_MISMATCH_RAIL";

    private static final String WALLET_RECEIVE_BUSINESS_SN = "INSTRUMENT_WALLET_RECEIVE_001";

    @Autowired
    private FundingAccountService fundingAccountService;

    @Autowired
    private PaymentInstrumentService paymentInstrumentService;

    @Autowired
    private SpendSubjectFundingRelationService fundingRelationService;

    @Autowired
    private FundsSubjectBalanceQueryService balanceQueryService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerBalanceProjectionServiceImpl ledgerBalanceProjectionService;

    @Autowired
    private FundingAccountMapper fundingAccountMapper;

    @Autowired
    private PaymentInstrumentTransactionApplicationServiceImpl paymentInstrumentTransactionApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 场景：VA/ACH 等收款工具入口完成预交易准入后，委派账户主体型充值交易内核。
     * 输入：收款工具绑定资金账户，资金责任解析到同一资金账户，外部银行账户通过 ACH 入金 80。
     * 输出：返回充值交易号，资金账户 AVAILABLE 增加 80，并产生标准 TOPUP 交易、route 和账本事实。
     * 红线：wallet 应用入口负责把业务 rail 解析为交易层渠道，不直接写账；交易内核 canonical 入参仍是已解析的资金账户主体。
     */
    @Test
    void testReceiveByInstrumentShouldResolveTargetAccountAndDelegateTopupKernel() {
        createReceiveScenario();
        FundsAccountId receiveAccount = receiveAccountId();
        FundsSubjectBalanceDTO beforeReceive = balance(receiveAccount);
        assertBucket(beforeReceive, LedgerSubjectCode.AVAILABLE, 0L, CurrencyIsoCode.USD);

        String transactionSn = paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(transactionSn).isNotBlank();
        FundsSubjectBalanceDTO afterReceive = balance(receiveAccount);
        assertBucket(afterReceive, LedgerSubjectCode.AVAILABLE, 80L, CurrencyIsoCode.USD);
        assertThat(fundsTransactionState(RECEIVE_BUSINESS_SN)).isEqualTo(FundsTransactionState.CLOSED.name());
        assertThat(fundsTransactionDetailStatuses(RECEIVE_BUSINESS_SN))
                .hasSize(3)
                .containsOnly(FundsTransactionDetailState.SUCCEEDED.name());
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
     * 场景：同一 VA 入金资金事实经不同通知重复进入，调用方重新生成业务流水。
     * 输入：相同 externalSourceCode 和 externalFundsFactSn，不同 channelTransactionSn 和 businessSn。
     * 输出：返回第一次资金交易号，目标账户 AVAILABLE 只增加一次。
     * 红线：支付工具收款入口不得因业务流水变化重复入账。
     */
    @Test
    void testReceiveByInstrumentSameExternalFundsFactShouldReuseTransaction() {
        createReceiveScenario();

        String firstTransactionSn = paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system());
        String replayTransactionSn = paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_REPLAY_BUSINESS_SN, RECEIVE_INSTRUMENT_SN)
                        .setChannelTransactionSn("INSTRUMENT_RECEIVE_REPLAY_CHANNEL"),
                WindOperatorFactory.system());

        assertThat(replayTransactionSn).isEqualTo(firstTransactionSn);
        assertBucket(balance(receiveAccountId()), LedgerSubjectCode.AVAILABLE, 80L, CurrencyIsoCode.USD);
        assertThat(externalFundsFactTransactionCount()).isOne();
    }

    /**
     * 场景：已成功入金后支付工具绑定被暂停，随后收到同一外部资金事实的重复通知。
     * 输入：原事实已 CLOSED，当前绑定从 ACTIVE 变为 SUSPENDED，重放仍携带原绑定版本。
     * 输出：相同金额返回原交易号；篡改金额时按原事实冲突拒绝，均不重复生成资金或账务事实。
     * 红线：已成立资金事实的幂等重放不得被当前绑定状态破坏，也不得绕过不可变载荷校验。
     */
    @Test
    void testReceiveByInstrumentEstablishedReplayShouldIgnoreCurrentBindingState() {
        createReceiveScenario();
        String firstTransactionSn = paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system());
        suspendReceiveBinding();
        FundsSubjectBalanceDTO balanceAfterFirstReceive = balance(receiveAccountId());
        LedgerFactSnapshot factsAfterFirstReceive = ledgerFactSnapshot(jdbcTemplate);

        String replayedTransactionSn = paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_REPLAY_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system());

        assertThat(replayedTransactionSn).isEqualTo(firstTransactionSn);
        assertThat(balance(receiveAccountId())).isEqualTo(balanceAfterFirstReceive);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstReceive);
        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_REPLAY_BUSINESS_SN, RECEIVE_INSTRUMENT_SN).setAmount(81L),
                WindOperatorFactory.system()))
                .hasMessageContaining("已成立支付工具收款请求参数不一致");
        assertThat(balance(receiveAccountId())).isEqualTo(balanceAfterFirstReceive);
        assertLedgerFactsUnchanged(jdbcTemplate, factsAfterFirstReceive);
        assertThat(externalFundsFactTransactionCount()).isOne();
    }

    /**
     * 场景：外部入金首次到达前，支付工具绑定已经暂停。
     * 输入：请求仍携带旧绑定版本，但不存在已成功消费的外部资金事实。
     * 输出：当前准入失败，不创建资金交易、route、posting plan、账本交易或分录。
     * 红线：延迟首次入金不得按当前或历史审计快照猜测资金责任，原目标账户应由上层确认后走账户主体入口。
     */
    @Test
    void testReceiveByInstrumentFirstArrivalAfterBindingSuspendedShouldRejectWithoutFundsFacts() {
        createReceiveScenario();
        suspendReceiveBinding();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.receiveByInstrument(
                receiveRequest(RECEIVE_BUSINESS_SN, RECEIVE_INSTRUMENT_SN), WindOperatorFactory.system()))
                .hasMessageContaining("支付工具绑定");

        assertNoFundsOrLedgerFacts(RECEIVE_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：外部钱包支付工具完成已确认入金。
     * 输入：PAYPAL 接入工具、DIGITAL_WALLET rail 和外部钱包来源账户。
     * 输出：交易按 DIGITAL_WALLET 大类入账，route snapshot 保留 PAYPAL provider 和钱包 rail。
     * 红线：provider、外部 rail 和交易渠道不得混为同一个字段。
     */
    @Test
    void testReceiveByInstrumentShouldPreserveWalletProviderAndRail() {
        createReceiveScenario();
        paymentInstrumentService.createPaymentInstrument(createPaymentInstrumentRequest(WALLET_INSTRUMENT_SN,
                PaymentInstrumentFlowDirection.INBOUND, "WALLET-****-1357", "EXTERNAL_WALLET", "PAYPAL"));
        paymentInstrumentService.createPaymentInstrumentBinding(createBindingRequest(WALLET_INSTRUMENT_SN));
        ReceiveByInstrumentRequest request = receiveRequest(WALLET_RECEIVE_BUSINESS_SN, WALLET_INSTRUMENT_SN)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_wallet_lifecycle_receive",
                        DefaultFundsAccountType.EXTERNAL_WALLET))
                .setExternalRailCode(FundsTransactionChannel.DIGITAL_WALLET.name());

        paymentInstrumentTransactionApplicationService.receiveByInstrument(request, WindOperatorFactory.system());

        JsonNode routeSnapshot = jsonObject(routeSnapshotJson(WALLET_RECEIVE_BUSINESS_SN));
        JsonNode externalAccountRef = routeSnapshot.path("externalAccountRef");
        assertThat(externalAccountRef.path("providerCode").asString()).isEqualTo("PAYPAL");
        assertThat(externalAccountRef.path("channelCode").asString())
                .isEqualTo(FundsTransactionChannel.DIGITAL_WALLET.name());
    }

    /**
     * 场景：收款入口收到未知 rail / channel 编码。
     * 输入：支付工具、绑定版本和资金责任均有效，但 externalRailCode 为 mystery_rail。
     * 输出：服务层入口给出可读的渠道拒绝原因，不泄露 Java enum valueOf 实现细节。
     * 红线：非法 rail 不得进入交易内核，不得生成资金交易、route、posting plan、账本交易或分录。
     */
    @Test
    void testReceiveByInstrumentShouldRejectUnsupportedRailCodeWithoutFundsFacts() {
        createReceiveScenario();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        ReceiveByInstrumentRequest request = receiveRequest(UNSUPPORTED_CHANNEL_BUSINESS_SN,
                RECEIVE_INSTRUMENT_SN)
                .setExternalRailCode("mystery_rail");

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.receiveByInstrument(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("收款外部 rail 编码不支持")
                .hasMessageContaining("mystery_rail")
                .hasMessageContaining("支持的外部 rail");

        assertNoFundsOrLedgerFacts(UNSUPPORTED_CHANNEL_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：本次外部 rail 与支付工具类型不一致。
     * 输入：VA 支付工具声明 DIGITAL_WALLET rail。
     * 输出：准入阶段拒绝且不生成资金或账本事实。
     * 红线：调用方不能把 VA 收款伪装为外部钱包入金。
     */
    @Test
    void testReceiveByInstrumentShouldRejectInstrumentRailMismatchWithoutFundsFacts() {
        createReceiveScenario();
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        ReceiveByInstrumentRequest request = receiveRequest(MISMATCH_RAIL_BUSINESS_SN, RECEIVE_INSTRUMENT_SN)
                .setExternalRailCode(FundsTransactionChannel.DIGITAL_WALLET.name());

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.receiveByInstrument(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("支付工具类型与外部 rail 不匹配");

        assertNoFundsOrLedgerFacts(MISMATCH_RAIL_BUSINESS_SN);
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

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.receiveByInstrument(
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

        assertThatThrownBy(() -> paymentInstrumentTransactionApplicationService.receiveByInstrument(request,
                WindOperatorFactory.system()))
                .hasMessageContaining("支付工具收款绑定版本不能为空");

        assertNoFundsOrLedgerFacts(MISSING_BINDING_VERSION_BUSINESS_SN);
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @BeforeEach
    void setUpPaymentInstrumentTransactionTestData() {
        cleanupPaymentInstrumentTransactionTestData();
    }

    @AfterEach
    void tearDownPaymentInstrumentTransactionTestData() {
        cleanupPaymentInstrumentTransactionTestData();
    }

    private void cleanupPaymentInstrumentTransactionTestData() {
        jdbcTemplate.update("""
                DELETE FROM t_ledger_posting_plan
                WHERE ledger_transaction_sn IN (
                    SELECT sn FROM t_ledger_transaction
                    WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)
                )
                """, BUSINESS_SCENE, RECEIVE_BUSINESS_SN, RECEIVE_REPLAY_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN, UNSUPPORTED_CHANNEL_BUSINESS_SN,
                MISMATCH_RAIL_BUSINESS_SN, WALLET_RECEIVE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_ledger_entry WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, RECEIVE_REPLAY_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, MISMATCH_RAIL_BUSINESS_SN, WALLET_RECEIVE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_ledger_transaction WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, RECEIVE_REPLAY_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, MISMATCH_RAIL_BUSINESS_SN, WALLET_RECEIVE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_funds_transaction_detail WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, RECEIVE_REPLAY_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, MISMATCH_RAIL_BUSINESS_SN, WALLET_RECEIVE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_funds_frozen_order WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, RECEIVE_REPLAY_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, MISMATCH_RAIL_BUSINESS_SN, WALLET_RECEIVE_BUSINESS_SN);
        jdbcTemplate.update(
                "DELETE FROM t_funds_transaction WHERE business_scene = ? AND business_sn IN (?, ?, ?, ?, ?, ?, ?)",
                BUSINESS_SCENE, RECEIVE_BUSINESS_SN, RECEIVE_REPLAY_BUSINESS_SN, DIRECTION_FAIL_BUSINESS_SN,
                MISSING_BINDING_VERSION_BUSINESS_SN,
                UNSUPPORTED_CHANNEL_BUSINESS_SN, MISMATCH_RAIL_BUSINESS_SN, WALLET_RECEIVE_BUSINESS_SN);
        jdbcTemplate.update("DELETE FROM t_spend_subject_funding_rel WHERE tenant_id = ? AND spend_subject_id = ?",
                TENANT_ID, RECEIVE_ACCOUNT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding_history WHERE instrument_sn IN (?, ?, ?)",
                RECEIVE_INSTRUMENT_SN, PAYMENT_ONLY_INSTRUMENT_SN, WALLET_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument_binding WHERE instrument_sn IN (?, ?, ?)",
                RECEIVE_INSTRUMENT_SN, PAYMENT_ONLY_INSTRUMENT_SN, WALLET_INSTRUMENT_SN);
        jdbcTemplate.update("DELETE FROM t_payment_instrument WHERE sn IN (?, ?, ?)",
                RECEIVE_INSTRUMENT_SN, PAYMENT_ONLY_INSTRUMENT_SN, WALLET_INSTRUMENT_SN);
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
                .setState(FundsAccountState.ACTIVE));
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
        account.setState(FundsAccountState.ACTIVE);
        account.setDescription("instrument lifecycle platform funding account");
        account.setVersion(0);
        fundingAccountMapper.insertSelective(account);
    }

    private void createTestLedger(FundsAccountId accountId,
                                  LedgerSubjectCode subjectCode,
                                  long initialBalance) {
        ledgerService.initializeRequiredLedgers(new InitializeSubjectLedgerRequest()
                .setTenantId(TENANT_ID)
                .setSubjectId(accountId.id())
                .setSubjectType(FundsSubjectType.FUNDING_ACCOUNT)
                .setCurrency(CurrencyIsoCode.USD)
                .setLedgerProfileCode(LedgerProfileCode.FUNDING_PLATFORM)
                .setLedgerProfileVersion(1));
        Long ledgerId = jdbcTemplate.queryForObject("""
                SELECT id FROM t_ledger
                WHERE tenant_id = ? AND subject_id = ? AND ledger_subject_code = ?
                """, Long.class, TENANT_ID, accountId.id(), subjectCode.name());
        if (initialBalance != 0L) {
            var ledger = ledgerService.getLedgerById(ledgerId);
            ledgerBalanceProjectionService.project(List.of(balanceEntry(
                    ledger,
                    initialBalance > 0L ? ledger.getNormalBalanceSide() : ledger.getNormalBalanceSide().reverse(),
                    Math.abs(initialBalance))), LedgerPostingAccessType.NORMAL);
        }
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentFlowDirection direction) {
        return createPaymentInstrumentRequest(instrumentSn, direction, RECEIVE_INSTRUMENT_NO);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentFlowDirection direction,
                                                                          String instrumentNo) {
        return createPaymentInstrumentRequest(instrumentSn, direction, instrumentNo, "VA", PROVIDER_CODE);
    }

    private CreatePaymentInstrumentRequest createPaymentInstrumentRequest(String instrumentSn,
                                                                          PaymentInstrumentFlowDirection direction,
                                                                          String instrumentNo,
                                                                          String instrumentType,
                                                                          String providerCode) {
        return new CreatePaymentInstrumentRequest()
                .setSn(instrumentSn)
                .setTenantId(TENANT_ID)
                .setOwnerId(OWNER_ID)
                .setOwnerType(FundsAccountOwnerType.USER)
                .setInstrumentType(instrumentType)
                .setFlowDirection(direction)
                .setInstrumentNo(instrumentNo)
                .setChannelCode(providerCode)
                .setExternalInstrumentId(externalInstrumentId(instrumentSn))
                .setCurrency(CurrencyIsoCode.USD)
                .setState(FundsAccountState.ACTIVE);
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
                .setExternalRailCode(EXTERNAL_RAIL_CODE)
                .setChannelTransactionSn(businessSn + "_CHANNEL")
                .setExternalSourceCode(EXTERNAL_SOURCE_CODE)
                .setExternalFundsFactSn(EXTERNAL_FUNDS_FACT_SN)
                .setBusinessScene(BUSINESS_SCENE)
                .setBusinessSn(businessSn)
                .setExpectedBindingVersion(1)
                .setDescription("instrument receive flow");
    }

    private void suspendReceiveBinding() {
        String bindingSn = jdbcTemplate.queryForObject("""
                SELECT sn FROM t_payment_instrument_binding
                WHERE tenant_id = ? AND instrument_sn = ?
                """, String.class, TENANT_ID, RECEIVE_INSTRUMENT_SN);
        paymentInstrumentService.changePaymentInstrumentBinding(new ChangePaymentInstrumentBindingRequest()
                .setTenantId(TENANT_ID)
                .setBindingSn(bindingSn)
                .setState(PaymentInstrumentBindingState.SUSPENDED)
                .setOperatorId("TEST")
                .setChangeReason("verify established receive replay"));
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

    private FundsSubjectBalanceDTO balance(FundsAccountId accountId) {
        return balanceQueryService.getRequiredCurrentBalance(new FundsSubjectBalanceQuery()
                .setTenantId(TENANT_ID)
                .setSubjectRefs(List.of(accountId))
                .setCurrency(CurrencyIsoCode.USD));
    }

    private String fundsTransactionState(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT status FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private Integer externalFundsFactTransactionCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_funds_transaction
                WHERE external_source_code = ? AND external_funds_fact_sn = ?
                """, Integer.class, EXTERNAL_SOURCE_CODE, EXTERNAL_FUNDS_FACT_SN);
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
        return jsonObject(routeSnapshotJson(businessSn))
                .path("legs")
                .size();
    }

    private String routeSnapshotJson(String businessSn) {
        return jdbcTemplate.queryForObject("""
                SELECT route_snapshot FROM t_funds_transaction
                WHERE business_scene = ? AND business_sn = ?
                """, String.class, BUSINESS_SCENE, businessSn);
    }

    private void assertReceiveRouteSnapshot(String businessSn) {
        JsonNode routeSnapshot = jsonObject(routeSnapshotJson(businessSn));
        JsonNode paymentInstrumentRef = routeSnapshot.path("paymentInstrumentRef");
        assertThat(paymentInstrumentRef).isNotNull().isNotEmpty();
        assertThat(paymentInstrumentRef.path("instrumentId").asString()).isEqualTo(RECEIVE_INSTRUMENT_SN);
        assertThat(paymentInstrumentRef.path("instrumentType").asString()).isEqualTo("VA");
        assertThat(paymentInstrumentRef.path("instrumentNo").asString()).isEqualTo(RECEIVE_INSTRUMENT_NO);
        assertThat(paymentInstrumentRef.path("ownerId").asString()).isEqualTo(OWNER_ID);
        assertThat(paymentInstrumentRef.path("ownerType").asString()).isEqualTo(FundsAccountOwnerType.USER.name());
        assertThat(paymentInstrumentRef.path("currency").asString()).isEqualTo(CurrencyIsoCode.USD.name());
        assertThat(paymentInstrumentRef.path("status").asString()).isEqualTo(FundsAccountState.ACTIVE.name());
        assertThat(paymentInstrumentRef.toString()).doesNotContain("va_lifecycle_2468");
        JsonNode bindingSnapshot = paymentInstrumentRef.path("bindingSnapshot");
        assertThat(bindingSnapshot).isNotNull().isNotEmpty();
        assertThat(bindingSnapshot.path("bindingSn").asString()).startsWith("PIB");
        assertThat(bindingSnapshot.path("bindingVersion").asInt()).isEqualTo(1);
        assertThat(bindingSnapshot.path("bindingRole").asString())
                .isEqualTo(PaymentInstrumentBindingRole.RECEIVE_SUBJECT.name());
        assertThat(bindingSnapshot.path("subjectType").asString()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT.name());
        assertThat(bindingSnapshot.path("subjectId").asString()).isEqualTo(RECEIVE_ACCOUNT_SN);
        assertThat(bindingSnapshot.path("admissionAction").asString())
                .isEqualTo(PaymentInstrumentAction.RECEIVE.name());
        JsonNode externalAccountRef = routeSnapshot.path("externalAccountRef");
        assertThat(externalAccountRef).isNotNull().isNotEmpty();
        assertThat(externalAccountRef.path("externalAccountId").asString())
                .isEqualTo("external_bank_lifecycle_receive");
        assertThat(externalAccountRef.path("externalAccountType").asString())
                .isEqualTo(DefaultFundsAccountType.EXTERNAL_BANK.name());
        assertThat(externalAccountRef.path("providerCode").asString()).isEqualTo(PROVIDER_CODE);
        assertThat(externalAccountRef.path("channelCode").asString()).isEqualTo(EXTERNAL_RAIL_CODE);
    }

    private void assertReceiveFactsKeepDirectContextMinimal(String businessSn) {
        assertThat(queryContextVariables("t_funds_transaction", businessSn))
                .singleElement()
                .satisfies(context -> assertThat(jsonObject(context)).isEmpty());
        assertThat(queryContextVariables("t_funds_transaction_detail", businessSn))
                .isNotEmpty()
                .allSatisfy(context -> assertThat(jsonObject(context).propertyNames())
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
                .satisfies(context -> assertThat(jsonObject(context)).isEmpty());
        assertThat(queryContextVariables("t_ledger_entry", businessSn))
                .isNotEmpty()
                .allSatisfy(context -> assertThat(context).doesNotContain("va_lifecycle_2468"));
    }

    private List<String> queryContextVariables(String tableName, String businessSn) {
        return jdbcTemplate.queryForList(
                "SELECT context_variables FROM " + tableName + " WHERE business_scene = ? AND business_sn = ?",
                String.class, BUSINESS_SCENE, businessSn);
    }

    private static JsonNode jsonObject(String json) {
        return WindJson.parseObject(json, JsonNode.class);
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
            LedgerProfileCatalog.class,
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
