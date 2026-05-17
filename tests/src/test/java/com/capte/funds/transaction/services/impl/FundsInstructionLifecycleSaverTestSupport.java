package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsTransaction;
import com.capte.funds.transaction.dal.entities.FundsTransactionDetail;
import com.capte.funds.transaction.dal.mapper.FundsTransactionDetailMapper;
import com.capte.funds.transaction.dal.mapper.FundsTransactionMapper;
import com.capte.funds.transaction.enums.FundsEffectType;
import com.capte.funds.transaction.enums.FundsTransactionDetailStatus;
import com.capte.funds.transaction.enums.FundsTransactionMode;
import com.capte.funds.transaction.enums.FundsTransactionStatus;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionReferenceSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionReferenceType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class FundsInstructionLifecycleSaverTestSupport {

    private FundsInstructionLifecycleSaverTestSupport() {
    }

    static FundsTransaction transaction() {
        FundsTransaction transaction = new FundsTransaction();
        transaction.setId(401L);
        transaction.setSn("FT_001");
        transaction.setTenantId(1L);
        transaction.setBusinessScene("CARD_AUTH");
        transaction.setBusinessSn("AUTH_BUSINESS_0001");
        transaction.setTransactionMode(FundsTransactionMode.AUTHORIZATION);
        transaction.setTransactionType(DefaultFundsTransactionType.PAY);
        transaction.setStatus(FundsTransactionStatus.PROCESSING);
        transaction.setAmount(1_000L);
        transaction.setCurrency(CurrencyIsoCode.USD);
        transaction.setAuthorizedAmount(0L);
        transaction.setReversedAmount(0L);
        transaction.setSettledAmount(0L);
        transaction.setRefundedAmount(0L);
        transaction.setDeclinedAmount(0L);
        transaction.setFeeAmount(0L);
        return transaction;
    }

    static FundsTransactionDetail detail(String sn, RouteParticipantRole participantRole) {
        FundsTransactionDetail detail = new FundsTransactionDetail();
        detail.setId(402L);
        detail.setSn(sn);
        detail.setTenantId(1L);
        detail.setTransactionSn("FT_001");
        detail.setBusinessScene("CARD_AUTH");
        detail.setBusinessSn("AUTH_BUSINESS_0001");
        detail.setTransactionType(DefaultFundsTransactionType.PAY);
        detail.setEventType(FundsTransactionEventType.AUTHORIZE);
        detail.setSubjectId(participantRole == RouteParticipantRole.AUTH_HOLDER
                ? "credit_001"
                : "platform_revenue_001");
        detail.setSubjectType(participantRole == RouteParticipantRole.AUTH_HOLDER
                ? "CREDIT_ACCOUNT"
                : "FUNDING_ACCOUNT");
        detail.setParticipantRole(participantRole);
        detail.setRequestHash("same_hash");
        detail.setFundsEffectType(FundsEffectType.HOLD);
        detail.setAmount(1_000L);
        detail.setCurrency(CurrencyIsoCode.USD);
        detail.setStatus(FundsTransactionDetailStatus.PROCESSING);
        return detail;
    }

    static FundsTransactionDetail returnDetail(String sn, FundsTransactionEventType eventType, long amount) {
        FundsTransactionDetail detail = detail(sn, RouteParticipantRole.AUTH_HOLDER);
        detail.setTransactionType(DefaultFundsTransactionType.REFUND);
        detail.setEventType(eventType);
        detail.setFundsEffectType(FundsEffectType.RETURN);
        detail.setAmount(amount);
        return detail;
    }

    static FundsTransactionDetail rejectedAuthorizationDetail(String sn, RouteParticipantRole participantRole) {
        FundsTransactionDetail detail = detail(sn, participantRole);
        detail.setContextVariables("{\"" + FundsInstructionContextKeys.APPROVED + "\":false}");
        return detail;
    }

    static RouteSnapshotSpec copySnapshotWithMetadata(RouteSnapshotSpec snapshot,
                                                      String snapshotId,
                                                      LocalDateTime resolvedAt,
                                                      LocalDateTime expiresAt) {
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(snapshot.getTenantId())
                .snapshotId(snapshotId)
                .snapshotSchemaVersion(snapshot.getSnapshotSchemaVersion())
                .routeCode(snapshot.getRouteCode())
                .routeVersion(snapshot.getRouteVersion())
                .businessScene(snapshot.getBusinessScene())
                .businessSn(snapshot.getBusinessSn())
                .instructionType(snapshot.getInstructionType())
                .eventType(snapshot.getEventType())
                .transactionType(snapshot.getTransactionType())
                .participants(snapshot.getParticipants())
                .legs(snapshot.getLegs())
                .routingDecision(snapshot.getRoutingDecision())
                .paymentInstrumentRef(snapshot.getPaymentInstrumentRef())
                .externalAccountRef(snapshot.getExternalAccountRef())
                .platformAccounts(snapshot.getPlatformAccounts())
                .resolvedAt(resolvedAt)
                .expiresAt(expiresAt)
                .description(snapshot.getDescription())
                .contextVariables(snapshot.getContextVariables())
                .build();
    }

    static Map<String, Object> queryValues(QueryWrapper query) {
        Map<String, Object> result = new LinkedHashMap<>();
        QueryCondition condition = whereCondition(query);
        while (condition != null) {
            if (condition.checkEffective()) {
                QueryColumn column = condition.getColumn();
                if (column != null) {
                    result.put(column.getName(), condition.getValue());
                }
            }
            condition = nextCondition(condition);
        }
        return result;
    }

    private static QueryCondition whereCondition(QueryWrapper query) {
        try {
            Field field = query.getClass().getSuperclass().getDeclaredField("whereQueryCondition");
            field.setAccessible(true);
            return (QueryCondition) field.get(query);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取 QueryWrapper 查询条件失败", exception);
        }
    }

    private static QueryCondition nextCondition(QueryCondition condition) {
        try {
            Field field = QueryCondition.class.getDeclaredField("next");
            field.setAccessible(true);
            return (QueryCondition) field.get(condition);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取 QueryCondition 链路失败", exception);
        }
    }

    static DefaultFundsInstructionLifecycleSaver lifecycleSaver(FundsTransaction transaction,
                                                                FundsTransactionDetail detail,
                                                                AtomicReference<FundsTransaction> updated) {
        return lifecycleSaver(transaction, detail, updated, new AtomicReference<>());
    }

    static DefaultFundsInstructionLifecycleSaver lifecycleSaver(FundsTransaction transaction,
                                                                FundsTransactionDetail detail,
                                                                AtomicReference<FundsTransaction> updated,
                                                                AtomicReference<FundsTransactionDetail> updatedDetail) {
        return new DefaultFundsInstructionLifecycleSaver(
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> transaction,
                        entity -> {
                            updated.set((FundsTransaction) entity);
                            return 1;
                        }
                ),
                FundsAccountServiceTestSupport.mapper(
                        FundsTransactionDetailMapper.class,
                        entity -> {
                            throw new UnsupportedOperationException("insertSelective");
                        },
                        query -> detail,
                        entity -> {
                            updatedDetail.set((FundsTransactionDetail) entity);
                            return 1;
                        }
                )
        );
    }

    static DefaultFundsInstructionLifecycleSaver newLifecycleSaver(
            FundsTransactionMapper fundsTransactionMapper,
            FundsTransactionDetailMapper fundsTransactionDetailMapper) {
        return new DefaultFundsInstructionLifecycleSaver(fundsTransactionMapper, fundsTransactionDetailMapper);
    }

    static BeforePostingFixture beforePostingSaver() {
        AtomicReference<FundsTransaction> insertedTransaction = new AtomicReference<>();
        List<FundsTransactionDetail> insertedDetails = new ArrayList<>();
        DefaultFundsInstructionLifecycleSaver saver = newLifecycleSaver(
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
        return new BeforePostingFixture(saver, insertedTransaction, insertedDetails);
    }

    record BeforePostingFixture(DefaultFundsInstructionLifecycleSaver saver,
                                AtomicReference<FundsTransaction> insertedTransaction,
                                List<FundsTransactionDetail> insertedDetails) {
    }

    static FundsTransaction directTransaction(DefaultFundsTransactionType transactionType) {
        FundsTransaction transaction = transaction();
        transaction.setTransactionMode(FundsTransactionMode.DIRECT);
        transaction.setTransactionType(transactionType);
        return transaction;
    }

    static FundsTransactionDetail directDetail(String sn, RouteParticipantRole participantRole, long amount) {
        FundsTransactionDetail detail = detail(sn, participantRole);
        detail.setTransactionType(DefaultFundsTransactionType.TRANSFER);
        detail.setEventType(FundsTransactionEventType.TRANSFER);
        detail.setFundsEffectType(FundsEffectType.DIRECT);
        detail.setAmount(amount);
        return detail;
    }

    static FundsTransactionDetail withdrawDetail(String sn, long amount) {
        FundsTransactionDetail detail = directDetail(sn, RouteParticipantRole.PAYER, amount);
        detail.setTransactionType(DefaultFundsTransactionType.WITHDRAW);
        detail.setEventType(FundsTransactionEventType.WITHDRAW);
        detail.setFundsEffectType(FundsEffectType.CONSUME);
        return detail;
    }

    static class SimpleInstruction implements FundsInstructionSpec {

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.AUTHORIZATION_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.AUTHORIZE;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.PAY;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(1_000L, CurrencyIsoCode.USD);
        }

        @Override
        public @NonNull Money getOriginalAmount() {
            return getAmount();
        }

        @Override
        public @NonNull BigDecimal getExchangeRate() {
            return BigDecimal.ONE;
        }

        @Override
        public @Nullable PaymentInstrumentRefSpec getInstrumentRef() {
            return null;
        }

        @Override
        public @Nullable ExternalAccountRefSpec getExternalAccountRef() {
            return null;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return null;
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "AUTH_BUSINESS_0001";
        }

        @Override
        public @NonNull LocalDateTime getEventTime() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @Nullable String getDescription() {
            return "auth";
        }

        @Override
        public @NonNull FundsOperationActorSpec getOperator() {
            return systemActor();
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static final class BalanceControlInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        BalanceControlInstruction(FundsTransactionEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.BALANCE_CONTROL;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }
    }

    static final class TransactionTypeChangedInstruction extends SimpleInstruction {

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.REFUND;
        }
    }

    static FundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }

    static final class ReferencedInstruction extends SimpleInstruction {

        private final FundsInstructionReferenceType referenceType;

        ReferencedInstruction(FundsInstructionReferenceType referenceType) {
            this.referenceType = referenceType;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return new SimpleReference(referenceType);
        }
    }

    static final class FreezeOrderReferencedWithdrawInstruction extends SimpleInstruction {

        private final String freezeOrderSn;

        private final long amount;

        FreezeOrderReferencedWithdrawInstruction(String freezeOrderSn, long amount) {
            this.freezeOrderSn = freezeOrderSn;
            this.amount = amount;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.WITHDRAW;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.WITHDRAW;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(amount, CurrencyIsoCode.USD);
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return new FreezeOrderReference(freezeOrderSn);
        }
    }

    static final class FreezeOrderReference implements FundsInstructionReferenceSpec {

        private final String freezeOrderSn;

        FreezeOrderReference(String freezeOrderSn) {
            this.freezeOrderSn = freezeOrderSn;
        }

        @Override
        public @NonNull FundsInstructionReferenceType getReferenceType() {
            return FundsInstructionReferenceType.FREEZE_ORDER;
        }

        @Override
        public @Nullable String getReferenceSn() {
            return freezeOrderSn;
        }

        @Override
        public @Nullable String getReferenceBusinessSn() {
            return null;
        }

        @Override
        public @Nullable String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public @Nullable String getExternalTransactionId() {
            return null;
        }

        @Override
        public @Nullable String getAuthCode() {
            return null;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static final class SimpleReference implements FundsInstructionReferenceSpec {

        private final FundsInstructionReferenceType referenceType;

        SimpleReference(FundsInstructionReferenceType referenceType) {
            this.referenceType = referenceType;
        }

        @Override
        public @NonNull FundsInstructionReferenceType getReferenceType() {
            return referenceType;
        }

        @Override
        public @Nullable String getReferenceSn() {
            return "FT_001";
        }

        @Override
        public @Nullable String getReferenceBusinessSn() {
            return null;
        }

        @Override
        public @Nullable String getReferenceLedgerTransactionSn() {
            return null;
        }

        @Override
        public @Nullable String getExternalTransactionId() {
            return null;
        }

        @Override
        public @Nullable String getAuthCode() {
            return null;
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static class SimpleResolvedRoute implements ResolvedRouteSpec {

        private final long amount;

        private final String replayRefLegId;

        SimpleResolvedRoute(long amount) {
            this(amount, null);
        }

        SimpleResolvedRoute(long amount, String replayRefLegId) {
            this.amount = amount;
            this.replayRefLegId = replayRefLegId;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getRouteVersion() {
            return "v2";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "AUTH_BUSINESS_0001";
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.AUTHORIZATION_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.AUTHORIZE;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.PAY;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(
                    new SimpleParticipant(RouteParticipantRole.AUTH_HOLDER,
                            new SimpleSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT), amount),
                    new SimpleParticipant(RouteParticipantRole.PLATFORM_FUNDING_ACCOUNT,
                            new SimpleSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT), amount)
            );
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return List.of(new SimpleLeg(amount, replayRefLegId));
        }

        @Override
        public @NonNull LocalDateTime getResolvedAt() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static final class SharedCardResolvedRoute implements ResolvedRouteSpec {

        private final long amount;

        SharedCardResolvedRoute(long amount) {
            this.amount = amount;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "CARD_AUTH_SHARED";
        }

        @Override
        public @NonNull String getRouteVersion() {
            return "v2";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "CARD_AUTH";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "AUTH_BUSINESS_0001";
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.AUTHORIZATION_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.AUTHORIZE;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.PAY;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of(
                    new SimpleParticipant(RouteParticipantRole.AUTH_HOLDER,
                            new SimpleSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT), amount),
                    new SimpleParticipant(RouteParticipantRole.BUDGET_CONTROLLER,
                            new SimpleSubjectRef("budget_001", FundsSubjectType.BUDGET_GROUP), amount),
                    new SimpleParticipant(RouteParticipantRole.REAL_FUNDING_SOURCE,
                            new SimpleSubjectRef("funding_001", FundsSubjectType.FUNDING_ACCOUNT), amount)
            );
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return List.of(new SimpleLeg(amount));
        }

        @Override
        public @NonNull LocalDateTime getResolvedAt() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @NonNull Map<String, Object> getContextVariables() {
            return Map.of();
        }
    }

    static final class SimpleParticipant implements RouteParticipantSpec {

        private final RouteParticipantRole role;

        private final SubjectRef subjectRef;

        private final long amount;

        SimpleParticipant(RouteParticipantRole role, SubjectRef subjectRef, long amount) {
            this.role = role;
            this.subjectRef = subjectRef;
            this.amount = amount;
        }

        @Override
        public @NonNull RouteParticipantRole getParticipantRole() {
            return role;
        }

        @Override
        public @NonNull SubjectRef getSubjectRef() {
            return subjectRef;
        }

        @Override
        public @Nullable String getLedgerProfileCode() {
            return role == RouteParticipantRole.AUTH_HOLDER ? "CREDIT_BASIC" : "FUNDING_PLATFORM";
        }

        @Override
        public @Nullable String getCurrency() {
            return CurrencyIsoCode.USD.name();
        }

        @Override
        public @Nullable Money getAmount() {
            return Money.immutable(amount, CurrencyIsoCode.USD);
        }
    }

    static final class SimpleLeg implements RouteLegSpec {

        private final long amount;

        private final String replayRefLegId;

        SimpleLeg(long amount) {
            this(amount, null);
        }

        SimpleLeg(long amount, String replayRefLegId) {
            this.amount = amount;
            this.replayRefLegId = replayRefLegId;
        }

        @Override
        public @NonNull String getLegId() {
            return "LEG_001";
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.HOLD;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return new SimpleNode(new SimpleSubjectRef("credit_001", FundsSubjectType.CREDIT_ACCOUNT),
                    LedgerSubjectCode.AVAILABLE, RouteNodeRole.SOURCE);
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return new SimpleNode(new SimpleSubjectRef("platform_revenue_001", FundsSubjectType.FUNDING_ACCOUNT),
                    LedgerSubjectCode.AUTHORIZATION, RouteNodeRole.TARGET);
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(amount, CurrencyIsoCode.USD);
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return LedgerBalanceEffectType.HOLD;
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return LedgerPhaseCode.AUTHORIZATION;
        }

        @Override
        public @NonNull AccountBalancePeriodType getPeriodType() {
            return AccountBalancePeriodType.LIFETIME;
        }

        @Override
        public @NonNull Map<String, LedgerBalanceConstraintType> getConstraintOverrides() {
            return Map.of();
        }

        @Override
        public @NonNull RouteReplayPolicy getReplayPolicy() {
            return RouteReplayPolicy.FULL_ONLY;
        }

        @Override
        public @Nullable String getReplayRefLegId() {
            return replayRefLegId;
        }
    }

    static final class SimpleNode implements RouteNodeSpec {

        private final SubjectRef subjectRef;

        private final LedgerSubjectCode subjectCode;

        private final RouteNodeRole nodeRole;

        SimpleNode(SubjectRef subjectRef, LedgerSubjectCode subjectCode, RouteNodeRole nodeRole) {
            this.subjectRef = subjectRef;
            this.subjectCode = subjectCode;
            this.nodeRole = nodeRole;
        }

        @Override
        public @NonNull RouteNodeType getNodeType() {
            return RouteNodeType.SUBJECT;
        }

        @Override
        public @NonNull SubjectRef getSubjectRef() {
            return subjectRef;
        }

        @Override
        public @NonNull LedgerSubjectCode getLedgerSubjectCode() {
            return subjectCode;
        }

        @Override
        public @NonNull RouteNodeRole getNodeRole() {
            return nodeRole;
        }
    }

    static class SimpleSubjectRef implements SubjectRef {

        private final String subjectId;

        private final FundsSubjectType subjectType;

        SimpleSubjectRef(String subjectId, FundsSubjectType subjectType) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
        }

        @Override
        public Long getTenantId() {
            return 1L;
        }

        @Override
        public @NonNull String getSubjectId() {
            return subjectId;
        }

        @Override
        public @NonNull FundsSubjectType getSubjectType() {
            return subjectType;
        }
    }
}
