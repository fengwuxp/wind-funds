package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationStageGateEvidence;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 阶段动作消费的门禁证据映射器。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Mapper
public interface ReconciliationStageGateEvidenceMapper extends BaseMapper<ReconciliationStageGateEvidence> {

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, stage_kind, stage_identity_owner_namespace, stage_identity_value,
                   requirement_identity_owner_namespace, requirement_identity_value, requirement_version,
                   requirement_semantic_digest, requirement_evidence_bundle_digest, consumed_pair_evidence,
                   decision_digest, evidence_refs, created_by FROM t_reconciliation_stage_gate_evidence
            WHERE tenant_id = #{tenantId}
              AND stage_kind = #{stageKind}
              AND stage_identity_owner_namespace = #{stageIdentityOwnerNamespace}
              AND stage_identity_value = #{stageIdentityValue}
            """)
    ReconciliationStageGateEvidence selectByStage(
            @Param("tenantId") Long tenantId,
            @Param("stageKind") String stageKind,
            @Param("stageIdentityOwnerNamespace") String stageIdentityOwnerNamespace,
            @Param("stageIdentityValue") String stageIdentityValue);

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, stage_kind, stage_identity_owner_namespace, stage_identity_value,
                   requirement_identity_owner_namespace, requirement_identity_value, requirement_version,
                   requirement_semantic_digest, requirement_evidence_bundle_digest, consumed_pair_evidence,
                   decision_digest, evidence_refs, created_by FROM t_reconciliation_stage_gate_evidence
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ReconciliationStageGateEvidence selectBySn(@Param("tenantId") Long tenantId,
                                               @Param("sn") String sn);

    @Delete("""
            DELETE FROM t_reconciliation_stage_gate_evidence
            WHERE tenant_id = #{tenantId}
              AND stage_kind = #{stageKind}
              AND stage_identity_owner_namespace = #{stageIdentityOwnerNamespace}
              AND stage_identity_value = #{stageIdentityValue}
            """)
    int deleteByStage(@Param("tenantId") Long tenantId,
                      @Param("stageKind") String stageKind,
                      @Param("stageIdentityOwnerNamespace") String stageIdentityOwnerNamespace,
                      @Param("stageIdentityValue") String stageIdentityValue);
}
