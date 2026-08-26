package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationSourceSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对账来源快照 Mapper。
 */
@Mapper
public interface ReconciliationSourceSnapshotMapper extends BaseMapper<ReconciliationSourceSnapshot> {

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, reconciliation_batch_sn, source_role, source_namespace,
                   snapshot_owner_namespace, snapshot_identity_value, snapshot_version, coverage_complete,
                   coverage_watermark, coverage_member_count, source_digest, semantic_digest,
                   evidence_bundle_digest, evidence_refs, created_by
            FROM t_reconciliation_source_snapshot
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
              AND source_role = #{sourceRole}
            """)
    ReconciliationSourceSnapshot selectByBatchAndRole(@Param("tenantId") Long tenantId,
                                                      @Param("reconciliationBatchSn") String reconciliationBatchSn,
                                                      @Param("sourceRole") String sourceRole);

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, reconciliation_batch_sn, source_role, source_namespace,
                   snapshot_owner_namespace, snapshot_identity_value, snapshot_version, coverage_complete,
                   coverage_watermark, coverage_member_count, source_digest, semantic_digest,
                   evidence_bundle_digest, evidence_refs, created_by
            FROM t_reconciliation_source_snapshot
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
            ORDER BY source_role
            """)
    List<ReconciliationSourceSnapshot> selectByBatch(@Param("tenantId") Long tenantId,
                                                     @Param("reconciliationBatchSn") String reconciliationBatchSn);
}
