package com.capte.funds.transaction.flow;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.route.FundsRouteTestSupport;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.capte.funds.transaction.FundsTransactionTestSupport;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionOrchestrationFlowTests {

    private static final Long TENANT_ID = 1L;

    private static final com.wind.transaction.core.enums.CurrencyIsoCode CURRENCY =
            com.wind.transaction.core.enums.CurrencyIsoCode.USD;

    private RecordingRouteResolver routeResolver;

    private RecordingLifecycleSaver lifecycleSaver;

    private RecordingLedgerPostingAssembler postingAssembler;

    private RecordingPostingService postingService;

    private RecordingTransactionQueryService transactionQueryService;

    private FundsAuthorizationInstructionConverter authorizationInstructionConverter;

    private FundsTransactionCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        PlatformFundingAccountService platformFundingAccountService = platformFundingAccountService();
        authorizationInstructionConverter = new FundsAuthorizationInstructionConverter(
                FundsRouteTestSupport.accountQueryService(CURRENCY));
        RouteSubjectSupport routeSubjectSupport = new RouteSubjectSupport();
        PlatformAccountRouteSupport platformAccountRouteSupport = new PlatformAccountRouteSupport(
                platformFundingAccountService);
        RouteParticipantFactory routeParticipantFactory = new RouteParticipantFactory();
        transactionQueryService = new RecordingTransactionQueryService();
        RouteResolver delegate = new CompositeRouteResolver(List.of(
                new DefaultRouteReplayService(transactionQueryService),
                new TransferFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new BalanceControlFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new AuthorizationFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport)
        ));
        routeResolver = new RecordingRouteResolver(delegate);
        lifecycleSaver = new RecordingLifecycleSaver();
        postingAssembler = new RecordingLedgerPostingAssembler();
        postingService = new RecordingPostingService();
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );
        service = new FundsTransactionCommandServiceImpl(
                new FundsDirectTransactionInstructionConverter(platformFundingAccountService,
                        FundsRouteTestSupport.accountQueryService(CURRENCY)),
                new FundsBalanceControlInstructionConverter(FundsRouteTestSupport.accountQueryService(CURRENCY)),
                authorizationInstructionConverter,
                orchestrator
        );
    }

    @AfterEach
    void tearDown() {
        ThreadContextTenantIdHolder.remove();
    }

    /**
     * 场景：授权请求被拒绝，业务只需要记录生命周期结果。
     * 输入：`approved=false` 的授权指令。
     * 输出：交易流水号、解析路径、账本组装记录和生命周期成功回填值。
     * 预期：Route 可解析但 legs 为空，不进入账本组装与入账链路，成功结果不回填账本交易号。
     */
    @Test
    void testAuthorizeDeclinedShouldCompleteWithoutPostingLedger() {
        String transactionSn = service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(creditAccount("credit_001"))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                .setApproved(Boolean.FALSE)
                .setDeclineReason("insufficient_funds")
                .setBusinessScene("CARD_AUTH")
                .setBusinessSn("AUTH_DECLINED_0001")
                .setDescription("declined"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs()).isEmpty();
        assertThat(postingAssembler.route.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get()).isNull();
    }

    /**
     * 场景：已授权金额在后续被撤销释放。
     * 输入：带原授权交易号的 reversal 请求，且能查询到原授权快照。
     * 输出：回放路径编码、事件类型、账本阶段和成功回填的账本交易号。
     * 预期：编排器通过统一 RouteResolver 分发到 replay resolver，基于原授权快照生成
     * `AUTHORIZATION_REVERSAL_REPLAY` 路径并完成入账。
     */
    @Test
    void testReversalShouldReplayOriginalAuthorizationPathThroughOrchestrator() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalAuthorizationSnapshot());

        String transactionSn = service.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(creditAccount("credit_001"))
                .setAmount(amount(100L))
                .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                .setBusinessScene("CARD_REVERSAL")
                .setBusinessSn("REVERSAL_0001")
                .setDescription("reversal"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("AUTHORIZATION_REVERSAL_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.REVERSAL);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getPhaseCode)
                .containsOnly(LedgerPhaseCode.REVERSAL);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：已授权金额在后续进入结算扣款。
     * 输入：带原授权交易号的 settle 请求，且能查询到原授权快照。
     * 输出：回放路径编码、事件类型、账本阶段和成功回填的账本交易号。
     * 预期：编排器通过统一 RouteResolver 分发到 replay resolver，基于原授权快照生成
     * `AUTHORIZATION_SETTLE_REPLAY` 路径并完成入账。
     */
    @Test
    void testSettleShouldReplayOriginalAuthorizationPathThroughOrchestrator() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalAuthorizationSnapshot());

        String transactionSn = service.settle(new FundsAuthorizationTransactionSettleRequest()
                .setAccountId(creditAccount("credit_001"))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                .setBusinessScene("CARD_SETTLE")
                .setBusinessSn("SETTLE_0001")
                .setDescription("settle"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.SETTLE);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getPhaseCode)
                .containsOnly(LedgerPhaseCode.SETTLEMENT);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：钱包充值从外部账户入金到账户余额。
     * 输入：外部银行账户来源、充值目标账户、金额与渠道流水。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `TOPUP_STANDARD` 路由，包含 `FUND_IN -> SETTLEMENT` 两段路径，并完成账本入账。
     */
    @Test
    void testTopupShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.topup(new FundsTransactionTopupRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        com.wind.integration.funds.wallet.enums.DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("BANK_TXN_0001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_0001")
                .setDescription("topup"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("TOPUP_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("FUND_IN", "TOPUP_SETTLEMENT");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：钱包提现从冻结余额出金到外部账户。
     * 输入：提现账户、外部收款账户、冻结引用号与提现金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `WITHDRAW_STANDARD` 路由，包含 `WITHDRAW_SETTLEMENT -> FUND_OUT` 两段路径，并完成账本入账。
     */
    @Test
    void testWithdrawShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeId(FundsAccountId.immutable("external_bank_001",
                        com.wind.integration.funds.wallet.enums.DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn("FREEZE_0001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_0001")
                .setDescription("withdraw"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("WITHDRAW_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("WITHDRAW_SETTLEMENT", "FUND_OUT");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：原收款方把已入账资金退回给目标账户。
     * 输入：退款到账账户、原收款方账户、原收款方账本编码与退款金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `DIRECT_REFUND_STANDARD` 路由，包含单条 `REFUND` 恢复路径，并完成账本入账。
     */
    @Test
    void testRefundShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.refund(new FundsTransactionRefundRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayerId(FundsAccountId.immutable("merchant_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(amount(100L))
                .setBusinessScene("REFUND")
                .setBusinessSn("REFUND_0001")
                .setDescription("refund"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("DIRECT_REFUND_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("REFUND");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：两个内部资金账户之间发生普通转账。
     * 输入：付款账户、收款账户与转账金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `INTERNAL_TRANSFER_STANDARD` 路由，包含单条 `TRANSFER` 路径，并完成账本入账。
     */
    @Test
    void testTransferShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeAccountId(FundsAccountId.immutable("funding_002", FundsSubjectType.FUNDING_ACCOUNT))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_0001")
                .setDescription("transfer"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("INTERNAL_TRANSFER_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("TRANSFER");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：账户发生独立手续费扣收。
     * 输入：扣费账户、手续费类型与手续费金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `FEE_STANDARD` 路由，包含单条 `FEE` 路径，并完成账本入账。
     */
    @Test
    void testFeeShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.fee(new FundsTransactionFeeRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setAmount(amount(30L))
                .setFeeType(com.wind.integration.funds.transaction.enums.DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_0001")
                .setDescription("fee"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("FEE_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("FEE");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：付款账户把余额支付给收款账户的指定账本桶。
     * 输入：付款账户、收款账户、收款账本编码与支付金额。
     * 输出：路径编码、路径 leg 顺序、账本组装记录和成功回填的账本交易号。
     * 预期：编排器解析出 `DIRECT_PAY_STANDARD` 路由，包含单条 `PAY` 路径，并完成账本入账。
     */
    @Test
    void testPayShouldResolveAndPostLedgerThroughOrchestrator() {
        String transactionSn = service.pay(new FundsTransactionPayRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeId(FundsAccountId.immutable("merchant_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_0001")
                .setDescription("pay"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("DIRECT_PAY_STANDARD");
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getLegId)
                .containsExactly("PAY");
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：已结算授权交易发生拒付/争议。
     * 输入：带原授权交易号的 chargeback 请求，且能查询到原结算快照。
     * 输出：回放路径编码、事件类型、账本阶段和成功回填的账本交易号。
     * 预期：编排器通过统一 RouteResolver 分发到 replay resolver，基于原快照生成
     * `CHARGEBACK_REPLAY` 路径并完成入账。
     */
    @Test
    void testChargebackShouldReplayOriginalSettlementPathThroughOrchestrator() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalSettlementSnapshot());

        String transactionSn = service.chargeback(new FundsAuthorizationTransactionChargebackRequest()
                .setAccountId(creditAccount("credit_001"))
                .setAmount(amount(100L))
                .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                .setBusinessScene("CARD_POST_SETTLEMENT_DISPUTE")
                .setBusinessSn("CHARGEBACK_0001")
                .setDescription("chargeback"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("CHARGEBACK_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getPhaseCode)
                .containsOnly(LedgerPhaseCode.CHARGEBACK);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingService.transaction.get()).isNotNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get())
                .isEqualTo(postingService.transaction.get().getSn());
    }

    /**
     * 场景：解冻请求带冻结单引用。
     * 输入：`referenceType=FREEZE_ORDER` 的 unfreeze 请求。
     * 输出：回放路径编码、账本阶段、原 leg 引用和成功回填的账本交易号。
     * 预期：编排器通过冻结单号定位原冻结路径快照，生成 `BALANCE_UNFREEZE_REPLAY` 路径并完成入账。
     */
    @Test
    void testUnfreezeWithFreezeOrderReferenceShouldReplayOriginalFreezePath() {
        transactionQueryService.freezeOrderSnapshots.put("FREEZE_ORDER_0001", originalFreezeSnapshot());

        String transactionSn = service.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                .setAmount(amount(100L))
                .setReferenceFreezeSn("FREEZE_ORDER_0001")
                .setBusinessScene("RISK_UNFREEZE")
                .setBusinessSn("UNFREEZE_0001")
                .setDescription("unfreeze"), WindOperator.system());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isNotNull();
        assertThat(lifecycleSaver.beforePostingRoute.get().getRouteCode()).isEqualTo("BALANCE_UNFREEZE_REPLAY");
        assertThat(lifecycleSaver.beforePostingRoute.get().getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(lifecycleSaver.beforePostingRoute.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .allSatisfy(value -> assertThat(value).isNotBlank());
        assertThat(postingAssembler.route.get()).isNotNull();
        assertThat(postingService.transaction.get()).isNotNull();
    }

    private RouteSnapshotSpec originalSettlementSnapshot() {
        transactionQueryService.routeSnapshots.put("AUTH_TX_ORIGINAL", originalAuthorizationSnapshot());
        FundsInstructionSpec settleInstruction = authorizationInstructionConverter.convertToSettleInstruction(
                new FundsAuthorizationTransactionSettleRequest()
                        .setAccountId(creditAccount("credit_001"))
                        .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                        .setAuthorizationTransactionSn("AUTH_TX_ORIGINAL")
                        .setBusinessScene("CARD_SETTLE")
                        .setBusinessSn("SETTLE_0001")
                        .setDescription("settle"),
                WindOperator.system());
        ResolvedRouteSpec route = routeResolver.delegate.resolve(settleInstruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    private RouteSnapshotSpec originalFreezeSnapshot() {
        FundsInstructionSpec freezeInstruction = new FundsBalanceControlInstructionConverter(
                FundsRouteTestSupport.accountQueryService(CURRENCY))
                .convertToFreezeInstruction(new com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest()
                        .setAccountId(FundsAccountId.immutable("funding_001", FundsSubjectType.FUNDING_ACCOUNT))
                        .setAmount(amount(100L))
                        .setBusinessScene("FREEZE")
                        .setBusinessSn("FREEZE_0001")
                        .setDescription("freeze"), WindOperator.system());
        ResolvedRouteSpec route = routeResolver.delegate.resolve(freezeInstruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    private RouteSnapshotSpec originalAuthorizationSnapshot() {
        FundsInstructionSpec authorizeInstruction = authorizationInstructionConverter.convertToAuthorizeInstruction(
                new FundsAuthorizationTransactionAuthorizeRequest()
                        .setAccountId(creditAccount("credit_001"))
                        .setTransactionAmount(TransactionAmount.sameCurrency(amount(100L)))
                        .setApproved(Boolean.TRUE)
                        .setBusinessScene("CARD_AUTH")
                        .setBusinessSn("AUTH_0001")
                        .setDescription("authorize"),
                WindOperator.system());
        ResolvedRouteSpec route = routeResolver.delegate.resolve(authorizeInstruction);
        return new DefaultRouteSnapshotFactory().createSnapshot(route);
    }

    private static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private static com.wind.transaction.core.Money amount(long value) {
        return com.wind.transaction.core.Money.immutable(value, CURRENCY);
    }

    private static PlatformFundingAccountService platformFundingAccountService() {
        return new PlatformFundingAccountService() {
            @Override
            public FundsAccountId requireAccountId(com.wind.transaction.core.enums.CurrencyIsoCode currency,
                                                   PlatformFundingAccountRole role) {
                return requireAccountId(TENANT_ID, currency, role);
            }

            @Override
            public FundsAccountId requireAccountId(Long tenantId,
                                                   com.wind.transaction.core.enums.CurrencyIsoCode currency,
                                                   PlatformFundingAccountRole role) {
                return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                        FundsSubjectType.FUNDING_ACCOUNT);
            }
        };
    }

    private static final class RecordingRouteResolver implements RouteResolver {

        private final RouteResolver delegate;

        private final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        private RecordingRouteResolver(RouteResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return delegate.supports(instruction);
        }

        @Override
        public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
            this.instruction.set(instruction);
            return delegate.resolve(instruction);
        }
    }

    private static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver {

        private final AtomicReference<ResolvedRouteSpec> beforePostingRoute = new AtomicReference<>();

        private final AtomicReference<String> succeededLedgerTransactionSn = new AtomicReference<>();

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public @NonNull FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                                      @NonNull ResolvedRouteSpec resolvedRoute,
                                                                      @NonNull RouteSnapshotSpec routeSnapshot) {
            beforePostingRoute.set(resolvedRoute);
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn("FT_001")
                    .setTransactionDetailSns(List.of("FTD_001"))
                    .setCompleted(false);
        }

        @Override
        public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                                  @NonNull FundsInstructionLifecycleResult result,
                                  @Nullable String ledgerTransactionSn) {
            succeededLedgerTransactionSn.set(ledgerTransactionSn);
        }

        @Override
        public void markFailed(@NonNull FundsInstructionSpec instruction,
                               @NonNull FundsInstructionLifecycleResult result,
                               @NonNull Throwable cause) {
            throw new AssertionError("unexpected failure", cause);
        }
    }

    private static final class RecordingLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec> {

        private final AtomicReference<ResolvedRouteSpec> route = new AtomicReference<>();

        @Override
        public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                       @NonNull String fundsTransactionSn,
                                                       @NonNull ResolvedRouteSpec resolvedRoute) {
            route.set(resolvedRoute);
            return LedgerTransactionSpecFactory.createLedgerTransaction(instruction, fundsTransactionSn,
                    ledgerTransactionSn -> {
                        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(
                                LedgerPhaseCode.TRANSFER,
                                List.of(entry(ledgerTransactionSn, EntrySide.DEBIT),
                                        entry(ledgerTransactionSn, EntrySide.CREDIT))
                        );
                        return List.of(LedgerTransactionSpecFactory.postingPlan(
                                LedgerPostingIntentType.TRANSFER,
                                ledgerTransactionSn,
                                null,
                                LedgerBalanceEffectType.CONSUME,
                                List.of(phase)
                        ));
                    });
        }

        @Override
        public boolean supports(@NonNull ResolvedRouteSpec resolvedRoute) {
            return true;
        }
    }

    private static LedgerEntrySpec entry(String ledgerTransactionSn, EntrySide entrySide) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                entrySide == EntrySide.DEBIT ? "funding_001" : "funding_002",
                FundsSubjectType.FUNDING_ACCOUNT.name(),
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                ledgerTransactionSn,
                "TRANSFER",
                "BIZ_0001",
                100L,
                CURRENCY,
                LocalDateTime.of(2026, 5, 9, 12, 0)
        ).setBalanceEffectType(LedgerBalanceEffectType.CONSUME)
                .setIntent(LedgerPostingIntentType.TRANSFER)
                .setPhaseCode(LedgerPhaseCode.TRANSFER)
                .setContextVariables(Map.of());
    }

    private static final class RecordingPostingService implements LedgerTransactionPostingService {

        private final AtomicReference<LedgerTransactionSpec> transaction = new AtomicReference<>();

        @Override
        public void post(LedgerTransactionSpec transaction) {
            this.transaction.set(transaction);
        }
    }

    private static final class RecordingTransactionQueryService implements FundsTransactionQueryService {

        private final Map<String, RouteSnapshotSpec> routeSnapshots = new ConcurrentHashMap<>();

        private final Map<String, RouteSnapshotSpec> freezeOrderSnapshots = new ConcurrentHashMap<>();

        @Override
        public @NonNull Optional<FundsTransactionDTO> queryFundsTransaction(@NonNull String transactionSn) {
            return Optional.empty();
        }

        @Override
        public @NonNull List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(@NonNull String referenceTransactionSn,
                                            @NonNull FundsTransactionEventType eventType,
                                            @NonNull String replayRefLegId) {
            return false;
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            return Optional.ofNullable(routeSnapshots.get(transactionSn));
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.ofNullable(freezeOrderSnapshots.get(freezeOrderSn));
        }
    }
}
