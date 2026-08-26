package com.wind.funds.reconciliation;

import com.wind.jackson.WindJson;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.funds.reconciliation.model.request.NormalizedComparisonFactInput;
import com.wind.funds.reconciliation.model.request.RecordReconciliationSourceSnapshotRequest;
import com.wind.funds.reconciliation.model.value.ComparisonRuleRef;
import com.wind.funds.reconciliation.model.value.GateStageRef;
import com.wind.funds.reconciliation.model.value.SnapshotCoverage;
import com.wind.funds.reconciliation.model.value.StableIdentity;
import com.wind.funds.transaction.support.FundsStableHashSupport;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * 对账集成测试来源事实准备工具。
 */
public final class ReconciliationTestFixture {

    public static final String TEST_RULE_NAMESPACE = "test.rule";

    public static final String TEST_RULE_IDENTITY = "strict-exact";

    private ReconciliationTestFixture() {
    }

    public static StableIdentity identity(String ownerNamespace, String value) {
        return new StableIdentity().setOwnerNamespace(ownerNamespace).setValue(value);
    }

    public static ComparisonRuleRef rule(String version) {
        return new ComparisonRuleRef()
                .setNamespace(TEST_RULE_NAMESPACE)
                .setIdentity(TEST_RULE_IDENTITY)
                .setVersion(version);
    }

    public static GateStageRef stage(String stageKind, String stageIdentity) {
        return new GateStageRef()
                .setStageKind(stageKind)
                .setStageIdentity(identity(stageOwner(stageKind), stageIdentity));
    }

    public static RecordReconciliationSourceSnapshotRequest sourceSnapshotRequest(
            Long tenantId,
            String batchSn,
            ReconciliationSourceRole sourceRole,
            String sourceNamespace,
            List<String> sourceFactRefs,
            List<String> evidenceRefs) {
        return sourceSnapshotRequest(tenantId, batchSn, sourceRole, sourceNamespace,
                sourceFactRefs, evidenceRefs, null, 1L, "CONFIRMED");
    }

    public static RecordReconciliationSourceSnapshotRequest sourceSnapshotRequest(
            Long tenantId,
            String batchSn,
            ReconciliationSourceRole sourceRole,
            String sourceNamespace,
            List<String> sourceFactRefs,
            List<String> evidenceRefs,
            @Nullable String comparisonIdentityValue,
            long amount,
            String comparisonStatusCode) {
        List<NormalizedComparisonFactInput> facts = sourceFactRefs.stream()
                .map(sourceFactRef -> normalizedFact(sourceFactRef, evidenceRefs,
                        comparisonIdentityValue, amount, comparisonStatusCode))
                .toList();
        return new RecordReconciliationSourceSnapshotRequest()
                .setTenantId(tenantId)
                .setReconciliationBatchSn(batchSn)
                .setSourceRole(sourceRole)
                .setSourceNamespace(sourceNamespace)
                .setSnapshotIdentity(identity("test.snapshot", batchSn + ":" + sourceRole.name()))
                .setSnapshotVersion("v1")
                .setCoverage(new SnapshotCoverage()
                        .setComplete(true)
                        .setWatermark("test-watermark")
                        .setMemberCount(facts.size()))
                .setFacts(facts)
                .setEvidenceRefs(evidenceRefs);
    }

    private static NormalizedComparisonFactInput normalizedFact(String sourceFactRef,
                                                                List<String> evidenceRefs,
                                                                @Nullable String comparisonIdentityValue,
                                                                long amount,
                                                                String comparisonStatusCode) {
        return new NormalizedComparisonFactInput()
                .setSourceFactRef(identity("test.fact", sourceFactRef))
                .setComparisonIdentity(identity("test.compare",
                        comparisonIdentityValue == null ? sourceFactRef : comparisonIdentityValue))
                .setAmount(amount)
                .setCurrency(CurrencyIsoCode.USD)
                .setComparisonRuleRef(rule("v1"))
                .setComparisonStatusCode(comparisonStatusCode)
                .setComparisonProven(true)
                .setClaimKind("FUNDS")
                .setEconomicComponent("PRINCIPAL")
                .setDirection("CREDIT")
                .setNormalizationVersion("v1")
                .setEvidenceRefs(evidenceRefs);
    }

