package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.enums.FundsTransactionChannel;
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

class FundsTransactionCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

    /**
     * 场景：外部银行账户向资金账户充值。
     * 输入：目标 FundingAccount、外部银行账户、WIRE_TRANSFER 通道和充值金额 1000。
     * 输出：DIRECT_TRANSACTION / TOPUP 资金指令，以及 FUND_IN + SETTLEMENT 两段 route。
     * 预期：外部入金先进入平台 CASH/PREPAYMENT，再结转到目标账户 AVAILABLE，外部账户引用保留。
     */
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

    /**
     * 场景：付款请求携带业务层已决策的原始币种金额和账户记账金额。
     * 输入：付款金额 USD 1100、原始业务金额 EUR 1000。
     * 输出：资金指令和 route leg 同步保留 amount、originalAmount 和 exchangeRate。
     * 预期：交易门面只传递换汇事实快照，不在本层重新调用 FX 决策。
     */
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

    /**
     * 场景：用户基于冻结单确认提现出款。
     * 输入：付款 FundingAccount、外部银行收款账户、冻结单引用和提现金额 800。
     * 输出：WITHDRAW 资金指令，以及 SETTLEMENT 消耗冻结余额 + FUND_OUT 外部出金 route。
     * 预期：冻结单引用进入资金指令，提现只扣减 FROZEN 并流向外部账户。
     */
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

    /**
     * 场景：两个内部资金账户之间发生普通转账。
     * 输入：付款 FundingAccount、收款 FundingAccount 和转账金额 500。
     * 输出：TRANSFER 资金指令和单段 INTERNAL_TRANSFER route。
     * 预期：转账只在付款方 AVAILABLE 与收款方 AVAILABLE 之间迁移，不生成外部出入金路径。
     */
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

    /**
     * 场景：资金账户付款给商户并进入商户待结算余额。
     * 输入：付款 FundingAccount、商户 FundingAccount、收款账目 SETTLEMENT 和付款金额 700。
     * 输出：PAY 资金指令和单段 INTERNAL_TRANSFER route。
     * 预期：付款方 AVAILABLE 扣减，商户 SETTLEMENT 增加。
     */
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
     * 场景：商户对已结算口径的付款发起退款。
     * 输入：收款用户、付款商户、商户扣减账目 SETTLEMENT 和退款金额 300。
     * 输出：REFUND 资金指令和 RESTORE route。
     * 预期：退款从商户 SETTLEMENT 回退到用户 AVAILABLE。
     */
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
}
