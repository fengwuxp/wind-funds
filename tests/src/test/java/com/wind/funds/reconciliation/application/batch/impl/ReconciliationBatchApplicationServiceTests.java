package com.wind.funds.reconciliation.application.batch.impl;

import com.wind.funds.AbstractFundsServiceTest;
import com.wind.funds.reconciliation.ReconciliationTestFixture;
import com.wind.funds.reconciliation.application.batch.ReconciliationBatchApplicationService;
import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.enums.ReconciliationSourceType;
import com.wind.funds.reconciliation.model.dto.ReconciliationBatchDTO;
import com.wind.funds.reconciliation.model.dto.ReconciliationSourceSnapshotDTO;
import com.wind.funds.reconciliation.model.request.CreateReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.AbortReconciliationBatchRequest;
import com.wind.funds.reconciliation.model.request.ReconciliationSourceItemInput;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.reconciliation.model.request.ReplaceReconciliationBatchRequest;
import com.wind.funds.transaction.support.FundsStableHashSupport;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 对账批次及来源快照应用服务测试。
 */
@SpringJUnitConfig({
        AbstractFundsServiceTest.TestInfrastructureConfig.class,
        ReconciliationBatchApplicationServiceTests.Config.class
})
@TestPropertySource(properties = "wind.funds.test.flex-transaction-manager-enabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReconciliationBatchApplicationServiceTests extends AbstractFundsServiceTest {

    @Autowired
    private ReconciliationBatchApplicationService reconciliationBatchApplicationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBatchFacts() {
        ReconciliationTestFixture.clearRunAndBatchFacts(jdbcTemplate);
    }

    @Test
    void testCreateShouldRejectTenantDifferentFromCurrentContext() {
        CreateReconciliationBatchRequest request = minimumCreateRequest("recon-rule-v1")
                .setTenantId(TENANT_ID + 1);

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("tenantId 与当前租户不一致");
        assertThat(batchCount()).isZero();
    }

    /**
     * 场景：同一租户、准入对象、规则和窗口重复创建批次。
     * 结果：返回同一内部流水号，批次保持 CREATED，只固化一条事实。
     */
    @Test
    void testCreateShouldPersistCreatedBatchAndReuseSameScope() {
        CreateReconciliationBatchRequest request = minimumCreateRequest("recon-rule-v1");

        ReconciliationBatchDTO first = reconciliationBatchApplicationService.createBatch(
                request, WindOperatorFactory.system());
        ReconciliationBatchDTO replay = reconciliationBatchApplicationService.createBatch(
                request, WindOperatorFactory.system());

        assertThat(first.getSn()).startsWith("RCB");
        assertThat(first.getStatus()).isEqualTo(ReconciliationBatchStatus.CREATED);
        assertThat(first.getBatchDigest()).hasSize(64);
        assertThat(replay.getSn()).isEqualTo(first.getSn());
        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：当前批次来源或范围证据被操作人确认无效。
     * 结果：批次终止并保留操作人、时间和原因；相同请求重放返回同一事实。
     */
    @Test
    void testAbortShouldPersistReasonAndBeIdempotent() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        AbortReconciliationBatchRequest request = new AbortReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batch.getSn())
                .setReason("来源文件选择错误");

        ReconciliationBatchDTO aborted = reconciliationBatchApplicationService.abortBatch(
                request, WindOperatorFactory.system());
        ReconciliationBatchDTO replay = reconciliationBatchApplicationService.abortBatch(
                request, WindOperatorFactory.system());

        assertThat(aborted.getStatus()).isEqualTo(ReconciliationBatchStatus.ABORTED);
        assertThat(aborted.getAbortedBy()).isNotBlank();
        assertThat(aborted.getAbortedTime()).isNotNull();
        assertThat(aborted.getAbortReason()).isEqualTo("来源文件选择错误");
        assertThat(replay).isEqualTo(aborted);

        assertThatThrownBy(() -> reconciliationBatchApplicationService.abortBatch(
                request.setReason("另一终止原因"), WindOperatorFactory.system()))
                .hasMessageContaining("不同终止事实");
    }

    /**
     * 场景：已完成批次已经形成不可变对账事实。
     * 结果：拒绝终止，业务修正必须通过显式替代批次推进。
     */
    @Test
    void testAbortShouldRejectCompletedBatch() {
        ReconciliationBatchDTO completed = createCompletedBatch();

        assertThatThrownBy(() -> reconciliationBatchApplicationService.abortBatch(
                new AbortReconciliationBatchRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(completed.getSn())
                        .setReason("完成后发现来源选择错误"),
                WindOperatorFactory.system()))
                .hasMessageContaining("已完成对账批次不能终止")
                .hasMessageContaining("replaceBatch");

        assertThat(batchStatus(completed.getSn())).isEqualTo(ReconciliationBatchStatus.COMPLETED.name());
    }

    /**
     * 场景：批次终止后，以它作为上一批次重新采集来源。
     * 结果：新批次成为当前血缘头，原批次及其终止证据保持不变。
     */
    @Test
    void testCreateReplacementShouldAllowAbortedPreviousBatch() {
        ReconciliationBatchDTO aborted = reconciliationBatchApplicationService.abortBatch(
                new AbortReconciliationBatchRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(createBatch("recon-rule-v1").getSn())
                        .setReason("来源文件选择错误"),
                WindOperatorFactory.system());

        ReconciliationBatchDTO replacement = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(aborted.getSn()),
                WindOperatorFactory.system());

        assertThat(replacement.getPreviousBatchSn()).isEqualTo(aborted.getSn());
        assertThat(replacement.getStatus()).isEqualTo(ReconciliationBatchStatus.CREATED);
        assertThat(batchStatus(aborted.getSn())).isEqualTo(ReconciliationBatchStatus.ABORTED.name());
    }

    /**
     * 场景：尝试终止已被后继批次替代的历史批次。
     * 结果：拒绝修改非当前血缘头，避免恢复链路分叉。
     */
    @Test
    void testAbortShouldRejectNonCurrentLineageHead() {
        ReconciliationBatchDTO previous = createCompletedBatch();
        reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.abortBatch(
                new AbortReconciliationBatchRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(previous.getSn())
                        .setReason("错误终止历史批次"),
                WindOperatorFactory.system()))
                .hasMessageContaining("当前批次血缘头");
    }

    /**
     * 场景：已完成批次上的差错已经进入运营处置生命周期。
     * 结果：拒绝终止批次，避免借终止操作隐式撤销差错状态。
     */
    @Test
    void testAbortShouldRejectCompletedBatchWithAnchoredDifference() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(batch.getSn());
        materializeDifferenceWithoutAction(batch.getSn());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.abortBatch(
                new AbortReconciliationBatchRequest()
                        .setTenantId(TENANT_ID)
                        .setReconciliationBatchSn(batch.getSn())
                        .setReason("运行结果被误判"),
                WindOperatorFactory.system()))
                .hasMessageContaining("已完成对账批次不能终止");

        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.COMPLETED.name());
    }

    /**
     * 场景：已完成批次的来源、解析或匹配证据被确认无效。
     * 结果：新批次成为当前血缘头，旧批次保持 COMPLETED，依赖旧证据的差错统一失效。
     */
    @Test
    void testReplaceShouldCreateCurrentBatchAndInvalidateAnchoredDifferences() {
        ReconciliationBatchDTO completed = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(completed.getSn());
        materializeDifferenceWithoutAction(completed.getSn());
        ReplaceReconciliationBatchRequest request = replacementRequest(completed.getSn());

        ReconciliationBatchDTO replacement = reconciliationBatchApplicationService.replaceBatch(
                request, WindOperatorFactory.system());
        ReconciliationBatchDTO replay = reconciliationBatchApplicationService.replaceBatch(
                request, WindOperatorFactory.system());

        assertThat(replacement.getSn()).isNotEqualTo(completed.getSn());
        assertThat(replacement.getPreviousBatchSn()).isEqualTo(completed.getSn());
        assertThat(replacement.getStatus()).isEqualTo(ReconciliationBatchStatus.CREATED);
        assertThat(replacement.getRuleVersion()).isEqualTo("recon-rule-v2");
        assertThat(replacement.getReplacementReason()).isEqualTo("外部清算文件解析版本错误");
        assertThat(replacement.getReplacementEvidenceRef()).isEqualTo("evidence:parser-incident-001");
        assertThat(replay.getSn()).isEqualTo(replacement.getSn());
        assertThat(batchStatus(completed.getSn())).isEqualTo(ReconciliationBatchStatus.COMPLETED.name());
        assertThat(differenceStatus(completed.getSn() + ":DIFFERENCE")).isEqualTo("INVALIDATED");
        assertThat(lineageCurrentBatchSn()).isEqualTo(replacement.getSn());
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：同一已完成批次已被替代，调用方又以不同修正事实请求替代。
     * 结果：联合唯一约束只保留一个直接后继，事实漂移必须失败。
     */
    @Test
    void testReplaceShouldRejectChangedReplacementFacts() {
        ReconciliationBatchDTO completed = createCompletedBatch();
        reconciliationBatchApplicationService.replaceBatch(
                replacementRequest(completed.getSn()), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.replaceBatch(
                replacementRequest(completed.getSn()).setReason("另一个修正原因"),
                WindOperatorFactory.system()))
                .hasMessageContaining("已由不同替代事实处理");

        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：两个线程并发提交完全相同的完成态批次替代事实。
     * 结果：只创建一个替代批次，两个调用均返回唯一胜者。
     */
    @Test
    void testReplaceShouldReuseWinnerForConcurrentSameFacts() throws Exception {
        ReconciliationBatchDTO completed = createCompletedBatch();

        List<BatchCreateAttempt> results = concurrentlyReplace(
                replacementRequest(completed.getSn()), replacementRequest(completed.getSn()));

        assertThat(results).allMatch(BatchCreateAttempt::succeeded);
        assertThat(results).extracting(BatchCreateAttempt::sn).containsOnly(results.getFirst().sn());
        assertThat(lineageCurrentBatchSn()).isEqualTo(results.getFirst().sn());
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：两个线程并发提交不同的完成态批次替代事实。
     * 结果：只允许一个请求成功，另一请求明确拒绝事实漂移。
     */
    @Test
    void testReplaceShouldAllowOnlyOneConcurrentDifferentFacts() throws Exception {
        ReconciliationBatchDTO completed = createCompletedBatch();

        List<BatchCreateAttempt> results = concurrentlyReplace(
                replacementRequest(completed.getSn()),
                replacementRequest(completed.getSn()).setReason("另一个修正原因"));

        assertThat(results).filteredOn(BatchCreateAttempt::succeeded).hasSize(1);
        assertThat(results).filteredOn(result -> !result.succeeded()).singleElement()
                .extracting(BatchCreateAttempt::message)
                .asString()
                .contains("已由不同替代事实处理");
        String winnerSn = results.stream()
                .filter(BatchCreateAttempt::succeeded)
                .findFirst()
                .orElseThrow()
                .sn();
        assertThat(lineageCurrentBatchSn()).isEqualTo(winnerSn);
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：未完成批次尝试走完成态证据替代通道。
     * 结果：拒绝替代，应继续收集或使用 abortBatch 终止。
     */
    @Test
    void testReplaceShouldRejectIncompleteBatch() {
        ReconciliationBatchDTO created = createBatch("recon-rule-v1");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.replaceBatch(
                replacementRequest(created.getSn()), WindOperatorFactory.system()))
                .hasMessageContaining("只有已完成对账批次可以替代");

        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：账户日切等纯对账作业没有清分、结算或出款准入对象。
     * 结果：允许创建不绑定 Gate 的对账批次，后续只用于固化运行结果和差错证据。
     */
    @Test
    void testCreateShouldAllowBatchWithoutGateObject() {
        CreateReconciliationBatchRequest request = minimumCreateRequest("account-daily-recon-v1")
                .setReconciliationScopeRef("bank-account:account-001:USD")
                .setGateObjectType(null)
                .setGateObjectSn(null);

        ReconciliationBatchDTO result = reconciliationBatchApplicationService.createBatch(
                request, WindOperatorFactory.system());

        assertThat(result.getGateObjectType()).isNull();
        assertThat(result.getGateObjectSn()).isNull();
        assertThat(result.getStatus()).isEqualTo(ReconciliationBatchStatus.CREATED);
        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：同一租户、规则和时间窗口对两个不同账户执行纯对账。
     * 结果：范围引用参与批次摘要，两个账户各自形成独立批次。
     */
    @Test
    void testCreateShouldNotReuseBatchAcrossReconciliationScopes() {
        CreateReconciliationBatchRequest firstRequest = minimumCreateRequest("account-daily-recon-v1")
                .setReconciliationScopeRef("bank-account:account-001:USD")
                .setGateObjectType(null)
                .setGateObjectSn(null);
        CreateReconciliationBatchRequest secondRequest = minimumCreateRequest("account-daily-recon-v1")
                .setReconciliationScopeRef("bank-account:account-002:USD")
                .setGateObjectType(null)
                .setGateObjectSn(null);

        ReconciliationBatchDTO first = reconciliationBatchApplicationService.createBatch(
                firstRequest, WindOperatorFactory.system());
        ReconciliationBatchDTO second = reconciliationBatchApplicationService.createBatch(
                secondRequest, WindOperatorFactory.system());

        assertThat(second.getSn()).isNotEqualTo(first.getSn());
        assertThat(second.getBatchDigest()).isNotEqualTo(first.getBatchDigest());
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：同一准入对象和对账范围已经存在一条批次血缘，调用方不引用当前批次另起根批次。
     * 结果：拒绝平行根血缘，避免旧 BALANCED 结果绕过新差异结论。
     */
    @Test
    void testCreateShouldRejectParallelRootForSameGateObject() {
        ReconciliationBatchDTO first = createBatch("recon-rule-v1");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2"), WindOperatorFactory.system()))
                .hasMessageContaining("同一准入对象对账血缘已存在")
                .hasMessageContaining(first.getSn());

        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：同一准入对象已经存在 Gate 血缘，调用方更换对账范围后尝试另起根批次。
     * 结果：准入对象身份仍只能命中同一条血缘，不能通过新 scope 挑选有利对账结果。
     */
    @Test
    void testCreateShouldRejectParallelRootForSameGateObjectAcrossScopes() {
        ReconciliationBatchDTO first = createBatch("recon-rule-v1");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v1")
                        .setReconciliationScopeRef("clearing:alternate-scope"),
                WindOperatorFactory.system()))
                .hasMessageContaining("同一准入对象对账血缘已存在")
                .hasMessageContaining(first.getSn());

        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：两个线程并发为同一准入对象创建不同事实的根批次。
     * 结果：血缘对象唯一约束只允许一个请求成功，不能形成平行根。
     */
    @Test
    void testCreateShouldAllowOnlyOneConcurrentRootForSameGateObject() throws Exception {
        List<BatchCreateAttempt> results = concurrentlyCreate(
                minimumCreateRequest("recon-rule-v1"), minimumCreateRequest("recon-rule-v2"));

        assertThat(results).filteredOn(BatchCreateAttempt::succeeded).hasSize(1);
        assertThat(results).filteredOn(result -> !result.succeeded()).singleElement()
                .extracting(BatchCreateAttempt::message)
                .asString()
                .contains("同一准入对象对账血缘已存在");
        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：调用方只提交准入对象类型或只提交准入对象流水号。
     * 结果：在持久化前快速失败，避免形成无法可靠消费的半绑定批次。
     */
    @Test
    void testCreateShouldRejectPartialGateObject() {
        CreateReconciliationBatchRequest missingSn = minimumCreateRequest("recon-rule-v1")
                .setGateObjectSn(null);
        CreateReconciliationBatchRequest missingType = minimumCreateRequest("recon-rule-v1")
                .setGateObjectType(null);

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                missingSn, WindOperatorFactory.system()))
                .hasMessageContaining("必须同时提供或同时为空");
        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                missingType, WindOperatorFactory.system()))
                .hasMessageContaining("必须同时提供或同时为空");
        assertThat(batchCount()).isZero();
    }

    /**
     * 场景：调用方未提供稳定的业务对账范围。
     * 结果：在持久化前快速失败，避免仅凭时间窗口或规则版本形成范围不明的对账批次。
     */
    @Test
    void testCreateShouldRejectMissingReconciliationScope() {
        CreateReconciliationBatchRequest missingScope = minimumCreateRequest("recon-rule-v1")
                .setReconciliationScopeRef(" ");
        CreateReconciliationBatchRequest overlongScope = minimumCreateRequest("recon-rule-v1")
                .setReconciliationScopeRef("scope:" + "x".repeat(123));

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                missingScope, WindOperatorFactory.system()))
                .hasMessageContaining("范围引用不能为空");
        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                overlongScope, WindOperatorFactory.system()))
                .hasMessageContaining("范围引用长度不能超过 128");
        assertThat(batchCount()).isZero();
    }

    /**
     * 场景：依次冻结基准侧和核对侧来源，且重复提交同一基准侧快照。
     * 结果：来源引用规范化后不可变复用，批次由 DATA_COLLECTING 推进到 DATA_READY。
     */
    @Test
    void testRecordSourceSnapshotShouldFreezeFactsAndAdvanceBatchStatus() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        RecordReconciliationSourceSnapshotRequest referenceRequest = sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:002", " transaction:001 "),
                List.of("report:reference", " report:reference "));

        ReconciliationSourceSnapshotDTO reference = reconciliationBatchApplicationService.recordSourceSnapshot(
                referenceRequest, WindOperatorFactory.system());
        ReconciliationSourceSnapshotDTO replay = reconciliationBatchApplicationService.recordSourceSnapshot(
                sourceSnapshotRequest(batch.getSn(), ReconciliationSourceRole.REFERENCE,
                        ReconciliationSourceType.TRANSACTION,
                        List.of("transaction:001", "transaction:002"), List.of("report:reference")),
                WindOperatorFactory.system());

        assertThat(reference.getSn()).startsWith("RSS");
        assertThat(reference.getSourceDigest()).hasSize(64);
        assertThat(reference.getRecordCount()).isEqualTo(2);
        assertThat(reference.getSourceItemRefs()).containsExactly("transaction:001", "transaction:002");
        assertThat(reference.getEvidenceRefs()).containsExactly("report:reference");
        assertThat(replay.getSn()).isEqualTo(reference.getSn());
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.DATA_COLLECTING.name());

        ReconciliationSourceSnapshotDTO comparison = reconciliationBatchApplicationService.recordSourceSnapshot(
                sourceSnapshotRequest(batch.getSn(), ReconciliationSourceRole.COMPARISON,
                        ReconciliationSourceType.SETTLEMENT_REPORT, List.of(), List.of("report:comparison")),
                WindOperatorFactory.system());

        assertThat(comparison.getRecordCount()).isZero();
        assertThat(comparison.getSourceItemRefs()).isEmpty();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.DATA_READY.name());
        assertThat(snapshotCount()).isEqualTo(2);
        assertThat(sourceItemCount()).isEqualTo(2);
    }

    /**
     * 场景：同一批次同一来源角色已经冻结后，再提交不同成员事实。
     * 结果：拒绝覆盖原快照，已冻结来源保持不变。
     */
    @Test
    void testRecordSourceSnapshotShouldRejectChangedFactsForSameRole() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001"), List.of("report:reference")), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:002"), List.of("report:reference")), WindOperatorFactory.system()))
                .hasMessageContaining("快照事实不一致");

        assertThat(snapshotCount()).isOne();
        assertThat(sourceItemCount()).isOne();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.DATA_COLLECTING.name());
    }

    /**
     * 场景：上游用同一稳定引用重放来源事实，但规范化内容已经变化。
     * 结果：内容摘要参与来源快照身份，拒绝把被改写事实复用为原快照。
     */
    @Test
    void testRecordSourceSnapshotShouldRejectChangedContentForSameSourceItemRef() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        RecordReconciliationSourceSnapshotRequest original = sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001"), List.of("report:reference"));
        reconciliationBatchApplicationService.recordSourceSnapshot(original, WindOperatorFactory.system());

        RecordReconciliationSourceSnapshotRequest changed = sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001"), List.of("report:reference"));
        changed.getSourceItems().getFirst().setContentDigest(FundsStableHashSupport.sha256("changed-content"));

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(
                changed, WindOperatorFactory.system()))
                .hasMessageContaining("快照事实不一致");
        assertThat(snapshotCount()).isOne();
        assertThat(sourceItemCount()).isOne();
    }

    /**
     * 场景：来源抽取器没有提供规范的内容 SHA-256。
     * 结果：在冻结来源快照前快速失败，不接受无法复核的内容身份。
     */
    @Test
    void testRecordSourceSnapshotShouldRejectInvalidContentDigest() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        RecordReconciliationSourceSnapshotRequest request = sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001"), List.of("report:reference"));
        request.getSourceItems().getFirst().setContentDigest("not-a-sha256");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(
                request, WindOperatorFactory.system()))
                .hasMessageContaining("内容摘要必须是 64 位小写 SHA-256");
        assertThat(snapshotCount()).isZero();
        assertThat(sourceItemCount()).isZero();
    }

    /**
     * 场景：调用方把超过单次原子封版容量的来源成员一次性提交。
     * 结果：在锁定批次和写入快照前快速失败，不留下部分快照或成员记录。
     */
    @Test
    void testRecordSourceSnapshotShouldRejectOversizedSourceItemsBeforePersistence() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        List<String> sourceItemRefs = IntStream.rangeClosed(
                        1, RecordReconciliationSourceSnapshotRequest.MAX_SOURCE_ITEM_COUNT + 1)
                .mapToObj(index -> "transaction:" + index)
                .toList();

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                sourceItemRefs, List.of("report:reference")), WindOperatorFactory.system()))
                .hasMessageContaining("来源成员数量不能超过")
                .hasMessageContaining(String.valueOf(
                        RecordReconciliationSourceSnapshotRequest.MAX_SOURCE_ITEM_COUNT));

        assertThat(snapshotCount()).isZero();
        assertThat(sourceItemCount()).isZero();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.CREATED.name());
    }

    @Test
    void testRecordSourceSnapshotShouldRejectOversizedSourceItemRefBeforePersistence() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("x".repeat(ReconciliationSourceItemInput.MAX_SOURCE_ITEM_REF_LENGTH + 1)),
                List.of("report:reference")), WindOperatorFactory.system()))
                .hasMessageContaining("来源成员引用长度不能超过 128");

        assertThat(snapshotCount()).isZero();
        assertThat(sourceItemCount()).isZero();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.CREATED.name());
    }

    @Test
    void testRecordSourceSnapshotShouldRejectOversizedEvidenceBeforePersistence() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        List<String> tooManyEvidenceRefs = IntStream.rangeClosed(
                        1, RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_COUNT + 1)
                .mapToObj(index -> "report:" + index)
                .toList();

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001"), tooManyEvidenceRefs), WindOperatorFactory.system()))
                .hasMessageContaining("来源证据引用数量不能超过");
        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001"),
                List.of("x".repeat(RecordReconciliationSourceSnapshotRequest.MAX_EVIDENCE_REF_LENGTH + 1))),
                WindOperatorFactory.system()))
                .hasMessageContaining("来源证据引用长度不能超过");

        assertThat(snapshotCount()).isZero();
        assertThat(sourceItemCount()).isZero();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.CREATED.name());
    }

    /**
     * 场景：同一来源快照包含重复成员引用。
     * 结果：快速失败，不创建部分快照或成员记录。
     */
    @Test
    void testRecordSourceSnapshotShouldRejectDuplicateSourceItemRefs() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of("transaction:001", " transaction:001 "), List.of("report:reference")),
                WindOperatorFactory.system()))
                .hasMessageContaining("来源成员引用不能重复");

        assertThat(snapshotCount()).isZero();
        assertThat(sourceItemCount()).isZero();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.CREATED.name());
    }

    /**
     * 场景：第一侧已冻结为空集合，第二侧也尝试冻结为空集合。
     * 结果：第二侧事务回滚，批次保留 DATA_COLLECTING，之后仍可补入非空核对侧来源。
     */
    @Test
    void testRecordSourceSnapshotShouldRejectBothEmptyAndRemainRecoverable() {
        ReconciliationBatchDTO batch = createBatch("recon-rule-v1");
        reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.REFERENCE, ReconciliationSourceType.TRANSACTION,
                List.of(), List.of("report:reference")), WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.COMPARISON, ReconciliationSourceType.SETTLEMENT_REPORT,
                List.of(), List.of("report:comparison")), WindOperatorFactory.system()))
                .hasMessageContaining("两侧来源不能同时为空");

        assertThat(snapshotCount()).isOne();
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.DATA_COLLECTING.name());

        reconciliationBatchApplicationService.recordSourceSnapshot(sourceSnapshotRequest(
                batch.getSn(), ReconciliationSourceRole.COMPARISON, ReconciliationSourceType.SETTLEMENT_REPORT,
                List.of("comparison:001"), List.of("report:comparison")), WindOperatorFactory.system());

        assertThat(snapshotCount()).isEqualTo(2);
        assertThat(batchStatus(batch.getSn())).isEqualTo(ReconciliationBatchStatus.DATA_READY.name());
    }

    /**
     * 场景：创建窗口倒置或使用无效时区。
     * 结果：在生成持久化事实前快速失败。
     */
    @Test
    void testCreateShouldRejectInvalidWindowOrTimezone() {
        CreateReconciliationBatchRequest invalidWindow = minimumCreateRequest("recon-rule-v1")
                .setWindowEnd(LocalDateTime.of(2026, 7, 20, 0, 0));
        CreateReconciliationBatchRequest invalidTimezone = minimumCreateRequest("recon-rule-v1")
                .setTimezoneId("Mars/Olympus");

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                invalidWindow, WindOperatorFactory.system()))
                .hasMessageContaining("开始时间必须早于结束时间");
        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                invalidTimezone, WindOperatorFactory.system()))
                .hasMessageContaining("时区 ID 无效");
        assertThat(batchCount()).isZero();
    }

    /**
     * 场景：已完成批次因规则调整发起重跑。
     * 结果：允许规则版本变化，但准入对象和对账窗口必须保持一致，并生成新批次事实。
     */
    @Test
    void testCreateRerunShouldAllowNewRuleVersionForSameScope() {
        ReconciliationBatchDTO previous = createCompletedBatch();

        ReconciliationBatchDTO rerun = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system());

        assertThat(rerun.getSn()).isNotEqualTo(previous.getSn());
        assertThat(rerun.getPreviousBatchSn()).isEqualTo(previous.getSn());
        assertThat(rerun.getRuleVersion()).isEqualTo("recon-rule-v2");
        assertThat(rerun.getStatus()).isEqualTo(ReconciliationBatchStatus.CREATED);
        assertThat(batchCount()).isEqualTo(2);

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v3")
                        .setPreviousBatchSn(previous.getSn())
                        .setGateObjectSn("clearing-candidate-002"),
                WindOperatorFactory.system()))
                .hasMessageContaining("准入对象必须与上一批次一致");
        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v3")
                        .setPreviousBatchSn(previous.getSn())
                        .setReconciliationScopeRef("clearing:clearing-candidate-002"),
                WindOperatorFactory.system()))
                .hasMessageContaining("对账范围必须与上一批次一致");
    }

    /**
     * 场景：Gate 对账已经发现逐笔差异，但差错对象尚未全部物化时发起重跑。
     * 结果：拒绝创建重跑批次，避免新结果覆盖尚未进入运营处置链的差异事实。
     */
    @Test
    void testCreateRerunShouldRejectUnmaterializedGateDifferences() {
        ReconciliationBatchDTO previous = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(previous.getSn());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("Gate 对账差异尚未全部物化");

        assertThat(batchCount()).isOne();
    }

    /**
     * 场景：Gate 差异已经物化，但运营尚未完成处理动作就发起重跑。
     * 结果：拒绝推进血缘，避免进入重跑后不能补动作、对平后也不能关闭差错的死路。
     */
    @Test
    void testCreateRerunShouldRejectDifferenceWithoutCompletedAction() {
        ReconciliationBatchDTO previous = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(previous.getSn());
        materializeDifferenceWithoutAction(previous.getSn());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("Gate 对账差错必须先完成处理动作");

        assertThat(batchCount()).isOne();
    }

    @Test
    void testCreateRerunShouldAllowDifferenceWithCompletedAction() {
        ReconciliationBatchDTO previous = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(previous.getSn());
        materializeDifferenceWithoutAction(previous.getSn());
        completeDifferenceAction(previous.getSn());

        ReconciliationBatchDTO rerun = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system());

        assertThat(rerun.getPreviousBatchSn()).isEqualTo(previous.getSn());
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：首轮处理后重跑仍未对平，同一差错追加第二个处理动作并再次重跑。
     * 结果：第二轮重跑按差错当前批次锚点校验处理动作，不要求重复物化新差错。
     */
    @Test
    void testCreateRerunShouldUseCurrentDifferenceBatchAfterUnbalancedRerun() {
        ReconciliationBatchDTO original = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(original.getSn());
        materializeDifferenceWithoutAction(original.getSn());
        completeDifferenceAction(original.getSn());
        ReconciliationBatchDTO firstRerun = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(original.getSn()),
                WindOperatorFactory.system());
        completeBatchWithUnmaterializedDifference(firstRerun.getSn());
        moveDifferenceToNextAction(original.getSn(), firstRerun.getSn());

        ReconciliationBatchDTO secondRerun = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v3").setPreviousBatchSn(firstRerun.getSn()),
                WindOperatorFactory.system());

        assertThat(secondRerun.getPreviousBatchSn()).isEqualTo(firstRerun.getSn());
        assertThat(batchCount()).isEqualTo(3);
    }

    /**
     * 场景：历史差错已在当前批次对平关闭，随后继续创建下一轮对账批次。
     * 结果：已关闭差错不再计入当前批次待物化、待处理差错，血缘可以继续推进。
     */
    @Test
    void testCreateRerunShouldIgnoreResolvedHistoricalDifference() {
        ReconciliationBatchDTO original = createBatch("recon-rule-v1");
        completeBatchWithUnmaterializedDifference(original.getSn());
        materializeDifferenceWithoutAction(original.getSn());
        completeDifferenceAction(original.getSn());
        ReconciliationBatchDTO firstRerun = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(original.getSn()),
                WindOperatorFactory.system());
        completeBatch(firstRerun.getSn(), "BALANCED", 1, 1, 0);
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'RESOLVED', last_rerun_batch_sn = ?, last_rerun_sn = ?,
                    last_rerun_balanced = TRUE, resolved_by = 'SYSTEM', resolved_time = CURRENT_TIMESTAMP
                WHERE tenant_id = ? AND difference_sn = ?
                """, firstRerun.getSn(), firstRerun.getSn() + ":RUN",
                TENANT_ID, original.getSn() + ":DIFFERENCE");

        ReconciliationBatchDTO secondRerun = reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v3").setPreviousBatchSn(firstRerun.getSn()),
                WindOperatorFactory.system());

        assertThat(secondRerun.getPreviousBatchSn()).isEqualTo(firstRerun.getSn());
        assertThat(batchCount()).isEqualTo(3);
    }

    /**
     * 场景：同一已完成批次已经创建一个重跑批次后，再次创建不同规则版本的同级重跑。
     * 结果：拒绝形成分叉，重跑血缘保持单链。
     */
    @Test
    void testCreateRerunShouldRejectSiblingBatchForSamePreviousBatch() {
        ReconciliationBatchDTO previous = createCompletedBatch();
        reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system());

        assertThatThrownBy(() -> reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest("recon-rule-v3").setPreviousBatchSn(previous.getSn()),
                WindOperatorFactory.system()))
                .hasMessageContaining("只允许创建一个直接重跑批次");

        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：两个线程并发创建完全相同的重跑批次。
     * 结果：上一批次行锁内复查摘要，两个调用都复用同一批次事实。
     */
    @Test
    void testCreateRerunShouldReuseWinnerForConcurrentSameFacts() throws Exception {
        ReconciliationBatchDTO previous = createCompletedBatch();
        CreateReconciliationBatchRequest request = minimumCreateRequest("recon-rule-v2")
                .setPreviousBatchSn(previous.getSn());

        List<BatchCreateAttempt> results = concurrentlyCreate(request, request);

        assertThat(results).allMatch(BatchCreateAttempt::succeeded);
        assertThat(results).extracting(BatchCreateAttempt::sn).containsOnly(results.getFirst().sn());
        assertThat(batchCount()).isEqualTo(2);
    }

    /**
     * 场景：两个线程并发为同一上一批次创建不同事实的重跑批次。
     * 结果：只允许一个请求成功，另一请求被线性血缘约束拒绝。
     */
    @Test
    void testCreateRerunShouldAllowOnlyOneConcurrentDifferentFacts() throws Exception {
        ReconciliationBatchDTO previous = createCompletedBatch();

        List<BatchCreateAttempt> results = concurrentlyCreate(
                minimumCreateRequest("recon-rule-v2").setPreviousBatchSn(previous.getSn()),
                minimumCreateRequest("recon-rule-v3").setPreviousBatchSn(previous.getSn()));

        assertThat(results).filteredOn(BatchCreateAttempt::succeeded).hasSize(1);
        assertThat(results).filteredOn(result -> !result.succeeded()).singleElement()
                .extracting(BatchCreateAttempt::message)
                .asString()
                .contains("只允许创建一个直接重跑批次");
        assertThat(batchCount()).isEqualTo(2);
    }

    private ReconciliationBatchDTO createCompletedBatch() {
        ReconciliationBatchDTO result = createBatch("recon-rule-v1");
        completeBatch(result.getSn(), "BALANCED", 1, 1, 0);
        return result;
    }

    private void completeBatchWithUnmaterializedDifference(String batchSn) {
        completeBatch(batchSn, "DIFFERENCE_FOUND", 1, 0, 1);
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_match_result
                    (sn, tenant_id, reconciliation_run_result_sn, reconciliation_batch_sn,
                     reference_source_ref, comparison_source_ref, source_quality, match_strength,
                     difference_type, severity, evidence_ref, match_identity_digest, match_digest, created_by)
                VALUES (?, ?, ?, ?, 'internal:test-001', 'external:test-001', 'VERIFIED', 'UNMATCHED',
                        'STATUS_MISMATCH', 'S1_MAJOR', 'report:test#line-1', ?, ?, 'SYSTEM')
                """, batchSn + ":MATCH", TENANT_ID, batchSn + ":RUN", batchSn,
                "5".repeat(64), "6".repeat(64));
    }

    private void materializeDifferenceWithoutAction(String batchSn) {
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_difference (
                    difference_sn, tenant_id, reconciliation_batch_sn, reconciliation_match_result_sn,
                    source_quality, match_strength, difference_type, severity, status,
                    responsible_party_ref, blocking_object_type, blocking_object_sn,
                    rule_version, evidence_ref, created_by)
                VALUES (?, ?, ?, ?, 'VERIFIED', 'UNMATCHED', 'STATUS_MISMATCH', 'S1_MAJOR', 'BLOCKED',
                        'processor:test', 'CLEARING', 'clearing-candidate-001',
                        'recon-rule-v1', 'report:test#line-1', 'SYSTEM')
                """, batchSn + ":DIFFERENCE", TENANT_ID, batchSn, batchSn + ":MATCH");
    }

    private void completeDifferenceAction(String batchSn) {
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_difference_action (
                    sn, tenant_id, difference_sn, action_type, adjustment_sn, idempotency_key,
                    original_fact_ref, approval_ref, evidence_ref, reason, created_by)
                VALUES (?, ?, ?, 'ADJUSTMENT', ?, ?, 'transaction:test-001',
                        'approval:test-001', 'evidence:test-001', '测试处理动作', 'SYSTEM')
                """, batchSn + ":ACTION", TENANT_ID, batchSn + ":DIFFERENCE",
                batchSn + ":ADJUSTMENT", batchSn + ":IDEMPOTENCY");
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'ADJUSTING', action_type = 'ADJUSTMENT', adjustment_sn = ?,
                    adjustment_idempotency_key = ?, original_fact_ref = 'transaction:test-001',
                    adjustment_approval_ref = 'approval:test-001',
                    adjustment_evidence_ref = 'evidence:test-001', adjustment_reason = '测试处理动作',
                    adjusted_by = 'SYSTEM', adjusted_time = CURRENT_TIMESTAMP
                WHERE tenant_id = ? AND difference_sn = ?
                """, batchSn + ":ADJUSTMENT", batchSn + ":IDEMPOTENCY",
                TENANT_ID, batchSn + ":DIFFERENCE");
    }

    private void moveDifferenceToNextAction(String originalBatchSn, String rerunBatchSn) {
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_difference_action (
                    sn, tenant_id, difference_sn, action_type, adjustment_sn, idempotency_key,
                    original_fact_ref, approval_ref, evidence_ref, reason, created_by)
                VALUES (?, ?, ?, 'RECOVER', ?, ?, 'transaction:test-001',
                        'approval:test-002', 'evidence:test-002', '重跑未对平后的第二次处理动作', 'SYSTEM')
                """, rerunBatchSn + ":ACTION", TENANT_ID, originalBatchSn + ":DIFFERENCE",
                rerunBatchSn + ":ADJUSTMENT", rerunBatchSn + ":IDEMPOTENCY");
        jdbcTemplate.update("""
                UPDATE t_reconciliation_difference
                SET status = 'ADJUSTING', action_type = 'RECOVER', adjustment_sn = ?,
                    adjustment_idempotency_key = ?, adjustment_approval_ref = 'approval:test-002',
                    adjustment_evidence_ref = 'evidence:test-002',
                    adjustment_reason = '重跑未对平后的第二次处理动作', adjusted_by = 'SYSTEM',
                    adjusted_time = CURRENT_TIMESTAMP, last_rerun_sn = ?, last_rerun_batch_sn = ?,
                    last_rerun_rule_version = 'recon-rule-v2', last_rerun_balanced = FALSE, rerun_count = 1
                WHERE tenant_id = ? AND difference_sn = ?
                """, rerunBatchSn + ":ADJUSTMENT", rerunBatchSn + ":IDEMPOTENCY",
                rerunBatchSn + ":RUN", rerunBatchSn, TENANT_ID, originalBatchSn + ":DIFFERENCE");
    }

    private void completeBatch(String batchSn, String status, int totalCount, int matchedCount, int differenceCount) {
        String runResultSn = batchSn + ":RUN";
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_run_result
                    (sn, tenant_id, reconciliation_batch_sn, reconciliation_scope_ref,
                     gate_object_type, gate_object_sn, status, rule_version,
                     reference_source_digest, comparison_source_digest, source_digest, result_digest,
                     total_count, matched_count, difference_count, evidence_refs, created_by)
                VALUES (?, ?, ?, 'clearing:clearing-candidate-001',
                        'CLEARING', 'clearing-candidate-001', ?, 'recon-rule-v1',
                        ?, ?, ?, ?, ?, ?, ?, '["report:test"]', 'SYSTEM')
                """, runResultSn, TENANT_ID, batchSn, status,
                "1".repeat(64), "2".repeat(64), "3".repeat(64), "4".repeat(64),
                totalCount, matchedCount, differenceCount);
        jdbcTemplate.update("UPDATE t_reconciliation_batch SET status = 'COMPLETED', run_result_sn = ? WHERE sn = ?",
                runResultSn, batchSn);
    }

    private List<BatchCreateAttempt> concurrentlyCreate(CreateReconciliationBatchRequest firstRequest,
                                                        CreateReconciliationBatchRequest secondRequest)
            throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BatchCreateAttempt> first = executor.submit(concurrentCreateAttempt(startGate, firstRequest));
            Future<BatchCreateAttempt> second = executor.submit(concurrentCreateAttempt(startGate, secondRequest));
            startGate.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<BatchCreateAttempt> concurrentCreateAttempt(CountDownLatch startGate,
                                                                 CreateReconciliationBatchRequest request) {
        return () -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                startGate.await();
                ReconciliationBatchDTO result = reconciliationBatchApplicationService.createBatch(
                        request, WindOperatorFactory.system());
                return new BatchCreateAttempt(true, result.getSn(), null);
            } catch (RuntimeException exception) {
                return new BatchCreateAttempt(false, null, exception.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        };
    }

    private List<BatchCreateAttempt> concurrentlyReplace(ReplaceReconciliationBatchRequest firstRequest,
                                                         ReplaceReconciliationBatchRequest secondRequest)
            throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<BatchCreateAttempt> first = executor.submit(concurrentReplaceAttempt(startGate, firstRequest));
            Future<BatchCreateAttempt> second = executor.submit(concurrentReplaceAttempt(startGate, secondRequest));
            startGate.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<BatchCreateAttempt> concurrentReplaceAttempt(CountDownLatch startGate,
                                                                  ReplaceReconciliationBatchRequest request) {
        return () -> {
            TenantContextHolder.setTenantId(TENANT_ID);
            try {
                startGate.await();
                ReconciliationBatchDTO result = reconciliationBatchApplicationService.replaceBatch(
                        request, WindOperatorFactory.system());
                return new BatchCreateAttempt(true, result.getSn(), null);
            } catch (RuntimeException exception) {
                return new BatchCreateAttempt(false, null, exception.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        };
    }

    private ReconciliationBatchDTO createBatch(String ruleVersion) {
        return reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest(ruleVersion), WindOperatorFactory.system());
    }

    private ReplaceReconciliationBatchRequest replacementRequest(String batchSn) {
        return new ReplaceReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batchSn)
                .setRuleVersion("recon-rule-v2")
                .setReason("外部清算文件解析版本错误")
                .setEvidenceRef("evidence:parser-incident-001");
    }

    private CreateReconciliationBatchRequest minimumCreateRequest(String ruleVersion) {
        return new CreateReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationScopeRef("clearing:clearing-candidate-001")
                .setGateObjectType(ReconciliationGateObjectType.CLEARING)
                .setGateObjectSn("clearing-candidate-001")
                .setRuleVersion(ruleVersion)
                .setWindowStart(LocalDateTime.of(2026, 7, 21, 0, 0))
                .setWindowEnd(LocalDateTime.of(2026, 7, 22, 0, 0))
                .setTimezoneId("Asia/Shanghai");
    }

    private RecordReconciliationSourceSnapshotRequest sourceSnapshotRequest(
            String batchSn,
            ReconciliationSourceRole sourceRole,
            ReconciliationSourceType sourceType,
            List<String> sourceItemRefs,
            List<String> evidenceRefs) {
        return new RecordReconciliationSourceSnapshotRequest()
                .setTenantId(TENANT_ID)
                .setReconciliationBatchSn(batchSn)
                .setSourceRole(sourceRole)
                .setSourceType(sourceType)
                .setSourceItems(sourceItemRefs.stream()
                        .map(sourceItemRef -> new ReconciliationSourceItemInput()
                                .setSourceItemRef(sourceItemRef)
                                .setContentDigest(FundsStableHashSupport.sha256(sourceItemRef.trim())))
                        .toList())
                .setEvidenceRefs(evidenceRefs);
    }

    private int batchCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_batch", Integer.class);
    }

    private int snapshotCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_source_snapshot", Integer.class);
    }

    private int sourceItemCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_reconciliation_source_item", Integer.class);
    }

    private String batchStatus(String batchSn) {
        return jdbcTemplate.queryForObject("SELECT status FROM t_reconciliation_batch WHERE sn = ?",
                String.class, batchSn);
    }

    private String differenceStatus(String differenceSn) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM t_reconciliation_difference WHERE tenant_id = ? AND difference_sn = ?",
                String.class, TENANT_ID, differenceSn);
    }

    private String lineageCurrentBatchSn() {
        return jdbcTemplate.queryForObject("""
                SELECT current_batch_sn
                FROM t_reconciliation_batch_lineage
                WHERE tenant_id = ? AND gate_object_type = 'CLEARING'
                  AND gate_object_sn = 'clearing-candidate-001'
                """, String.class, TENANT_ID);
    }

    @Configuration
    @Import(ReconciliationBatchApplicationServiceImpl.class)
    static class Config {
    }

    private record BatchCreateAttempt(boolean succeeded, String sn, String message) {
    }
}
