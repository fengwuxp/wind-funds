package com.capte.funds.route;

import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.authorizationSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.replayRequest;

class DefaultRouteReplayServiceTests {

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testReplayReleaseShouldReverseAuthorizationLegs() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L)));

        assertThat(route.getRouteCode()).isEqualTo("AUTHORIZATION_REVERSAL_REPLAY");
        assertThat(route.getLegs()).hasSize(3);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getLegType())
                .containsOnly(RouteLegType.RELEASE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getSourceNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AUTHORIZATION);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getTargetNode().getLedgerSubjectCode())
                .containsOnly(LedgerSubjectCode.AVAILABLE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getBalanceEffectType())
                .containsOnly(LedgerBalanceEffectType.RELEASE);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getAmount().getAmount())
                .containsOnly(200L);
    }
}
