package com.capte.funds.transaction.application.orchestration;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRoutedFundsInstructionOrchestratorFeeReplayTests
        extends DefaultRoutedFundsInstructionOrchestratorTestSupport {

    /**
     * 场景：费用退款按原费用 leg 回放。
     * 输入：`FEE_REFUND` 指令和原费用 route snapshot。
     * 输出：费用退款回放路径和原 leg 引用。
     * 预期：事件类型保持为 `FEE_REFUND`，回放 leg 指向原费用 leg。
     * 红线：费用退款不得错误冲销主交易本金 leg。
     */
    @Test
    void testExecuteShouldReplayFeeRefundAsFeeReplayType() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.routeSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(feeRoute()));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.FEE_REFUND));

        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(postingAssembler.route.get().getRouteCode()).isEqualTo("DIRECT_REFUND_REPLAY");
        assertThat(postingAssembler.route.get().getEventType()).isEqualTo(FundsTransactionEventType.FEE_REFUND);
        assertThat(postingAssembler.route.get().getLegs()).singleElement()
                .satisfies(leg -> assertThat(leg.getReplayRefLegId()).isEqualTo("LEG_001"));
    }

    /**
     * 场景：手续费退回已部分消费原费用 leg，后续再次退费超过原手续费金额。
     * 输入：原费用 leg 金额 100，历史已退 60，本次手续费退回 100。
     * 输出：编排异常。
     * 预期：在账本入账前拒绝，不调用普通 RouteResolver，不创建生命周期或账本交易。
     * 红线：手续费退回累计金额不得超过原手续费事实。
     */
    @Test
    void testExecuteShouldRejectFeeRefundWhenConsumedAmountExceedsOriginalFeeLeg() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.routeSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(feeRoute()));
        transactionQueryService.consumedReplayLegAmount.set(Money.immutable(60L, CurrencyIsoCode.USD));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        assertThatThrownBy(() -> orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.FEE_REFUND)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("回放累计金额不能大于原 RouteLeg 金额");
        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(lifecycleSaver.beforePostingInstruction.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
    }
}
