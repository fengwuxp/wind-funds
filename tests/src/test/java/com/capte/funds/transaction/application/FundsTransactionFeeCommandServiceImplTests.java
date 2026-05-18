package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRefundRequest;
import com.capte.funds.transaction.model.request.FundsTransactionFeeRequest;
import com.capte.funds.transaction.model.request.FundsTransactionPayRequest;
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
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.integration.funds.wallet.FundsAccountId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FundsTransactionFeeCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

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

    /**
     * 场景：资金账户发生独立手续费扣收。
     * 输入：FundingAccount、手续费金额 30、默认 feeType。
     * 输出：FEE 资金交易和独立手续费 route。
     * 预期：费用从付款方 AVAILABLE 扣到平台 FEE，不触碰信用 LIMIT。
     */
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

    /**
     * 场景：对原手续费交易发起手续费退回。
     * 输入：原 FEE 交易快照、退款金额 30 和原手续费交易流水。
     * 输出：FEE_REFUND 指令引用原手续费交易，并按原费用 leg 回放。
     * 预期：只把平台 FEE 回退到付款方 AVAILABLE，不重放付款本金路径。
     */
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

        service.refundFee(new FundsTransactionFeeRefundRequest()
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
