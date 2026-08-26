package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ClearingSplittableDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 可清分明细 Mapper。
 */
@Mapper
public interface ClearingSplittableDetailMapper extends BaseMapper<ClearingSplittableDetail> {

    /**
     * 按来源账本分录查询唯一准入结果。
     */
    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, funds_transaction_sn, funds_transaction_detail_sn,
                   ledger_transaction_sn, posting_plan_sn, ledger_entry_sn, subject_type, subject_id, currency,
                   amount, refund_amount, business_line, split_period, split_rule_code, split_rule_version,
                   admission_result, exclusion_reason, reconciliation_decision_result, gate_evidence_ref,
                   reconciliation_evidence_refs, route_snapshot_digest, source_digest, created_by
            FROM t_clearing_splittable_detail
            WHERE tenant_id = #{tenantId}
              AND ledger_entry_sn = #{ledgerEntrySn}
            """)
    ClearingSplittableDetail selectByLedgerEntrySn(@Param("tenantId") Long tenantId,
                                                   @Param("ledgerEntrySn") String ledgerEntrySn);

    @Select("""
            <script>
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, funds_transaction_sn, funds_transaction_detail_sn,
                   ledger_transaction_sn, posting_plan_sn, ledger_entry_sn, subject_type, subject_id, currency,
                   amount, refund_amount, business_line, split_period, split_rule_code, split_rule_version,
                   admission_result, exclusion_reason, reconciliation_decision_result, gate_evidence_ref,
                   reconciliation_evidence_refs, route_snapshot_digest, source_digest, created_by
            FROM t_clearing_splittable_detail
            WHERE tenant_id = #{tenantId}
              AND sn IN
              <foreach collection="sns" item="sn" open="(" separator="," close=")">
                #{sn}
              </foreach>
            ORDER BY sn
            </script>
            """)
    List<ClearingSplittableDetail> selectBySns(@Param("tenantId") Long tenantId,
                                               @Param("sns") List<String> sns);
}
