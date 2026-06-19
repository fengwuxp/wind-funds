package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 对账差错 Mapper。
 */
@Mapper
public interface ReconciliationDifferenceMapper extends BaseMapper<ReconciliationDifference> {

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
     * 查询命中准入对象的对账差错。
     *
     * <p>对象级差错按对象类型和流水号精确命中；历史差错缺少阻断对象字段时保守按类型级范围命中。</p>
     *
     * @param tenantId           租户 ID
     * @param blockingScope      阻断范围，例如 CLEARING、SETTLEMENT、PAYOUT
     * @param blockingObjectType 阻断对象类型，例如 CLEARING、SETTLEMENT、PAYOUT
     * @param blockingObjectSn   阻断对象流水号
     * @return 对账差错列表
     */
    @Select("""
            SELECT *
            FROM t_reconciliation_difference
            WHERE tenant_id = #{tenantId}
              AND (
                  blocking_scope = #{blockingScope}
                  OR blocking_scope LIKE CONCAT(#{blockingScope}, ',%')
                  OR blocking_scope LIKE CONCAT('%,', #{blockingScope})
                  OR blocking_scope LIKE CONCAT('%,', #{blockingScope}, ',%')
              )
              AND (
                  (
                      blocking_object_type = #{blockingObjectType}
                      AND blocking_object_sn = #{blockingObjectSn}
                  )
                  OR (
                      blocking_object_type IS NULL
                      AND blocking_object_sn IS NULL
                  )
              )
            ORDER BY id ASC
            """)
    List<ReconciliationDifference> selectByGateObject(@Param("tenantId") Long tenantId,
                                                      @Param("blockingScope") String blockingScope,
                                                      @Param("blockingObjectType") String blockingObjectType,
                                                      @Param("blockingObjectSn") String blockingObjectSn);
}
