package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifferenceAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对账差错处理动作事实 Mapper。
 */
@Mapper
public interface ReconciliationDifferenceActionMapper extends BaseMapper<ReconciliationDifferenceAction> {

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, difference_sn, action_type, adjustment_sn, idempotency_key,
                   original_fact_ref, adjustment_transaction_sn, approval_ref, evidence_ref, reason, created_by
            FROM t_reconciliation_difference_action
            WHERE tenant_id = #{tenantId}
              AND adjustment_sn = #{adjustmentSn}
            FOR UPDATE
            """)
    ReconciliationDifferenceAction selectByAdjustmentSnForUpdate(@Param("tenantId") Long tenantId,
                                                                 @Param("adjustmentSn") String adjustmentSn);

    @Select("""
            SELECT id, gmt_create, sn, tenant_id, difference_sn, action_type, adjustment_sn, idempotency_key,
                   original_fact_ref, adjustment_transaction_sn, approval_ref, evidence_ref, reason, created_by
            FROM t_reconciliation_difference_action
            WHERE tenant_id = #{tenantId}
              AND idempotency_key = #{idempotencyKey}
            FOR UPDATE
            """)
    ReconciliationDifferenceAction selectByIdempotencyKeyForUpdate(@Param("tenantId") Long tenantId,
                                                                   @Param("idempotencyKey") String idempotencyKey);

    /**
     * 按发生顺序查询单笔差错的全部处理动作事实。
     */
    @Select("""
            SELECT id, gmt_create, sn, tenant_id, difference_sn, action_type, adjustment_sn, idempotency_key,
                   original_fact_ref, adjustment_transaction_sn, approval_ref, evidence_ref, reason, created_by
            FROM t_reconciliation_difference_action
            WHERE tenant_id = #{tenantId}
              AND difference_sn = #{differenceSn}
            ORDER BY id ASC
            """)
    List<ReconciliationDifferenceAction> selectByDifferenceSn(@Param("tenantId") Long tenantId,
                                                              @Param("differenceSn") String differenceSn);
}
