package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationGateRequirementPair;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 门禁必需对账对映射器。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Mapper
public interface ReconciliationGateRequirementPairMapper extends BaseMapper<ReconciliationGateRequirementPair> {

    @Select("""
            SELECT * FROM t_reconciliation_gate_requirement_pair
            WHERE tenant_id = #{tenantId}
              AND requirement_identity_owner_namespace = #{requirementIdentityOwnerNamespace}
              AND requirement_identity_value = #{requirementIdentityValue}
            ORDER BY scope_owner_namespace, scope_identity_value,
                     pair_owner_namespace, pair_identity_value
            """)
    List<ReconciliationGateRequirementPair> selectByRequirement(
            @Param("tenantId") Long tenantId,
            @Param("requirementIdentityOwnerNamespace") String requirementIdentityOwnerNamespace,
            @Param("requirementIdentityValue") String requirementIdentityValue);
}