    public static void prepareReadyBatch(JdbcTemplate jdbcTemplate,
                                         Long tenantId,
                                         String batchSn,
                                         String stageKind,
                                         String gateObjectSn,
                                         String ruleVersion,
                                         String evidenceRef,
                                         String referenceSourceRef,
                                         String comparisonSourceRef) {
        prepareReadyBatch(jdbcTemplate, tenantId, batchSn, stageKind, gateObjectSn, ruleVersion,
                evidenceRef, referenceSourceRef, comparisonSourceRef, null, 1L, "CONFIRMED");
    }

    public static void prepareReadyBatch(JdbcTemplate jdbcTemplate,
                                         Long tenantId,
                                         String batchSn,
                                         String stageKind,
                                         String gateObjectSn,
                                         String ruleVersion,
                                         String evidenceRef,
                                         String referenceSourceRef,
                                         String comparisonSourceRef,
                                         @Nullable String previousBatchSn) {
        prepareReadyBatch(jdbcTemplate, tenantId, batchSn, stageKind, gateObjectSn, ruleVersion,
                evidenceRef, referenceSourceRef, comparisonSourceRef, previousBatchSn, 1L, "CONFIRMED");
    }

    public static void prepareReadyBatch(JdbcTemplate jdbcTemplate,
                                         Long tenantId,
                                         String batchSn,
                                         String stageKind,
                                         String gateObjectSn,
                                         String ruleVersion,
                                         String evidenceRef,
                                         String referenceSourceRef,
                                         String comparisonSourceRef,
                                         @Nullable String previousBatchSn,
                                         long comparisonAmount,
                                         String comparisonStatusCode) {
        prepareReadyBatch(jdbcTemplate, tenantId, batchSn, stageKind, gateObjectSn, ruleVersion,
                evidenceRef, referenceSourceRef, comparisonSourceRef, previousBatchSn,
                comparisonAmount, comparisonStatusCode, null);
    }

