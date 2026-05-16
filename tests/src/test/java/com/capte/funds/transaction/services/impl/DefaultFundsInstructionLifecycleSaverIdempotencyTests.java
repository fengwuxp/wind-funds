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
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /**
     * 场景：同一资金事件重放时持久化流水、展示文案和 traceId 重新生成。
     * 输入：同一 FundsInstruction 和同一路由语义，第二次仅替换 transactionSn、route/participant/subject 展示文案与 traceId。
     * 输出：交易明细 requestHash。
     * 预期：幂等摘要保持稳定；金额等核心资金事实变化时摘要变化。
     * 红线：幂等摘要不得绑定数据库 ID、持久化流水、审计时间、展示文案或调用链 traceId。
     */
    @Test
    void testDetailRequestHashShouldUseStableBusinessFactsOnly() throws ReflectiveOperationException {
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> null
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> null
                )
        );
        RouteSnapshotSpec snapshot = new DefaultRouteSnapshotFactory().createSnapshot(
                new DescribedResolvedRoute(1_000L, "initial route", "first detail", "holder", "TRACE_001"));
        RouteSnapshotSpec replaySnapshot = new DefaultRouteSnapshotFactory().createSnapshot(
                new DescribedResolvedRoute(1_000L, "replayed route", "retry detail", "renamed holder", "TRACE_002"));
        RouteSnapshotSpec changedSnapshot = new DefaultRouteSnapshotFactory().createSnapshot(
                new DescribedResolvedRoute(2_000L, "replayed route", "retry detail", "renamed holder", "TRACE_002"));

        String originalHash = computeDetailRequestHash(saver, new SimpleInstruction(), snapshot,
                snapshot.getParticipants().getFirst());
        String replayHash = computeDetailRequestHash(saver, new SimpleInstruction(), replaySnapshot,
                replaySnapshot.getParticipants().getFirst());
        String changedHash = computeDetailRequestHash(saver, new SimpleInstruction(), changedSnapshot,
                changedSnapshot.getParticipants().getFirst());

        assertThat(replayHash).isEqualTo(originalHash);
        assertThat(changedHash).isNotEqualTo(originalHash);
    }

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

    private static String computeDetailRequestHash(DefaultFundsInstructionLifecycleSaver saver,
                                                   SimpleInstruction instruction,
                                                   RouteSnapshotSpec snapshot,
                                                   RouteParticipantSpec participant)
            throws ReflectiveOperationException {
        Method method = DefaultFundsInstructionLifecycleSaver.class.getDeclaredMethod("computeDetailRequestHash",
                com.wind.integration.funds.spec.transaction.FundsInstructionSpec.class,
                RouteSnapshotSpec.class,
                RouteParticipantSpec.class);
        method.setAccessible(true);
        return (String) method.invoke(saver, instruction, snapshot, participant);
    }

    private static final class DescribedResolvedRoute extends SimpleResolvedRoute {

        private final String routeDescription;

        private final String participantDescription;

        private final String subjectName;

        private final String traceId;

        private DescribedResolvedRoute(long amount,
                                       String routeDescription,
                                       String participantDescription,
                                       String subjectName,
                                       String traceId) {
            super(amount);
            this.routeDescription = routeDescription;
            this.participantDescription = participantDescription;
            this.subjectName = subjectName;
            this.traceId = traceId;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(new DescribedParticipant(getAmount(), participantDescription, subjectName, traceId));
        }

        @Override
        public @Nullable String getDescription() {
            return routeDescription;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of("traceId", traceId);
        }

        private long getAmount() {
            return getLegs().getFirst().getAmount().getAmount();
        }
    }

    private static final class DescribedParticipant implements RouteParticipantSpec {

        private final long amount;

        private final String description;

        private final String subjectName;

        private final String traceId;

        private DescribedParticipant(long amount, String description, String subjectName, String traceId) {
            this.amount = amount;
            this.description = description;
            this.subjectName = subjectName;
            this.traceId = traceId;
        }

        @Override
        public @NonNull RouteParticipantRole getParticipantRole() {
            return RouteParticipantRole.AUTH_HOLDER;
        }

        @Override
        public @NonNull SubjectRef getSubjectRef() {
            return new DescribedSubjectRef(subjectName);
        }

        @Override
        public @Nullable String getLedgerProfileCode() {
            return "CREDIT_BASIC";
        }

        @Override
        public @Nullable String getCurrency() {
            return CurrencyIsoCode.USD.name();
        }

        @Override
        public @Nullable Money getAmount() {
            return Money.immutable(amount, CurrencyIsoCode.USD);
        }

        @Override
        public @Nullable String getDescription() {
            return description;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of("traceId", traceId);
        }
    }

    private static final class DescribedSubjectRef extends SimpleSubjectRef {

        private final String subjectName;

        private DescribedSubjectRef(String subjectName) {
            super("credit_001", FundsSubjectType.CREDIT_ACCOUNT);
            this.subjectName = subjectName;
        }

        @Override
        public @Nullable String getSubjectName() {
            return subjectName;
        }
    }
}
