package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationGateRequirementHead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 当前门禁要求头部映射器。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Mapper
public interface ReconciliationGateRequirementHeadMapper extends BaseMapper<ReconciliationGateRequirementHead> {

    @Select("""
            SELECT * FROM t_reconciliation_gate_requirement_head
            WHERE tenant_id = #{tenantId}
              AND stage_kind = #{stageKind}
              AND stage_identity_owner_namespace = #{stageIdentityOwnerNamespace}
              AND stage_identity_value = #{stageIdentityValue}
            """)
    ReconciliationGateRequirementHead selectByStage(
            @Param("tenantId") Long tenantId,
            @Param("stageKind") String stageKind,
            @Param("stageIdentityOwnerNamespace") String stageIdentityOwnerNamespace,
            @Param("stageIdentityValue") String stageIdentityValue);

    @Select("""
            SELECT * FROM t_reconciliation_gate_requirement_head
            WHERE tenant_id = #{tenantId}
              AND stage_kind = #{stageKind}
              AND stage_identity_owner_namespace = #{stageIdentityOwnerNamespace}
              AND stage_identity_value = #{stageIdentityValue}
            FOR UPDATE
            """)
    ReconciliationGateRequirementHead selectForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("stageKind") String stageKind,
            @Param("stageIdentityOwnerNamespace") String stageIdentityOwnerNamespace,
            @Param("stageIdentityValue") String stageIdentityValue);

    @Update("""
            UPDATE t_reconciliation_gate_requirement_head
            SET current_requirement_identity_owner_namespace = #{newOwnerNamespace},
                current_requirement_identity_value = #{newIdentityValue},
                current_requirement_version = #{newVersion},
                current_semantic_digest = #{newSemanticDigest},
                current_evidence_bundle_digest = #{newEvidenceBundleDigest},
                version = version + 1
            WHERE tenant_id = #{tenantId}
              AND stage_kind = #{stageKind}
              AND stage_identity_owner_namespace = #{stageIdentityOwnerNamespace}
              AND stage_identity_value = #{stageIdentityValue}
              AND current_requirement_identity_owner_namespace = #{expectedOwnerNamespace}
              AND current_requirement_identity_value = #{expectedIdentityValue}
              AND current_requirement_version = #{expectedVersion}
              AND current_semantic_digest = #{expectedSemanticDigest}
              AND current_evidence_bundle_digest = #{expectedEvidenceBundleDigest}
            """)
    int advance(
            @Param("tenantId") Long tenantId,
            @Param("stageKind") String stageKind,
            @Param("stageIdentityOwnerNamespace") String stageIdentityOwnerNamespace,
            @Param("stageIdentityValue") String stageIdentityValue,
            @Param("expectedOwnerNamespace") String expectedOwnerNamespace,
            @Param("expectedIdentityValue") String expectedIdentityValue,
            @Param("expectedVersion") String expectedVersion,
            @Param("expectedSemanticDigest") String expectedSemanticDigest,
            @Param("expectedEvidenceBundleDigest") String expectedEvidenceBundleDigest,
            @Param("newOwnerNamespace") String newOwnerNamespace,
            @Param("newIdentityValue") String newIdentityValue,
            @Param("newVersion") String newVersion,
            @Param("newSemanticDigest") String newSemanticDigest,
            @Param("newEvidenceBundleDigest") String newEvidenceBundleDigest);
}
