package com.wind.funds.reconciliation.application.run.impl;

import com.wind.common.query.WindPagination;
import com.wind.common.query.supports.DefaultOrderField;
import com.wind.common.query.supports.DefaultPageQueryOptions;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.application.batch.impl.ReconciliationBatchApplicationServiceImpl;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.enums.ReconciliationBatchState;
import com.wind.funds.reconciliation.enums.ReconciliationRunOutcome;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationMatchResultDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import com.wind.integration.core.context.TenantContextHolder;
import com.wind.integration.operator.WindOperatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
import static com.wind.funds.reconciliation.ReconciliationTestFixture.identity;
import static com.wind.funds.reconciliation.ReconciliationTestFixture.rule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 对账运行结果应用服务流程测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationRunResultApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationRunResultApplicationServiceTests extends AbstractFundsServiceTest {

    @Autowired
    private ReconciliationBatchApplicationService reconciliationBatchApplicationService;

    @Autowired
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRunResults() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_source_item");
        jdbcTemplate.update("DELETE FROM t_reconciliation_source_snapshot");
        jdbcTemplate.update("DELETE FROM t_reconciliation_batch_lineage");
        jdbcTemplate.update("DELETE FROM t_reconciliation_batch");
    }

    @Test
    void testRecordShouldRejectTenantDifferentFromCurrentContext() {
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
        assertThat(runResultCount()).isZero();
    }

    /**
     * 场景：可信 matcher 一次提交超过单次原子封版容量的逐笔匹配结论。
     * 结果：在查询和锁定批次前快速失败，不生成运行结果或逐笔匹配事实。
     */
    @Test
    void testRecordShouldRejectOversizedMatchResultsBeforeBatchLookup() {
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn("missing-batch");

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("对账批次不存在");

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
    }

    @Test
    void testRecordShouldRejectOversizedMatchReferencesBeforeBatchLookup() {
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn("missing-batch"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账批次不存在");
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                new RecordReconciliationRunResultRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn("missing-batch"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账批次不存在");

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
    }

    /**
     * 场景：冻结来源已经完整匹配。
     * 结果：记录不可变 BALANCED 结果并把批次原子推进到 COMPLETED；重放返回原结果。
     * 红线：记录对账结果不得创建或修改交易、posting、ledger transaction 或 ledger entry。
     */
    @Test
    void testRecordShouldPersistBalancedResultAndReuseSameBusinessFact() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);
        RecordReconciliationRunResultRequest request = readyRequest(List.of("001"));

        ReconciliationRunResultDTO first = reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system());
        ReconciliationRunResultDTO replay = reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system());

        assertThat(first.getSn()).isNotBlank();
        assertThat(first.getResultDigest()).hasSize(64);
        assertThat(first.getReferenceSourceDigest()).hasSize(64);
        assertThat(first.getComparisonSourceDigest()).hasSize(64);
        assertThat(first.getSourceDigest()).hasSize(64);
        assertThat(first.getOutcome()).isEqualTo(ReconciliationRunOutcome.BALANCED);
        assertThat(first.getTotalCount()).isOne();
        assertThat(first.getMatchedCount()).isOne();
        assertThat(first.getDifferenceCount()).isZero();
        assertThat(first.getEvidenceRefs()).containsExactly("report:comparison", "report:reference");
        assertThat(replay.getSn()).isEqualTo(first.getSn());
        assertThat(replay.getResultDigest()).isEqualTo(first.getResultDigest());
        assertThat(batchState(request.getReconciliationBatchSn())).isEqualTo(ReconciliationBatchState.COMPLETED.name());
        assertThat(runResultCount()).isOne();
        assertThat(matchResultCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    @Test
    void testQueryShouldReadRunResultAndPageMatchEvidence() {
        ReconciliationRunResultDTO recorded = reconciliationRunResultApplicationService.executeStrictExact(
                readyRequest(List.of("001", "002")), WindOperatorFactory.system());

        ReconciliationRunResultDTO queried = reconciliationRunResultApplicationService.getRunResult(
                TENANT_ID, recorded.getSn());
        WindPagination<ReconciliationMatchResultDTO> firstPage =
                reconciliationRunResultApplicationService.queryMatchResults(
                        TENANT_ID, recorded.getSn(), DefaultPageQueryOptions.defaults(1, 1));
        WindPagination<ReconciliationMatchResultDTO> secondPage =
                reconciliationRunResultApplicationService.queryMatchResults(
                        TENANT_ID, recorded.getSn(), DefaultPageQueryOptions.defaults(2, 1));

        assertThat(queried.getResultDigest()).isEqualTo(recorded.getResultDigest());
        assertThat(firstPage.getTotal()).isEqualTo(2);
        assertThat(firstPage.getRecords())
                .extracting(ReconciliationMatchResultDTO::getEvidenceRefs)
                .allMatch(evidenceRefs -> !evidenceRefs.isEmpty());
        assertThat(secondPage.getRecords())
                .extracting(ReconciliationMatchResultDTO::getEvidenceRefs)
                .allMatch(evidenceRefs -> !evidenceRefs.isEmpty());
    }

    @Test
    void testQueryShouldRejectCrossTenantOrMissingRunResult() {
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.getRunResult(
                TENANT_ID + 1, "missing-run-result"))
                .hasMessageContaining("tenantId 与当前租户不一致");
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.queryMatchResults(
                TENANT_ID, "missing-run-result", DefaultPageQueryOptions.defaults()))
                .hasMessageContaining("对账运行结果不存在");
    }

    @Test
    void testQueryShouldRejectCustomSorting() {
        ReconciliationRunResultDTO recorded = reconciliationRunResultApplicationService.executeStrictExact(
                readyRequest(List.of("001")), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.queryMatchResults(
                TENANT_ID, recorded.getSn(), DefaultPageQueryOptions.asc(DefaultOrderField.GMT_CREATE)))
                .hasMessageContaining("不支持自定义排序");
    }

    /**
     * 场景：冻结的两侧来源各有两条事实，但匹配调用方只提交其中一条。
     * 结果：拒绝派生运行结果，不能把局部匹配伪装成全量 BALANCED。
     */
    @Test
    void testRecordShouldRejectIncompleteSourceCoverage() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of("reference:001"), "report:reference");
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.COMPARISON,
                "settlement", List.of("comparison:001"), "report:comparison", 1L, false);
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());

        ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system());

        assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.DIFFERENCE_FOUND);
        assertThat(result.getMatchedCount()).isOne();
        assertThat(result.getDifferenceCount()).isZero();
    }

    /**
     * 场景：冻结来源事实落库后，成员数、成员摘要或快照摘要被异常改写。
     * 结果：运行结果生成必须逐层失败关闭，且不留下完成态结果或改变批次状态。
     */
    @Test
    void testRecordShouldRejectPersistedSourceFactDrift() {
        RecordReconciliationRunResultRequest request = readyRequest(List.of("001"));
        String batchSn = request.getReconciliationBatchSn();
        String snapshotSn = jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_source_snapshot
                WHERE reconciliation_batch_sn = ? AND source_role = 'REFERENCE'
                """, String.class, batchSn);
        String itemSn = jdbcTemplate.queryForObject("""
                SELECT sn
                FROM t_reconciliation_source_item
                WHERE source_snapshot_sn = ?
                """, String.class, snapshotSn);
        String semanticDigest = jdbcTemplate.queryForObject(
                "SELECT semantic_digest FROM t_reconciliation_source_item WHERE sn = ?", String.class, itemSn);
        String sourceDigest = jdbcTemplate.queryForObject(
                "SELECT source_digest FROM t_reconciliation_source_snapshot WHERE sn = ?", String.class, snapshotSn);

        jdbcTemplate.update("UPDATE t_reconciliation_source_snapshot SET coverage_member_count = 2 WHERE sn = ?", snapshotSn);
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("快照成员数冲突");
        jdbcTemplate.update("UPDATE t_reconciliation_source_snapshot SET coverage_member_count = 1 WHERE sn = ?", snapshotSn);

        jdbcTemplate.update("UPDATE t_reconciliation_source_item SET semantic_digest = ? WHERE sn = ?",
                "x".repeat(64), itemSn);
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("来源事实摘要不一致");

        jdbcTemplate.update("UPDATE t_reconciliation_source_item SET semantic_digest = ?, amount = 2 WHERE sn = ?",
                semanticDigest, itemSn);
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("来源事实冲突");
        jdbcTemplate.update("UPDATE t_reconciliation_source_item SET amount = 1 WHERE sn = ?", itemSn);

        jdbcTemplate.update("UPDATE t_reconciliation_source_snapshot SET source_digest = ? WHERE sn = ?",
                "0".repeat(64), snapshotSn);
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("来源快照摘要不一致");

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
        assertThat(batchState(batchSn)).isEqualTo(ReconciliationBatchState.DATA_READY.name());
        jdbcTemplate.update("UPDATE t_reconciliation_source_snapshot SET source_digest = ? WHERE sn = ?",
                sourceDigest, snapshotSn);
    }

    /**
     * 场景：调用方引用不存在的对账批次。
     * 结果：快速失败，不能使用调用方自报摘要生成运行结果。
     */
    @Test
    void testRecordShouldRejectUnknownBatch() {
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn("missing-batch");

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("对账批次不存在");
    }

    /**
     * 场景：批次只冻结了一侧来源。
     * 结果：拒绝运行，等待另一侧快照，不得形成完成态结果。
     */
    @Test
    void testRecordShouldRejectBatchWithOneSourceSnapshot() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of("reference:001"), "report:reference");
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("来源尚未冻结完整");
    }

    /**
     * 场景：基准侧和核对侧快照都已冻结，但两个来源集合都为空。
     * 结果：拒绝生成没有任何来源事实支撑的完成态运行结果。
     */
    @Test
    void testRecordShouldRejectWhenBothSourceSnapshotsAreEmpty() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of(), "report:reference");
        insertEmptyComparisonSnapshotBypassingBatchService(batch.getSn());
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("两侧来源不能同时为空");

        assertThat(runResultCount()).isZero();
    }

    /**
     * 场景：匹配结果引用了冻结来源集合之外的记录。
     * 结果：快速失败，不能借匹配结果扩张批次范围。
     */
    @Test
    void testRecordShouldRejectSourceReferenceOutsideSnapshot() {
        RecordReconciliationRunResultRequest request = readyRequest(List.of("001"));
        jdbcTemplate.update("UPDATE t_reconciliation_source_item SET comparison_identity_value = 'outside'");

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("对账来源事实冲突");
    }

    /**
     * 场景：逐笔结果包含未匹配项。
     * 结果：服务从完整覆盖的逐笔事实派生差错状态和计数。
     */
    @Test
    void testRecordShouldDeriveDifferenceResultFromMatchFacts() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of("reference:001"), "report:reference");
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.COMPARISON,
                "settlement", List.of("comparison:001"), "report:comparison", 2L, true);
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());

        ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system());

        assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.DIFFERENCE_FOUND);
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getDifferenceCount()).isOne();
    }

    /**
     * 场景：核对侧存在记录，但基准侧冻结为空集合。
     * 结果：允许固化 REFERENCE_MISSING 差错，不要求伪造基准侧引用。
     */
    @Test
    void testRecordShouldPersistReferenceMissingResult() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of(), "report:reference");
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.COMPARISON,
                "settlement", List.of("comparison:orphan-001"), "report:comparison");
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());

        ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system());

        assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.DIFFERENCE_FOUND);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_reconciliation_match_result
                WHERE reference_fact_identity_value IS NULL
                  AND result_kind = 'REFERENCE_MISSING'
                """, Integer.class)).isOne();
    }

    /**
     * 场景：同一来源对仅更换逐笔证据引用后重复提交。
     * 结果：按来源覆盖身份快速失败，证据引用不能制造第二笔匹配计数。
     */
    @Test
    void testRecordShouldRejectDuplicateSourcePairWithDifferentEvidence() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of("reference:001"), "report:reference");

        assertThatThrownBy(() -> recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of("reference:001"), "report:changed"))
                .hasMessageContaining("快照事实冲突");
    }

    /**
     * 场景：两条基准侧事实重复匹配同一条核对侧事实。
     * 结果：当前一对一匹配模型快速失败，不能通过集合覆盖制造假 BALANCED。
     */
    @Test
    void testRecordShouldRejectReusingOneSideAcrossMatchResults() {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE, "transaction",
                List.of("reference:first:001", "reference:second:001"), "report:reference");
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.COMPARISON,
                "settlement",
                List.of("comparison:001"), "report:comparison");
        RecordReconciliationRunResultRequest request = new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());

        ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.executeStrictExact(
                request, WindOperatorFactory.system());

        assertThat(result.getOutcome()).isEqualTo(ReconciliationRunOutcome.DIFFERENCE_FOUND);
        assertThat(result.getDifferenceCount()).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT result_kind FROM t_reconciliation_match_result",
                String.class)).isEqualTo("IDENTITY_CONFLICT");
    }

    /**
     * 场景：逐笔明细证据引用超过数据库字段宽度。
     * 结果：在锁定批次和写入批次头前快速失败，批次保持 DATA_READY。
     */
    @Test
    void testRecordShouldRejectOversizedEvidenceBeforePersistence() {
        ReconciliationBatchDTO batch = createBatch();

        assertThatThrownBy(() -> recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                "transaction", List.of("reference:001"), "x".repeat(257)))
                .hasMessageContaining("来源证据引用长度不能超过");

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
        assertThat(batchState(batch.getSn())).isEqualTo(ReconciliationBatchState.CREATED.name());
    }

    /**
     * 场景：两个线程并发重放完全相同的完成态对账结果。
     * 结果：批次行锁串行固化，两个调用都复用同一结果。
     */
    @Test
    void testRecordShouldReuseWinnerForConcurrentSameFacts() throws Exception {
        RecordReconciliationRunResultRequest request = readyRequest(List.of("001"));
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RunResultAttempt> first = executor.submit(concurrentRecordAttempt(startGate, request));
            Future<RunResultAttempt> second = executor.submit(concurrentRecordAttempt(startGate, request));
            startGate.countDown();

            List<RunResultAttempt> results = List.of(first.get(), second.get());
            assertThat(results).allMatch(RunResultAttempt::succeeded);
            assertThat(results).extracting(RunResultAttempt::sn).containsOnly(results.getFirst().sn());
            assertThat(runResultCount()).isOne();
            assertThat(matchResultCount()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private RecordReconciliationRunResultRequest readyRequest(List<String> suffixes) {
        ReconciliationBatchDTO batch = createBatch();
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.REFERENCE, "transaction",
                suffixes.stream().map(suffix -> "reference:" + suffix).toList(), "report:reference");
        recordSnapshot(batch.getSn(), ReconciliationSourceRole.COMPARISON, "settlement",
                suffixes.stream().map(suffix -> "comparison:" + suffix).toList(), "report:comparison");
        return new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn());
    }

    private ReconciliationBatchDTO createBatch() {
        return reconciliationBatchApplicationService.createBatch(new CreateReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
                .setScopeIdentity(identity("test.scope", "clearing:clearing-candidate-001"))
                .setPairIdentity(identity("test.pair", "clearing-candidate-001"))
                .setCurrency(CurrencyIsoCode.USD)
                .setComparisonRuleRef(rule("recon-rule-v1"))
                .setWindowStart(LocalDateTime.of(2026, 7, 21, 0, 0))
                .setWindowEnd(LocalDateTime.of(2026, 7, 22, 0, 0))
                .setTimeSemantics("occurredAt")
                .setTimezoneId("Asia/Shanghai"), WindOperatorFactory.system());
    }

    private void recordSnapshot(String batchSn,
                                ReconciliationSourceRole sourceRole,
                                String sourceNamespace,
                                List<String> sourceItemRefs,
                                String evidenceRef) {
        recordSnapshot(batchSn, sourceRole, sourceNamespace, sourceItemRefs, evidenceRef, 1L, true);
    }

    private void recordSnapshot(String batchSn,
                                ReconciliationSourceRole sourceRole,
                                String sourceNamespace,
                                List<String> sourceItemRefs,
                                String evidenceRef,
                                long amount,
                                boolean coverageComplete) {
        RecordReconciliationSourceSnapshotRequest request =
                com.wind.funds.reconciliation.ReconciliationTestFixture.sourceSnapshotRequest(
                        TENANT_ID, batchSn, sourceRole, sourceNamespace, sourceItemRefs, List.of(evidenceRef));
        request.getCoverage().setComplete(coverageComplete);
        request.getFacts().forEach(fact -> {
            String sourceFactValue = fact.getSourceFactRef().getValue().trim();
            int separator = sourceFactValue.lastIndexOf(':');
            fact.setComparisonIdentity(identity("test.compare",
                    separator < 0 ? sourceFactValue : sourceFactValue.substring(separator + 1)));
            fact.setAmount(amount);
        });
        reconciliationBatchApplicationService.recordSourceSnapshot(
                request, WindOperatorFactory.system());
    }

    private void insertEmptyComparisonSnapshotBypassingBatchService(String batchSn) {
        String semanticDigest = FundsStableHashSupport.sha256Json(List.of());
        TreeMap<String, Object> snapshot = new TreeMap<>();
        snapshot.put("tenantId", TENANT_ID);
        snapshot.put("batchSn", batchSn);
        snapshot.put("sourceRole", ReconciliationSourceRole.COMPARISON);
        snapshot.put("sourceNamespace", "settlement");
        snapshot.put("snapshotIdentity", "test.snapshot:" + batchSn + ":COMPARISON");
        snapshot.put("snapshotVersion", "v1");
        snapshot.put("coverageComplete", true);
        snapshot.put("coverageWatermark", "test-watermark");
        snapshot.put("coverageMemberCount", 0);
        snapshot.put("semanticDigest", semanticDigest);
        String sourceDigest = FundsStableHashSupport.sha256Json(snapshot);
        String evidenceBundleDigest = FundsStableHashSupport.sha256Json(List.of("report:comparison"));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_snapshot
                    (sn, tenant_id, reconciliation_batch_sn, source_role, source_namespace,
                     snapshot_owner_namespace, snapshot_identity_value, snapshot_version,
                     coverage_complete, coverage_watermark, coverage_member_count,
                     source_digest, semantic_digest, evidence_bundle_digest, evidence_refs, created_by)
                VALUES (?, ?, ?, 'COMPARISON', 'settlement', 'test.snapshot', ?, 'v1',
                        1, 'test-watermark', 0, ?, ?, ?, '["report:comparison"]', 'SYSTEM')
                """, batchSn + ":CORRUPT_COMPARISON", TENANT_ID, batchSn,
                batchSn + ":COMPARISON", sourceDigest, semanticDigest, evidenceBundleDigest);
        jdbcTemplate.update("UPDATE t_reconciliation_batch SET state = 'DATA_READY' WHERE sn = ?", batchSn);
    }

    private Callable<RunResultAttempt> concurrentRecordAttempt(CountDownLatch startGate,
                                                               RecordReconciliationRunResultRequest request) {
        return () -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                startGate.await();
                ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.executeStrictExact(
                        request, WindOperatorFactory.system());
                return new RunResultAttempt(true, result.getSn(), null);
            } catch (RuntimeException exception) {
                return new RunResultAttempt(false, null, exception.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        };
    }

    private int runResultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_run_result", Integer.class);
    }

    private int matchResultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_match_result", Integer.class);
    }

    private String batchState(String batchSn) {
        return jdbcTemplate.queryForObject("SELECT state FROM t_reconciliation_batch WHERE sn = ?",
                String.class, batchSn);
    }

    @Configuration
    @Import({ReconciliationBatchApplicationServiceImpl.class, ReconciliationRunResultApplicationServiceImpl.class})
    static class Config {
    }

    private record RunResultAttempt(boolean succeeded, String sn, String message) {
    }
}
