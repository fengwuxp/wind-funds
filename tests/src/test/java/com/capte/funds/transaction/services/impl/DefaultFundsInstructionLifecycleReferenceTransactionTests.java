package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.ReferencedInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleResolvedRoute;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.newLifecycleSaver;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.transaction;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsInstructionLifecycleReferenceTransactionTests {

    /**
     * 场景：授权后续事件通过原交易快照引用复用主交易。
     * 输入：referenceType=AUTHORIZATION 的指令和已存在的原资金交易。
     * 输出：生命周期保存结果。
     * 预期：复用原主交易号，不创建新的主交易。
     * 红线：只有交易快照类引用可复用原资金交易，不能把所有 reference 都当作交易引用。
     */
    @Test
    void testBeforePostingShouldReuseReferencedFundsTransactionOnlyForSnapshotReference() {
        FundsTransaction referencedTransaction = transaction();
        AtomicInteger transactionQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new AssertionError("should not create transaction");
                        },
                        query -> transactionQueryIndex.getAndIncrement() == 0 ? referencedTransaction : null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);

        FundsInstructionLifecycleResult result = saver.beforePosting(
                new ReferencedInstruction(FundsInstructionReferenceType.AUTHORIZATION), route, snapshot);

        assertThat(result.getTransactionSn()).isEqualTo(referencedTransaction.getSn());
        assertThat(transactionQueryIndex).hasValue(1);
    }

    /**
     * 场景：解冻或提现等指令携带冻结单引用进入 beforePosting。
     * 输入：referenceType=FREEZE_ORDER 的指令和新的 route snapshot。
     * 输出：新创建的资金交易号和 referenceTransactionSn。
     * 预期：冻结单引用不复用原资金交易，只在新交易上保存引用关系。
     * 红线：冻结单不是消费交易，不能把 FREEZE_ORDER 引用当作 AUTHORIZATION 交易复用。
     */
    @Test
    void testBeforePostingShouldNotReuseFundsTransactionForFreezeOrderReference() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        AtomicInteger transactionQueryIndex = new AtomicInteger();
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            FundsTransaction transaction = (FundsTransaction) entity;
                            transaction.setId(501L);
                            insertedTransaction.set(transaction);
                        },
                        query -> {
                            transactionQueryIndex.incrementAndGet();
                            return null;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> ((FundsTransactionDetail) entity).setId(502L),
                        query -> null
                )
        );
        ResolvedRouteSpec route = new SimpleResolvedRoute(1_000L);
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(route);

        FundsInstructionLifecycleResult result = saver.beforePosting(
                new ReferencedInstruction(FundsInstructionReferenceType.FREEZE_ORDER), route, snapshot);

        assertThat(result.getTransactionSn()).isEqualTo(insertedTransaction.get().getSn());
        assertThat(insertedTransaction.get().getReferenceTransactionSn()).isEqualTo("FT_001");
        assertThat(transactionQueryIndex).hasValue(1);
    }
}
