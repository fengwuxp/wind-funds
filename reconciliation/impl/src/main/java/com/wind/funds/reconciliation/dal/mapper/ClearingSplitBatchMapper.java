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
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, subject_type, subject_id, currency, business_line,
                   split_period, split_rule_code, split_rule_version, detail_count, total_amount, member_digest,
                   batch_digest, active_batch_digest, state, created_by, submitted_by, submitted_time,
                   confirmed_by, confirmed_time, cancelled_by, cancelled_time, cancel_reason FROM t_clearing_split_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingSplitBatch selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, subject_type, subject_id, currency, business_line,
                   split_period, split_rule_code, split_rule_version, detail_count, total_amount, member_digest,
                   batch_digest, active_batch_digest, state, created_by, submitted_by, submitted_time,
                   confirmed_by, confirmed_time, cancelled_by, cancelled_time, cancel_reason FROM t_clearing_split_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    ClearingSplitBatch selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, subject_type, subject_id, currency, business_line,
                   split_period, split_rule_code, split_rule_version, detail_count, total_amount, member_digest,
                   batch_digest, active_batch_digest, state, created_by, submitted_by, submitted_time,
                   confirmed_by, confirmed_time, cancelled_by, cancelled_time, cancel_reason FROM t_clearing_split_batch
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
