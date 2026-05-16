package com.capte.funds.transaction;

import com.capte.domain.core.context.ThreadContextTenantIdHolder;
import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.FundsBalanceAdjustRequest;
import com.capte.funds.transaction.model.request.FundsBalanceFreezeRequest;
import com.capte.funds.transaction.model.request.FundsBalanceUnfreezeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.route.AuthorizationFundsInstructionRouteResolver;
import com.capte.funds.route.BalanceControlFundsInstructionRouteResolver;
import com.capte.funds.route.CompositeRouteResolver;
import com.capte.funds.route.TransferFundsInstructionRouteResolver;
import com.capte.funds.route.support.PlatformAccountRouteSupport;
import com.capte.funds.route.support.RouteParticipantFactory;
import com.capte.funds.route.support.RouteSubjectSupport;
import com.capte.funds.transaction.converter.FundsAuthorizationInstructionConverter;
import com.capte.funds.transaction.converter.FundsBalanceControlInstructionConverter;
import com.capte.funds.transaction.converter.FundsDirectTransactionInstructionConverter;
import com.capte.funds.transaction.application.impl.FundsTransactionCommandServiceImpl;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.wind.integration.funds.wallet.enums.PlatformFundingAccountRole;
import com.capte.funds.wallet.service.PlatformFundingAccountService;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.FundsAccount;
import com.wind.integration.funds.wallet.FundsAccountBalanceView;
import com.wind.integration.funds.wallet.FundsAccountOwner;
import com.wind.integration.funds.wallet.FundsAccountQueryService;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.transaction.FundsAccountTransactionFeeProvider;
import com.wind.integration.funds.transaction.FundsInstructionOrchestrator;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsTransactionCommandServiceImplTests {

    private static final Long TENANT_ID = 1L;

    private static final CurrencyIsoCode CURRENCY = CurrencyIsoCode.USD;

    private RecordingOrchestrator orchestrator;

    private FundsTransactionCommandServiceImpl service;

    private RouteResolver routeResolver;

    @BeforeEach
    void setUp() {
        ThreadContextTenantIdHolder.setTenantId(TENANT_ID);
        orchestrator = new RecordingOrchestrator();
        PlatformFundingAccountService platformFundingAccountService = platformFundingAccountService();
        RouteSubjectSupport routeSubjectSupport = new RouteSubjectSupport();
        PlatformAccountRouteSupport platformAccountRouteSupport = new PlatformAccountRouteSupport(platformFundingAccountService);
        service = new FundsTransactionCommandServiceImpl(
                new FundsDirectTransactionInstructionConverter(platformFundingAccountService, accountQueryService(CURRENCY)),
                new FundsBalanceControlInstructionConverter(accountQueryService(CURRENCY)),
                new FundsAuthorizationInstructionConverter(accountQueryService(CURRENCY)),
                orchestrator);
        RouteParticipantFactory routeParticipantFactory = new RouteParticipantFactory();
        routeResolver = new CompositeRouteResolver(List.of(
                new TransferFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport, noFeeProvider()),
                new BalanceControlFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport),
                new AuthorizationFundsInstructionRouteResolver(routeParticipantFactory, routeSubjectSupport,
                        platformAccountRouteSupport)
        ));
    }

    @AfterEach
    void tearDown() {
        ThreadContextTenantIdHolder.remove();
    }

    @Test
    void testTopupShouldBuildFundInAndSettlementRoute() {
        FundsAccountId target = fundingAccount("funding_001");

        String transactionSn = service.topup(new FundsTransactionTopupRequest()
                .setAccountId(target)
                .setFundsSourceAccountId(FundsAccountId.immutable("external_bank_001",
                        DefaultFundsAccountType.EXTERNAL_BANK))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("bank_txn_001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(1_000L)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_00000001")
                .setDescription("topup"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(transactionSn).isEqualTo("FT_CAPTURED");
        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.DIRECT_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.TOPUP);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TOPUP);
        assertThat(instruction.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(route.getLegs()).hasSize(2);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactlyInAnyOrder(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name(),
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name(),
                        RouteParticipantRole.PAYEE.name());
        assertLeg(route.getLegs().get(0), RouteLegType.EXTERNAL_IN, LedgerSubjectCode.CASH,
                LedgerSubjectCode.PREPAYMENT, LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.FUND_IN);
        assertLeg(route.getLegs().get(1), RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.PREPAYMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.SETTLEMENT);
        assertThat(route.getExternalAccountRef()).isNotNull();
        assertThat(route.getExternalAccountRef().getExternalAccountId()).isEqualTo("external_bank_001");
    }

    @Test
    void testTopupShouldRejectNonExternalSourceBeforeOrchestrator() {
        FundsAccountId target = fundingAccount("funding_001");

        assertThatThrownBy(() -> service.topup(new FundsTransactionTopupRequest()
                .setAccountId(target)
                .setFundsSourceAccountId(fundingAccount("funding_002"))
                .setChannel(FundsTransactionChannel.WIRE_TRANSFER)
                .setChannelTransactionSn("bank_txn_001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(1_000L)))
                .setBusinessScene("TOPUP")
                .setBusinessSn("TOPUP_INVALID_SOURCE")
                .setDescription("topup"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("top-up funds source must external account");
        assertThat(instruction()).isNull();
    }

    @Test
    void testPayShouldUseExplicitTransactionAmountAndPropagateFxFactsToRouteLeg() {
        service.pay(new FundsTransactionPayRequest()
                .setAccountId(fundingAccount("funding_001"))
                .setPayeeId(fundingAccount("merchant_001"))
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.converted(Money.immutable(1_100L, CurrencyIsoCode.USD),
                        Money.immutable(1_000L, CurrencyIsoCode.EUR)))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_FX_0001")
                .setDescription("pay with fx"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = routeResolver.resolve(instruction);

        assertThat(instruction.getAmount()).isEqualTo(Money.immutable(1_100L, CurrencyIsoCode.USD));
        assertThat(instruction.getOriginalAmount()).isEqualTo(Money.immutable(1_000L, CurrencyIsoCode.EUR));
        assertThat(instruction.getExchangeRate()).isEqualByComparingTo("1.1");
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getAmount()).isEqualTo(Money.immutable(1_100L, CurrencyIsoCode.USD));
            assertThat(leg.getOriginalAmount()).isEqualTo(Money.immutable(1_000L, CurrencyIsoCode.EUR));
            assertThat(leg.getExchangeRate()).isEqualByComparingTo("1.1");
        });
    }

    @Test
    void testWithdrawShouldBuildSettlementAndFundOutRoute() {
        FundsAccountId payer = fundingAccount("funding_001");

        service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(payer)
                .setPayeeId(FundsAccountId.immutable("external_bank_001", DefaultFundsAccountType.EXTERNAL_BANK))
                .setReferenceFreezeSn("FREEZE_00000001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(800L)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_00000001")
                .setDescription("withdraw"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.WITHDRAW);
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FREEZE_ORDER);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("FREEZE_00000001");
        assertThat(route.getLegs()).hasSize(2);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactlyInAnyOrder(RouteParticipantRole.PAYER.name(),
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name(),
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name());
        assertLeg(route.getLegs().get(0), RouteLegType.CONSUME, LedgerSubjectCode.FROZEN,
                LedgerSubjectCode.PREPAYMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
        assertLeg(route.getLegs().get(1), RouteLegType.EXTERNAL_OUT, LedgerSubjectCode.PREPAYMENT,
                LedgerSubjectCode.CASH, LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.FUND_OUT);
    }

    @Test
    void testWithdrawShouldRejectNonExternalPayeeBeforeOrchestrator() {
        FundsAccountId payer = fundingAccount("funding_001");

        assertThatThrownBy(() -> service.withdraw(new FundsTransactionWithdrawRequest()
                .setAccountId(payer)
                .setPayeeId(fundingAccount("funding_002"))
                .setReferenceFreezeSn("FREEZE_00000001")
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(800L)))
                .setBusinessScene("WITHDRAW")
                .setBusinessSn("WITHDRAW_INVALID_PAYEE")
                .setDescription("withdraw"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("withdraw payee must external account");
        assertThat(instruction()).isNull();
    }

    @Test
    void testTransferShouldBuildSingleInternalTransferRoute() {
        FundsAccountId payer = fundingAccount("funding_001");
        FundsAccountId payee = fundingAccount("funding_002");

        service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(payer)
                .setPayeeAccountId(payee)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(500L)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_00000001")
                .setDescription("transfer"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.TRANSFER);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.TRANSFER);
        assertThat(route.getLegs()).hasSize(1);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactlyInAnyOrder(RouteParticipantRole.PAYER.name(), RouteParticipantRole.PAYEE.name());
        assertLeg(route.getLegs().getFirst(), RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.TRANSFER);
    }

    @Test
    void testTransferShouldRejectSamePayerAndPayeeBeforeOrchestrator() {
        FundsAccountId account = fundingAccount("funding_001");

        assertThatThrownBy(() -> service.transfer(new FundsTransactionTransferRequest()
                .setPayerAccountId(account)
                .setPayeeAccountId(account)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(500L)))
                .setBusinessScene("TRANSFER")
                .setBusinessSn("TRANSFER_SAME_ACCOUNT")
                .setDescription("transfer"), WindOperator.system()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("不能一致");
        assertThat(instruction()).isNull();
    }

    @Test
    void testPayShouldBuildSettlementRoute() {
        FundsAccountId payer = fundingAccount("funding_001");
        FundsAccountId merchant = fundingAccount("merchant_001");

        service.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(merchant)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(700L)))
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_00000001")
                .setDescription("pay"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.PAY);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.PAY);
        assertThat(route.getLegs()).hasSize(1);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactlyInAnyOrder(RouteParticipantRole.PAYER.name(), RouteParticipantRole.PAYEE.name());
        assertLeg(route.getLegs().getFirst(), RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.SETTLEMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
    }

    @Test
    void testRefundShouldBuildRestoreRoute() {
        FundsAccountId payer = fundingAccount("merchant_001");
        FundsAccountId payee = fundingAccount("funding_001");

        service.refund(new FundsTransactionRefundRequest()
                .setAccountId(payee)
                .setPayerId(payer)
                .setPayerLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setAmount(amount(300L))
                .setBusinessScene("REFUND")
                .setBusinessSn("REFUND_00000001")
                .setDescription("refund"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.REFUND);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertLeg(leg, RouteLegType.RESTORE, LedgerSubjectCode.SETTLEMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RESTORE, LedgerPhaseCode.REFUND);
    }

    @Test
    void testAuthorizeShouldBuildHoldRouteWhenApproved() {
        FundsAccountId credit = creditAccount("credit_001");

        service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(credit)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                .setApproved(Boolean.TRUE)
                .setBusinessScene("CARD_AUTH")
                .setBusinessSn("AUTH_00000001")
                .setDescription("auth"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
        assertThat(route().getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name());
        assertLeg(leg, RouteLegType.HOLD, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.AUTHORIZATION, LedgerBalanceEffectType.HOLD, LedgerPhaseCode.AUTHORIZATION);
        assertThat(leg.getConstraintOverrides())
                .containsEntry(constraintKey(credit, LedgerSubjectCode.AVAILABLE),
                        LedgerBalanceConstraintType.MUST_NOT_BE_NEGATIVE);
    }

    @Test
    void testAuthorizeShouldNotBuildLedgerLegWhenDeclined() {
        service.authorize(new FundsAuthorizationTransactionAuthorizeRequest()
                .setAccountId(creditAccount("credit_001"))
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(600L)))
                .setApproved(Boolean.FALSE)
                .setDeclineReason("insufficient_funds")
                .setBusinessScene("CARD_AUTH")
                .setBusinessSn("AUTH_00000002")
                .setDescription("declined"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTHORIZE);
        assertThat(instruction.getContextVariables()).containsEntry("declineReason", "insufficient_funds");
        assertThat(route().getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name());
        assertThat(route().getLegs()).isEmpty();
    }

    @Test
    void testReversalShouldBuildAuthorizationReleaseRoute() {
        FundsAccountId credit = creditAccount("credit_001");

        service.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(credit)
                .setAmount(amount(600L))
                .setAuthorizationTransactionSn("AUTH_TX_00000001")
                .setBusinessScene("CARD_REVERSAL")
                .setBusinessSn("REVERSAL_00000001")
                .setDescription("reversal"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.REVERSAL);
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_00000001");
        assertLeg(leg, RouteLegType.RELEASE, LedgerSubjectCode.AUTHORIZATION,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RELEASE, LedgerPhaseCode.REVERSAL);
    }

    @Test
    void testSettleShouldBuildAuthorizationCaptureRoute() {
        FundsAccountId credit = creditAccount("credit_001");

        service.settle(new FundsAuthorizationTransactionSettleRequest()
                .setAccountId(credit)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(500L)))
                .setAuthorizationTransactionSn("AUTH_TX_00000001")
                .setBusinessScene("CARD_SETTLE")
                .setBusinessSn("SETTLE_00000001")
                .setDescription("settle"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.SETTLE);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactlyInAnyOrder(RouteParticipantRole.AUTH_HOLDER.name(), RouteParticipantRole.PAYEE.name());
        assertLeg(route.getLegs().getFirst(), RouteLegType.CONSUME, LedgerSubjectCode.AUTHORIZATION,
                LedgerSubjectCode.SETTLEMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
    }

    @Test
    void testSettleRefundShouldBuildAuthorizationRefundRoute() {
        FundsAccountId credit = creditAccount("credit_001");

        service.settleRefund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(credit)
                .setAmount(amount(200L))
                .setAuthorizationTransactionSn("AUTH_TX_00000001")
                .setBusinessScene("CARD_REFUND")
                .setBusinessSn("AUTH_REFUND_00000001")
                .setDescription("auth refund"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactlyInAnyOrder(RouteParticipantRole.PAYER.name(), RouteParticipantRole.AUTH_HOLDER.name());
        assertLeg(route.getLegs().getFirst(), RouteLegType.RESTORE, LedgerSubjectCode.SETTLEMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RESTORE, LedgerPhaseCode.REFUND);
    }

    @Test
    void testChargebackShouldBuildPostSettlementDisputeReplayInstruction() {
        FundsAccountId credit = creditAccount("credit_001");

        String transactionSn = service.chargeback(new FundsAuthorizationTransactionChargebackRequest()
                .setAccountId(credit)
                .setAmount(amount(100L))
                .setAuthorizationTransactionSn("AUTH_TX_00000001")
                .setBusinessScene("CARD_POST_SETTLEMENT_DISPUTE")
                .setBusinessSn("CHARGEBACK_00000001")
                .setDescription("chargeback"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        assertThat(transactionSn).isEqualTo("FT_CAPTURED");
        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.AUTHORIZATION_TRANSACTION);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(instruction.getBusinessScene()).isEqualTo("CARD_POST_SETTLEMENT_DISPUTE");
        assertThat(instruction.getBusinessSn()).isEqualTo("CHARGEBACK_00000001");
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_00000001");
    }

    @Test
    void testFreezeShouldBuildHoldRoute() {
        FundsAccountId payer = fundingAccount("funding_001");

        service.freeze(new FundsBalanceFreezeRequest()
                .setAccountId(payer)
                .setAmount(amount(400L))
                .setBusinessScene("FREEZE")
                .setBusinessSn("FREEZE_00000001")
                .setDescription("freeze"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getInstructionType()).isEqualTo(FundsInstructionType.BALANCE_CONTROL);
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FREEZE);
        assertLeg(leg, RouteLegType.HOLD, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FROZEN, LedgerBalanceEffectType.HOLD, LedgerPhaseCode.FREEZE);
    }

    @Test
    void testUnfreezeShouldBuildReleaseRouteWithReference() {
        FundsAccountId payer = fundingAccount("funding_001");

        service.unfreeze(new FundsBalanceUnfreezeRequest()
                .setAccountId(payer)
                .setAmount(amount(200L))
                .setReferenceFreezeSn("FREEZE_00000001")
                .setBusinessScene("UNFREEZE")
                .setBusinessSn("UNFREEZE_00000001")
                .setDescription("unfreeze"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FREEZE_ORDER);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("FREEZE_00000001");
        assertLeg(leg, RouteLegType.RELEASE, LedgerSubjectCode.FROZEN,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RELEASE, LedgerPhaseCode.UNFREEZE);
    }

    @Test
    void testAdjustCreditAccountShouldBuildLimitAdjustmentRoute() {
        FundsAccountId credit = creditAccount("credit_001");

        service.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(credit)
                .setAmount(amount(5_000L))
                .setIncrease(Boolean.TRUE)
                .setBusinessScene("LIMIT")
                .setBusinessSn("LIMIT_00000001")
                .setDescription("increase limit"), WindOperator.system());

        RouteLegSpec leg = route().getLegs().getFirst();
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.LIMIT, LedgerSubjectCode.AVAILABLE,
                LedgerBalanceEffectType.INCREASE, LedgerPhaseCode.ADJUSTMENT);
        assertThat(instruction().getEventType()).isEqualTo(FundsTransactionEventType.LIMIT_ADJUST);
    }


    @Test
    void testAdjustBudgetGroupShouldBuildBudgetLimitAdjustmentRoute() {
        FundsAccountId budgetGroup = FundsAccountId.immutable("budget_001", FundsSubjectType.BUDGET_GROUP);

        service.adjust(new FundsBalanceAdjustRequest()
                .setAccountId(budgetGroup)
                .setAmount(amount(2_000L))
                .setIncrease(Boolean.FALSE)
                .setBusinessScene("BUDGET")
                .setBusinessSn("BUDGET_00000001")
                .setDescription("decrease budget"), WindOperator.system());

        RouteLegSpec leg = route().getLegs().getFirst();
        assertLeg(leg, RouteLegType.ADJUST, LedgerSubjectCode.AVAILABLE, LedgerSubjectCode.LIMIT,
                LedgerBalanceEffectType.DECREASE, LedgerPhaseCode.ADJUSTMENT);
    }

    @Test
    void testFeeShouldBuildIndependentFeeRoute() {
        FundsAccountId payer = fundingAccount("funding_001");

        service.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(amount(30L))
                .setFeeType(DefaultFeeType.FEE)
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_00000001")
                .setDescription("fee"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.FEE);
        assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FEE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.FEE);
    }

    private FundsInstructionSpec instruction() {
        return orchestrator.instruction.get();
    }

    private ResolvedRouteSpec route() {
        return routeResolver.resolve(instruction());
    }

    private static void assertLeg(RouteLegSpec leg,
                                  RouteLegType legType,
                                  LedgerSubjectCode fromCode,
                                  LedgerSubjectCode toCode,
                                  LedgerBalanceEffectType effectType,
                                  LedgerPhaseCode phaseCode) {
        assertThat(leg.getLegType()).isEqualTo(legType);
        assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(fromCode);
        assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(toCode);
        assertThat(leg.getBalanceEffectType()).isEqualTo(effectType);
        assertThat(leg.getPhaseCode()).isEqualTo(phaseCode);
    }

    private static FundsAccountId fundingAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.FUNDING_ACCOUNT);
    }

    private static FundsAccountId creditAccount(String accountId) {
        return FundsAccountId.immutable(accountId, FundsSubjectType.CREDIT_ACCOUNT);
    }

    private static Money amount(long value) {
        return Money.immutable(value, CURRENCY);
    }

    private static String constraintKey(FundsAccountId accountId, LedgerSubjectCode subjectCode) {
        return accountId.type() + ":" + accountId.id() + ":" + subjectCode.name();
    }

    private static FundsAccountTransactionFeeProvider noFeeProvider() {
        return new FundsAccountTransactionFeeProvider() {
            @Override
            public FeeSpec apply(FundsAccountId accountId, String businessScene) {
                return null;
            }

            @Override
            public boolean supports(FundsAccountId accountId) {
                return false;
            }
        };
    }

    private static PlatformFundingAccountService platformFundingAccountService() {
        return new PlatformFundingAccountService() {
            @Override
            public FundsAccountId requireAccountId(CurrencyIsoCode currency, PlatformFundingAccountRole role) {
                return requireAccountId(TENANT_ID, currency, role);
            }

            @Override
            public FundsAccountId requireAccountId(Long tenantId, CurrencyIsoCode currency,
                                                   PlatformFundingAccountRole role) {
                return FundsAccountId.immutable("platform_" + role.name().toLowerCase(),
                        FundsSubjectType.FUNDING_ACCOUNT);
            }
        };
    }

    private static FundsAccountQueryService accountQueryService(CurrencyIsoCode currency) {
        return new FundsAccountQueryService() {
            @Override
            public @org.jspecify.annotations.NonNull FundsAccount getAccount(
                    @org.jspecify.annotations.NonNull FundsAccountId accountId) {
                return new TestFundsAccount(accountId, currency);
            }

            @Override
            public @org.jspecify.annotations.NonNull FundsAccountBalanceView getBalance(
                    @org.jspecify.annotations.NonNull FundsAccountId accountId) {
                throw new UnsupportedOperationException("balance is not required by this test");
            }

            @Override
            public boolean supports(@org.jspecify.annotations.NonNull FundsAccountId accountId) {
                return true;
            }
        };
    }

    private record TestFundsAccount(FundsAccountId accountId,
                                    CurrencyIsoCode currency) implements FundsAccount {

        @Override
        public @org.jspecify.annotations.NonNull Long getId() {
            return 1L;
        }

        @Override
        public @org.jspecify.annotations.NonNull FundsAccountId getAccountId() {
            return accountId;
        }

        @Override
        public @org.jspecify.annotations.NonNull FundsAccountOwner getOwner() {
            return FundsAccountOwner.of("owner_001", FundsAccountOwnerType.USER);
        }

        @Override
        public @org.jspecify.annotations.NonNull FundsAccountStatus getStatus() {
            return FundsAccountStatus.ACTIVE;
        }

        @Override
        public @org.jspecify.annotations.NonNull Map<LedgerSubjectCode, Long> getAccountLedgerIds() {
            return Map.of();
        }

        @Override
        public CurrencyIsoCode getCurrency() {
            return currency;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public @org.jspecify.annotations.NonNull Integer getVersion() {
            return 0;
        }

        @Override
        public Long getTenantId() {
            return TENANT_ID;
        }
    }

    private static final class RecordingOrchestrator implements FundsInstructionOrchestrator<FundsInstructionSpec> {

        private final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        @Override
        public String execute(FundsInstructionSpec spec) {
            instruction.set(spec);
            return "FT_CAPTURED";
        }

        @Override
        public boolean supports(Class<FundsInstructionSpec> specType) {
            return FundsInstructionSpec.class.isAssignableFrom(specType);
        }
    }
}
