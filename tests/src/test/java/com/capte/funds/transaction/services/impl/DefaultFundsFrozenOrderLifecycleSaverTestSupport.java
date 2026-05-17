package com.capte.funds.transaction.services.impl;

import com.capte.funds.support.FundsAccountServiceTestSupport;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.dal.mapper.FundsFrozenOrderMapper;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteParticipantSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteParticipantRole;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

abstract class DefaultFundsFrozenOrderLifecycleSaverTestSupport {

    protected static FundsFrozenOrderMapper mapper(
            AtomicReference<FundsFrozenOrder> savedOrder,
            java.util.function.Function<Integer, FundsFrozenOrder> selectHandler) {
        AtomicInteger selectCount = new AtomicInteger();
        return FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entity -> {
                    FundsFrozenOrder order = (FundsFrozenOrder) entity;
                    order.setId(1L);
                    savedOrder.set(order);
                },
                query -> selectHandler.apply(selectCount.incrementAndGet()),
                entity -> {
                    savedOrder.set((FundsFrozenOrder) entity);
                    return 1;
                });
    }

    protected static FundsFrozenOrderMapper unfreezeMapper(AtomicReference<FundsFrozenOrder> originalOrder,
                                                           AtomicReference<FundsFrozenOrder> releaseRecord) {
        AtomicInteger selectCount = new AtomicInteger();
        return FundsAccountServiceTestSupport.mapper(
                FundsFrozenOrderMapper.class,
                entity -> {
                    FundsFrozenOrder order = (FundsFrozenOrder) entity;
                    order.setId(2L);
                    releaseRecord.set(order);
                },
                query -> switch (selectCount.incrementAndGet()) {
                    case 1 -> originalOrder.get();
                    case 2 -> null;
                    case 3 -> originalOrder.get();
                    case 4 -> releaseRecord.get();
                    case 5 -> originalOrder.get();
                    default -> throw new UnsupportedOperationException("unexpected selectOneByQuery");
                },
                entity -> {
                    FundsFrozenOrder order = (FundsFrozenOrder) entity;
                    if (order.getSn().equals(originalOrder.get().getSn())) {
                        originalOrder.set(order);
                    } else {
                        releaseRecord.set(order);
                    }
                    return 1;
                });
    }

    protected static FundsFrozenOrder frozenOrder(long releasedAmount) {
        return frozenOrder(releasedAmount, 0L);
    }

    protected static FundsFrozenOrder frozenOrder(long releasedAmount, long consumedAmount) {
        FundsFrozenOrder order = new FundsFrozenOrder();
        order.setId(1L);
        order.setSn("FO_0001");
        order.setTenantId(1001L);
        order.setSubjectId("funding_001");
        order.setSubjectType(FundsSubjectType.FUNDING_ACCOUNT);
        order.setFreezeType("WITHDRAW");
        order.setBusinessScene("RISK_FREEZE");
        order.setBusinessSn("FREEZE_0001");
        order.setFreezeLedgerTransactionSn("LT_FREEZE_0001");
        order.setAmount(100L);
        order.setReleasedAmount(releasedAmount);
        order.setConsumedAmount(consumedAmount);
        order.setCurrency(CurrencyIsoCode.USD);
        order.setStatus(FundsFrozenOrderStatus.FROZEN);
        return order;
    }

    protected static FundsInstructionSpec instruction(FundsTransactionEventType eventType,
                                                      FundsInstructionReferenceSpec reference,
                                                      String businessScene,
                                                      String businessSn,
                                                      long amount) {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(1001L)
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(eventType)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .amount(Money.immutable(amount, CurrencyIsoCode.USD))
                .originalAmount(Money.immutable(amount, CurrencyIsoCode.USD))
                .exchangeRate(BigDecimal.ONE)
                .reference(reference)
                .businessScene(businessScene)
                .businessSn(businessSn)
                .eventTime(LocalDateTime.of(2026, 5, 14, 12, 0))
                .description("balance control")
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(-1L)
                        .operatorType("SYSTEM")
                        .operatorName("SYSTEM")
                        .appName("capte-tests")
                        .contextVariables(Map.of())
                        .build())
                .contextVariables(Map.of(FundsInstructionContextKeys.FREEZE_TYPE, "WITHDRAW"))
                .build();
    }

    protected static FundsInstructionReferenceSpec reference(String freezeOrderSn) {
        return ImmutableFundsInstructionReferenceSpec.builder()
                .referenceType(FundsInstructionReferenceType.FREEZE_ORDER)
                .referenceSn(freezeOrderSn)
                .contextVariables(Map.of())
                .build();
    }

    protected static RouteSnapshotSpec routeSnapshot() {
        List<RouteParticipantSpec> participants = participants();
        return ImmutableRouteSnapshotSpec.builder()
                .tenantId(1001L)
                .snapshotId("RS_0001")
                .snapshotSchemaVersion("v1")
                .routeCode("BALANCE_FREEZE")
                .routeVersion("v1")
                .businessScene("RISK_FREEZE")
                .businessSn("FREEZE_0001")
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.FREEZE)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .participants(participants)
                .legs(List.of())
                .resolvedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .contextVariables(Map.of())
                .build();
    }

    protected static ResolvedRouteSpec resolvedRoute() {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(1001L)
                .routeCode("BALANCE_FREEZE")
                .routeVersion("v1")
                .businessScene("RISK_FREEZE")
                .businessSn("FREEZE_0001")
                .instructionType(FundsInstructionType.BALANCE_CONTROL)
                .eventType(FundsTransactionEventType.FREEZE)
                .transactionType(DefaultFundsTransactionType.ADJUSTMENT)
                .participants(participants())
                .legs(List.of())
                .resolvedAt(LocalDateTime.of(2026, 5, 14, 12, 0))
                .contextVariables(Map.of())
                .build();
    }

    private static List<RouteParticipantSpec> participants() {
        return List.of(ImmutableRouteParticipantSpec.builder()
                .participantRole(RouteParticipantRole.PAYER)
                .subjectRef(ImmutableSubjectRef.builder()
                        .tenantId(1001L)
                        .subjectId("funding_001")
                        .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                        .build())
                .amount(Money.immutable(100L, CurrencyIsoCode.USD))
                .contextVariables(Map.of())
                .build());
    }
}
