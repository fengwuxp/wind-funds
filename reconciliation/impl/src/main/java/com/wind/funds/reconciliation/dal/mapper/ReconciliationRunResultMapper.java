package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationRunResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 对账运行结果 Mapper。
 */
@Mapper
public interface ReconciliationRunResultMapper extends BaseMapper<ReconciliationRunResult> {

    /**
     * 按流水号查询运行结果。
     *
     * @param tenantId 租户 ID
     * @param sn 运行结果流水号
     * @return 运行结果；不存在时返回 null
     */
    @Select("""
            SELECT id, gmt_create, sn, tenant_id, reconciliation_batch_sn, scope_owner_namespace,
                   scope_identity_value, pair_owner_namespace, pair_identity_value, currency, outcome,
                   rule_namespace, rule_identity, rule_version, reference_snapshot_sn, comparison_snapshot_sn,
                   reference_source_digest, comparison_source_digest, source_digest, result_digest, total_count,
                   matched_count, difference_count, evidence_refs, created_by
            FROM t_reconciliation_run_result
            WHERE tenant_id = #{tenantId}
              AND sn = #{sn}
            """)
    ReconciliationRunResult selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    /**
     * 按对账批次查询不可变运行结果。
     *
     * @param tenantId 租户 ID
     * @param reconciliationBatchSn 对账批次流水号
     * @return 运行结果；不存在时返回 null
     */
    @Select("""
            SELECT id, gmt_create, sn, tenant_id, reconciliation_batch_sn, scope_owner_namespace,
                   scope_identity_value, pair_owner_namespace, pair_identity_value, currency, outcome,
                   rule_namespace, rule_identity, rule_version, reference_snapshot_sn, comparison_snapshot_sn,
                   reference_source_digest, comparison_source_digest, source_digest, result_digest, total_count,
                   matched_count, difference_count, evidence_refs, created_by
            FROM t_reconciliation_run_result
            WHERE tenant_id = #{tenantId}
              AND reconciliation_batch_sn = #{reconciliationBatchSn}
            """)
    ReconciliationRunResult selectByBatch(@Param("tenantId") Long tenantId,
                                          @Param("reconciliationBatchSn") String reconciliationBatchSn);
}
