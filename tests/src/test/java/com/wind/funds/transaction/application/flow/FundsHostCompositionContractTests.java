package com.wind.funds.transaction.application.flow;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.governance.enums.ProjectionReplayMode;
import com.wind.funds.governance.enums.ProjectionReplayTaskStatus;
import com.wind.funds.governance.projection.CreateFundsProjectionReplayTaskRequest;
import com.wind.funds.governance.projection.FundsProjectionReplayApplicationService;
import com.wind.funds.governance.projection.FundsProjectionReplayService;
import com.wind.funds.governance.projection.FundsProjectionReplayTaskDTO;
import com.wind.funds.governance.projection.FundsTransactionProjectionDifference;
import com.wind.funds.governance.projection.FundsTransactionProjectionReplayResult;
import com.wind.funds.governance.projection.FundsTransactionProjectionReplayRange;
import com.wind.funds.governance.projection.RunFundsProjectionReplayTaskRequest;
import com.wind.funds.governance.projection.impl.DefaultFundsTransactionProjectionReplaySource;
import com.wind.funds.governance.projection.impl.DefaultFundsTransactionProjectionWriter;
import com.wind.funds.support.FundsBalanceAssertionSupport.BalanceSnapshot;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.dal.entities.FundsTransaction;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplainApplicationService;
import com.wind.funds.transaction.projection.FundsTransactionProjectionExplanation;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanBatch;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanCursor;
import com.wind.funds.transaction.projection.FundsTransactionProjectionScanQuery;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.integration.operator.WindOperatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.wind.funds.support.FundsBalanceAssertionSupport.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 资金模块最小宿主装配与持久投影恢复契约测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        FundsTransactionFlowTestSupport.Config.class,
        FundsHostCompositionContractTests.GovernanceConfig.class
})
class FundsHostCompositionContractTests extends FundsTransactionFlowTestSupport {

    @Autowired
    private FundsTransactionProjectionExplainApplicationService projectionExplainApplicationService;

