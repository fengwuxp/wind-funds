package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.PayoutOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PayoutOrderMapper extends BaseMapper<PayoutOrder> {

    @Select("""
            SELECT * FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            """)
    PayoutOrder selectBySn(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND sn = #{sn}
            FOR UPDATE
            """)
    PayoutOrder selectBySnForUpdate(@Param("tenantId") Long tenantId, @Param("sn") String sn);

    @Select("""
            SELECT * FROM t_payout_order
            WHERE tenant_id = #{tenantId} AND settlement_order_sn = #{settlementOrderSn}
            """)
    PayoutOrder selectBySettlementOrderSn(@Param("tenantId") Long tenantId,
                                          @Param("settlementOrderSn") String settlementOrderSn);

    @Update("""
            UPDATE t_payout_order
            SET external_reference = #{externalReference}
            WHERE id = #{id} AND external_reference IS NULL
            """)
    int claimExternalReference(@Param("id") Long id,
                               @Param("externalReference") String externalReference);
}
