package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 清算批次 Mapper。
 */
@Mapper
public interface ClearingBatchMapper extends BaseMapper<ClearingBatch> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, subject_type, subject_id, currency, business_line,
                   clearing_period, clearing_rule_code, clearing_rule_version, candidate_count, total_amount,
                   amount_digest, active_amount_digest, funds_transaction_sn, state, created_by, submitted_by,
                   submitted_time, confirmed_by, confirmed_time, returned_by, returned_time, return_reason,
                   cancelled_by, cancelled_time, cancel_reason, failed_by, failed_time, failure_reason
            FROM t_clearing_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingBatch selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, subject_type, subject_id, currency, business_line,
                   clearing_period, clearing_rule_code, clearing_rule_version, candidate_count, total_amount,
                   amount_digest, active_amount_digest, funds_transaction_sn, state, created_by, submitted_by,
                   submitted_time, confirmed_by, confirmed_time, returned_by, returned_time, return_reason,
                   cancelled_by, cancelled_time, cancel_reason, failed_by, failed_time, failure_reason
            FROM t_clearing_batch
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    ClearingBatch selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            <script>
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, subject_type, subject_id, currency, business_line,
                   clearing_period, clearing_rule_code, clearing_rule_version, candidate_count, total_amount,
                   amount_digest, active_amount_digest, funds_transaction_sn, state, created_by, submitted_by,
                   submitted_time, confirmed_by, confirmed_time, returned_by, returned_time, return_reason,
                   cancelled_by, cancelled_time, cancel_reason, failed_by, failed_time, failure_reason
            FROM t_clearing_batch
            WHERE tenant_id = #{tenantId}
              AND sn IN
              <foreach collection="sns" item="sn" open="(" separator="," close=")">
                #{sn}
              </foreach>
            ORDER BY sn
            FOR UPDATE
            </script>
            """)
    List<ClearingBatch> selectBySnsForUpdate(@Param("tenantId") Long tenantId,
                                             @Param("sns") List<String> sns);

    @Update("""
            UPDATE t_clearing_batch
            SET active_amount_digest = NULL
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    int releaseActiveAmountDigest(@Param("tenantId") Long tenantId, @Param("sn") String sn);
}
