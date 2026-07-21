package com.wind.funds.reconciliation.application.run.impl;

import com.wind.integration.operator.WindOperatorFactory;
import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.application.run.ReconciliationRunResultApplicationService;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
import com.wind.funds.reconciliation.model.dto.ReconciliationRunResultDTO;
import com.wind.funds.reconciliation.model.request.ReconciliationMatchResultItem;
import com.wind.funds.reconciliation.model.request.RecordReconciliationRunResultRequest;
import com.wind.funds.support.FundsBalanceAssertionSupport.LedgerFactSnapshot;
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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.wind.funds.support.FundsBalanceAssertionSupport.assertLedgerFactsUnchanged;
import static com.wind.funds.support.FundsBalanceAssertionSupport.ledgerFactSnapshot;
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
    private ReconciliationRunResultApplicationService reconciliationRunResultApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanRunResults() {
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
    }

    /**
     * 场景：一次清分前对账已经完整执行并对平。
     * 结果：记录不可变运行结果，内部生成流水号和结果摘要；同一业务事实重放返回原结果。
     * 红线：记录对账结果不得创建或修改交易、posting、ledger transaction 或 ledger entry。
     */
    @Test
    void testRecordShouldPersistBalancedResultAndReuseSameBusinessFact() {
        LedgerFactSnapshot before = ledgerFactSnapshot(jdbcTemplate);

        ReconciliationRunResultDTO first = reconciliationRunResultApplicationService.recordRunResult(
                minimumRequest(), WindOperatorFactory.system());
        ReconciliationRunResultDTO replay = reconciliationRunResultApplicationService.recordRunResult(
                minimumRequest(), WindOperatorFactory.system());

        assertThat(first.getSn()).isNotBlank();
        assertThat(first.getResultDigest()).hasSize(64);
        assertThat(first.getInternalSourceDigest()).isEqualTo("b".repeat(64));
        assertThat(first.getExternalSourceDigest()).isEqualTo("c".repeat(64));
        assertThat(first.getSourceDigest()).hasSize(64);
        assertThat(first.getStatus()).isEqualTo(ReconciliationRunResultStatus.BALANCED);
        assertThat(first.getTotalCount()).isOne();
        assertThat(first.getMatchedCount()).isOne();
        assertThat(first.getDifferenceCount()).isZero();
        assertThat(first.getEvidenceRefs()).containsExactly("report:clearing-recon-run-001");
        assertThat(replay.getSn()).isEqualTo(first.getSn());
        assertThat(replay.getResultDigest()).isEqualTo(first.getResultDigest());
        assertThat(runResultCount()).isOne();
        assertThat(matchResultCount()).isOne();
        assertLedgerFactsUnchanged(jdbcTemplate, before);
    }

    /**
     * 场景：同一批次和准入对象被使用不同来源摘要重复提交。
     * 结果：拒绝覆盖第一次运行结果。
     */
    @Test
    void testRecordShouldRejectChangedFactForSameBusinessKey() {
        reconciliationRunResultApplicationService.recordRunResult(minimumRequest(), WindOperatorFactory.system());

        RecordReconciliationRunResultRequest changed = minimumRequest();
        changed.getMatchResults().getFirst().setExternalSourceRef("external:clearing:002");
        assertThatThrownBy(() -> reconciliationRunResultApplicationService.recordRunResult(
                changed, WindOperatorFactory.system()))
                .hasMessageContaining("对账运行结果事实不一致");

        assertThat(runResultCount()).isOne();
        assertThat(matchResultCount()).isOne();
    }

    /**
     * 场景：逐笔结果包含未匹配项。
     * 结果：服务从可信上游提交的逐笔事实派生差错状态和计数，不接收独立汇总字段。
     */
    @Test
    void testRecordShouldDeriveDifferenceResultFromMatchFacts() {
        RecordReconciliationRunResultRequest request = minimumRequest()
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setInternalSourceRef("internal:funds-transaction:001")
                        .setExternalSourceRef("external:clearing:001")
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.UNMATCHED)
                        .setDifferenceType(ReconciliationDifferenceType.STATUS_MISMATCH)
                        .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                        .setDifferenceAmount(0L)
                        .setEvidenceRef("report:clearing-recon-run-001#line-1")));

        ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.recordRunResult(
                request, WindOperatorFactory.system());

        assertThat(result.getStatus()).isEqualTo(ReconciliationRunResultStatus.DIFFERENCE_FOUND);
        assertThat(result.getTotalCount()).isOne();
        assertThat(result.getMatchedCount()).isZero();
        assertThat(result.getDifferenceCount()).isOne();
        assertThat(matchResultCount()).isOne();
    }

    /**
     * 场景：调用方把只有内部来源的一项声明为完全匹配。
     * 结果：快速失败，不允许形成缺少外部证据的正向结果。
     */
    @Test
    void testRecordShouldRejectAutomaticMatchWithoutBothSources() {
        RecordReconciliationRunResultRequest request = minimumRequest();
        request.getMatchResults().getFirst().setExternalSourceRef(null);

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.recordRunResult(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("自动对平必须同时存在内部和外部来源引用");

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
    }

    /**
     * 场景：外部来源存在，但内部资金事实缺失。
     * 结果：允许固化 INTERNAL_MISSING 逐笔事实并派生差错结果，不要求伪造内部引用。
     */
    @Test
    void testRecordShouldPersistInternalMissingResult() {
        RecordReconciliationRunResultRequest request = minimumRequest()
                .setMatchResults(List.of(new ReconciliationMatchResultItem()
                        .setExternalSourceRef("external:clearing:orphan-001")
                        .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                        .setMatchStrength(ReconciliationMatchStrength.UNMATCHED)
                        .setDifferenceType(ReconciliationDifferenceType.INTERNAL_MISSING)
                        .setSeverity(ReconciliationDifferenceSeverity.S1_MAJOR)
                        .setEvidenceRef("report:clearing-recon-run-001#line-orphan")));

        ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.recordRunResult(
                request, WindOperatorFactory.system());

        assertThat(result.getStatus()).isEqualTo(ReconciliationRunResultStatus.DIFFERENCE_FOUND);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_reconciliation_match_result
                WHERE internal_source_ref IS NULL
                  AND difference_type = 'INTERNAL_MISSING'
                """, Integer.class)).isOne();
    }

    /**
     * 场景：同一来源对仅更换逐笔证据引用后重复提交。
     * 结果：按来源覆盖身份快速失败，证据引用不能制造第二笔匹配计数。
     */
    @Test
    void testRecordShouldRejectDuplicateSourcePairWithDifferentEvidence() {
        RecordReconciliationRunResultRequest request = minimumRequest()
                .setMatchResults(List.of(
                        exactMatchResult("001", "report:clearing-recon-run-001#line-1"),
                        exactMatchResult("001", "report:clearing-recon-run-001#line-2")));

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.recordRunResult(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("不能重复使用同一内部和外部来源对");

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
    }

    /**
     * 场景：相同逐笔事实和批次证据使用不同输入顺序重放。
     * 结果：排序后的摘要保持稳定，返回原运行结果且不重复写明细。
     */
    @Test
    void testRecordShouldReuseSameFactsInDifferentOrder() {
        ReconciliationMatchResultItem firstItem = exactMatchResult("001", "report:run#line-1");
        ReconciliationMatchResultItem secondItem = exactMatchResult("002", "report:run#line-2");
        RecordReconciliationRunResultRequest firstRequest = minimumRequest()
                .setMatchResults(List.of(firstItem, secondItem))
                .setEvidenceRefs(List.of("report:run-b", "report:run-a"));
        RecordReconciliationRunResultRequest replayRequest = minimumRequest()
                .setMatchResults(List.of(secondItem, firstItem))
                .setEvidenceRefs(List.of("report:run-a", "report:run-b"));

        ReconciliationRunResultDTO first = reconciliationRunResultApplicationService.recordRunResult(
                firstRequest, WindOperatorFactory.system());
        ReconciliationRunResultDTO replay = reconciliationRunResultApplicationService.recordRunResult(
                replayRequest, WindOperatorFactory.system());

        assertThat(replay.getSn()).isEqualTo(first.getSn());
        assertThat(replay.getResultDigest()).isEqualTo(first.getResultDigest());
        assertThat(runResultCount()).isOne();
        assertThat(matchResultCount()).isEqualTo(2);
    }

    /**
     * 场景：批次头写入后，逐笔明细因数据库字段约束写入失败。
     * 结果：整个事务回滚，不留下批次头或部分逐笔事实。
     */
    @Test
    void testRecordShouldRollbackHeaderAndDetailsWhenDetailInsertFails() {
        RecordReconciliationRunResultRequest request = minimumRequest()
                .setMatchResults(List.of(
                        exactMatchResult("001", "report:run#line-1"),
                        exactMatchResult("002", "x".repeat(300))));

        assertThatThrownBy(() -> reconciliationRunResultApplicationService.recordRunResult(
                request, WindOperatorFactory.system()));

        assertThat(runResultCount()).isZero();
        assertThat(matchResultCount()).isZero();
    }

    /**
     * 场景：两个线程并发重放完全相同的完成态对账结果。
     * 结果：两个调用都复用同一结果，数据库只保留一个批次头和一条逐笔事实。
     */
    @Test
    void testRecordShouldReuseWinnerForConcurrentSameFacts() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RunResultAttempt> first = executor.submit(concurrentRecordAttempt(startGate));
            Future<RunResultAttempt> second = executor.submit(concurrentRecordAttempt(startGate));

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

    private RecordReconciliationRunResultRequest minimumRequest() {
        return new RecordReconciliationRunResultRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn("recon-run-batch-001")
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn("clearing-candidate-001")
                .setRuleVersion("recon-rule-v1")
                .setInternalSourceDigest("b".repeat(64))
                .setExternalSourceDigest("c".repeat(64))
                .setMatchResults(List.of(exactMatchResult("001", "report:clearing-recon-run-001#line-1")))
                .setEvidenceRefs(List.of("report:clearing-recon-run-001"));
    }

    private ReconciliationMatchResultItem exactMatchResult(String suffix, String evidenceRef) {
        return new ReconciliationMatchResultItem()
                .setInternalSourceRef("internal:funds-transaction:" + suffix)
                .setExternalSourceRef("external:clearing:" + suffix)
                .setSourceQuality(ReconciliationSourceQuality.VERIFIED)
                .setMatchStrength(ReconciliationMatchStrength.EXACT_MATCH)
                .setEvidenceRef(evidenceRef);
    }

    private Callable<RunResultAttempt> concurrentRecordAttempt(CountDownLatch startGate) {
        return () -> {
            startGate.await();
            try {
                ReconciliationRunResultDTO result = reconciliationRunResultApplicationService.recordRunResult(
                        minimumRequest(), WindOperatorFactory.system());
                return new RunResultAttempt(true, result.getSn(), null);
            } catch (RuntimeException exception) {
                return new RunResultAttempt(false, null, exception.getMessage());
            }
        };
    }

    private int runResultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_run_result", Integer.class);
    }

    private int matchResultCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_match_result", Integer.class);
    }

    @Configuration
    @Import(ReconciliationRunResultApplicationServiceImpl.class)
    static class Config {
    }

    private record RunResultAttempt(boolean succeeded, String sn, String message) {
    }
}
