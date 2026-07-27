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
        String reconciliationScopeRef = gateObjectType == null
                ? "test-scope:" + batchSn
                : gateObjectType.name().toLowerCase() + ":" + gateObjectSn;
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_batch
                    (sn, tenant_id, reconciliation_scope_ref, gate_object_type, gate_object_sn, rule_version,
                     window_start, window_end, timezone_id, previous_batch_sn, status, batch_digest, created_by)
                VALUES (?, ?, ?, ?, ?, ?, '2026-07-21 00:00:00', '2026-07-22 00:00:00',
                        'Asia/Shanghai', ?, 'DATA_READY', ?, 'SYSTEM')
                """, batchSn, tenantId, reconciliationScopeRef,
                gateObjectType == null ? null : gateObjectType.name(), gateObjectSn, ruleVersion,
                previousBatchSn, batchDigest);
        if (gateObjectType != null) {
            int updated = jdbcTemplate.update("""
                    UPDATE t_reconciliation_batch_lineage
                    SET current_batch_sn = ?
                    WHERE tenant_id = ?
                      AND gate_object_type = ?
                      AND gate_object_sn = ?
                    """, batchSn, tenantId, gateObjectType.name(), gateObjectSn);
            if (updated == 0) {
                jdbcTemplate.update("""
                        INSERT INTO t_reconciliation_batch_lineage
                            (tenant_id, reconciliation_scope_ref, gate_object_type, gate_object_sn, current_batch_sn)
                        VALUES (?, ?, ?, ?, ?)
                        """, tenantId, reconciliationScopeRef, gateObjectType.name(), gateObjectSn, batchSn);
            }
        }
        prepareSourceSnapshot(jdbcTemplate, tenantId, batchSn, ReconciliationSourceRole.REFERENCE,
                ReconciliationSourceType.TRANSACTION, referenceSourceRef, evidenceRef);
        prepareSourceSnapshot(jdbcTemplate, tenantId, batchSn, ReconciliationSourceRole.COMPARISON,
                ReconciliationSourceType.SETTLEMENT_REPORT, comparisonSourceRef, evidenceRef);
    }

    public static void clearRunAndBatchFacts(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM t_reconciliation_difference_action");
        jdbcTemplate.update("DELETE FROM t_reconciliation_match_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_run_result");
        jdbcTemplate.update("DELETE FROM t_reconciliation_source_item");
        jdbcTemplate.update("DELETE FROM t_reconciliation_source_snapshot");
        jdbcTemplate.update("DELETE FROM t_reconciliation_batch_lineage");
        jdbcTemplate.update("DELETE FROM t_reconciliation_batch");
    }

    public static void withMatchBatchAsCurrentHead(JdbcTemplate jdbcTemplate,
                                                   Long tenantId,
                                                   String matchResultSn,
                                                   Runnable action) {
        Map<String, Object> identity = jdbcTemplate.queryForMap("""
                SELECT m.reconciliation_batch_sn, b.gate_object_type, b.gate_object_sn
                FROM t_reconciliation_match_result m
                JOIN t_reconciliation_batch b
                  ON b.tenant_id = m.tenant_id
                 AND b.sn = m.reconciliation_batch_sn
                WHERE m.tenant_id = ?
                  AND m.sn = ?
                """, tenantId, matchResultSn);
        String gateObjectType = (String) identity.get("GATE_OBJECT_TYPE");
        String gateObjectSn = (String) identity.get("GATE_OBJECT_SN");
        String currentBatchSn = jdbcTemplate.queryForObject("""
                SELECT current_batch_sn FROM t_reconciliation_batch_lineage
                WHERE tenant_id = ? AND gate_object_type = ? AND gate_object_sn = ?
                """, String.class, tenantId, gateObjectType, gateObjectSn);
        try {
            updateCurrentBatch(jdbcTemplate, tenantId, gateObjectType, gateObjectSn,
                    (String) identity.get("RECONCILIATION_BATCH_SN"));
            action.run();
        } finally {
            updateCurrentBatch(jdbcTemplate, tenantId, gateObjectType, gateObjectSn, currentBatchSn);
        }
    }

    private static void updateCurrentBatch(JdbcTemplate jdbcTemplate,
                                           Long tenantId,
                                           String gateObjectType,
                                           String gateObjectSn,
                                           String currentBatchSn) {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch_lineage SET current_batch_sn = ?
                WHERE tenant_id = ? AND gate_object_type = ? AND gate_object_sn = ?
                """, currentBatchSn, tenantId, gateObjectType, gateObjectSn);
    }

    private static void prepareSourceSnapshot(JdbcTemplate jdbcTemplate,
                                              Long tenantId,
                                              String batchSn,
                                              ReconciliationSourceRole sourceRole,
                                              ReconciliationSourceType sourceType,
                                              String sourceItemRef,
                                              String evidenceRef) {
        String snapshotSn = batchSn + ":" + sourceRole.name();
        String contentDigest = FundsStableHashSupport.sha256(sourceItemRef);
        String sourceDigest = ReconciliationDigestSupport.sourceDigest(
                sourceRole, sourceType, Map.of(sourceItemRef, contentDigest));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_snapshot
                    (sn, tenant_id, reconciliation_batch_sn, source_role, source_type,
                     source_digest, record_count, evidence_refs, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 1, ?, 'SYSTEM')
                """, snapshotSn, tenantId, batchSn, sourceRole.name(), sourceType.name(),
                sourceDigest, JSON.toJSONString(List.of(evidenceRef)));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_item
                    (sn, tenant_id, source_snapshot_sn, source_item_ref, content_digest, created_by)
                VALUES (?, ?, ?, ?, ?, 'SYSTEM')
                """, snapshotSn + ":ITEM", tenantId, snapshotSn, sourceItemRef, contentDigest);
    }
}
