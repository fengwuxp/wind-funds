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
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
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
        ReconciliationBatchDTO previous = createBatch("recon-rule-v1");
        jdbcTemplate.update("UPDATE t_reconciliation_batch SET status = 'COMPLETED' WHERE sn = ?", previous.getSn());

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
    }

    private ReconciliationBatchDTO createBatch(String ruleVersion) {
        return reconciliationBatchApplicationService.createBatch(
                minimumCreateRequest(ruleVersion), WindOperatorFactory.system());
    }

    private CreateReconciliationBatchRequest minimumCreateRequest(String ruleVersion) {
        return new CreateReconciliationBatchRequest()
                .setTenantId(TENANT_ID)
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
                .setSourceItemRefs(sourceItemRefs)
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

    @Configuration
    @Import(ReconciliationBatchApplicationServiceImpl.class)
    static class Config {
    }
}