    public static void prepareReadyBatch(JdbcTemplate jdbcTemplate,
                                         Long tenantId,
                                         String batchSn,
                                         String stageKind,
                                         String gateObjectSn,
                                         String ruleVersion,
                                         String evidenceRef,
                                         String referenceSourceRef,
                                         String comparisonSourceRef,
                                         @Nullable String previousBatchSn,
                                         long comparisonAmount,
                                         String comparisonStatusCode,
                                         @Nullable String comparisonIdentityValue) {
        String batchDigest = FundsStableHashSupport.sha256Json(Map.of("batchSn", batchSn));
        String scopeValue = stageKind == null ? "test-scope:" + batchSn : stageKind + ":" + gateObjectSn;
        String pairValue = previousBatchSn == null ? "test-pair:" + batchSn
                : jdbcTemplate.query("SELECT pair_identity_value FROM t_reconciliation_batch WHERE tenant_id = ? AND sn = ?",
                        resultSet -> resultSet.next() ? resultSet.getString(1) : "test-pair:" + batchSn,
                        tenantId, previousBatchSn);
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_batch
                    (sn, tenant_id, scope_owner_namespace, scope_identity_value,
                     pair_owner_namespace, pair_identity_value, currency,
                     rule_namespace, rule_identity, rule_version,
                     window_start, window_end, time_semantics, timezone_id,
                     previous_batch_sn, state, batch_digest, created_by)
                VALUES (?, ?, 'test.scope', ?, 'test.pair', ?, 'USD',
                        'test.rule', 'strict-exact', ?,
                        '2026-07-21 00:00:00', '2026-07-22 00:00:00', 'occurredAt',
                        'Asia/Shanghai', ?, 'DATA_READY', ?, 'SYSTEM')
                """, batchSn, tenantId, scopeValue, pairValue, ruleVersion, previousBatchSn, batchDigest);
        if (stageKind != null) {
            int updated = jdbcTemplate.update("""
                    UPDATE t_reconciliation_batch_lineage
                    SET current_batch_sn = ?
                    WHERE tenant_id = ?
                      AND scope_owner_namespace = 'test.scope'
                      AND scope_identity_value = ?
                      AND pair_owner_namespace = 'test.pair'
                      AND pair_identity_value = ?
                    """, batchSn, tenantId, scopeValue, pairValue);
            if (updated == 0) {
                jdbcTemplate.update("""
                        INSERT INTO t_reconciliation_batch_lineage
                            (tenant_id, scope_owner_namespace, scope_identity_value,
                             pair_owner_namespace, pair_identity_value, current_batch_sn)
                        VALUES (?, 'test.scope', ?, 'test.pair', ?, ?)
                        """, tenantId, scopeValue, pairValue, batchSn);
            }
        }
        String factComparisonIdentity = comparisonIdentityValue == null ? pairValue : comparisonIdentityValue;
        prepareSourceSnapshot(jdbcTemplate, tenantId, batchSn, ReconciliationSourceRole.REFERENCE,
                "transaction", referenceSourceRef, factComparisonIdentity, evidenceRef, 1L, "CONFIRMED");
        prepareSourceSnapshot(jdbcTemplate, tenantId, batchSn, ReconciliationSourceRole.COMPARISON,
                "settlement", comparisonSourceRef, factComparisonIdentity, evidenceRef,
                comparisonAmount, comparisonStatusCode);
        if (stageKind != null) {
            prepareGateRequirement(jdbcTemplate, tenantId, batchSn, stageKind, gateObjectSn, evidenceRef);
        }
    }

    public static void prepareGateRequirement(JdbcTemplate jdbcTemplate,
                                              Long tenantId,
                                              String batchSn,
                                              String stageKind,
                                              String stageIdentity,
                                              String evidenceRef) {
        Map<String, Object> batch = jdbcTemplate.queryForMap("""
                SELECT scope_owner_namespace, scope_identity_value,
                       pair_owner_namespace, pair_identity_value,
                       rule_namespace, rule_identity, rule_version
                FROM t_reconciliation_batch WHERE tenant_id = ? AND sn = ?
                """, tenantId, batchSn);
        String stageOwner = stageOwner(stageKind);
        String requirementVersion = "fixture:" + batchSn;
        Map<String, Object> stage = Map.of(
                "stageKind", stageKind,
                "stageIdentity", Map.of("ownerNamespace", stageOwner, "value", stageIdentity));
        String requirementIdentity = FundsStableHashSupport.sha256Json(
                Map.of("stage", stage, "version", requirementVersion));
        String semanticDigest = FundsStableHashSupport.sha256Json(Map.of(
                "tenantId", tenantId,
                "stage", stage,
                "requirementVersion", requirementVersion,
                "requiredPairs", List.of(Map.of(
                        "scopeIdentity", Map.of("ownerNamespace", batch.get("SCOPE_OWNER_NAMESPACE"),
                                "value", batch.get("SCOPE_IDENTITY_VALUE")),
                        "pairIdentity", Map.of("ownerNamespace", batch.get("PAIR_OWNER_NAMESPACE"),
                                "value", batch.get("PAIR_IDENTITY_VALUE")),
                        "ruleNamespace", batch.get("RULE_NAMESPACE"),
                        "ruleIdentity", batch.get("RULE_IDENTITY"),
                        "ruleVersion", batch.get("RULE_VERSION")))));
        String evidenceRefs = WindJson.toJsonString(List.of(evidenceRef));
        String evidenceBundleDigest = FundsStableHashSupport.sha256Json(List.of(evidenceRef));
        List<Map<String, Object>> heads = jdbcTemplate.queryForList("""
                SELECT current_requirement_identity_owner_namespace,
                       current_requirement_identity_value, current_requirement_version,
                       current_semantic_digest, current_evidence_bundle_digest, version
                FROM t_reconciliation_gate_requirement_head
                WHERE tenant_id = ? AND stage_kind = ?
                  AND stage_identity_owner_namespace = ? AND stage_identity_value = ?
                """, tenantId, stageKind, stageOwner, stageIdentity);
        Map<String, Object> previous = heads.isEmpty() ? null : heads.getFirst();
        Integer existingRequirement = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM t_reconciliation_gate_requirement
                WHERE tenant_id = ?
                  AND requirement_identity_owner_namespace = 'wind-funds.reconciliation.gate-requirement'
                  AND requirement_identity_value = ?
                """, Integer.class, tenantId, requirementIdentity);
        if (existingRequirement != null && existingRequirement == 1) {
            if (previous == null) {
                jdbcTemplate.update("""
                        INSERT INTO t_reconciliation_gate_requirement_head
                            (tenant_id, stage_kind, stage_identity_owner_namespace, stage_identity_value,
                             current_requirement_identity_owner_namespace, current_requirement_identity_value,
                             current_requirement_version, current_semantic_digest,
                             current_evidence_bundle_digest, version)
                        VALUES (?, ?, ?, ?, 'wind-funds.reconciliation.gate-requirement', ?, ?, ?, ?, 0)
                        """, tenantId, stageKind, stageOwner, stageIdentity, requirementIdentity,
                        requirementVersion, semanticDigest, evidenceBundleDigest);
            } else if (!requirementIdentity.equals(previous.get("CURRENT_REQUIREMENT_IDENTITY_VALUE"))) {
                jdbcTemplate.update("""
                        UPDATE t_reconciliation_gate_requirement_head
                        SET current_requirement_identity_owner_namespace = 'wind-funds.reconciliation.gate-requirement',
                            current_requirement_identity_value = ?, current_requirement_version = ?,
                            current_semantic_digest = ?, current_evidence_bundle_digest = ?, version = version + 1
                        WHERE tenant_id = ? AND stage_kind = ?
                          AND stage_identity_owner_namespace = ? AND stage_identity_value = ?
                        """, requirementIdentity, requirementVersion, semanticDigest, evidenceBundleDigest,
                        tenantId, stageKind, stageOwner, stageIdentity);
            }
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_gate_requirement
                    (tenant_id, stage_kind, stage_identity_owner_namespace, stage_identity_value,
                     requirement_identity_owner_namespace, requirement_identity_value, requirement_version,
                     semantic_digest, evidence_refs, evidence_bundle_digest,
                     previous_requirement_identity_owner_namespace, previous_requirement_identity_value,
                     previous_requirement_version, previous_semantic_digest, previous_evidence_bundle_digest,
                     created_by)
                VALUES (?, ?, ?, ?, 'wind-funds.reconciliation.gate-requirement', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYSTEM')
                """, tenantId, stageKind, stageOwner, stageIdentity, requirementIdentity, requirementVersion,
                semanticDigest, evidenceRefs, evidenceBundleDigest,
                previous == null ? null : previous.get("CURRENT_REQUIREMENT_IDENTITY_OWNER_NAMESPACE"),
                previous == null ? null : previous.get("CURRENT_REQUIREMENT_IDENTITY_VALUE"),
                previous == null ? null : previous.get("CURRENT_REQUIREMENT_VERSION"),
                previous == null ? null : previous.get("CURRENT_SEMANTIC_DIGEST"),
                previous == null ? null : previous.get("CURRENT_EVIDENCE_BUNDLE_DIGEST"));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_gate_requirement_pair
                    (tenant_id, requirement_identity_owner_namespace, requirement_identity_value,
                     scope_owner_namespace, scope_identity_value, pair_owner_namespace, pair_identity_value,
                     rule_namespace, rule_identity, rule_version)
                VALUES (?, 'wind-funds.reconciliation.gate-requirement', ?, ?, ?, ?, ?, ?, ?, ?)
                """, tenantId, requirementIdentity,
                batch.get("SCOPE_OWNER_NAMESPACE"), batch.get("SCOPE_IDENTITY_VALUE"),
                batch.get("PAIR_OWNER_NAMESPACE"), batch.get("PAIR_IDENTITY_VALUE"),
                batch.get("RULE_NAMESPACE"), batch.get("RULE_IDENTITY"), batch.get("RULE_VERSION"));
        if (previous == null) {
            jdbcTemplate.update("""
                    INSERT INTO t_reconciliation_gate_requirement_head
                        (tenant_id, stage_kind, stage_identity_owner_namespace, stage_identity_value,
                         current_requirement_identity_owner_namespace, current_requirement_identity_value,
                         current_requirement_version, current_semantic_digest,
                         current_evidence_bundle_digest, version)
                    VALUES (?, ?, ?, ?, 'wind-funds.reconciliation.gate-requirement', ?, ?, ?, ?, 0)
                    """, tenantId, stageKind, stageOwner, stageIdentity, requirementIdentity,
                    requirementVersion, semanticDigest, evidenceBundleDigest);
        } else {
            jdbcTemplate.update("""
                    UPDATE t_reconciliation_gate_requirement_head
                    SET current_requirement_identity_owner_namespace = 'wind-funds.reconciliation.gate-requirement',
                        current_requirement_identity_value = ?, current_requirement_version = ?,
                        current_semantic_digest = ?, current_evidence_bundle_digest = ?, version = version + 1
                    WHERE tenant_id = ? AND stage_kind = ?
                      AND stage_identity_owner_namespace = ? AND stage_identity_value = ?
                    """, requirementIdentity, requirementVersion, semanticDigest, evidenceBundleDigest,
                    tenantId, stageKind, stageOwner, stageIdentity);
        }
    }

