package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleResolvedRoute;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleSubjectRef;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.copySnapshotWithMetadata;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.newLifecycleSaver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金交易幂等摘要契约测试。
 */
class FundsIdempotencyDigestContractTests {

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

    private static IdempotencyFixture createFixture(DescribedResolvedRoute route) {
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
        createSaver.beforePosting(new SimpleInstruction(), route,
                new DefaultRouteSnapshotFactory().createSnapshot(route));
        return new IdempotencyFixture(insertedTransaction, insertedDetails);
    }

    private record IdempotencyFixture(AtomicReference<FundsTransaction> insertedTransaction,
                                      List<FundsTransactionDetail> insertedDetails) {

        private DefaultFundsInstructionLifecycleSaver reuseSaver() {
            AtomicInteger detailQueryIndex = new AtomicInteger();
            return newLifecycleSaver(
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
        }
    }

    private static final class DescribedResolvedRoute extends SimpleResolvedRoute {

        private final long amount;

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
            this.amount = amount;
            this.routeDescription = routeDescription;
            this.participantDescription = participantDescription;
            this.subjectName = subjectName;
            this.traceId = traceId;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(new DescribedParticipant(amount, participantDescription, subjectName, traceId));
        }

        @Override
        public @Nullable String getDescription() {
            return routeDescription;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of("traceId", traceId);
        }
    }

    private record DescribedParticipant(long amount,
                                        String description,
                                        String subjectName,
                                        String traceId) implements RouteParticipantSpec {

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
