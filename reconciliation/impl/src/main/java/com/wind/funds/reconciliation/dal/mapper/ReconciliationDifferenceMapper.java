package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 对账差错 Mapper。
 */
@Mapper
public interface ReconciliationDifferenceMapper extends BaseMapper<ReconciliationDifference> {

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
              AND (
                    last_rerun_batch_sn = #{reconciliationBatchSn}
                 OR (last_rerun_batch_sn IS NULL AND reconciliation_batch_sn = #{reconciliationBatchSn})
              )
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
              AND (
                    d.last_rerun_batch_sn = #{reconciliationBatchSn}
                 OR (d.last_rerun_batch_sn IS NULL
                     AND d.reconciliation_batch_sn = #{reconciliationBatchSn})
              )
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
              AND (
                    last_rerun_batch_sn = #{reconciliationBatchSn}
                 OR (last_rerun_batch_sn IS NULL AND reconciliation_batch_sn = #{reconciliationBatchSn})
              )
            """)
    int invalidateByCurrentBatch(@Param("tenantId") Long tenantId,
                                 @Param("reconciliationBatchSn") String reconciliationBatchSn);

    /**
     * 查询命中准入对象且当前仍阻断的对账差错。
     *
     * <p>差错只按对象类型和对象流水号精确命中，不允许空对象或类型级通配阻断。</p>
     *
     * @param tenantId           租户 ID
     * @param blockingObjectType 阻断对象类型，例如 CLEARING、SETTLEMENT、PAYOUT
     * @param blockingObjectSn   阻断对象流水号
     * @param limit              最大返回数
     * @param lockRows           是否锁定命中的差错行；权威 Gate 必须传 true
     * @return 对账差错列表
     */
    @Select("""
            <script>
            SELECT *
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND blocking_object_type = #{blockingObjectType}
              AND blocking_object_sn = #{blockingObjectSn}
              AND status != 'INVALIDATED'
              AND (
                    status != 'RESOLVED'
                 OR last_rerun_balanced IS NULL
                 OR last_rerun_balanced != TRUE
                 OR last_rerun_batch_sn IS NULL
                 OR last_rerun_batch_sn NOT IN
                    <foreach collection="currentLineageBatchSns" item="batchSn" open="(" separator="," close=")">
                        #{batchSn}
                    </foreach>
              )
            ORDER BY id ASC
            LIMIT #{limit}
            <if test="lockRows">
            FOR UPDATE
            </if>
            </script>
            """)
    List<ReconciliationDifference> selectBlockingByGateObject(
            @Param("tenantId") Long tenantId,
            @Param("blockingObjectType") String blockingObjectType,
            @Param("blockingObjectSn") String blockingObjectSn,
            @Param("currentLineageBatchSns") List<String> currentLineageBatchSns,
            @Param("limit") int limit,
            @Param("lockRows") boolean lockRows);

    /**
     * 统计已在当前血缘重新对平并关闭的历史差错。
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND blocking_object_type = #{blockingObjectType}
              AND blocking_object_sn = #{blockingObjectSn}
              AND status = 'RESOLVED'
              AND last_rerun_balanced = TRUE
              AND last_rerun_batch_sn IN
                    <foreach collection="currentLineageBatchSns" item="batchSn" open="(" separator="," close=")">
                        #{batchSn}
                    </foreach>
            </script>
            """)
    int countResolvedByGateObject(
            @Param("tenantId") Long tenantId,
            @Param("blockingObjectType") String blockingObjectType,
            @Param("blockingObjectSn") String blockingObjectSn,
            @Param("currentLineageBatchSns") List<String> currentLineageBatchSns);
}
