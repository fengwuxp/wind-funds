package com.capte.funds.transaction;

import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRoutedFundsInstructionOrchestratorTests extends DefaultRoutedFundsInstructionOrchestratorTestSupport {

    /**
     * 场景：普通资金指令进入路由编排器并完成入账。
     * 输入：可解析出账本 leg 的资金指令。
     * 输出：资金交易流水、route snapshot、posting plan 和 ledger transaction。
     * 预期：先保存生命周期快照，再装配并提交账本交易，交易核心字段来自原指令。
     * 红线：编排器不得绕过 route snapshot 或直接写账本事实。
     */
    @Test
    void testExecuteShouldResolveSnapshotAssembleAndPostLedgerTransaction() {
        RecordingRouteResolver routeResolver = new RecordingRouteResolver(route(true));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );
        FundsInstructionSpec instruction = new SimpleInstruction();

        String transactionSn = orchestrator.execute(instruction);

        LedgerTransactionSpec posted = postingService.transaction.get();
        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(routeResolver.instruction.get()).isSameAs(instruction);
        assertThat(lifecycleSaver.beforePostingInstruction.get()).isSameAs(instruction);
        assertThat(lifecycleSaver.beforePostingRoute.get()).isSameAs(routeResolver.route);
        assertThat(lifecycleSaver.beforePostingSnapshot.get()).isNotNull();
        assertThat(postingAssembler.fundsTransactionSn.get()).isEqualTo("FT_001");
        assertThat(postingAssembler.instruction.get()).isSameAs(instruction);
        assertThat(postingAssembler.route.get()).isSameAs(routeResolver.route);
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get()).isEqualTo(posted.getSn());
        assertThat(posted.getTenantId()).isEqualTo(instruction.getTenantId());
        assertThat(posted.getInstructionType()).isEqualTo(instruction.getInstructionType());
        assertThat(posted.getFundsTransactionSn()).isEqualTo("FT_001");
        assertThat(posted.getTransactionType()).isEqualTo(instruction.getTransactionType());
        assertThat(posted.getEventType()).isEqualTo(instruction.getEventType());
        assertThat(posted.getOriginalAmount()).isEqualTo(instruction.getOriginalAmount());
        assertThat(posted.getExchangeRate()).isEqualByComparingTo(instruction.getExchangeRate());
        assertThat(posted.getPostingPlans()).hasSize(1);
        assertThat(posted.isBalanced()).isTrue();
    }

    /**
     * 场景：同一资金指令重复执行且生命周期已完成。
     * 输入：lifecycle saver 返回 completed=true。
     * 输出：已有资金交易流水。
     * 预期：短路返回，不再次装配 posting plan，也不再次提交 ledger transaction。
     * 红线：幂等重放不得产生第二次账务影响。
     */
    @Test
    void testExecuteShouldShortCircuitWhenLifecycleAlreadyCompleted() {
        RecordingRouteResolver routeResolver = new RecordingRouteResolver(route(true));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(true);
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        String transactionSn = orchestrator.execute(new SimpleInstruction());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(postingAssembler.fundsTransactionSn.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get()).isNull();
    }

    /**
     * 场景：指令解析成功但没有需要入账的 route leg。
     * 输入：空 legs 的 resolved route。
     * 输出：资金交易流水。
     * 预期：生命周期可标记成功，但不生成 posting plan 或 ledger transaction。
     * 红线：无账务路径的事件不得伪造空分录入账。
     */
    @Test
    void testExecuteShouldMarkSucceededWithoutPostingWhenRouteHasNoLegs() {
        RecordingRouteResolver routeResolver = new RecordingRouteResolver(route(false));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        String transactionSn = orchestrator.execute(new SimpleInstruction());

        assertThat(transactionSn).isEqualTo("FT_001");
        assertThat(postingAssembler.fundsTransactionSn.get()).isNull();
        assertThat(postingService.transaction.get()).isNull();
        assertThat(lifecycleSaver.succeededLedgerTransactionSn.get()).isNull();
    }

    /**
     * 场景：账本提交阶段抛出异常。
     * 输入：可解析 route 和会失败的 posting service。
     * 输出：原始异常和失败生命周期记录。
     * 预期：编排器记录失败 cause 后向上抛出，调用方可感知交易未完成。
     * 红线：账本提交失败不得被吞掉或标记为成功。
     */
    @Test
    void testExecuteShouldMarkFailedWhenPostingThrows() {
        RecordingRouteResolver routeResolver = new RecordingRouteResolver(route(true));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(true);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );

        assertThatThrownBy(() -> orchestrator.execute(new SimpleInstruction()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("posting failed");
        assertThat(lifecycleSaver.failedCause.get()).isInstanceOf(IllegalStateException.class);
    }

    /**
     * 场景：无原交易引用的直接退款进入普通路由。
     * 输入：没有 reference 的 direct refund 指令。
     * 输出：普通 RouteResolver 解析出的 route 和 ledger transaction。
     * 预期：按当前指令路由编排，生成独立资金事实。
     * 红线：无引用退款不得伪造原交易回放关系。
     */
    @Test
    void testExecuteShouldResolveDirectRefundWhenNoReferenceProvided() {
        RecordingRouteResolver routeResolver = new RecordingRouteResolver(route(true));
        RecordingLedgerPostingAssembler postingAssembler = new RecordingLedgerPostingAssembler(false);
        RecordingPostingService postingService = new RecordingPostingService(false);
        RecordingLifecycleSaver lifecycleSaver = new RecordingLifecycleSaver(false);
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                routeResolver,
                new DefaultRouteSnapshotFactory(),
                postingAssembler,
                postingService,
                lifecycleSaver
        );
        FundsInstructionSpec instruction = new DirectRefundInstruction();

        orchestrator.execute(instruction);

        assertThat(routeResolver.instruction.get()).isSameAs(instruction);
        assertThat(postingAssembler.route.get()).isSameAs(routeResolver.route);
        assertThat(postingService.transaction.get()).isNotNull();
    }

    /**
     * 场景：编排器能力匹配资金指令契约。
     * 输入：`FundsInstructionSpec.class`。
     * 输出：supports 判断结果。
     * 预期：编排器声明支持资金指令契约。
     * 红线：能力匹配不得扩大到非资金指令类型。
     */
    @Test
    void testSupportsShouldMatchFundsInstructionType() {
        DefaultRoutedFundsInstructionOrchestrator orchestrator = new DefaultRoutedFundsInstructionOrchestrator(
                new RecordingRouteResolver(route(true)),
                new DefaultRouteSnapshotFactory(),
                new RecordingLedgerPostingAssembler(false),
                new RecordingPostingService(false),
                new RecordingLifecycleSaver(false)
        );

        assertThat(orchestrator.supports(FundsInstructionSpec.class)).isTrue();
    }

}
