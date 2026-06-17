package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.ReconciliationDifference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
