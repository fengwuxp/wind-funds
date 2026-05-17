package com.capte.funds.route;

import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.assertNoLimitNodes;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.authorizationSettlementSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.authorizationSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.replayRequest;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultRouteReplayAuthorizationTests {

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testReplayAuthorizationSettlementShouldConsumeControlAndFundingSubjects() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.AUTHORIZATION_SETTLEMENT, FundsRouteTestSupport.amount(300L)));

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_SETTLE_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.SETTLE);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.SETTLEMENT);
        assertNoLimitNodes(route);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getPhaseCode())
                .containsOnly(LedgerPhaseCode.SETTLEMENT);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.CONSUME);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getReplayRefLegId())
                .containsExactly("AUTHORIZATION_1", "AUTHORIZATION_2", "AUTHORIZATION_3");
        assertThat(route.getParticipants()).hasSize(4);
        assertThat(route.getParticipants())
                .filteredOn(participant -> participant.getSubjectRef().getSubjectId().equals("funding_001"))
                .singleElement()
                .satisfies(participant -> assertThat(participant.getParticipantRole())
                        .isEqualTo(RouteParticipantRole.REAL_FUNDING_SOURCE));
    }

    @Test
    void testReplayAuthorizationRefundShouldRestoreOriginalRoute() {
        RouteSnapshotSpec captureSnapshot = authorizationSettlementSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(captureSnapshot,
                replayRequest(RouteReplayType.AUTHORIZATION_REFUND, FundsRouteTestSupport.amount(100L)));

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REFUND_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
        assertThat(route.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.RESTORE);
        assertNoLimitNodes(route);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AVAILABLE);
    }

    @Test
    void testReplayChargebackShouldUseIndependentChargebackPhase() {
        RouteSnapshotSpec captureSnapshot = authorizationSettlementSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(captureSnapshot,
                replayRequest(RouteReplayType.CHARGEBACK, FundsRouteTestSupport.amount(100L)));

        assertThat(route.getRouteCode()).isEqualTo("CHARGEBACK_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(route.getTransactionType()).isEqualTo(DefaultFundsTransactionType.REFUND);
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.RESTORE);
        assertNoLimitNodes(route);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getPhaseCode())
                .containsOnly(LedgerPhaseCode.CHARGEBACK);
    }
}
