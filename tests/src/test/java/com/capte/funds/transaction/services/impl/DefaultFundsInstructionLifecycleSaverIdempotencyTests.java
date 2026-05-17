package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.BeforePostingReplayFixture;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleResolvedRoute;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.beforePostingReplayFixture;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.copySnapshotWithMetadata;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.routeSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFundsInstructionLifecycleSaverIdempotencyTests {

    /**
     * 场景：同一业务事件重复进入 beforePosting，历史明细已成功入账。
     * 输入：首次创建的交易与明细，明细状态均为 SUCCEEDED 且绑定账本交易号。
     * 输出：生命周期保存结果。
     * 预期：复用既有交易和明细，并返回 completed=true。
     * 红线：已完成事件不得再次创建交易明细或重复入账。
     */
    @Test
    void testBeforePostingShouldReuseCompletedDetails() {
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        BeforePostingReplayFixture fixture = beforePostingReplayFixture(route);
        fixture.insertedDetails().forEach(detail -> {
            detail.setStatus(FundsTransactionDetailStatus.SUCCEEDED);
            detail.setLedgerTransactionSn("LE_001");
        });

        FundsInstructionLifecycleResult result = fixture.reuseSaver().beforePosting(new SimpleInstruction(), route,
                fixture.snapshot());

        assertThat(result.getTransactionSn()).isEqualTo(fixture.insertedTransaction().get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(fixture.insertedDetails().get(0).getSn(), fixture.insertedDetails().get(1).getSn());
        assertThat(result.getLedgerTransactionSn()).isEqualTo("LE_001");
        assertThat(result.isCompleted()).isTrue();
    }

    /**
     * 场景：同一业务事件重放时路由语义发生变化。
     * 输入：首次保存 1000 金额路径，第二次用 2000 金额路径重试。
     * 输出：请求参数不一致异常。
     * 预期：拒绝复用或覆盖既有交易明细。
     * 红线：同一业务流水不得静默切换历史资金路径。
     */
    @Test
    void testBeforePostingShouldRejectChangedRouteForSameBusinessEvent() {
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        BeforePostingReplayFixture fixture = beforePostingReplayFixture(route);
        ResolvedRouteSpec changedRoute = new SimpleResolvedRoute(2_000L);
        RouteSnapshotSpec changedSnapshot = routeSnapshot(changedRoute);

        assertThatThrownBy(() -> fixture.reuseSaver().beforePosting(new SimpleInstruction(), changedRoute,
                changedSnapshot))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金交易明细请求参数不一致");
    }

    /**
     * 场景：同一业务流水重复送达，但本次 RouteSnapshot 的快照号和解析时间重新生成。
     * 输入：同一 FundsInstruction 和同一路由语义，第二次仅替换 snapshotId、resolvedAt、expiresAt。
     * 输出：生命周期保存结果。
     * 预期：requestHash 只绑定稳定业务事实和路由语义，忽略快照流水与审计时间后幂等复用既有明细。
     * 红线：幂等摘要不得绑定 route snapshot 临时流水或审计时间。
     */
    @Test
    void testBeforePostingShouldIgnoreRouteSnapshotMetadataWhenComparingRequestHash() {
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        BeforePostingReplayFixture fixture = beforePostingReplayFixture(route);
        RouteSnapshotSpec regeneratedSnapshot = copySnapshotWithMetadata(fixture.snapshot(),
                "AUTH_BUSINESS_0001_ROUTE_RETRY", LocalDateTime.of(2026, 5, 9, 12, 1),
                LocalDateTime.of(2026, 5, 10, 12, 1));

        FundsInstructionLifecycleResult result = fixture.reuseSaver().beforePosting(new SimpleInstruction(), route,
                regeneratedSnapshot);

        assertThat(result.getTransactionSn()).isEqualTo(fixture.insertedTransaction().get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(fixture.insertedDetails().get(0).getSn(), fixture.insertedDetails().get(1).getSn());
    }

}
