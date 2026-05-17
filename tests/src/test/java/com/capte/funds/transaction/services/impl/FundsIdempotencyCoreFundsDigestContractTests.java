package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.junit.jupiter.api.Test;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金交易核心事实幂等摘要契约测试。
 */
class FundsIdempotencyCoreFundsDigestContractTests extends FundsIdempotencyDigestContractTestSupport {

    /**
     * 场景：同一业务流水重复提交，但资金核心事实发生变化。
     * 输入：首次保存 1000 金额路径，第二次使用 2000 金额路径重试。
     * 输出：请求参数不一致异常。
     * 预期：幂等摘要变化并拒绝复用历史明细。
     * 红线：不得因排除了易变字段而忽略金额、币种、主体和 route 语义变化。
     */
    @Test
    void testIdempotencyDigestShouldIncludeCoreFundsFacts() {
        IdempotencyFixture fixture = createFixture(new DescribedResolvedRoute(1_000L,
                "initial route", "first detail", "holder", "TRACE_001"));
        RouteSnapshotSpec changedSnapshot = new DefaultRouteSnapshotFactory().createSnapshot(
                new DescribedResolvedRoute(2_000L, "replayed route", "retry detail", "renamed holder", "TRACE_002"));

        assertThatThrownBy(() -> fixture.reuseSaver().beforePosting(new SimpleInstruction(),
                new DescribedResolvedRoute(2_000L, "replayed route", "retry detail", "renamed holder", "TRACE_002"),
                changedSnapshot))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("资金交易明细请求参数不一致");
    }
}
