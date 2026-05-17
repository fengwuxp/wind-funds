package com.capte.funds.transaction.application.orchestration;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRoutedFundsInstructionOrchestratorReplayTests
        extends DefaultRoutedFundsInstructionOrchestratorTestSupport {

    /**
     * 场景：退款等逆向事件基于原资金交易快照回放路径。
     * 输入：带引用交易号的退款指令和可查询的原 route snapshot。
     * 输出：回放 route code、leg 和 replayRefLegId。
     * 预期：不重新解析普通 RouteResolver，按原路径快照生成逆向账务路径。
     * 红线：逆向交易不得基于当前实时路由重新决定历史资金路径。
     */
    @Test
    void testExecuteShouldReplaySavedRouteSnapshotForLifecycleEvent() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.routeSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(route(true)));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.REFUND));

        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(postingAssembler.route.get().getRouteCode()).isEqualTo("DIRECT_REFUND_REPLAY");
        assertThat(postingAssembler.route.get().getLegs()).hasSize(1);
        assertThat(postingAssembler.route.get().getLegs().getFirst().getReplayRefLegId()).isEqualTo("LEG_001");
    }

    /**
     * 场景：授权链路退款需要保留授权事件语义。
     * 输入：`AUTH_REFUND` 指令和原授权 route snapshot。
     * 输出：`AUTHORIZATION_REFUND_REPLAY` 路径。
     * 预期：回放路径事件类型保持为 `AUTH_REFUND`，不落入普通退款语义。
     * 红线：授权退款不得与普通消费退款混同。
     */
    @Test
    void testExecuteShouldReplayAuthorizationRefundAsAuthorizationReplayType() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.routeSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(route(true)));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.AUTH_REFUND));

        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(postingAssembler.route.get().getRouteCode()).isEqualTo("AUTHORIZATION_REFUND_REPLAY");
        assertThat(postingAssembler.route.get().getEventType()).isEqualTo(FundsTransactionEventType.AUTH_REFUND);
        assertThat(postingAssembler.route.get().getLegs()).hasSize(1);
    }

    /**
     * 场景：`REPLAY_ONCE` 原路径已经被同一 replay 事件成功消费。
     * 输入：带引用交易号的退款指令、原快照中存在 `REPLAY_ONCE` leg，且查询服务返回已消费。
     * 输出：编排异常和普通 RouteResolver 调用记录。
     * 预期：编排器在 replay 前拒绝第二次消费，不重新解析 Route，不创建生命周期或账本交易。
     * 红线：`REPLAY_ONCE` 的原路径 leg 不得被重复消费。
     */
    @Test
    void testExecuteShouldRejectSecondReplayOnceConsumption() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.routeSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(route(true,
                RouteReplayPolicy.REPLAY_ONCE)));
        transactionQueryService.consumedReplayLegId.set("LEG_001");
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        assertThatThrownBy(() -> orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.REFUND)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("仅允许成功回放一次");
        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(lifecycleSaver.beforePostingInstruction.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
    }

    /**
     * 场景：结算后拒付事件进入编排器。
     * 输入：带引用交易号的 `CHARGEBACK` 指令，且原路径快照可查询。
     * 输出：回放路径编码、事件类型和 replayRefLegId。
     * 预期：编排器直接回放原快照，生成 `CHARGEBACK_REPLAY` 路径，不再调用普通 RouteResolver。
     * 红线：拒付不得按当前实时路由重算，也不得修改原账本事实。
     */
    @Test
    void testExecuteShouldReplaySavedRouteSnapshotForChargebackEvent() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.routeSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(route(true)));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.CHARGEBACK));

        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(postingAssembler.route.get().getRouteCode()).isEqualTo("CHARGEBACK_REPLAY");
        assertThat(postingAssembler.route.get().getEventType()).isEqualTo(FundsTransactionEventType.CHARGEBACK);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .containsOnly("LEG_001");
    }

    /**
     * 场景：逆向事件引用原交易但原 route snapshot 缺失。
     * 输入：带引用交易号的退款指令，查询服务未返回原快照。
     * 输出：缺失快照异常。
     * 预期：拒绝继续编排，不调用普通 RouteResolver，不提交账本交易。
     * 红线：没有原路径快照时不得猜测逆向资金路径。
     */
    @Test
    void testExecuteShouldFailWhenReplaySnapshotMissing() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(true));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(new RecordingTransactionQueryService(), fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        assertThatThrownBy(() -> orchestrator.execute(new ReferencedInstruction(FundsTransactionEventType.REFUND)))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("未找到原路径快照");
        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(postingAssembler.route.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
    }

    /**
     * 场景：解冻事件带冻结单引用进入编排器。
     * 输入：`referenceType=FREEZE_ORDER` 的 `UNFREEZE` 指令，且可通过冻结单号定位原冻结快照。
     * 输出：回放路径编码、事件类型和普通 RouteResolver 调用记录。
     * 预期：编排器不再走普通 RouteResolver，而是基于原冻结快照生成 `BALANCE_UNFREEZE_REPLAY` 路径。
     * 红线：解冻只回放同主体 `AVAILABLE <-> FROZEN` 控制，不表达消费、扣划或跨主体转移。
     */
    @Test
    void testExecuteShouldReplayFreezeSnapshotForUnfreezeWithFreezeOrderReference() {
        RecordingRouteResolver fallbackRouteResolver = new RecordingRouteResolver(route(true));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        RecordingTransactionQueryService transactionQueryService = new RecordingTransactionQueryService();
        transactionQueryService.freezeOrderRouteSnapshot.set(new DefaultRouteSnapshotFactory().createSnapshot(route(true)));
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                replayRouteResolver(transactionQueryService, fallbackRouteResolver),
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );
        FundsInstructionSpec instruction = new FreezeOrderReferencedInstruction(FundsTransactionEventType.UNFREEZE);

        orchestrator.execute(instruction);

        assertThat(fallbackRouteResolver.instruction.get()).isNull();
        assertThat(postingAssembler.route.get().getRouteCode()).isEqualTo("BALANCE_UNFREEZE_REPLAY");
        assertThat(postingAssembler.route.get().getEventType()).isEqualTo(FundsTransactionEventType.UNFREEZE);
        assertThat(postingAssembler.route.get().getLegs())
                .extracting(RouteLegSpec::getReplayRefLegId)
                .containsOnly("LEG_001");
        assertThat(postingService.transaction.get()).isNotNull();
    }
}
