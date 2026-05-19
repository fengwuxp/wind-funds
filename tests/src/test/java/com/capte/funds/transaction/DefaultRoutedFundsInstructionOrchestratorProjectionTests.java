package com.capte.funds.transaction;

import com.capte.funds.transaction.model.dto.FundsInstructionLifecycleResult;
import com.capte.funds.transaction.projection.FundsTransactionProjectionPublishContext;
import com.capte.funds.transaction.projection.FundsTransactionProjectionPublisher;
import com.capte.funds.transaction.services.FundsInstructionLifecycleRecorder;
import com.wind.integration.funds.ledger.LedgerPostingAssembler;
import com.wind.integration.funds.ledger.LedgerTransactionPostingService;
import com.wind.integration.funds.ledger.enums.LedgerBalanceEffectType;
import com.wind.integration.funds.ledger.enums.LedgerPhaseCode;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.integration.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.integration.funds.model.operation.ImmutableFundsOperationActorSpec;
import com.wind.integration.funds.model.route.ImmutableResolvedRouteSpec;
import com.wind.integration.funds.model.route.ImmutableRouteLegSpec;
import com.wind.integration.funds.model.route.ImmutableRouteNodeSpec;
import com.wind.integration.funds.model.route.ImmutableRouteSnapshotSpec;
import com.wind.integration.funds.model.route.ImmutableSubjectRef;
import com.wind.integration.funds.model.transaction.ImmutableFundsInstructionSpec;
import com.wind.integration.funds.route.RouteResolver;
import com.wind.integration.funds.route.RouteSnapshotFactory;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.wind.integration.funds.route.enums.RouteLegType;
import com.wind.integration.funds.route.enums.RouteNodeRole;
import com.wind.integration.funds.route.enums.RouteNodeType;
import com.wind.integration.funds.route.spec.ResolvedRouteSpec;
import com.wind.integration.funds.route.spec.RouteLegSpec;
import com.wind.integration.funds.route.spec.RouteNodeSpec;
import com.wind.integration.funds.route.spec.RouteSnapshotSpec;
import com.wind.integration.funds.spec.ledger.LedgerPostingPlanSpec;
import com.wind.integration.funds.spec.ledger.LedgerTransactionSpec;
import com.wind.integration.funds.spec.transaction.FundsInstructionSpec;
import com.wind.integration.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.integration.funds.transaction.enums.FundsInstructionType;
import com.wind.integration.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.Money;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 交易主链路投影入口测试。
 */
class DefaultRoutedFundsInstructionOrchestratorProjectionTests {

    private static final Long TENANT_ID = 1001L;

