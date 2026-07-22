package com.wind.funds.reconciliation;

import com.alibaba.fastjson2.JSON;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.enums.ReconciliationSourceType;
import com.wind.funds.reconciliation.support.ReconciliationDigestSupport;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 对账集成测试来源事实准备工具。
 */
public final class ReconciliationTestFixture {

    private ReconciliationTestFixture() {
    }

    public static void prepareReadyBatch(JdbcTemplate jdbcTemplate,
                                         Long tenantId,
                                         String batchSn,
                                         ReconciliationGateObjectType gateObjectType,
                                         String gateObjectSn,
                                         String ruleVersion,
                                         String evidenceRef,
                                         String referenceSourceRef,
                                         String comparisonSourceRef) {
        prepareReadyBatch(jdbcTemplate, tenantId, batchSn, gateObjectType, gateObjectSn, ruleVersion,
                evidenceRef, referenceSourceRef, comparisonSourceRef, null);
    }

    public static void prepareReadyBatch(JdbcTemplate jdbcTemplate,
                                         Long tenantId,
                                         String batchSn,
                                         ReconciliationGateObjectType gateObjectType,
                                         String gateObjectSn,
                                         String ruleVersion,
                                         String evidenceRef,
                                         String referenceSourceRef,
                                         String comparisonSourceRef,
                                         @Nullable String previousBatchSn) {
        String batchDigest = FundsStableHashSupport.sha256Json(Map.of("batchSn", batchSn));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_batch
                    (sn, tenant_id, gate_object_type, gate_object_sn, rule_version,
                     window_start, window_end, timezone_id, previous_batch_sn, status, batch_digest, created_by)
                VALUES (?, ?, ?, ?, ?, '2026-07-21 00:00:00', '2026-07-22 00:00:00',
                        'Asia/Shanghai', ?, 'DATA_READY', ?, 'SYSTEM')
                """, batchSn, tenantId, gateObjectType.name(), gateObjectSn, ruleVersion,
                previousBatchSn, batchDigest);
        prepareSourceSnapshot(jdbcTemplate, tenantId, batchSn, ReconciliationSourceRole.REFERENCE,
                ReconciliationSourceType.TRANSACTION, referenceSourceRef, evidenceRef);
        prepareSourceSnapshot(jdbcTemplate, tenantId, batchSn, ReconciliationSourceRole.COMPARISON,
                ReconciliationSourceType.SETTLEMENT_REPORT, comparisonSourceRef, evidenceRef);
    }

    public static void clearRunAndBatchFacts(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_source_item");
        jdbcTemplate.update("DELETE FROM t_reconciliation_source_snapshot");
        jdbcTemplate.update("DELETE FROM t_reconciliation_batch");
    }

    private static void prepareSourceSnapshot(JdbcTemplate jdbcTemplate,
                                              Long tenantId,
                                              String batchSn,
                                              ReconciliationSourceRole sourceRole,
                                              ReconciliationSourceType sourceType,
                                              String sourceItemRef,
                                              String evidenceRef) {
        String snapshotSn = batchSn + ":" + sourceRole.name();
        String itemDigest = ReconciliationDigestSupport.sourceItemDigest(sourceRole, sourceType, sourceItemRef);
        String sourceDigest = ReconciliationDigestSupport.sourceDigest(sourceRole, sourceType, List.of(itemDigest));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_snapshot
                    (sn, tenant_id, reconciliation_batch_sn, source_role, source_type,
                     source_digest, record_count, evidence_refs, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, 'SYSTEM')
                """, snapshotSn, tenantId, batchSn, sourceRole.name(), sourceType.name(),
                sourceDigest, JSON.toJSONString(List.of(evidenceRef)));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_item
                    (sn, tenant_id, source_snapshot_sn, source_item_ref, item_digest, created_by)
                VALUES (?, ?, ?, ?, ?, 'SYSTEM')
                """, snapshotSn + ":ITEM", tenantId, snapshotSn, sourceItemRef, itemDigest);
    }
}
