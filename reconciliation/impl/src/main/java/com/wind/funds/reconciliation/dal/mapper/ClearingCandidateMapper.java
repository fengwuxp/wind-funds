package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清算候选 Mapper。
 */
@Mapper
public interface ClearingCandidateMapper extends BaseMapper<ClearingCandidate> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, split_result_sn, split_batch_sn,
                   splittable_detail_sn, subject_type, subject_id, currency, business_line, clearing_period, amount,
                   funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn, posting_plan_sn,
                   ledger_entry_sn, route_snapshot_digest, clearing_available_time, clearing_rule_code,
                   clearing_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   candidate_digest, active_splittable_detail_sn, state, block_reason, exclusion_reason,
                   locked_clearing_batch_sn, created_by, updated_by, state_changed_time FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingCandidate selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, split_result_sn, split_batch_sn,
                   splittable_detail_sn, subject_type, subject_id, currency, business_line, clearing_period, amount,
                   funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn, posting_plan_sn,
                   ledger_entry_sn, route_snapshot_digest, clearing_available_time, clearing_rule_code,
                   clearing_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   candidate_digest, active_splittable_detail_sn, state, block_reason, exclusion_reason,
                   locked_clearing_batch_sn, created_by, updated_by, state_changed_time FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    ClearingCandidate selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, split_result_sn, split_batch_sn,
                   splittable_detail_sn, subject_type, subject_id, currency, business_line, clearing_period, amount,
                   funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn, posting_plan_sn,
                   ledger_entry_sn, route_snapshot_digest, clearing_available_time, clearing_rule_code,
                   clearing_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   candidate_digest, active_splittable_detail_sn, state, block_reason, exclusion_reason,
                   locked_clearing_batch_sn, created_by, updated_by, state_changed_time FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId} AND candidate_digest = #{candidateDigest}
            """)
    ClearingCandidate selectByDigest(@Param("tenantId") Long tenantId,
                                     @Param("candidateDigest") String candidateDigest);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, split_result_sn, split_batch_sn,
                   splittable_detail_sn, subject_type, subject_id, currency, business_line, clearing_period, amount,
                   funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn, posting_plan_sn,
                   ledger_entry_sn, route_snapshot_digest, clearing_available_time, clearing_rule_code,
                   clearing_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   candidate_digest, active_splittable_detail_sn, state, block_reason, exclusion_reason,
                   locked_clearing_batch_sn, created_by, updated_by, state_changed_time FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId}
              AND active_splittable_detail_sn = #{splittableDetailSn}
            FOR UPDATE
            """)
    ClearingCandidate selectByActiveDetailForUpdate(@Param("tenantId") Long tenantId,
                                                    @Param("splittableDetailSn") String splittableDetailSn);

    @Select("""
            <script>
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, split_result_sn, split_batch_sn,
                   splittable_detail_sn, subject_type, subject_id, currency, business_line, clearing_period, amount,
                   funds_transaction_sn, funds_transaction_detail_sn, ledger_transaction_sn, posting_plan_sn,
                   ledger_entry_sn, route_snapshot_digest, clearing_available_time, clearing_rule_code,
                   clearing_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   candidate_digest, active_splittable_detail_sn, state, block_reason, exclusion_reason,
                   locked_clearing_batch_sn, created_by, updated_by, state_changed_time FROM t_clearing_candidate
            WHERE tenant_id = #{tenantId}
              AND sn IN
              <foreach collection="sns" item="sn" open="(" separator="," close=")">
                #{sn}
              </foreach>
            ORDER BY sn
            </script>
            """)
    List<ClearingCandidate> selectBySns(@Param("tenantId") Long tenantId,
                                        @Param("sns") List<String> sns);

    @Update("""
            UPDATE t_clearing_candidate
            SET state = 'LOCKED', locked_clearing_batch_sn = #{clearingBatchSn},
                updated_by = #{updatedBy}, state_changed_time = #{stateChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn} AND state = 'READY'
            """)
    int lockReadyCandidate(@Param("tenantId") Long tenantId,
                           @Param("candidateSn") String candidateSn,
                           @Param("clearingBatchSn") String clearingBatchSn,
                           @Param("updatedBy") String updatedBy,
                           @Param("stateChangedTime") LocalDateTime stateChangedTime);

    @Update("""
            UPDATE t_clearing_candidate
            SET state = 'READY', locked_clearing_batch_sn = NULL, block_reason = NULL,
                updated_by = #{updatedBy}, state_changed_time = #{stateChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn}
              AND state = 'LOCKED' AND locked_clearing_batch_sn = #{clearingBatchSn}
            """)
    int releaseLockedCandidate(@Param("tenantId") Long tenantId,
                               @Param("candidateSn") String candidateSn,
                               @Param("clearingBatchSn") String clearingBatchSn,
                               @Param("updatedBy") String updatedBy,
                               @Param("stateChangedTime") LocalDateTime stateChangedTime);

    @Update("""
            UPDATE t_clearing_candidate
            SET state = 'CLEARED', locked_clearing_batch_sn = NULL,
                updated_by = #{updatedBy}, state_changed_time = #{stateChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn}
              AND state = 'LOCKED' AND locked_clearing_batch_sn = #{clearingBatchSn}
            """)
    int markLockedCandidateCleared(@Param("tenantId") Long tenantId,
                                   @Param("candidateSn") String candidateSn,
                                   @Param("clearingBatchSn") String clearingBatchSn,
                                   @Param("updatedBy") String updatedBy,
                                   @Param("stateChangedTime") LocalDateTime stateChangedTime);

    @Update("""
            UPDATE t_clearing_candidate
            SET state = 'BLOCKED', locked_clearing_batch_sn = NULL, block_reason = #{blockReason},
                updated_by = #{updatedBy}, state_changed_time = #{stateChangedTime}
            WHERE tenant_id = #{tenantId} AND sn = #{candidateSn}
              AND state = 'LOCKED' AND locked_clearing_batch_sn = #{clearingBatchSn}
            """)
    int blockLockedCandidate(@Param("tenantId") Long tenantId,
                             @Param("candidateSn") String candidateSn,
                             @Param("clearingBatchSn") String clearingBatchSn,
                             @Param("blockReason") String blockReason,
                             @Param("updatedBy") String updatedBy,
                             @Param("stateChangedTime") LocalDateTime stateChangedTime);
}
