package com.capte.funds.route;

import com.wind.common.exception.BaseException;
import com.wind.integration.funds.model.route.ImmutableReplayRequestSpec;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.enums.RouteReplayType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.authorizationSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.copyLegWithExchangeSnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.copyLegWithReplayPolicy;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.copySnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.fullOnlySnapshot;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.replayRequest;
import static com.capte.funds.route.DefaultRouteReplayServiceTestSupport.systemActor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRouteReplayPolicyTests {

    @BeforeEach
    void testSetUp() {
        FundsRouteTestSupport.bindTenant();
    }

    @AfterEach
    void testTearDown() {
        FundsRouteTestSupport.clearTenant();
    }

    @Test
    void testReplayShouldRejectPartialAmountWhenLegRequiresFullReplay() {
        RouteSnapshotSpec snapshot = fullOnlySnapshot();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(100L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("仅支持全量回放");
    }

    @Test
    void testReplayOnceShouldAllowOnlyFullReplayAndKeepSourceLegReference() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteLegSpec sourceLeg = copyLegWithReplayPolicy(sourceSnapshot.getLegs().getFirst(),
                RouteReplayPolicy.REPLAY_ONCE);
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of(sourceLeg));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, sourceLeg.getAmount()));

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getReplayPolicy()).isEqualTo(RouteReplayPolicy.REPLAY_ONCE);
            assertThat(leg.getReplayRefLegId()).isEqualTo(sourceLeg.getLegId());
            assertThat(leg.getAmount()).isEqualTo(sourceLeg.getAmount());
        });
    }

    @Test
    void testReplayOnceShouldRejectPartialReplayAmount() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteLegSpec sourceLeg = copyLegWithReplayPolicy(sourceSnapshot.getLegs().getFirst(),
                RouteReplayPolicy.REPLAY_ONCE);
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of(sourceLeg));

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, Money.immutable(
                        sourceLeg.getAmount().getAmount() - 1, sourceLeg.getAmount().getCurrency()))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("仅支持全量回放");
    }

    @Test
    void testReplayShouldSkipNonReplayableLegs() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();
        RouteLegSpec nonReplayableLeg = copyLegWithReplayPolicy(snapshot.getLegs().getFirst(),
                RouteReplayPolicy.NON_REPLAYABLE);
        RouteSnapshotSpec adjustedSnapshot = copySnapshot(snapshot, snapshot.getSnapshotSchemaVersion(),
                List.of(nonReplayableLeg, snapshot.getLegs().get(1), snapshot.getLegs().get(2)));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(adjustedSnapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L)));

        assertThat(route.getLegs()).hasSize(2);
        assertThat(route.getLegs())
                .extracting(leg -> leg.getReplayRefLegId())
                .containsExactly("AUTHORIZATION_2", "AUTHORIZATION_3");
    }

    @Test
    void testReplayShouldRejectAmountGreaterThanSourceLeg() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(700L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("回放金额不能大于原 RouteLeg 金额");
    }

    @Test
    void testReplayShouldRejectDifferentCurrency() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, Money.immutable(100L, CurrencyIsoCode.EUR))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("回放金额币种必须与原 RouteLeg 一致");
    }

    @Test
    void testReplayShouldRejectUnknownSnapshotSchemaVersion() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, "v3", sourceSnapshot.getLegs());

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("snapshotSchemaVersion");
    }

    @Test
    void testReplayShouldRejectEmptySnapshotLegs() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of());

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                replayRequest(RouteReplayType.RELEASE_HOLD, FundsRouteTestSupport.amount(200L))))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("legs 不能为空");
    }

    @Test
    void testReplayShouldRejectPartialMissingReplayLegIds() {
        RouteSnapshotSpec snapshot = authorizationSnapshot();
        String existingLegId = snapshot.getLegs().getFirst().getLegId();

        assertThatThrownBy(() -> new DefaultRouteReplayService().replay(snapshot,
                ImmutableReplayRequestSpec.builder()
                        .replayType(RouteReplayType.RELEASE_HOLD)
                        .businessScene("CARD_REPLAY")
                        .businessSn("PARTIAL_MISSING_LEG_0001")
                        .amount(FundsRouteTestSupport.amount(200L))
                        .originalAmount(FundsRouteTestSupport.amount(200L))
                        .replayLegIds(List.of(existingLegId, "MISSING_LEG"))
                        .eventTime(LocalDateTime.of(2026, 5, 9, 12, 30))
                        .operator(systemActor())
                        .contextVariables(Map.of())
                        .build()))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("leg 不存在或不可回放")
                .hasMessageContaining("MISSING_LEG");
    }

    @Test
    void testReplayShouldReuseOriginalExchangeSnapshotInsteadOfRequote() {
        RouteSnapshotSpec sourceSnapshot = authorizationSnapshot();
        RouteLegSpec sourceLeg = sourceSnapshot.getLegs().getFirst();
        Money sourceOriginalAmount = Money.immutable(540L, CurrencyIsoCode.EUR);
        BigDecimal sourceExchangeRate = new BigDecimal("1.111111");
        RouteLegSpec exchangedLeg = copyLegWithExchangeSnapshot(sourceLeg, sourceOriginalAmount, sourceExchangeRate);
        RouteSnapshotSpec snapshot = copySnapshot(sourceSnapshot, sourceSnapshot.getSnapshotSchemaVersion(),
                List.of(exchangedLeg));

        ResolvedRouteSpec route = new DefaultRouteReplayService().replay(snapshot,
                ImmutableReplayRequestSpec.builder()
                        .replayType(RouteReplayType.RELEASE_HOLD)
                        .businessScene("CARD_REPLAY")
                        .businessSn("REPLAY_EXCHANGE_0001")
                        .amount(FundsRouteTestSupport.amount(200L))
                        .originalAmount(Money.immutable(999L, CurrencyIsoCode.EUR))
                        .exchangeRate(new BigDecimal("9.999999"))
                        .replayLegIds(List.of(sourceLeg.getLegId()))
                        .eventTime(LocalDateTime.of(2026, 5, 9, 12, 30))
                        .operator(systemActor())
                        .contextVariables(Map.of())
                        .build());

        assertThat(route.getLegs()).singleElement().satisfies(leg -> {
            assertThat(leg.getAmount()).isEqualTo(FundsRouteTestSupport.amount(200L));
            assertThat(leg.getOriginalAmount()).isEqualTo(sourceOriginalAmount);
            assertThat(leg.getExchangeRate()).isEqualByComparingTo(sourceExchangeRate);
            assertThat(leg.getReplayRefLegId()).isEqualTo(sourceLeg.getLegId());
        });
    }
}
