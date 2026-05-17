package com.capte.funds.transaction.application;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FundsAuthorizationReplayCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

    /**
     * 场景：授权交易问询后发起部分撤销。
     * 输入：原授权 route snapshot 金额 600，本次撤销 200。
     * 输出：授权撤销 replay route。
     * 预期：只释放本次请求金额，原授权剩余额度仍由后续授权生命周期处理。
     * 红线：回放不得把部分撤销误处理为原授权全额释放。
     */
    @Test
    void testReversalShouldBuildAuthorizationReleaseRoute() {
        FundsAccountId credit = creditAccount("credit_001");
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalAuthorizationSnapshot());

        service.reversal(new FundsAuthorizationTransactionReversalRequest()
                .setAccountId(credit)
                .setAmount(amount(200L))
                .setAuthorizationTransactionSn("AUTH_TX_00000001")
                .setBusinessScene("CARD_REVERSAL")
                .setBusinessSn("REVERSAL_00000001")
                .setDescription("reversal"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.REVERSAL);
        assertThat(instruction.getReference().getReferenceType()).isEqualTo(FundsInstructionReferenceType.AUTHORIZATION);
        assertThat(instruction.getReference().getReferenceSn()).isEqualTo("AUTH_TX_00000001");
        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REVERSAL_REPLAY");
        assertThat(leg.getAmount()).isEqualTo(amount(200L));
        assertLeg(leg, RouteLegType.RELEASE, LedgerSubjectCode.AUTHORIZATION,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RELEASE, LedgerPhaseCode.REVERSAL);
    }

    /**
     * 场景：授权交易问询后直接部分结算。
     * 输入：原授权 route snapshot 金额 600，本次结算 500。
     * 输出：授权结算 replay route。
     * 预期：只捕获本次结算金额，并从 AUTHORIZATION 转入平台 SETTLEMENT。
     * 红线：普通授权结算不得触碰 LIMIT，也不得把部分结算误回放为原授权全额。
     */
    @Test
    void testSettleShouldBuildAuthorizationCaptureRoute() {
        FundsAccountId credit = creditAccount("credit_001");
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalAuthorizationSnapshot());

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
        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_REPLAY");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name(),
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name());
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(leg.getAmount()).isEqualTo(amount(500L));
        assertLeg(leg, RouteLegType.CONSUME, LedgerSubjectCode.AUTHORIZATION,
                LedgerSubjectCode.SETTLEMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
        assertNoLimitNodes(route);
    }

    /**
     * 场景：授权结算交易问询后发起部分退款。
     * 输入：原结算 route snapshot 金额 200，本次退款 80。
     * 输出：授权退款 replay route。
     * 预期：只按本次退款金额从平台 SETTLEMENT 恢复到信用主体 AVAILABLE。
     * 红线：授权退款必须区别于普通退款，且不得把部分退款误回放为原结算全额。
     */
    @Test
    void testSettleRefundShouldBuildAuthorizationRefundRoute() {
        FundsAccountId credit = creditAccount("credit_001");
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalSettlementSnapshot());

        service.settleRefund(new FundsAuthorizationTransactionRefundRequest()
                .setAccountId(credit)
                .setAmount(amount(80L))
                .setAuthorizationTransactionSn("AUTH_TX_00000001")
                .setBusinessScene("CARD_REFUND")
                .setBusinessSn("AUTH_REFUND_00000001")
                .setDescription("auth refund"), WindOperator.system());

        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = route();
        assertThat(instruction.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
        assertThat(instruction.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REFUND_REPLAY");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name(),
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name());
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(leg.getAmount()).isEqualTo(amount(80L));
        assertLeg(leg, RouteLegType.RESTORE, LedgerSubjectCode.SETTLEMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RESTORE, LedgerPhaseCode.REFUND);
        assertNoLimitNodes(route);
    }

    /**
     * 场景：授权结算交易问询后发起结算后拒付/争议。
     * 输入：原结算 route snapshot 金额 200，本次拒付 100。
     * 输出：chargeback replay route。
     * 预期：拒付按争议金额恢复信用主体可用余额，事件和 phase 保持 CHARGEBACK。
     * 红线：授权拒付不得复用授权拒绝语义，也不得触碰 LIMIT。
     */
    @Test
    void testChargebackShouldBuildPostSettlementDisputeReplayInstruction() {
        FundsAccountId credit = creditAccount("credit_001");
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalSettlementSnapshot());

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
        ResolvedRouteSpec route = route();
        RouteLegSpec leg = route.getLegs().getFirst();
        assertThat(route.getRouteCode()).isEqualTo("CHARGEBACK_REPLAY");
        assertThat(leg.getAmount()).isEqualTo(amount(100L));
        assertLeg(leg, RouteLegType.RESTORE, LedgerSubjectCode.SETTLEMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RESTORE, LedgerPhaseCode.CHARGEBACK);
        assertNoLimitNodes(route);
    }
}
