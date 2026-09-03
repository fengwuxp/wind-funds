package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.PayoutOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayoutOrderMapper extends BaseMapper<PayoutOrder> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_order_sn, settlement_subject_type,
                   settlement_subject_id, amount, currency, state, payout_account_ref, payee_endpoint_ref,
                   channel_ref, approval_ref, external_rule_evidence_digest, payout_gate_evidence_ref,
                   admission_decision_digest, admission_evidence_refs, submit_digest, external_reference,
                   completion_funds_transaction_sn, rollback_funds_transaction_sn, last_receipt_digest,
                   failure_code, failure_reason, created_by, submitted_by, submitted_time, completed_time,
                   cancelled_by, cancelled_time, cancel_reason, version FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    PayoutOrder selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_order_sn, settlement_subject_type,
                   settlement_subject_id, amount, currency, state, payout_account_ref, payee_endpoint_ref,
                   channel_ref, approval_ref, external_rule_evidence_digest, payout_gate_evidence_ref,
                   admission_decision_digest, admission_evidence_refs, submit_digest, external_reference,
                   completion_funds_transaction_sn, rollback_funds_transaction_sn, last_receipt_digest,
                   failure_code, failure_reason, created_by, submitted_by, submitted_time, completed_time,
                   cancelled_by, cancelled_time, cancel_reason, version FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    PayoutOrder selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_order_sn, settlement_subject_type,
                   settlement_subject_id, amount, currency, state, payout_account_ref, payee_endpoint_ref,
                   channel_ref, approval_ref, external_rule_evidence_digest, payout_gate_evidence_ref,
                   admission_decision_digest, admission_evidence_refs, submit_digest, external_reference,
                   completion_funds_transaction_sn, rollback_funds_transaction_sn, last_receipt_digest,
                   failure_code, failure_reason, created_by, submitted_by, submitted_time, completed_time,
                   cancelled_by, cancelled_time, cancel_reason, version FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND settlement_order_sn = #{settlementOrderSn}
            """)
    PayoutOrder selectBySettlementOrderSn(@Param("tenantId") Long tenantId,
                                          @Param("settlementOrderSn") String settlementOrderSn);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_order_sn, settlement_subject_type,
                   settlement_subject_id, amount, currency, state, payout_account_ref, payee_endpoint_ref,
                   channel_ref, approval_ref, external_rule_evidence_digest, payout_gate_evidence_ref,
                   admission_decision_digest, admission_evidence_refs, submit_digest, external_reference,
                   completion_funds_transaction_sn, rollback_funds_transaction_sn, last_receipt_digest,
                   failure_code, failure_reason, created_by, submitted_by, submitted_time, completed_time,
                   cancelled_by, cancelled_time, cancel_reason, version FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND settlement_order_sn = #{settlementOrderSn}
            FOR UPDATE
            """)
    PayoutOrder selectBySettlementOrderSnForUpdate(@Param("tenantId") Long tenantId,
                                                   @Param("settlementOrderSn") String settlementOrderSn);

    @Update("""
            UPDATE t_payout_order
            SET external_reference = #{externalReference}
            WHERE tenant_id = #{tenantId} AND id = #{id} AND external_reference IS NULL
            """)
    int claimExternalReference(@Param("tenantId") Long tenantId,
                               @Param("id") Long id,
                               @Param("externalReference") String externalReference);
}
