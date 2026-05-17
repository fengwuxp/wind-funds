package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.*;
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
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver createSaver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);
        createSaver.beforePosting(new SimpleInstruction(), route, snapshot);
        insertedDetails.forEach(detail -> {
            detail.setStatus(FundsTransactionDetailStatus.SUCCEEDED);
            detail.setLedgerTransactionSn("LE_001");
        });
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver reuseSaver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedTransaction.get()
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedDetails.get(detailQueryIndex.getAndIncrement())
                )
        );

        FundsInstructionLifecycleResult result = reuseSaver.beforePosting(new SimpleInstruction(), route, snapshot);

        assertThat(result.getTransactionSn()).isEqualTo(insertedTransaction.get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(insertedDetails.get(0).getSn(), insertedDetails.get(1).getSn());
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
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver createSaver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);
        createSaver.beforePosting(new SimpleInstruction(), route, snapshot);
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver reuseSaver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedTransaction.get()
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedDetails.get(detailQueryIndex.getAndIncrement())
                )
        );
        ResolvedRouteSpec changedRoute = new SimpleResolvedRoute(2_000L);
        RouteSnapshotSpec changedSnapshot = new DefaultRouteSnapshotFactory().createSnapshot(changedRoute);

        assertThatThrownBy(() -> reuseSaver.beforePosting(new SimpleInstruction(), changedRoute, changedSnapshot))
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
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver createSaver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            FundsTransactionDetail detail = (FundsTransactionDetail) entity;
                            detail.setId(502L + insertedDetails.size());
                            insertedDetails.add(detail);
                        },
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);
        createSaver.beforePosting(new SimpleInstruction(), route, snapshot);
        AtomicInteger detailQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver reuseSaver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedTransaction.get()
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> insertedDetails.get(detailQueryIndex.getAndIncrement())
                )
        );
        RouteSnapshotSpec regeneratedSnapshot = copySnapshotWithMetadata(snapshot, "AUTH_BUSINESS_0001_ROUTE_RETRY",
                LocalDateTime.of(2026, 5, 9, 12, 1), LocalDateTime.of(2026, 5, 10, 12, 1));

        FundsInstructionLifecycleResult result = reuseSaver.beforePosting(new SimpleInstruction(), route,
                regeneratedSnapshot);

        assertThat(result.getTransactionSn()).isEqualTo(insertedTransaction.get().getSn());
        assertThat(result.getTransactionDetailSns())
                .containsExactly(insertedDetails.get(0).getSn(), insertedDetails.get(1).getSn());
    }

}
