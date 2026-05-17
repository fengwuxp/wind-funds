package com.capte.funds.transaction;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
import com.capte.funds.transaction.model.request.FundsTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTopupRequest;
import com.capte.funds.transaction.model.request.FundsTransactionTransferRequest;
import com.capte.funds.transaction.model.request.FundsTransactionWithdrawRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFeeType;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.wallet.enums.DefaultFundsAccountType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundsTransactionCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

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
    void testFeeShouldBuildIndependentFeeRoute() {
        FundsAccountId payer = fundingAccount("funding_001");

        service.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(amount(30L))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_00000001")
                .setDescription("fee"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        RouteLegSpec leg = route().getLegs().getFirst();
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.FEE);
        assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FEE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.FEE);
        assertNoLimitNodes(route());
    }
}
