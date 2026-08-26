package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationMatchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对账匹配结果 Mapper。
 */
@Mapper
public interface ReconciliationMatchResultMapper extends BaseMapper<ReconciliationMatchResult> {

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, reconciliation_run_result_sn, reconciliation_batch_sn,
                   reference_fact_owner_namespace, reference_fact_identity_value, comparison_fact_owner_namespace,
                   comparison_fact_identity_value, comparison_owner_namespace, comparison_identity_value,
                   result_kind, absolute_difference_currency, absolute_difference_amount, larger_side,
                   evidence_refs, match_identity_digest, result_digest, created_by
            FROM t_reconciliation_match_result
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            """)
    ReconciliationMatchResult selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    /**
     * 按运行结果和来源对身份查询匹配结果。
     */
    @Select("""
            SELECT id, gmt_create, sn, tenant_id, reconciliation_run_result_sn, reconciliation_batch_sn,
                   reference_fact_owner_namespace, reference_fact_identity_value, comparison_fact_owner_namespace,
                   comparison_fact_identity_value, comparison_owner_namespace, comparison_identity_value,
                   result_kind, absolute_difference_currency, absolute_difference_amount, larger_side,
                   evidence_refs, match_identity_digest, result_digest, created_by
            FROM t_reconciliation_match_result
            WHERE tenant_id = #{tenantId}
              AND reconciliation_run_result_sn = #{reconciliationRunResultSn}
              AND match_identity_digest = #{matchIdentityDigest}
            """)
    ReconciliationMatchResult selectByRunResultAndIdentity(
            @Param("tenantId") Long tenantId,
            @Param("reconciliationRunResultSn") String reconciliationRunResultSn,
            @Param("matchIdentityDigest") String matchIdentityDigest);
}