    @Autowired
    private FundsProjectionReplayApplicationService projectionReplayApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testBoundedProjectionScanShouldUseStableCursorForTransactionFreezeAndUnfreezeFacts() {
        FundsAccountId accountId = fundingAccount("funding_user");
        topup(accountId, 100L, "PROJECTION_SCAN_TOPUP");
        String topupTransactionSn = fundsTransactionsByBusinessSn("PROJECTION_SCAN_TOPUP").getFirst().getSn();
        String freezeSn = freeze(accountId, 30L, "PROJECTION_SCAN_FREEZE");
        unfreeze(accountId, 10L, freezeSn, "PROJECTION_SCAN_UNFREEZE");

        FundsTransactionProjectionScanQuery initialQuery = FundsTransactionProjectionScanQuery.builder()
                .tenantId(TENANT_ID)
                .eventTypes(Set.of(FundsTransactionEventType.TOPUP,
                        FundsTransactionEventType.FREEZE, FundsTransactionEventType.UNFREEZE))
                .startTime(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endTime(LocalDateTime.of(2027, 1, 1, 0, 0))
                .maxBatchSize(1)
                .build();
        FundsTransactionProjectionScanCursor cursor = projectionExplainApplicationService
                .initializeScanCursor(initialQuery);

        topup(accountId, 5L, "PROJECTION_SCAN_LATE_TOPUP");
        String lateTransactionSn = fundsTransactionsByBusinessSn("PROJECTION_SCAN_LATE_TOPUP").getFirst().getSn();

        List<FundsTransactionProjectionExplanation> facts = new ArrayList<>();
        boolean hasMore;
        do {
            FundsTransactionProjectionScanBatch batch = projectionExplainApplicationService.scan(
                    initialQuery.withCursor(cursor));
            facts.addAll(batch.facts());
            cursor = batch.nextCursor();
            hasMore = batch.hasMore();
        } while (hasMore);

        assertThat(facts).extracting(FundsTransactionProjectionExplanation::eventType)
                .contains(FundsTransactionEventType.TOPUP,
                        FundsTransactionEventType.FREEZE, FundsTransactionEventType.UNFREEZE);
        assertThat(facts).extracting(FundsTransactionProjectionExplanation::fundsTransactionSn)
                .contains(topupTransactionSn, freezeSn)
                .doesNotContain(lateTransactionSn);
        assertThat(facts).allSatisfy(fact -> {
            assertThat(fact.tenantId()).isEqualTo(TENANT_ID);
            assertThat(fact.ownerId()).isEqualTo(accountId.id());
        });
    }

    @Test
    void testPersistentReplayTaskShouldResumeAndLimitWritesToProjectionTables() {
        FundsAccountId accountId = fundingAccount("funding_user");
        topup(accountId, 100L, "PROJECTION_REPLAY_TOPUP");
        FundsTransaction transaction = fundsTransactionsByBusinessSn("PROJECTION_REPLAY_TOPUP").getFirst();
        BalanceSnapshot balancesBefore = snapshot(balances(accountId, cashMappingAccount(), prepaymentAccount()));
        LedgerFactSnapshot ledgerFactsBefore = ledgerFactSnapshot();

        FundsProjectionReplayTaskDTO verifyTask = createTask("REPLAY-VERIFY-1", "digest-verify",
                ProjectionReplayMode.VERIFY_ONLY, transaction.getSn(), null, null);
        assertThat(projectionReplayApplicationService.queryBacklog(TENANT_ID, 10,
                WindOperatorFactory.system())).extracting(FundsProjectionReplayTaskDTO::taskSn)
                .contains(verifyTask.taskSn());
        FundsProjectionReplayTaskDTO duplicate = createTask("REPLAY-VERIFY-1", "digest-verify",
                ProjectionReplayMode.VERIFY_ONLY, transaction.getSn(), null, null);
        assertThat(duplicate.taskSn()).isEqualTo(verifyTask.taskSn());

        projectionReplayApplicationService.runTask(RunFundsProjectionReplayTaskRequest.builder()
                .tenantId(TENANT_ID)
                .taskSn(verifyTask.taskSn())
                .maxBatchSize(1)
                .build(), WindOperatorFactory.system());
        FundsProjectionReplayTaskDTO completedVerify = task(verifyTask.taskSn());
        assertThat(completedVerify.status()).isEqualTo(ProjectionReplayTaskStatus.COMPLETED);
        assertThat(completedVerify.differenceCount()).isPositive();
        assertThat(projectionRowCount("OFFICIAL", "OFFICIAL")).isZero();
        assertThat(projectionRowCount("SHADOW", verifyTask.taskSn())).isZero();

        FundsProjectionReplayTaskDTO shadowTask = createTask("REPLAY-SHADOW-1", "digest-shadow",
                ProjectionReplayMode.REBUILD_SHADOW, transaction.getSn(), null, null);
        run(shadowTask);
        assertThat(projectionRowCount("SHADOW", shadowTask.taskSn())).isEqualTo(1);
        assertThat(projectionRowCount("OFFICIAL", "OFFICIAL")).isZero();

        FundsProjectionReplayTaskDTO applyTask = createTask("REPLAY-APPLY-1", "digest-apply",
                ProjectionReplayMode.REBUILD_APPLY, transaction.getSn(), "APPROVAL-1", shadowTask.taskSn());
        run(applyTask);
        assertThat(projectionRowCount("OFFICIAL", "OFFICIAL")).isEqualTo(1);
        assertThat(ledgerFactSnapshot()).isEqualTo(ledgerFactsBefore);
        assertThat(snapshot(balances(accountId, cashMappingAccount(), prepaymentAccount())))
                .isEqualTo(balancesBefore);

        jdbcTemplate.update("""
                UPDATE t_funds_transaction_projection
                SET owner_type = 'CORRUPTED', owner_id = 'CORRUPTED', source_sn = 'CORRUPTED',
                    occurred_time = TIMESTAMP '2020-01-01 00:00:00'
                WHERE tenant_id = ? AND projection_scope = 'OFFICIAL' AND scope_ref = 'OFFICIAL'
                """, TENANT_ID);
        FundsProjectionReplayTaskDTO driftTask = createTask("REPLAY-VERIFY-DRIFT", "digest-verify-drift",
                ProjectionReplayMode.VERIFY_ONLY, transaction.getSn(), null, null);
        FundsTransactionProjectionReplayResult driftResult = run(driftTask);
        assertThat(driftResult.differences())
                .extracting(FundsTransactionProjectionDifference::fieldName)
                .contains("ownerType", "ownerId", "sourceSn", "occurredTime");
    }

    @Test
    void testReplayFailureShouldNotAdvancePersistentCheckpoint() {
        FundsAccountId accountId = fundingAccount("funding_user");
        topup(accountId, 100L, "PROJECTION_REPLAY_FAILURE_TOPUP");
        String transactionSn = fundsTransactionsByBusinessSn("PROJECTION_REPLAY_FAILURE_TOPUP").getFirst().getSn();
        FundsProjectionReplayTaskDTO task = createTask("REPLAY-FAILURE-1", "digest-failure",
                ProjectionReplayMode.VERIFY_ONLY, transactionSn, null, null);
        String checkpointBefore = task.checkpoint().checkpointSn();
        clearFundsTransactionRouteSnapshot(transactionSn);

        assertThatThrownBy(() -> run(task)).hasMessageContaining("RouteSnapshot");

        FundsProjectionReplayTaskDTO unchanged = task(task.taskSn());
        assertThat(unchanged.status()).isEqualTo(ProjectionReplayTaskStatus.CREATED);
        assertThat(unchanged.checkpoint().checkpointSn()).isEqualTo(checkpointBefore);
        assertThat(unchanged.successCount()).isZero();
    }

    @Test
    void testPersistentReplayTaskShouldRejectUnsupportedBatchRangeBeforeCreation() {
        CreateFundsProjectionReplayTaskRequest request = CreateFundsProjectionReplayTaskRequest.builder()
                .requestSn("REPLAY-BATCH-UNSUPPORTED")
                .requestDigest("digest-batch")
                .tenantId(TENANT_ID)
                .viewDomain("USER_BILL")
                .mode(ProjectionReplayMode.VERIFY_ONLY)
                .replayRange(FundsTransactionProjectionReplayRange.builder()
                        .batchType("CLEARING_BATCH")
                        .batchSn("CB-1")
                        .build())
                .reason("recover batch projection")
                .auditRef("AUDIT-W2-01B")
                .build();

        assertThatThrownBy(() -> projectionReplayApplicationService.createTask(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("不支持批次范围");
        assertThat(projectionReplayApplicationService.queryBacklog(TENANT_ID, 10,
                WindOperatorFactory.system())).isEmpty();
    }

    private FundsProjectionReplayTaskDTO createTask(String requestSn,
                                                    String requestDigest,
                                                    ProjectionReplayMode mode,
                                                    String sourceSn,
                                                    String approvalRef,
                                                    String validatedShadowTaskSn) {
        return projectionReplayApplicationService.createTask(CreateFundsProjectionReplayTaskRequest.builder()
                .requestSn(requestSn)
                .requestDigest(requestDigest)
                .tenantId(TENANT_ID)
                .viewDomain("USER_BILL")
                .mode(mode)
                .replayRange(FundsTransactionProjectionReplayRange.builder().sourceSn(sourceSn).build())
                .reason("recover committed projection")
                .auditRef("AUDIT-W2-01B")
                .approvalRef(approvalRef)
                .validatedShadowTaskSn(validatedShadowTaskSn)
                .build(), WindOperatorFactory.system());
    }

    private FundsTransactionProjectionReplayResult run(FundsProjectionReplayTaskDTO task) {
        return projectionReplayApplicationService.runTask(RunFundsProjectionReplayTaskRequest.builder()
                .tenantId(TENANT_ID)
                .taskSn(task.taskSn())
                .maxBatchSize(1)
                .build(), WindOperatorFactory.system());
    }

    private FundsProjectionReplayTaskDTO task(String taskSn) {
        return projectionReplayApplicationService.getTask(TENANT_ID, taskSn, WindOperatorFactory.system());
    }

    private int projectionRowCount(String scope, String scopeRef) {
        Integer result = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_funds_transaction_projection
                WHERE tenant_id = ? AND projection_scope = ? AND scope_ref = ?
                """, Integer.class, TENANT_ID, scope, scopeRef);
        return result == null ? 0 : result;
    }

    @Configuration
    @Import({
            FundsProjectionReplayService.class,
            DefaultFundsTransactionProjectionReplaySource.class,
            DefaultFundsTransactionProjectionWriter.class
    })
    static class GovernanceConfig {
    }
}
