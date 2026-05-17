package com.capte.funds.route;

import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.replayRequest;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.transferWithFeeSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultRouteReplayDirectRefundTests {

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testReplayRefundShouldExcludeFeeLegsByDefault() {
        RouteSnapshotSpec snapshot = transferWithFeeSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.REFUND, FundsTransactionEventType.REFUND,
                        FundsRouteTestSupport.amount(500L)));

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.RESTORE);
            assertThat(leg.getReplayRefLegId()).isEqualTo("TRANSFER");
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getSubjectRef().getSubjectId())
                .containsExactlyInAnyOrder("funding_001", "funding_002");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getAmount().getAmount())
                .containsOnly(500L);
    }

    @Test
    void testReplayFeeRefundShouldOnlyIncludeFeeLegs() {
        RouteSnapshotSpec snapshot = transferWithFeeSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.FEE_REFUND, FundsTransactionEventType.FEE_REFUND,
                        FundsRouteTestSupport.amount(30L)));

        assertThat(route.getRouteCode()).isEqualTo("DIRECT_REFUND_REPLAY");
        assertThat(route.getEventType()).isEqualTo(FundsTransactionEventType.FEE_REFUND);
        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getLegType()).isEqualTo(RouteLegType.RESTORE);
            assertThat(leg.getReplayRefLegId()).isEqualTo("FEE");
            assertThat(leg.getSourceNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.FEE);
            assertThat(leg.getTargetNode().getLedgerSubjectCode()).isEqualTo(LedgerSubjectCode.AVAILABLE);
        });
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getSubjectRef().getSubjectId())
                .containsExactlyInAnyOrder("funding_001", "platform_fee");
        assertThat(route.getParticipants())
                .extracting(participant -> participant.getAmount().getAmount())
                .containsOnly(30L);
        assertThat(route.getParticipants())
                .filteredOn(participant -> participant.getSubjectRef().getSubjectId().equals("platform_fee"))
                .singleElement()
                .satisfies(participant -> assertThat(participant.getParticipantRole())
                        .isEqualTo(RouteParticipantRole.FEE_RECEIVER));
    }
}
