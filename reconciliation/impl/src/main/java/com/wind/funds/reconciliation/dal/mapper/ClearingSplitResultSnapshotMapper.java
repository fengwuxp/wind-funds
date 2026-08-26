package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingSplitResultSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 清分结果快照 Mapper。
 */
@Mapper
public interface ClearingSplitResultSnapshotMapper extends BaseMapper<ClearingSplitResultSnapshot> {

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, split_batch_sn, splittable_detail_sn, subject_type, subject_id,
                   currency, business_line, split_period, amount, funds_transaction_sn, funds_transaction_detail_sn,
                   ledger_transaction_sn, posting_plan_sn, ledger_entry_sn, route_snapshot_digest, split_rule_code,
                   split_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   snapshot_digest, created_by FROM t_clearing_split_result_snapshot
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    ClearingSplitResultSnapshot selectBySn(@Param("tenantId") Long tenantId,
                                           @Param("sn") String sn);

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, split_batch_sn, splittable_detail_sn, subject_type, subject_id,
                   currency, business_line, split_period, amount, funds_transaction_sn, funds_transaction_detail_sn,
                   ledger_transaction_sn, posting_plan_sn, ledger_entry_sn, route_snapshot_digest, split_rule_code,
                   split_rule_version, gate_evidence_ref, reconciliation_evidence_refs, source_digest,
                   snapshot_digest, created_by FROM t_clearing_split_result_snapshot
            WHERE tenant_id = #{tenantId} AND split_batch_sn = #{splitBatchSn}
            ORDER BY splittable_detail_sn
            """)
    List<ClearingSplitResultSnapshot> selectByBatchSn(@Param("tenantId") Long tenantId,
                                                      @Param("splitBatchSn") String splitBatchSn);
}