    private static String stageOwner(String stageKind) {
        return switch (stageKind) {
            case "CLEARING_SPLITTABLE_IDENTIFY" -> "funds";
            case "CLEARING_SPLIT_CONFIRM_ITEM" -> "clearing-split-item";
            case "CLEARING_CONFIRM_ITEM" -> "clearing-candidate";
            case "SETTLEMENT_LOCK", "SETTLEMENT_RELEASE", "PAYOUT_CREATE_PREFLIGHT" -> "settlement-order";
            case "PAYOUT_SUBMIT" -> "payout-order";
            default -> throw new IllegalArgumentException("Unsupported test Gate stage: " + stageKind);
        };
    }

    public static void clearRunAndBatchFacts(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("DELETE FROM t_reconciliation_stage_gate_evidence");
        jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement_head");
        jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement_pair");
        jdbcTemplate.update("DELETE FROM t_reconciliation_gate_requirement");
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
        Map<String, Object> identity = matchBatchIdentity(jdbcTemplate, tenantId, matchResultSn);
        String currentBatchSn = jdbcTemplate.queryForObject("""
                SELECT current_batch_sn FROM t_reconciliation_batch_lineage
                WHERE tenant_id = ?
                  AND scope_owner_namespace = ? AND scope_identity_value = ?
                  AND pair_owner_namespace = ? AND pair_identity_value = ?
                """, String.class, tenantId,
                identity.get("SCOPE_OWNER_NAMESPACE"), identity.get("SCOPE_IDENTITY_VALUE"),
                identity.get("PAIR_OWNER_NAMESPACE"), identity.get("PAIR_IDENTITY_VALUE"));
        try {
            updateCurrentBatch(jdbcTemplate, tenantId, identity,
                    (String) identity.get("RECONCILIATION_BATCH_SN"));
            action.run();
        } finally {
            updateCurrentBatch(jdbcTemplate, tenantId, identity, currentBatchSn);
        }
    }

