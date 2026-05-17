package com.capte.funds.transaction;

import com.capte.domain.core.operator.WindOperator;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionAuthorizeRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionChargebackRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionRefundRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionReversalRequest;
import com.capte.funds.transaction.model.request.FundsAuthorizationTransactionSettleRequest;
import com.capte.funds.transaction.model.request.TransactionAmount;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
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

class FundsAuthorizationTransactionCommandServiceImplTests extends FundsTransactionCommandServiceImplTestSupport {

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
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalAuthorizationSnapshot());

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
        assertLeg(route.getLegs().getFirst(), RouteLegType.CONSUME, LedgerSubjectCode.AUTHORIZATION,
                LedgerSubjectCode.SETTLEMENT, LedgerBalanceEffectType.CONSUME, LedgerPhaseCode.SETTLEMENT);
        assertNoLimitNodes(route);
    }

    @Test
    void testSettleRefundShouldBuildAuthorizationRefundRoute() {
        FundsAccountId credit = creditAccount("credit_001");
        transactionQueryService.routeSnapshots.put("AUTH_TX_00000001", originalSettlementSnapshot());

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
        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REFUND_REPLAY");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getParticipantRole().name())
                .containsExactly(RouteParticipantRole.AUTH_HOLDER.name(),
                        RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT.name());
        assertLeg(route.getLegs().getFirst(), RouteLegType.RESTORE, LedgerSubjectCode.SETTLEMENT,
                LedgerSubjectCode.AVAILABLE, LedgerBalanceEffectType.RESTORE, LedgerPhaseCode.REFUND);
        assertNoLimitNodes(route);
    }

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
        assertNoLimitNodes(route());
    }
}
