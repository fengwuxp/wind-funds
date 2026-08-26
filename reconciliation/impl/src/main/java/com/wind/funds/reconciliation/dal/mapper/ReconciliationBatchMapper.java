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
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, scope_owner_namespace, scope_identity_value,
                   pair_owner_namespace, pair_identity_value, currency, rule_namespace, rule_identity, rule_version,
                   window_start, window_end, time_semantics, timezone_id, previous_batch_sn, state, run_result_sn,
                   aborted_by, aborted_time, abort_reason, replacement_reason, replacement_evidence_ref,
                   batch_digest, created_by
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            """)
    ReconciliationBatch selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, scope_owner_namespace, scope_identity_value,
                   pair_owner_namespace, pair_identity_value, currency, rule_namespace, rule_identity, rule_version,
                   window_start, window_end, time_semantics, timezone_id, previous_batch_sn, state, run_result_sn,
                   aborted_by, aborted_time, abort_reason, replacement_reason, replacement_evidence_ref,
                   batch_digest, created_by
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            FOR UPDATE
            """)
    ReconciliationBatch selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, scope_owner_namespace, scope_identity_value,
                   pair_owner_namespace, pair_identity_value, currency, rule_namespace, rule_identity, rule_version,
                   window_start, window_end, time_semantics, timezone_id, previous_batch_sn, state, run_result_sn,
                   aborted_by, aborted_time, abort_reason, replacement_reason, replacement_evidence_ref,
                   batch_digest, created_by
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND batch_digest = #{batchDigest}
            """)
    ReconciliationBatch selectByDigest(@Param("tenantId") Long tenantId,
                                       @Param("batchDigest") String batchDigest);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, scope_owner_namespace, scope_identity_value,
                   pair_owner_namespace, pair_identity_value, currency, rule_namespace, rule_identity, rule_version,
                   window_start, window_end, time_semantics, timezone_id, previous_batch_sn, state, run_result_sn,
                   aborted_by, aborted_time, abort_reason, replacement_reason, replacement_evidence_ref,
                   batch_digest, created_by
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND batch_digest = #{batchDigest}
            FOR UPDATE
            """)
    ReconciliationBatch selectByDigestForUpdate(@Param("tenantId") Long tenantId,
                                                @Param("batchDigest") String batchDigest);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, scope_owner_namespace, scope_identity_value,
                   pair_owner_namespace, pair_identity_value, currency, rule_namespace, rule_identity, rule_version,
                   window_start, window_end, time_semantics, timezone_id, previous_batch_sn, state, run_result_sn,
                   aborted_by, aborted_time, abort_reason, replacement_reason, replacement_evidence_ref,
                   batch_digest, created_by
            FROM t_reconciliation_batch
            WHERE tenant_id = #{tenantId}
              AND previous_batch_sn = #{previousBatchSn}
            FOR UPDATE
            """)
    ReconciliationBatch selectByPreviousBatchSnForUpdate(@Param("tenantId") Long tenantId,
                                                         @Param("previousBatchSn") String previousBatchSn);

    @Update("""
            UPDATE t_reconciliation_batch
            SET state = #{targetState}
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
              AND state = #{currentState}
            """)
    int updateState(@Param("tenantId") Long tenantId,
                     @Param("sn") String sn,
                     @Param("currentState") String currentState,
                     @Param("targetState") String targetState);

    @Update("""
            UPDATE t_reconciliation_batch
            SET state = 'COMPLETED', run_result_sn = #{runResultSn}
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
              AND state = 'DATA_READY'
              AND run_result_sn IS NULL
            """)
    int complete(@Param("tenantId") Long tenantId,
                 @Param("sn") String sn,
                 @Param("runResultSn") String runResultSn);

    @Update("""
            UPDATE t_reconciliation_batch
            SET state = 'ABORTED', aborted_by = #{abortedBy},
                aborted_time = #{abortedTime}, abort_reason = #{abortReason}
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
              AND state = #{currentState}
            """)
    int abort(@Param("tenantId") Long tenantId,
              @Param("sn") String sn,
              @Param("currentState") String currentState,
              @Param("abortedBy") String abortedBy,
              @Param("abortedTime") java.time.LocalDateTime abortedTime,
              @Param("abortReason") String abortReason);
}