    public static void setMatchBatchAsCurrentHead(JdbcTemplate jdbcTemplate,
                                                  Long tenantId,
                                                  String matchResultSn) {
        Map<String, Object> identity = matchBatchIdentity(jdbcTemplate, tenantId, matchResultSn);
        updateCurrentBatch(jdbcTemplate, tenantId, identity,
                (String) identity.get("RECONCILIATION_BATCH_SN"));
    }

    private static Map<String, Object> matchBatchIdentity(JdbcTemplate jdbcTemplate,
                                                           Long tenantId,
                                                           String matchResultSn) {
        return jdbcTemplate.queryForMap("""
                SELECT m.reconciliation_batch_sn,
                       b.scope_owner_namespace, b.scope_identity_value,
                       b.pair_owner_namespace, b.pair_identity_value
                FROM t_reconciliation_match_result m
                JOIN t_reconciliation_batch b
                  ON b.tenant_id = m.tenant_id
                 AND b.sn = m.reconciliation_batch_sn
                WHERE m.tenant_id = ?
                  AND m.sn = ?
                """, tenantId, matchResultSn);
    }

    private static void updateCurrentBatch(JdbcTemplate jdbcTemplate,
                                           Long tenantId,
                                           Map<String, Object> identity,
                                           String currentBatchSn) {
        jdbcTemplate.update("""
                UPDATE t_reconciliation_batch_lineage SET current_batch_sn = ?
                WHERE tenant_id = ?
                  AND scope_owner_namespace = ? AND scope_identity_value = ?
                  AND pair_owner_namespace = ? AND pair_identity_value = ?
                """, currentBatchSn, tenantId,
                identity.get("SCOPE_OWNER_NAMESPACE"), identity.get("SCOPE_IDENTITY_VALUE"),
                identity.get("PAIR_OWNER_NAMESPACE"), identity.get("PAIR_IDENTITY_VALUE"));
    }

