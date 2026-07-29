package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 清分批次 Mapper。
 */
@Mapper
public interface ClearingSplitBatchMapper extends BaseMapper<ClearingSplitBatch> {

    @Select("""
            SELECT * FROM t_clearing_split_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingSplitBatch selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_clearing_split_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    ClearingSplitBatch selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_clearing_split_batch
            WHERE tenant_id = #{tenantId} AND active_batch_digest = #{batchDigest}
            """)
    ClearingSplitBatch selectByDigest(@Param("tenantId") Long tenantId,
                                      @Param("batchDigest") String batchDigest);

    @Update("""
            UPDATE t_clearing_split_batch
            SET active_batch_digest = NULL
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    int releaseActiveDigest(@Param("tenantId") Long tenantId, @Param("sn") String sn);
}
