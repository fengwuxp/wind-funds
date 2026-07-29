package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 清算批次 Mapper。
 */
@Mapper
public interface ClearingBatchMapper extends BaseMapper<ClearingBatch> {

    @Select("""
            SELECT * FROM t_clearing_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingBatch selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_clearing_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    ClearingBatch selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Update("""
            UPDATE t_clearing_batch
            SET active_amount_digest = NULL
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    int releaseActiveAmountDigest(@Param("tenantId") Long tenantId, @Param("sn") String sn);
}
