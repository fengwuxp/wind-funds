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
              AND gate_object_type = #{gateObjectType}
              AND gate_object_sn = #{gateObjectSn}
            """)
    ReconciliationBatchLineage selectByGateObject(@Param("tenantId") Long tenantId,
                                                   @Param("gateObjectType") String gateObjectType,
                                                   @Param("gateObjectSn") String gateObjectSn);

    @Select("""
            SELECT *
            FROM t_reconciliation_batch_lineage
            WHERE tenant_id = #{tenantId}
              AND gate_object_type = #{gateObjectType}
              AND gate_object_sn = #{gateObjectSn}
            FOR UPDATE
            """)
    ReconciliationBatchLineage selectForUpdate(@Param("tenantId") Long tenantId,
                                               @Param("gateObjectType") String gateObjectType,
                                               @Param("gateObjectSn") String gateObjectSn);

    @Update("""
            UPDATE t_reconciliation_batch_lineage
            SET current_batch_sn = #{currentBatchSn}
            WHERE tenant_id = #{tenantId}
              AND gate_object_type = #{gateObjectType}
              AND gate_object_sn = #{gateObjectSn}
              AND current_batch_sn = #{previousBatchSn}
            """)
    int advance(@Param("tenantId") Long tenantId,
                @Param("gateObjectType") String gateObjectType,
                @Param("gateObjectSn") String gateObjectSn,
                @Param("previousBatchSn") String previousBatchSn,
                @Param("currentBatchSn") String currentBatchSn);
}
