package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;


/**
 * 对账差错 Mapper。
 */
@Mapper
public interface ReconciliationDifferenceMapper extends BaseMapper<ReconciliationDifference> {

    @Select("""
            SELECT COUNT(*) FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND scope_owner_namespace = #{scopeOwnerNamespace}
              AND scope_identity_value = #{scopeIdentityValue}
              AND pair_owner_namespace = #{pairOwnerNamespace}
              AND pair_identity_value = #{pairIdentityValue}
              AND current_lineage_ref = #{currentLineageRef}
              AND status NOT IN ('RESOLVED', 'INVALIDATED')
            """)
    int countBlockingByRequiredPair(
            @Param("tenantId") Long tenantId,
            @Param("scopeOwnerNamespace") String scopeOwnerNamespace,
            @Param("scopeIdentityValue") String scopeIdentityValue,
            @Param("pairOwnerNamespace") String pairOwnerNamespace,
            @Param("pairIdentityValue") String pairIdentityValue,
            @Param("currentLineageRef") String currentLineageRef);

    /**
     * 按差错流水号只读查询。
     *
     * @param tenantId     租户 ID
     * @param differenceSn 差错流水号
     * @return 对账差错；不存在时返回 null
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND difference_sn = #{differenceSn}
            """)
    ReconciliationDifference selectByDifferenceSn(@Param("tenantId") Long tenantId,
                                                  @Param("differenceSn") String differenceSn);

    /**
     * 按差错流水号加锁查询。
     *
     * @param tenantId     租户 ID
     * @param differenceSn 差错流水号
     * @return 对账差错；不存在时返回 null
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND difference_sn = #{differenceSn}
            FOR UPDATE
            """)
    ReconciliationDifference selectByDifferenceSnForUpdate(@Param("tenantId") Long tenantId,
                                                           @Param("differenceSn") String differenceSn);

    /**
     * 按逐笔匹配结果锁定 Gate 差错。
     *
     * @param tenantId 租户 ID
     * @param reconciliationMatchResultSn 逐笔匹配结果流水号
     * @return 对账差错；不存在时返回 null
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND reconciliation_match_result_sn = #{reconciliationMatchResultSn}
            FOR UPDATE
            """)
    ReconciliationDifference selectByMatchResultSnForUpdate(
            @Param("tenantId") Long tenantId,
            @Param("reconciliationMatchResultSn") String reconciliationMatchResultSn);

    /**
     * 统计当前锚定在指定批次的 Gate 差错数。
     */
    @Select("""
            SELECT COUNT(*)
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND status NOT IN ('RESOLVED', 'INVALIDATED')
              AND current_lineage_ref = #{reconciliationBatchSn}
            """)
    int countByCurrentBatch(@Param("tenantId") Long tenantId,
                            @Param("reconciliationBatchSn") String reconciliationBatchSn);

    /**
     * 锁定并返回批次中首个尚未完成处理动作的差错 ID。
     *
     * @return 未就绪差错 ID；全部就绪或无差错时返回 null
     */
    @Select("""
            SELECT d.id
            FROM t_reconciliation_difference d
            WHERE d.tenant_id = #{tenantId}
              AND d.status NOT IN ('RESOLVED', 'INVALIDATED')
              AND d.current_lineage_ref = #{reconciliationBatchSn}
              AND (
                    d.status != 'ADJUSTING'
                 OR d.adjustment_sn IS NULL
                 OR NOT EXISTS (
                        SELECT 1
                        FROM t_reconciliation_difference_action a
                        WHERE a.tenant_id = d.tenant_id
                          AND a.difference_sn = d.difference_sn
                          AND a.adjustment_sn = d.adjustment_sn
                    )
              )
            ORDER BY d.id
            LIMIT 1
            FOR UPDATE
            """)
    Long selectFirstUnreadyForRerunIdForUpdate(@Param("tenantId") Long tenantId,
                                               @Param("reconciliationBatchSn") String reconciliationBatchSn);

    /**
     * 使当前锚定在被替代批次的差错失效。
     */
    @Update("""
            UPDATE t_reconciliation_difference
            SET status = 'INVALIDATED'
            WHERE tenant_id = #{tenantId}
              AND status != 'INVALIDATED'
              AND current_lineage_ref = #{reconciliationBatchSn}
            """)
    int invalidateByCurrentBatch(@Param("tenantId") Long tenantId,
                                 @Param("reconciliationBatchSn") String reconciliationBatchSn);

}
