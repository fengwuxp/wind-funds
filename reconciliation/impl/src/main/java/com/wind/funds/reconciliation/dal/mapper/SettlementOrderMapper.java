package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.SettlementOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 结算单 Mapper。
 */
@Mapper
public interface SettlementOrderMapper extends BaseMapper<SettlementOrder> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_subject_type, settlement_subject_id,
                   currency, settlement_period, settlement_mode, settlement_destination, trigger_mode, timezone,
                   cutoff, total_amount, add_amount, deduct_amount, reserve_amount, net_amount, state,
                   settlement_approval_ref, lock_funds_transaction_sn, release_funds_transaction_sn,
                   release_freeze_order_sn, release_disposition, release_digest, release_gate_evidence_ref,
                   release_current_lineage_batch_sn, release_source_closure_digest,
                   release_authority_decision_digest, release_authority_evidence_refs, release_approval_ref,
                   release_reason, released_by, released_time, rule_code, rule_version, policy_approval_ref,
                   amount_digest, source_digest, policy_snapshot_digest, order_digest, created_by, submitted_by,
                   submitted_time, approved_by, approved_time, locked_by, locked_time, returned_by, returned_time,
                   return_reason, cancelled_by, cancelled_time, cancel_reason, lock_gate_evidence_ref,
                   active_order_digest, failed_by, failed_time, failure_reason, version FROM t_settlement_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    SettlementOrder selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_subject_type, settlement_subject_id,
                   currency, settlement_period, settlement_mode, settlement_destination, trigger_mode, timezone,
                   cutoff, total_amount, add_amount, deduct_amount, reserve_amount, net_amount, state,
                   settlement_approval_ref, lock_funds_transaction_sn, release_funds_transaction_sn,
                   release_freeze_order_sn, release_disposition, release_digest, release_gate_evidence_ref,
                   release_current_lineage_batch_sn, release_source_closure_digest,
                   release_authority_decision_digest, release_authority_evidence_refs, release_approval_ref,
                   release_reason, released_by, released_time, rule_code, rule_version, policy_approval_ref,
                   amount_digest, source_digest, policy_snapshot_digest, order_digest, created_by, submitted_by,
                   submitted_time, approved_by, approved_time, locked_by, locked_time, returned_by, returned_time,
                   return_reason, cancelled_by, cancelled_time, cancel_reason, lock_gate_evidence_ref,
                   active_order_digest, failed_by, failed_time, failure_reason, version FROM t_settlement_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    SettlementOrder selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_subject_type, settlement_subject_id,
                   currency, settlement_period, settlement_mode, settlement_destination, trigger_mode, timezone,
                   cutoff, total_amount, add_amount, deduct_amount, reserve_amount, net_amount, state,
                   settlement_approval_ref, lock_funds_transaction_sn, release_funds_transaction_sn,
                   release_freeze_order_sn, release_disposition, release_digest, release_gate_evidence_ref,
                   release_current_lineage_batch_sn, release_source_closure_digest,
                   release_authority_decision_digest, release_authority_evidence_refs, release_approval_ref,
                   release_reason, released_by, released_time, rule_code, rule_version, policy_approval_ref,
                   amount_digest, source_digest, policy_snapshot_digest, order_digest, created_by, submitted_by,
                   submitted_time, approved_by, approved_time, locked_by, locked_time, returned_by, returned_time,
                   return_reason, cancelled_by, cancelled_time, cancel_reason, lock_gate_evidence_ref,
                   active_order_digest, failed_by, failed_time, failure_reason, version FROM t_settlement_order
            WHERE tenant_id = #{tenantId} AND active_order_digest = #{orderDigest}
            """)
    SettlementOrder selectByDigest(@Param("tenantId") Long tenantId,
                                   @Param("orderDigest") String orderDigest);

    @Update("""
            UPDATE t_settlement_order
            SET active_order_digest = NULL
            WHERE tenant_id = #{tenantId} AND sn = #{sn} AND active_order_digest IS NOT NULL
            """)
    int releaseActiveOrderDigest(@Param("tenantId") Long tenantId, @Param("sn") String sn);
}
