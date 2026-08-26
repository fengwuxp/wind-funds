package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationGateRequirement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 门禁要求头部映射器。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Mapper
public interface ReconciliationGateRequirementMapper extends BaseMapper<ReconciliationGateRequirement> {

    @Select("""
            SELECT id, gmt_create, tenant_id, stage_kind, stage_identity_owner_namespace, stage_identity_value,
                   requirement_identity_owner_namespace, requirement_identity_value, requirement_version,
                   semantic_digest, evidence_refs, evidence_bundle_digest,
                   previous_requirement_identity_owner_namespace, previous_requirement_identity_value,
                   previous_requirement_version, previous_semantic_digest, previous_evidence_bundle_digest,
                   created_by FROM t_reconciliation_gate_requirement
            WHERE tenant_id = #{tenantId}
              AND stage_kind = #{stageKind}
              AND stage_identity_owner_namespace = #{stageIdentityOwnerNamespace}
              AND stage_identity_value = #{stageIdentityValue}
              AND requirement_version = #{requirementVersion}
            """)
    ReconciliationGateRequirement selectByStageAndVersion(
            @Param("tenantId") Long tenantId,
            @Param("stageKind") String stageKind,
            @Param("stageIdentityOwnerNamespace") String stageIdentityOwnerNamespace,
            @Param("stageIdentityValue") String stageIdentityValue,
            @Param("requirementVersion") String requirementVersion);
}
