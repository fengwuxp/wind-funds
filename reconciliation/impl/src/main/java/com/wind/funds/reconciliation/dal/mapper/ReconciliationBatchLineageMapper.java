package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatchLineage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Gate 对账批次血缘 Mapper。
 */
@Mapper
public interface ReconciliationBatchLineageMapper extends BaseMapper<ReconciliationBatchLineage> {

    @Select("""
            SELECT *
            FROM t_reconciliation_batch_lineage
            WHERE tenant_id = #{tenantId}
              AND scope_owner_namespace = #{scopeOwnerNamespace}
              AND scope_identity_value = #{scopeIdentityValue}
              AND pair_owner_namespace = #{pairOwnerNamespace}
              AND pair_identity_value = #{pairIdentityValue}
            """)
    ReconciliationBatchLineage selectByScopeAndPair(@Param("tenantId") Long tenantId,
                                                     @Param("scopeOwnerNamespace") String scopeOwnerNamespace,
                                                     @Param("scopeIdentityValue") String scopeIdentityValue,
                                                     @Param("pairOwnerNamespace") String pairOwnerNamespace,
                                                     @Param("pairIdentityValue") String pairIdentityValue);

    @Select("""
            SELECT *
            FROM t_reconciliation_batch_lineage
            WHERE tenant_id = #{tenantId}
              AND scope_owner_namespace = #{scopeOwnerNamespace}
              AND scope_identity_value = #{scopeIdentityValue}
              AND pair_owner_namespace = #{pairOwnerNamespace}
              AND pair_identity_value = #{pairIdentityValue}
            FOR UPDATE
            """)
    ReconciliationBatchLineage selectForUpdate(@Param("tenantId") Long tenantId,
                                               @Param("scopeOwnerNamespace") String scopeOwnerNamespace,
                                               @Param("scopeIdentityValue") String scopeIdentityValue,
                                               @Param("pairOwnerNamespace") String pairOwnerNamespace,
                                               @Param("pairIdentityValue") String pairIdentityValue);

    @Update("""
            UPDATE t_reconciliation_batch_lineage
            SET current_batch_sn = #{currentBatchSn}
            WHERE tenant_id = #{tenantId}
              AND scope_owner_namespace = #{scopeOwnerNamespace}
              AND scope_identity_value = #{scopeIdentityValue}
              AND pair_owner_namespace = #{pairOwnerNamespace}
              AND pair_identity_value = #{pairIdentityValue}
              AND current_batch_sn = #{previousBatchSn}
            """)
    int advance(@Param("tenantId") Long tenantId,
                @Param("scopeOwnerNamespace") String scopeOwnerNamespace,
                @Param("scopeIdentityValue") String scopeIdentityValue,
                @Param("pairOwnerNamespace") String pairOwnerNamespace,
                @Param("pairIdentityValue") String pairIdentityValue,
                @Param("previousBatchSn") String previousBatchSn,
                @Param("currentBatchSn") String currentBatchSn);
}
