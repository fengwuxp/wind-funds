package com.capte.funds.transaction.services.impl;

import com.capte.funds.transaction.dal.entities.FundsFrozenOrder;
import com.capte.funds.transaction.constant.FundsInstructionContextKeys;
import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFundsFrozenOrderLifecycleSaverTests extends DefaultFundsFrozenOrderLifecycleSaverTestSupport {

    @Test
    void testLifecycleSaverShouldOnlySupportFreezeEvents() {
        DefaultFundsFrozenOrderLifecycleSaver saver = new DefaultFundsFrozenOrderLifecycleSaver(
                mapper(new AtomicReference<>(), queryCount -> null));

        assertThat(saver.supports(instruction(FundsTransactionEventType.FREEZE, null,
                "RISK_FREEZE", "FREEZE_0001", 100L))).isTrue();
        assertThat(saver.supports(instruction(FundsTransactionEventType.UNFREEZE,
                reference("FO_0001"), "RISK_UNFREEZE", "UNFREEZE_0001", 30L))).isTrue();
        assertThat(saver.supports(instruction(FundsTransactionEventType.BALANCE_ADJUST, null,
                "BALANCE_ADJUST", "ADJUST_0001", 30L))).isFalse();
    }

    @Test
    void testFreezeShouldCreateFrozenOrderAndReturnFrozenOrderLifecycle() {
        AtomicReference<FundsFrozenOrder> savedOrder = new AtomicReference<>();
        DefaultFundsFrozenOrderLifecycleSaver saver = new DefaultFundsFrozenOrderLifecycleSaver(
                mapper(savedOrder, queryCount -> savedOrder.get()));

        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.FREEZE, null,
                "RISK_FREEZE", "FREEZE_0001", 100L);
        FundsInstructionLifecycleResult result = saver.beforePosting(instruction, resolvedRoute(), routeSnapshot());

        assertThat(savedOrder.get()).satisfies(order -> {
            assertThat(order.getSn()).isNotBlank();
            assertThat(order.getTenantId()).isEqualTo(1001L);
            assertThat(order.getSubjectId()).isEqualTo("funding_001");
            assertThat(order.getSubjectType()).isEqualTo(FundsSubjectType.FUNDING_ACCOUNT);
            assertThat(order.getFreezeType()).isEqualTo("WITHDRAW");
            assertThat(order.getContextVariables())
                    .contains("\"" + FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE + "\":\"FREEZE\"")
                    .contains("\"" + FundsInstructionContextKeys.ROUTE_SNAPSHOT + "\":");
            assertThat(order.getBusinessScene()).isEqualTo("RISK_FREEZE");
            assertThat(order.getBusinessSn()).isEqualTo("FREEZE_0001");
            assertThat(order.getAmount()).isEqualTo(100L);
            assertThat(order.getReleasedAmount()).isZero();
            assertThat(order.getConsumedAmount()).isZero();
            assertThat(order.getCurrency()).isEqualTo(CurrencyIsoCode.USD);
            assertThat(order.getStatus()).isEqualTo(FundsFrozenOrderStatus.CREATED);
            RouteSnapshotSpec snapshot = RouteSnapshotJsonSupport.parseRouteSnapshot(
                    com.alibaba.fastjson2.JSON.parseObject(order.getContextVariables())
                            .getString(FundsInstructionContextKeys.ROUTE_SNAPSHOT),
                    null);
            assertThat(snapshot.getRouteCode()).isEqualTo("BALANCE_FREEZE");
            assertThat(snapshot.getParticipants()).hasSize(1);
        });
        assertThat(result.getTransactionSn()).isEqualTo(savedOrder.get().getSn());
        assertThat(result.getTransactionDetailSns()).isEmpty();
        assertThat(result.isCompleted()).isFalse();

        saver.markSucceeded(instruction, result, "LT_FREEZE_0001");

        assertThat(savedOrder.get().getFreezeLedgerTransactionSn()).isEqualTo("LT_FREEZE_0001");
        assertThat(savedOrder.get().getStatus()).isEqualTo(FundsFrozenOrderStatus.FROZEN);
        assertThat(saver.beforePosting(instruction, resolvedRoute(), routeSnapshot()).isCompleted()).isTrue();
    }

    @Test
    void testUnfreezeShouldReleaseReferencedFrozenOrder() {
        AtomicReference<FundsFrozenOrder> originalOrder = new AtomicReference<>(frozenOrder(20L));
        AtomicReference<FundsFrozenOrder> releaseRecord = new AtomicReference<>();
        DefaultFundsFrozenOrderLifecycleSaver saver = new DefaultFundsFrozenOrderLifecycleSaver(
                unfreezeMapper(originalOrder, releaseRecord));

        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.UNFREEZE,
                reference("FO_0001"), "RISK_UNFREEZE", "UNFREEZE_0001", 30L);
        FundsInstructionLifecycleResult result = saver.beforePosting(instruction, resolvedRoute(), routeSnapshot());

        assertThat(result.getTransactionSn()).isEqualTo(releaseRecord.get().getSn());
        assertThat(releaseRecord.get().getFreezeType()).isEqualTo("WITHDRAW");
        assertThat(releaseRecord.get().getContextVariables())
                .contains("\"" + FundsInstructionContextKeys.FROZEN_ORDER_EVENT_TYPE + "\":\"UNFREEZE\"")
                .contains("\"" + FundsInstructionContextKeys.REFERENCE_FREEZE_SN + "\":\"FO_0001\"");
        assertThat(releaseRecord.get().getBusinessScene()).isEqualTo("RISK_UNFREEZE");
        assertThat(releaseRecord.get().getBusinessSn()).isEqualTo("UNFREEZE_0001");
        assertThat(releaseRecord.get().getAmount()).isEqualTo(30L);
        assertThat(result.isCompleted()).isFalse();

        saver.markSucceeded(instruction, result, "LT_UNFREEZE_0001");

        assertThat(releaseRecord.get().getFreezeLedgerTransactionSn()).isEqualTo("LT_UNFREEZE_0001");
        assertThat(releaseRecord.get().getStatus()).isEqualTo(FundsFrozenOrderStatus.RELEASED);
        assertThat(originalOrder.get().getReleasedAmount()).isEqualTo(50L);
        assertThat(originalOrder.get().getFreezeLedgerTransactionSn()).isEqualTo("LT_FREEZE_0001");
        assertThat(originalOrder.get().getStatus()).isEqualTo(FundsFrozenOrderStatus.PARTIALLY_RELEASED);
        assertThat(originalOrder.get().getReleaseTime()).isNotNull();
    }

    @Test
    void testUnfreezeShouldUseReleasedAmountOnlyForReleasableBalance() {
        AtomicReference<FundsFrozenOrder> originalOrder = new AtomicReference<>(frozenOrder(20L, 60L));
        AtomicReference<FundsFrozenOrder> releaseRecord = new AtomicReference<>();
        DefaultFundsFrozenOrderLifecycleSaver saver = new DefaultFundsFrozenOrderLifecycleSaver(
                unfreezeMapper(originalOrder, releaseRecord));

        FundsInstructionSpec instruction = instruction(FundsTransactionEventType.UNFREEZE,
                reference("FO_0001"), "RISK_UNFREEZE", "UNFREEZE_OVER_AMOUNT", 30L);

        FundsInstructionLifecycleResult result = saver.beforePosting(instruction, resolvedRoute(), routeSnapshot());
        saver.markSucceeded(instruction, result, "LT_UNFREEZE_0002");

        assertThat(releaseRecord.get().getStatus()).isEqualTo(FundsFrozenOrderStatus.RELEASED);
        assertThat(originalOrder.get().getReleasedAmount()).isEqualTo(50L);
        assertThat(originalOrder.get().getConsumedAmount()).isEqualTo(60L);
        assertThat(originalOrder.get().getStatus()).isEqualTo(FundsFrozenOrderStatus.PARTIALLY_RELEASED);
    }
}
