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
            SELECT *
            FROM t_reconciliation_source_snapshot
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
              AND source_role = #{sourceRole}
            """)
    ReconciliationSourceSnapshot selectByBatchAndRole(@Param("tenantId") Long tenantId,
                                                      @Param("reconciliationBatchSn") String reconciliationBatchSn,
                                                      @Param("sourceRole") String sourceRole);

    @Select("""
            SELECT *
            FROM t_reconciliation_source_snapshot
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
            ORDER BY source_role
            """)
    List<ReconciliationSourceSnapshot> selectByBatch(@Param("tenantId") Long tenantId,
                                                     @Param("reconciliationBatchSn") String reconciliationBatchSn);
}