    private static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 5, 19, 14, 30);

    /**
     * 场景：资金指令路由、生命周期和账本入账均成功。
     * 输入：一笔包含账务 leg 的支付指令。
     * 输出：交易主链路发布普通交易投影上下文。
     * 预期：投影上下文包含资金指令、Route、RouteSnapshot、生命周期结果和账本交易流水。
     * 红线：交易成功后不能缺失普通交易投影入口。
     */
    @Test
    void testSuccessfulPostingShouldPublishTransactionProjection() {
        FundsInstructionSpec instruction = instruction();
        ResolvedRouteSpec route = resolvedRoute();
        LedgerTransactionSpec ledgerTransaction = ledgerTransaction();
        RecordingLifecycleRecorder lifecycleRecorder = new RecordingLifecycleRecorder();
        RecordingLedgerPostingService postingService = new RecordingLedgerPostingService();
        RecordingProjectionPublisher projectionPublisher = new RecordingProjectionPublisher();
        DefaultRoutedFundsInstructionOrchestrator orchestrator = orchestrator(route, ledgerTransaction,
                postingService, lifecycleRecorder, List.of(projectionPublisher));

        String transactionSn = orchestrator.execute(instruction);

        assertThat(transactionSn).isEqualTo("FT-202605190001");
        assertThat(postingService.postedTransactions()).containsExactly(ledgerTransaction);
        assertThat(lifecycleRecorder.succeededLedgerTransactionSn()).isEqualTo("LT-202605190001");
        assertThat(lifecycleRecorder.failedCause()).isNull();
        assertThat(projectionPublisher.contexts()).singleElement().satisfies(context -> {
            assertThat(context.instruction()).isSameAs(instruction);
            assertThat(context.resolvedRoute()).isSameAs(route);
            assertThat(context.routeSnapshot().getSnapshotId()).isEqualTo("RS-202605190001");
            assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo("FT-202605190001");
            assertThat(context.lifecycleResult().getLedgerTransactionSn()).isEqualTo("LT-202605190001");
            assertThat(context.ledgerTransaction()).isSameAs(ledgerTransaction);
        });
    }

    /**
     * 场景：账本入账和生命周期保存已成功，但普通交易投影发布失败。
     * 输入：投影发布器抛出运行时异常。
     * 输出：编排器返回交易流水，保留已成功的交易和账本事实。
     * 预期：投影失败被隔离，后续由归档重放治理模块校验或修复。
     * 红线：普通投影失败不得触发交易事实或账本事实回滚。
     */
    @Test
    void testProjectionFailureShouldNotRollbackPostedFacts() {
        ResolvedRouteSpec route = resolvedRoute();
        LedgerTransactionSpec ledgerTransaction = ledgerTransaction();
        RecordingLifecycleRecorder lifecycleRecorder = new RecordingLifecycleRecorder();
        RecordingLedgerPostingService postingService = new RecordingLedgerPostingService();
        ThrowingProjectionPublisher projectionPublisher = new ThrowingProjectionPublisher();
        DefaultRoutedFundsInstructionOrchestrator orchestrator = orchestrator(route, ledgerTransaction,
                postingService, lifecycleRecorder, List.of(projectionPublisher));

        String transactionSn = orchestrator.execute(instruction());

        assertThat(transactionSn).isEqualTo("FT-202605190001");
        assertThat(postingService.postedTransactions()).containsExactly(ledgerTransaction);
        assertThat(lifecycleRecorder.succeededLedgerTransactionSn()).isEqualTo("LT-202605190001");
        assertThat(lifecycleRecorder.failedCause()).isNull();
        assertThat(projectionPublisher.invocationCount()).isOne();
    }

    /**
     * 场景：编排器运行在 Spring 事务同步上下文中。
     * 输入：交易主链路执行成功，事务尚未提交。
     * 输出：普通交易投影注册为提交后发布。
     * 预期：提交前不发布，提交后才发布。
     * 红线：普通交易投影不得先于交易事实和账本事实提交。
     */
    @Test
    void testProjectionShouldPublishAfterTransactionCommitWhenSynchronizationActive() {
        ResolvedRouteSpec route = resolvedRoute();
        LedgerTransactionSpec ledgerTransaction = ledgerTransaction();
        RecordingProjectionPublisher projectionPublisher = new RecordingProjectionPublisher();
        DefaultRoutedFundsInstructionOrchestrator orchestrator = orchestrator(route, ledgerTransaction,
                new RecordingLedgerPostingService(), new RecordingLifecycleRecorder(), List.of(projectionPublisher));

        TransactionSynchronizationManager.initSynchronization();
        try {
            String transactionSn = orchestrator.execute(instruction());
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

            assertThat(transactionSn).isEqualTo("FT-202605190001");
            assertThat(projectionPublisher.contexts()).isEmpty();
            assertThat(synchronizations).hasSize(1);

            synchronizations.forEach(TransactionSynchronization::afterCommit);

            assertThat(projectionPublisher.contexts()).singleElement().satisfies(context -> {
                assertThat(context.lifecycleResult().getTransactionSn()).isEqualTo("FT-202605190001");
                assertThat(context.ledgerTransaction()).isSameAs(ledgerTransaction);
            });
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static DefaultRoutedFundsInstructionOrchestrator orchestrator(ResolvedRouteSpec route,
                                                                         LedgerTransactionSpec ledgerTransaction,
                                                                         RecordingLedgerPostingService postingService,
                                                                         RecordingLifecycleRecorder lifecycleRecorder,
                                                                         List<FundsTransactionProjectionPublisher> publishers) {
        return new DefaultRoutedFundsInstructionOrchestrator(new FixedRouteResolver(route),
                new CopyingRouteSnapshotFactory(), new FixedPostingAssembler(ledgerTransaction), postingService,
                lifecycleRecorder, publishers);
    }

    private static FundsInstructionSpec instruction() {
        return ImmutableFundsInstructionSpec.builder()
                .tenantId(TENANT_ID)
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .amount(amount())
                .businessScene("ORDER_PAY")
                .businessSn("ORDER-202605190001")
                .eventTime(OCCURRED_AT)
                .operator(ImmutableFundsOperationActorSpec.builder()
                        .operatorId(10001L)
                        .operatorType("SYSTEM")
                        .appName("funds-tests")
                        .build())
                .build();
    }

    private static ResolvedRouteSpec resolvedRoute() {
        return ImmutableResolvedRouteSpec.builder()
                .tenantId(TENANT_ID)
                .routeCode("DIRECT_PAY")
                .routeVersion("v1")
                .businessScene("ORDER_PAY")
                .businessSn("ORDER-202605190001")
                .instructionType(FundsInstructionType.DIRECT_TRANSACTION)
                .eventType(FundsTransactionEventType.PAY)
                .transactionType(DefaultFundsTransactionType.PAY)
                .legs(List.of(routeLeg()))
                .resolvedAt(OCCURRED_AT)
                .build();
    }

    private static RouteLegSpec routeLeg() {
        return ImmutableRouteLegSpec.builder()
                .legId("LEG-1")
                .sequence(1)
                .legType(RouteLegType.INTERNAL_TRANSFER)
                .sourceNode(routeNode("FA-PAYER", RouteNodeRole.SOURCE))
                .targetNode(routeNode("FA-MERCHANT", RouteNodeRole.TARGET))
                .amount(amount())
                .balanceEffectType(LedgerBalanceEffectType.DECREASE)
                .phaseCode(LedgerPhaseCode.TRANSFER)
                .build();
    }

    private static RouteNodeSpec routeNode(String subjectId, RouteNodeRole role) {
        return ImmutableRouteNodeSpec.builder()
                .nodeType(RouteNodeType.SUBJECT)
                .subjectRef(ImmutableSubjectRef.builder()
                        .tenantId(TENANT_ID)
                        .subjectId(subjectId)
                        .subjectType(FundsSubjectType.FUNDING_ACCOUNT)
                        .currency(CurrencyIsoCode.USD.name())
                        .build())
                .ledgerSubjectCode(LedgerSubjectCode.AVAILABLE)
                .nodeRole(role)
                .build();
    }

    private static Money amount() {
        return Money.immutable(100L, CurrencyIsoCode.USD);
    }

    private static LedgerTransactionSpec ledgerTransaction() {
        return new LedgerTransactionSpec() {

            @Override
            public Long getTenantId() {
                return TENANT_ID;
            }

            @Override
            public String getSn() {
                return "LT-202605190001";
            }

            @Override
            public FundsTransactionEventType getEventType() {
                return FundsTransactionEventType.PAY;
            }

            @Override
            public LedgerTransactionStatus getStatus() {
                return LedgerTransactionStatus.POSTED;
            }

            @Override
            public Money getAmount() {
                return amount();
            }

            @Override
            public String getBusinessSn() {
                return "ORDER-202605190001";
            }

            @Override
            public String getFundsTransactionSn() {
                return "FT-202605190001";
            }

            @Override
            public DefaultFundsTransactionType getTransactionType() {
                return DefaultFundsTransactionType.PAY;
            }

            @Override
            public String getBusinessScene() {
                return "ORDER_PAY";
            }

            @Override
            public String getReferenceLedgerTransactionSn() {
                return "";
            }

            @Override
            public LocalDateTime getTransactionTime() {
                return OCCURRED_AT;
            }

            @Override
            public String getDescription() {
                return "test ledger transaction";
            }

            @Override
            public List<LedgerPostingPlanSpec> getPostingPlans() {
                return List.of();
            }

            @Override
            public Map<String, Object> getContextVariables() {
                return Map.of();
            }
        };
    }

    private record FixedRouteResolver(ResolvedRouteSpec route) implements RouteResolver {

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public ResolvedRouteSpec resolve(FundsInstructionSpec instruction) {
            return route;
        }
    }

    private static final class CopyingRouteSnapshotFactory implements RouteSnapshotFactory {

        @Override
        public RouteSnapshotSpec createSnapshot(ResolvedRouteSpec resolvedRoute) {
            return ImmutableRouteSnapshotSpec.builder()
                    .tenantId(resolvedRoute.getTenantId())
                    .snapshotId("RS-202605190001")
                    .snapshotSchemaVersion("v1")
                    .routeCode(resolvedRoute.getRouteCode())
                    .routeVersion(resolvedRoute.getRouteVersion())
                    .businessScene(resolvedRoute.getBusinessScene())
                    .businessSn(resolvedRoute.getBusinessSn())
                    .instructionType(resolvedRoute.getInstructionType())
                    .eventType(resolvedRoute.getEventType())
                    .transactionType(resolvedRoute.getTransactionType())
                    .participants(resolvedRoute.getParticipants())
                    .legs(resolvedRoute.getLegs())
                    .resolvedAt(resolvedRoute.getResolvedAt())
                    .build();
        }
    }

    private record FixedPostingAssembler(LedgerTransactionSpec ledgerTransaction)
            implements LedgerPostingAssembler<ResolvedRouteSpec> {

        @Override
        public LedgerTransactionSpec assemble(FundsInstructionSpec instruction,
                                              String fundsTransactionSn,
                                              ResolvedRouteSpec resolvedRoute) {
            return ledgerTransaction;
        }

        @Override
        public boolean supports(ResolvedRouteSpec resolvedRoute) {
            return true;
        }
    }

    private static final class RecordingLedgerPostingService implements LedgerTransactionPostingService {

        private final List<LedgerTransactionSpec> postedTransactions = new ArrayList<>();

        @Override
        public void post(LedgerTransactionSpec transaction) {
            postedTransactions.add(transaction);
        }

        private List<LedgerTransactionSpec> postedTransactions() {
            return postedTransactions;
        }
    }

    private static final class RecordingLifecycleRecorder implements FundsInstructionLifecycleRecorder {

        private final FundsInstructionLifecycleResult result = new FundsInstructionLifecycleResult()
                .setTransactionSn("FT-202605190001")
                .setCompleted(false);

        private String succeededLedgerTransactionSn;

        private Throwable failedCause;

        @Override
        public boolean supports(FundsInstructionSpec instruction) {
            return true;
        }

        @Override
        public FundsInstructionLifecycleResult beforePosting(FundsInstructionSpec instruction,
                                                             ResolvedRouteSpec resolvedRoute,
                                                             RouteSnapshotSpec routeSnapshot) {
            return result;
        }

        @Override
        public void markSucceeded(FundsInstructionSpec instruction,
                                  FundsInstructionLifecycleResult result,
                                  String ledgerTransactionSn) {
            this.succeededLedgerTransactionSn = ledgerTransactionSn;
            result.setLedgerTransactionSn(ledgerTransactionSn)
                    .setCompleted(true);
        }

        @Override
        public void markFailed(FundsInstructionSpec instruction,
                               FundsInstructionLifecycleResult result,
                               Throwable cause) {
            this.failedCause = cause;
        }

        private String succeededLedgerTransactionSn() {
            return succeededLedgerTransactionSn;
        }

        private Throwable failedCause() {
            return failedCause;
        }
    }

    private static final class RecordingProjectionPublisher implements FundsTransactionProjectionPublisher {

        private final List<FundsTransactionProjectionPublishContext> contexts = new ArrayList<>();

        @Override
        public void publish(FundsTransactionProjectionPublishContext context) {
            contexts.add(context);
        }

        private List<FundsTransactionProjectionPublishContext> contexts() {
            return contexts;
        }
    }

    private static final class ThrowingProjectionPublisher implements FundsTransactionProjectionPublisher {

        private int invocationCount;

        @Override
        public void publish(FundsTransactionProjectionPublishContext context) {
            invocationCount++;
            throw new IllegalStateException("projection unavailable");
        }

        private int invocationCount() {
            return invocationCount;
        }
    }
}
