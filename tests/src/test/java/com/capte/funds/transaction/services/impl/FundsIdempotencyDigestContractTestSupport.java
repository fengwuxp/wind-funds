package com.capte.funds.transaction.services.impl;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleInstruction;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleResolvedRoute;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.SimpleSubjectRef;
import static com.capte.funds.transaction.services.impl.FundsInstructionLifecycleSaverTestSupport.newLifecycleSaver;

abstract class FundsIdempotencyDigestContractTestSupport {

    protected static IdempotencyFixture createFixture(DescribedResolvedRoute route) {
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

    protected record IdempotencyFixture(AtomicReference<FundsTransaction> insertedTransaction,
                                        List<FundsTransactionDetail> insertedDetails) {

        protected DefaultFundsInstructionLifecycleSaver reuseSaver() {
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

    protected static final class DescribedResolvedRoute extends SimpleResolvedRoute {

        private final long amount;

        private final String routeDescription;

        private final String participantDescription;

        private final String subjectName;

        private final String traceId;

        protected DescribedResolvedRoute(long amount,
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
