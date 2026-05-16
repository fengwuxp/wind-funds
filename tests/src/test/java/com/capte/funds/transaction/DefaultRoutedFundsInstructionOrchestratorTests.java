package com.capte.funds.transaction;

import com.capte.funds.route.DefaultRouteReplayService;
import com.capte.funds.route.DefaultRouteSnapshotFactory;
import com.capte.funds.transaction.ledger.LedgerTransactionSpecFactory;
import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.model.dto.FundsTransactionDTO;
import com.capte.funds.transaction.model.dto.FundsTransactionDetailDTO;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionReferenceSpec;
import com.capte.funds.transaction.services.FundsInstructionLifecycleSaver;
import com.capte.funds.transaction.services.FundsTransactionQueryService;
import com.wind.common.exception.BaseException;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.integration.funds.ledger.enums.EntrySide;
import com.wind.integration.funds.ledger.enums.LedgerBalanceConstraintType;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerPostingIntentType;
import com.wind.integration.funds.ledger.enums.LedgerSettlementStatus;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteReplayPolicy;
import com.wind.integration.funds.route.ref.ExternalAccountRefSpec;
import com.wind.integration.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.integration.funds.route.ref.SubjectRef;
import com.wind.integration.funds.route.spec.PlatformAccountsSnapshotSpec;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteParticipantSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.route.spec.RoutingDecisionSpec;
import com.wind.integration.funds.operation.FundsOperationActorSpec;
import com.wind.integration.funds.spec.ledger.LedgerEntrySpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPhaseSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
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
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultRoutedFundsInstructionOrchestratorTests {

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

    private static ResolvedRouteSpec route(boolean withLedgerLeg) {
        return route(withLedgerLeg, RouteReplayPolicy.FULL_ONLY);
    }

    private static ResolvedRouteSpec route(boolean withLedgerLeg, RouteReplayPolicy replayPolicy) {
        List<RouteLegSpec> legs = withLedgerLeg ? List.of(new SimpleRouteLeg(replayPolicy)) : List.of();
        return new SimpleResolvedRoute(legs);
    }

    private static ResolvedRouteSpec feeRoute() {
        return new SimpleResolvedRoute(List.of(new SimpleRouteLeg(RouteReplayPolicy.FULL_ONLY,
                LedgerPhaseCode.FEE)));
    }

    private static RouteResolver replayRouteResolver(RecordingTransactionQueryService transactionQueryService,
                                                     RecordingRouteResolver fallbackRouteResolver) {
        return new com.capte.funds.route.CompositeRouteResolver(List.of(
                new DefaultRouteReplayService(transactionQueryService),
                fallbackRouteResolver
        ));
    }

    private static final class RecordingRouteResolver implements RouteResolver {

        private final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        private final ResolvedRouteSpec route;

        private RecordingRouteResolver(ResolvedRouteSpec route) {
            this.route = route;
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public @NonNull ResolvedRouteSpec resolve(@NonNull FundsInstructionSpec instruction) {
            this.instruction.set(instruction);
            return route;
        }
    }

    private static final class RecordingLedgerPostingAssembler implements LedgerPostingAssembler<ResolvedRouteSpec> {

        private final AtomicReference<FundsInstructionSpec> instruction = new AtomicReference<>();

        private final AtomicReference<String> fundsTransactionSn = new AtomicReference<>();

        private final AtomicReference<ResolvedRouteSpec> route = new AtomicReference<>();

        private final boolean unsupported;

        private RecordingLedgerPostingAssembler(boolean unsupported) {
            this.unsupported = unsupported;
        }

        @Override
        public @NonNull LedgerTransactionSpec assemble(@NonNull FundsInstructionSpec instruction,
                                                       @NonNull String fundsTransactionSn,
                                                       @NonNull ResolvedRouteSpec resolvedRoute) {
            if (unsupported) {
                throw new IllegalArgumentException("Not found supported LedgerPostingAssembler");
            }
            this.instruction.set(instruction);
            this.fundsTransactionSn.set(fundsTransactionSn);
            this.route.set(resolvedRoute);
            return LedgerTransactionSpecFactory.createLedgerTransaction(instruction, fundsTransactionSn,
                    ledgerTransactionSn -> {
                        LedgerPostingPhaseSpec phase = LedgerTransactionSpecFactory.postingPhase(
                                LedgerPhaseCode.TRANSFER,
                                List.of(entry(ledgerTransactionSn, EntrySide.DEBIT),
                                        entry(ledgerTransactionSn, EntrySide.CREDIT))
                        );
                        return List.of(LedgerTransactionSpecFactory.postingPlan(
                                LedgerPostingIntentType.TRANSFER,
                                ledgerTransactionSn,
                                null,
                                LedgerBalanceEffectType.CONSUME,
                                List.of(phase)
                        ));
                    });
        }

        @Override
        public boolean supports(@NonNull ResolvedRouteSpec resolvedRoute) {
            return !unsupported;
        }
    }

    private static LedgerEntrySpec entry(String ledgerTransactionSn, EntrySide entrySide) {
        return FundsTransactionTestSupport.ledgerEntrySpec(
                entrySide == EntrySide.DEBIT ? "funding_001" : "funding_002",
                "FUNDING_ACCOUNT",
                LedgerSubjectCode.AVAILABLE,
                LedgerSubjectCategory.LIABILITY,
                entrySide,
                ledgerTransactionSn,
                "TRANSFER",
                "BIZ_0001",
                100L,
                CurrencyIsoCode.USD,
                LocalDateTime.of(2026, 5, 9, 12, 0)
        ).setBalanceEffectType(LedgerBalanceEffectType.CONSUME)
                .setIntent(LedgerPostingIntentType.TRANSFER)
                .setPhaseCode(LedgerPhaseCode.TRANSFER)
                .setContextVariables(Map.of());
    }

    private static final class RecordingLifecycleSaver implements FundsInstructionLifecycleSaver {

        private final AtomicReference<FundsInstructionSpec> beforePostingInstruction = new AtomicReference<>();

        private final AtomicReference<ResolvedRouteSpec> beforePostingRoute = new AtomicReference<>();

        private final AtomicReference<RouteSnapshotSpec> beforePostingSnapshot = new AtomicReference<>();

        private final AtomicReference<String> succeededLedgerTransactionSn = new AtomicReference<>();

        private final AtomicReference<Throwable> failedCause = new AtomicReference<>();

        private final boolean completed;

        private RecordingLifecycleSaver(boolean completed) {
            this(completed, "FT_001");
        }

        private final String lifecycleSn;

        private RecordingLifecycleSaver(boolean completed, String lifecycleSn) {
            this.completed = completed;
            this.lifecycleSn = lifecycleSn;
        }

        @Override
        public boolean supports(@NonNull FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public FundsInstructionLifecycleResult beforePosting(@NonNull FundsInstructionSpec instruction,
                                                             @NonNull ResolvedRouteSpec resolvedRoute,
                                                             @NonNull RouteSnapshotSpec routeSnapshot) {
            beforePostingInstruction.set(instruction);
            beforePostingRoute.set(resolvedRoute);
            beforePostingSnapshot.set(routeSnapshot);
            return new FundsInstructionLifecycleResult()
                    .setTransactionSn(lifecycleSn)
                    .setTransactionDetailSns(List.of("FTD_001", "FTD_002"))
                    .setCompleted(completed);
        }

        @Override
        public void markSucceeded(@NonNull FundsInstructionSpec instruction,
                                  @NonNull FundsInstructionLifecycleResult result,
                                  @Nullable String ledgerTransactionSn) {
            succeededLedgerTransactionSn.set(ledgerTransactionSn);
        }

        @Override
        public void markFailed(@NonNull FundsInstructionSpec instruction,
                               @NonNull FundsInstructionLifecycleResult result,
                               @NonNull Throwable cause) {
            failedCause.set(cause);
        }

    }

    private static final class RecordingTransactionQueryService implements FundsTransactionQueryService {

        private final AtomicReference<RouteSnapshotSpec> routeSnapshot = new AtomicReference<>();

        private final AtomicReference<RouteSnapshotSpec> freezeOrderRouteSnapshot = new AtomicReference<>();

        private final AtomicReference<String> consumedReplayLegId = new AtomicReference<>();

        @Override
        public @NonNull Optional<FundsTransactionDTO> queryFundsTransaction(@NonNull String transactionSn) {
            return Optional.empty();
        }

        @Override
        public @NonNull List<FundsTransactionDetailDTO> queryFundsTransactionDetails(@NonNull String transactionSn) {
            return List.of();
        }

        @Override
        public boolean hasConsumedReplayLeg(@NonNull String referenceTransactionSn,
                                            @NonNull FundsTransactionEventType eventType,
                                            @NonNull String replayRefLegId) {
            return replayRefLegId.equals(consumedReplayLegId.get());
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByTransactionSn(@NonNull String transactionSn) {
            return Optional.ofNullable(routeSnapshot.get());
        }

        @Override
        public @NonNull Optional<RouteSnapshotSpec> findRouteSnapshotByFreezeOrderSn(@NonNull String freezeOrderSn) {
            return Optional.ofNullable(freezeOrderRouteSnapshot.get());
        }
    }

    private static final class RecordingPostingService implements LedgerTransactionPostingService {

        private final AtomicReference<LedgerTransactionSpec> transaction = new AtomicReference<>();

        private final boolean fail;

        private RecordingPostingService(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void post(LedgerTransactionSpec transaction) {
            this.transaction.set(transaction);
            if (fail) {
                throw new IllegalStateException("posting failed");
            }
        }
    }

    private static class SimpleInstruction implements FundsInstructionSpec {

        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TOPUP;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CurrencyIsoCode.USD);
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
            return "TRANSFER";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "BIZ_0001";
        }

        @Override
        public @NonNull LocalDateTime getEventTime() {
            return LocalDateTime.of(2026, 5, 9, 12, 0);
        }

        @Override
        public @Nullable String getDescription() {
            return "test";
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

    private static FundsOperationActorSpec systemActor() {
        return ImmutableFundsOperationActorSpec.builder()
                .operatorId(-1L)
                .operatorType("SYSTEM")
                .operatorName("SYSTEM")
                .appName("capte-tests")
                .build();
    }

    private static final class ReferencedInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        private ReferencedInstruction(FundsTransactionEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.REFUND;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return ImmutableFundsInstructionReferenceSpec.builder()
                    .referenceType(FundsInstructionReferenceType.ORIGINAL_TRANSACTION)
                    .referenceSn("FT_ORIGINAL")
                    .referenceBusinessSn("BIZ_ORIGINAL")
                    .contextVariables(Map.of())
                    .build();
        }
    }

    private static final class FreezeOrderReferencedInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        private FreezeOrderReferencedInstruction(FundsTransactionEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return eventType;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.ADJUSTMENT;
        }

        @Override
        public @Nullable FundsInstructionReferenceSpec getReference() {
            return ImmutableFundsInstructionReferenceSpec.builder()
                    .referenceType(FundsInstructionReferenceType.FREEZE_ORDER)
                    .referenceSn("FO_001")
                    .contextVariables(Map.of())
                    .build();
        }
    }

    private static final class BalanceControlInstruction extends SimpleInstruction {

        private final FundsTransactionEventType eventType;

        private BalanceControlInstruction(FundsTransactionEventType eventType) {
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

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.ADJUSTMENT;
        }
    }

    private static final class DirectRefundInstruction extends SimpleInstruction {

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.REFUND;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.REFUND;
        }
    }

    private static final class SimpleResolvedRoute implements ResolvedRouteSpec {

        private final List<RouteLegSpec> legs;

        private SimpleResolvedRoute(List<RouteLegSpec> legs) {
            this.legs = legs;
        }

        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public @NonNull String getRouteCode() {
            return "WALLET_DIRECT";
        }

        @Override
        public @NonNull String getRouteVersion() {
            return "v2";
        }

        @Override
        public @NonNull String getBusinessScene() {
            return "TRANSFER";
        }

        @Override
        public @NonNull String getBusinessSn() {
            return "BIZ_0001";
        }

        @Override
        public @NonNull FundsInstructionType getInstructionType() {
            return FundsInstructionType.DIRECT_TRANSACTION;
        }

        @Override
        public @NonNull FundsTransactionEventType getEventType() {
            return FundsTransactionEventType.TOPUP;
        }

        @Override
        public @NonNull DefaultFundsTransactionType getTransactionType() {
            return DefaultFundsTransactionType.TRANSFER;
        }

        @Override
        public @NonNull List<RouteParticipantSpec> getParticipants() {
            return List.of();
        }

        @Override
        public @NonNull List<RouteLegSpec> getLegs() {
            return legs;
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

    private static final class SimpleRouteLeg implements RouteLegSpec {

        private final RouteReplayPolicy replayPolicy;

        private final RouteNodeSpec sourceNode = new SimpleRouteNode("funding_001", RouteNodeRole.SOURCE);

        private final RouteNodeSpec targetNode = new SimpleRouteNode("funding_002", RouteNodeRole.TARGET);

        private final LedgerPhaseCode phaseCode;

        private SimpleRouteLeg(RouteReplayPolicy replayPolicy) {
            this(replayPolicy, LedgerPhaseCode.TRANSFER);
        }

        private SimpleRouteLeg(RouteReplayPolicy replayPolicy, LedgerPhaseCode phaseCode) {
            this.replayPolicy = replayPolicy;
            this.phaseCode = phaseCode;
        }

        @Override
        public @NonNull String getLegId() {
            return "LEG_001";
        }

        @Override
        public @NonNull RouteLegType getLegType() {
            return RouteLegType.INTERNAL_TRANSFER;
        }

        @Override
        public @NonNull RouteNodeSpec getSourceNode() {
            return sourceNode;
        }

        @Override
        public @NonNull RouteNodeSpec getTargetNode() {
            return targetNode;
        }

        @Override
        public @NonNull Money getAmount() {
            return Money.immutable(100L, CurrencyIsoCode.USD);
        }

        @Override
        public @NonNull LedgerBalanceEffectType getBalanceEffectType() {
            return LedgerBalanceEffectType.CONSUME;
        }

        @Override
        public @NonNull LedgerPhaseCode getPhaseCode() {
            return phaseCode;
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
            return replayPolicy;
        }
    }

    private static final class SimpleRouteNode implements RouteNodeSpec {

        private final SubjectRef subjectRef;

        private final RouteNodeRole nodeRole;

        private SimpleRouteNode(String subjectId, RouteNodeRole nodeRole) {
            this.subjectRef = new SimpleSubjectRef(subjectId);
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
            return LedgerSubjectCode.AVAILABLE;
        }

        @Override
        public @NonNull RouteNodeRole getNodeRole() {
            return nodeRole;
        }
    }

    private static final class SimpleSubjectRef implements SubjectRef {

        private final String subjectId;

        private SimpleSubjectRef(String subjectId) {
            this.subjectId = subjectId;
        }

        @Override
        public Long getTenantId() {
            return 1001L;
        }

        @Override
        public @NonNull String getSubjectId() {
            return subjectId;
        }

        @Override
        public @NonNull FundsSubjectType getSubjectType() {
            return FundsSubjectType.FUNDING_ACCOUNT;
        }
    }
}
