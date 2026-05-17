package com.capte.funds.transaction;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
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
import com.wind.integration.funds.spec.transaction.FeeSpec;
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

import java.math.BigDecimal;

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

    /**
     * 场景：信用账户付款时业务层显式传入 FeeSpec。
     * 输入：CREDIT_ACCOUNT 支付 700，FeeSpec 固定费用 25，费率费用受上下限约束。
     * 输出：付款主 leg 和平台 FEE leg。
     * 预期：FeeSpec 保留在 instruction context，手续费从信用账户 AVAILABLE 扣到平台 FEE。
     * 红线：交易门面不得丢失业务层费用决策，信用账户付款不得漏收手续费或触碰 LIMIT。
     */
    @Test
    void testPayShouldPropagateExplicitFeeSpecAndAppendCreditAccountFeeLeg() {
        FundsAccountId payer = creditAccount("credit_001");
        FundsAccountId merchant = fundingAccount("merchant_001");
        FeeSpec feeSpec = FeeSpec.builder()
                .feeType("SMALL_AMOUNT_FEE")
                .fixedFee(25)
                .feeRate(new BigDecimal("0.01"))
                .maxAmountWithRate(50)
                .minAmountWithRate(10)
                .build();

        service.pay(new FundsTransactionPayRequest()
                .setAccountId(payer)
                .setPayeeId(merchant)
                .setPayeeLedgerCode(LedgerSubjectCode.SETTLEMENT)
                .setTransactionAmount(TransactionAmount.sameCurrency(amount(700L)))
                .setFeeSpec(feeSpec)
                .setBusinessScene("PAY")
                .setBusinessSn("PAY_WITH_FEE_00000001")
                .setDescription("pay with explicit fee"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.FEE_SPEC, feeSpec);
        assertThat(route.getLegs()).hasSize(2);
        assertLeg(route.getLegs().get(0), RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.SETTLEMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
        assertThat(route.getLegs().get(1)).satisfies(leg -> {
            assertThat(leg.getLegId()).isEqualTo("FEE");
            assertThat(leg.getAmount()).isEqualTo(amount(75L));
            assertThat(leg.getSourceNode().getSubjectRef().getSubjectType().name())
                    .isEqualTo(payer.type());
            assertThat(leg.getSourceNode().getSubjectRef().getSubjectId()).isEqualTo(payer.id());
            assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                    LedgerSubjectCode.FEE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.FEE);
        });
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .contains(RouteParticipantRole.AUTH_HOLDER.name(), RouteParticipantRole.FEE_RECEIVER.name());
        assertNoLimitNodes(route);
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

    @Test
    void testFeeRefundShouldReplayOriginalFeeLegOnly() {
        FundsAccountId payer = fundingAccount("funding_001");
        service.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(amount(30L))
                .setFeeType(DefaultFeeType.FEE.getCode())
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_00000001")
                .setDescription("fee"), WindOperator.system());
        transactionQueryService.routeSnapshots.put("FEE_TX_00000001",
                new DefaultRouteSnapshotFactory().createSnapshot(route()));

        service.feeRefund(new FundsTransactionFeeRefundRequest()
                .setAccountId(payer)
                .setAmount(amount(30L))
                .setFeeSourceTransactionSn("FEE_TX_00000001")
                .setBusinessScene("FEE_REFUND")
                .setBusinessSn("FEE_REFUND_00000001")
                .setDescription("fee refund"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.FEE_REFUND);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.FEE);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("FEE_TX_00000001");
        assertLeg(leg, RouteLegType.RESTORE, LedgerSubjectCode.FEE,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RESTORE, LedgerPhaseCode.REFUND);
        assertNoLimitNodes(route);
    }

    /**
     * 场景：信用账户独立收取授权拒付类手续费。
     * 输入：CREDIT_ACCOUNT 发起 fee，feeType 使用业务自定义 code。
     * 输出：FEE_STANDARD route。
     * 预期：feeType 原样透传，费用从信用账户 AVAILABLE 扣到平台 FEE。
     * 红线：FeeType 不依赖账户级 FeeProvider，不把信用账户费用落入 LIMIT。
     */
    @Test
    void testFeeShouldSupportCustomFeeTypeForCreditAccount() {
        FundsAccountId payer = creditAccount("credit_001");

        service.fee(new FundsTransactionFeeRequest()
                .setAccountId(payer)
                .setAmount(amount(45L))
                .setFeeType("AUTH_DECLINE_FEE")
                .setBusinessScene("FEE")
                .setBusinessSn("FEE_CREDIT_00000001")
                .setDescription("credit account auth decline fee"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.FEE);
        assertThat(instruction.getContextVariables())
                .containsEntry(FundsInstructionContextKeys.FEE_TYPE, "AUTH_DECLINE_FEE");
        assertThat(leg.getSourceNode().getSubjectRef().getSubjectType().name()).isEqualTo(payer.type());
        assertThat(leg.getSourceNode().getSubjectRef().getSubjectId()).isEqualTo(payer.id());
        assertLeg(leg, RouteLegType.INTERNAL_TRANSFER, LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCode.FEE, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.FEE);
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .contains(RouteParticipantRole.AUTH_HOLDER.name(), RouteParticipantRole.FEE_RECEIVER.name());
        assertNoLimitNodes(route);
    }
}
