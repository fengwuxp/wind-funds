package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 对账批次 Mapper。
 */
@Mapper
public interface ReconciliationBatchMapper extends BaseMapper<ReconciliationBatch> {

    @Select("""
            SELECT *
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            """)
    ReconciliationBatch selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT *
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            FOR UPDATE
            """)
    ReconciliationBatch selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT *
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND batch_digest = #{batchDigest}
            """)
    ReconciliationBatch selectByDigest(@Param("tenantId") Long tenantId,
                                       @Param("batchDigest") String batchDigest);

    @Select("""
            SELECT *
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND batch_digest = #{batchDigest}
            FOR UPDATE
            """)
    ReconciliationBatch selectByDigestForUpdate(@Param("tenantId") Long tenantId,
                                                @Param("batchDigest") String batchDigest);

    @Select("""
            SELECT *
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND previous_batch_sn = #{previousBatchSn}
            FOR UPDATE
            """)
    ReconciliationBatch selectByPreviousBatchSnForUpdate(@Param("tenantId") Long tenantId,
                                                         @Param("previousBatchSn") String previousBatchSn);

    @Update("""
            UPDATE t_reconciliation_batch
            SET status = #{targetStatus}
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
              AND status = #{currentStatus}
            """)
    int updateStatus(@Param("tenantId") Long tenantId,
                     @Param("sn") String sn,
                     @Param("currentStatus") String currentStatus,
                     @Param("targetStatus") String targetStatus);

    @Update("""
            UPDATE t_reconciliation_batch
            SET status = 'COMPLETED', run_result_sn = #{runResultSn}
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
              AND status = 'DATA_READY'
              AND run_result_sn IS NULL
            """)
    int complete(@Param("tenantId") Long tenantId,
                 @Param("sn") String sn,
                 @Param("runResultSn") String runResultSn);
}
