package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.SettlementOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 结算单 Mapper。
 */
@Mapper
public interface SettlementOrderMapper extends BaseMapper<SettlementOrder> {

    @Select("""
            SELECT * FROM t_settlement_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    SettlementOrder selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_settlement_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    SettlementOrder selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_settlement_order
            WHERE tenant_id = #{tenantId} AND active_order_digest = #{orderDigest}
            """)
    SettlementOrder selectByDigest(@Param("tenantId") Long tenantId,
                                   @Param("orderDigest") String orderDigest);

    @Update("""
            UPDATE t_settlement_order
            SET active_order_digest = NULL
            WHERE tenant_id = #{tenantId} AND sn = #{sn} AND active_order_digest IS NOT NULL
            """)
    int releaseActiveOrderDigest(@Param("tenantId") Long tenantId, @Param("sn") String sn);
}
