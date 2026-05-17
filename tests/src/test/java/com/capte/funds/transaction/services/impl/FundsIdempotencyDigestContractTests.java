package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.copySnapshotWithMetadata;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 资金交易幂等摘要契约测试。
 */
class FundsIdempotencyDigestContractTests extends FundsIdempotencyDigestContractTestSupport {

    /**
     * 场景：同一资金事件重放时快照流水、审计时间、展示文案和 traceId 重新生成。
     * 输入：同一 FundsInstruction 和同一路由语义，第二次仅替换 snapshotId、resolvedAt、expiresAt、描述和 traceId。
     * 输出：生命周期保存结果。
     * 预期：requestHash 只绑定稳定业务事实和路由语义，幂等复用既有明细。
     * 红线：幂等摘要不得绑定数据库 ID、持久化流水、审计时间、展示文案或调用链 traceId。
     */
    @Test
    void testIdempotencyDigestShouldExcludePersistenceAuditDisplayTextAndTraceId() {
        IdempotencyFixture fixture = createFixture(new DescribedResolvedRoute(1_000L,
                "initial route", "first detail", "holder", "TRACE_001"));
        RouteSnapshotSpec replaySnapshot = copySnapshotWithMetadata(
                new DefaultRouteSnapshotFactory().createSnapshot(new DescribedResolvedRoute(1_000L,
                        "replayed route", "retry detail", "renamed holder", "TRACE_002")),
                "AUTH_BUSINESS_0001_ROUTE_RETRY",
                LocalDateTime.of(2026, 5, 9, 12, 1),
                LocalDateTime.of(2026, 5, 10, 12, 1));

        FundsInstructionLifecycleResult result = fixture.reuseSaver().beforePosting(new SimpleInstruction(),
                new DescribedResolvedRoute(1_000L, "replayed route", "retry detail", "renamed holder", "TRACE_002"),
                replaySnapshot);

        assertThat(result.getTransactionSn()).isEqualTo(fixture.insertedTransaction().get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(fixture.insertedDetails().get(0).getSn());
    }

}
