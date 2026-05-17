package com.capte.funds.transaction.services.impl;

import com.alibaba.fastjson2.JSON;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsTransactionRouteSnapshotQueryTests extends DefaultFundsTransactionQueryServiceTestSupport {

    /**
     * 场景：后续撤销、结算、退款或拒付需要沿首次保存的 RouteSnapshot 回放。
     * 输入：交易事实已保存带参与方、路径和路由决策的快照。
     * 输出：按交易号解析后的 RouteSnapshotSpec。
     * 预期：快照保留 routeCode、schemaVersion、participants、legs、routingDecision 和账户引用信息。
     * 红线：查询服务只读交易事实，不重新选路也不依赖生命周期写侧实现。
     */
    @Test
    void testFindRouteSnapshotByTransactionSnShouldReadSavedSnapshotForReplay() {
        DefaultFundsTransactionQueryService queryService = queryService(transactionWithRouteSnapshot(),
                List.of(), null);

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByTransactionSn("FT_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getParticipants()).hasSize(2);
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
        assertThat(snapshot.getSnapshotSchemaVersion()).isEqualTo("v4");
        assertThat(snapshot.getRoutingDecision().getPolicyCode()).isEqualTo("LOWEST_COST");
        assertThat(snapshot.getRoutingDecision().getSelectedCashFundingAccount()).isEqualTo("PF_CASH_USD");
        assertThat(snapshot.getRoutingDecision().getSelectedPlatformAccount()).isEqualTo("PF_SETTLEMENT_USD");
        assertThat(snapshot.getRoutingDecision().getFundingAllocations()).singleElement()
                .satisfies(allocation -> {
                    assertThat(allocation.getAllocationId()).isEqualTo("ALLOC_001");
                    assertThat(allocation.getPriority()).isEqualTo(1);
                    assertThat(allocation.getReason()).isEqualTo("default source");
                });
        assertThat(snapshot.getPaymentInstrumentRef().getTenantId()).isEqualTo(1L);
        assertThat(snapshot.getPaymentInstrumentRef().getDescription()).isEqualTo("primary card");
        assertThat(snapshot.getExternalAccountRef().getDescription()).isEqualTo("bank account");
        assertThat(snapshot.getExternalAccountRef().getContextVariables())
                .containsEntry("externalTransactionId", "EXT_001");
        assertThat(snapshot.getPlatformAccounts().getCashFundingAccount().getSubjectId())
                .isEqualTo("platform_cash_usd");
        assertThat(snapshot.getPlatformAccounts().getAdjustmentFundingAccount().getSubjectId())
                .isEqualTo("platform_adjustment_usd");
        assertThat(snapshot.getParticipants().getFirst().getSubjectRef().getLedgerProfileCode())
                .isEqualTo("CREDIT_BASIC");
    }

    /**
     * 场景：解冻请求以冻结单号作为引用时，需要定位原冻结交易并回放原冻结路径。
     * 输入：冻结单号绑定原资金交易号，原资金交易保存 RouteSnapshot。
     * 输出：按冻结单号解析得到的 RouteSnapshotSpec。
     * 预期：查询服务返回原冻结路径快照，不在解冻链路重新解析路径。
     * 红线：冻结单引用不得触发 route resolver 或 lifecycle recorder 写侧逻辑。
     */
    @Test
    void testFindRouteSnapshotByFreezeOrderSnShouldReadOriginalTransactionSnapshot() {
        FundsFrozenOrder frozenOrder = new FundsFrozenOrder();
        frozenOrder.setSn("FO_001");
        frozenOrder.setTransactionSn("FT_001");
        DefaultFundsTransactionQueryService queryService = queryService(transactionWithRouteSnapshot(),
                List.of(), frozenOrder);

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByFreezeOrderSn("FO_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
    }

    /**
     * 场景：冻结单自身已经保存 RouteSnapshot，且不再绑定标准资金交易号。
     * 输入：冻结单 contextVariables 中带有原冻结 RouteSnapshot。
     * 输出：按冻结单号解析得到的 RouteSnapshotSpec。
     * 预期：查询服务优先从冻结单事实自身读取快照，不再依赖 FundsTransaction。
     * 红线：冻结单已有快照时不得额外查询或回写标准资金交易。
     */
    @Test
    void testFindRouteSnapshotByFreezeOrderSnShouldReadFreezeOrderOwnSnapshotFirst() {
        RouteSnapshotSpec routeSnapshot = routeSnapshot();
        FundsFrozenOrder frozenOrder = new FundsFrozenOrder();
        frozenOrder.setSn("FO_001");
        frozenOrder.setContextVariables(JSON.toJSONString(Map.of(
                FundsInstructionContextKeys.ROUTE_SNAPSHOT, RouteSnapshotJsonSupport.toRouteSnapshotJson(routeSnapshot)
        )));
        DefaultFundsTransactionQueryService queryService = queryServiceRejectingTransactionLookup(frozenOrder);

        RouteSnapshotSpec snapshot = queryService.findRouteSnapshotByFreezeOrderSn("FO_001").orElseThrow();

        assertThat(snapshot.getRouteCode()).isEqualTo("CARD_AUTH");
        assertThat(snapshot.getLegs()).hasSize(1);
        assertThat(snapshot.getLegs().getFirst().getLegId()).isEqualTo("LEG_001");
    }
}
