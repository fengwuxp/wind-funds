package com.capte.funds.transaction.application.orchestration;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.DefaultRoutedFundsInstructionOrchestrator;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRoutedFundsInstructionOrchestratorReplayPolicyTests
        extends DefaultRoutedFundsInstructionOrchestratorTestSupport {

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
}