    private static void prepareSourceSnapshot(JdbcTemplate jdbcTemplate,
                                              Long tenantId,
                                              String batchSn,
                                              ReconciliationSourceRole sourceRole,
                                              String sourceNamespace,
                                              String sourceItemRef,
                                              String comparisonIdentity,
                                              String evidenceRef,
                                              long amount,
                                              String comparisonStatusCode) {
        String snapshotSn = batchSn + ":" + sourceRole.name();
        Map<String, Object> semanticFact = new java.util.TreeMap<>();
        semanticFact.put("sourceFactRef", "test.fact:" + sourceItemRef);
        semanticFact.put("comparisonIdentity", "test.compare:" + comparisonIdentity);
        semanticFact.put("amount", amount);
        semanticFact.put("currency", CurrencyIsoCode.USD);
        semanticFact.put("rule", "test.rule:strict-exact:v1");
        semanticFact.put("comparisonStatusCode", comparisonStatusCode);
        semanticFact.put("comparisonProven", true);
        semanticFact.put("claimKind", "FUNDS");
        semanticFact.put("economicComponent", "PRINCIPAL");
        semanticFact.put("direction", "CREDIT");
        semanticFact.put("normalizationVersion", "v1");
        String itemSemanticDigest = FundsStableHashSupport.sha256Json(semanticFact);
        String snapshotSemanticDigest = FundsStableHashSupport.sha256Json(List.of(semanticFact));
        Map<String, Object> snapshotFact = new java.util.TreeMap<>();
        snapshotFact.put("tenantId", tenantId);
        snapshotFact.put("batchSn", batchSn);
        snapshotFact.put("sourceRole", sourceRole);
        snapshotFact.put("sourceNamespace", sourceNamespace);
        snapshotFact.put("snapshotIdentity", "test.snapshot:" + snapshotSn);
        snapshotFact.put("snapshotVersion", "v1");
        snapshotFact.put("coverageComplete", true);
        snapshotFact.put("coverageWatermark", "test-watermark");
        snapshotFact.put("coverageMemberCount", 1);
        snapshotFact.put("semanticDigest", snapshotSemanticDigest);
        String sourceDigest = FundsStableHashSupport.sha256Json(snapshotFact);
        String evidenceRefs = WindJson.toJsonString(List.of(evidenceRef));
        String evidenceBundleDigest = FundsStableHashSupport.sha256Json(List.of(evidenceRef));
        String itemSn = FundsStableHashSupport.sha256Json(
                Map.of("snapshotSn", snapshotSn, "sourceItemRef", sourceItemRef));
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_snapshot
                    (sn, tenant_id, reconciliation_batch_sn, source_role, source_namespace,
                     snapshot_owner_namespace, snapshot_identity_value, snapshot_version,
                     coverage_complete, coverage_watermark, coverage_member_count,
                     source_digest, semantic_digest, evidence_bundle_digest, evidence_refs, created_by)
                VALUES (?, ?, ?, ?, ?, 'test.snapshot', ?, 'v1', TRUE, 'test-watermark', 1,
                        ?, ?, ?, ?, 'SYSTEM')
                """, snapshotSn, tenantId, batchSn, sourceRole.name(), sourceNamespace,
                snapshotSn, sourceDigest, snapshotSemanticDigest, evidenceBundleDigest, evidenceRefs);
        jdbcTemplate.update("""
                INSERT INTO t_reconciliation_source_item
                    (sn, tenant_id, source_snapshot_sn,
                     source_fact_owner_namespace, source_fact_identity_value,
                     comparison_owner_namespace, comparison_identity_value,
                     amount, currency, rule_namespace, rule_identity, rule_version,
                     comparison_status_code, comparison_proven, claim_kind,
                     economic_component, direction, normalization_version,
                     semantic_digest, evidence_bundle_digest, evidence_refs, created_by)
                VALUES (?, ?, ?, 'test.fact', ?, 'test.compare', ?, ?, 'USD',
                        'test.rule', 'strict-exact', 'v1', ?, TRUE,
                        'FUNDS', 'PRINCIPAL', 'CREDIT', 'v1', ?, ?, ?, 'SYSTEM')
                """, itemSn, tenantId, snapshotSn, sourceItemRef, comparisonIdentity,
                amount, comparisonStatusCode, itemSemanticDigest, evidenceBundleDigest, evidenceRefs);
    }
}
