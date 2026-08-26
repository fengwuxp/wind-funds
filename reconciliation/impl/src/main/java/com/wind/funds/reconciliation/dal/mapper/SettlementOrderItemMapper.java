package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.SettlementOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 结算金额项 Mapper。
 */
@Mapper
public interface SettlementOrderItemMapper extends BaseMapper<SettlementOrderItem> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, settlement_order_sn, item_type, direction,
                   source_type, source_sn, amount, currency, source_amount_digest, active_source_claim, created_by
            FROM t_settlement_order_item
            WHERE tenant_id = #{tenantId} AND settlement_order_sn = #{settlementOrderSn}
            ORDER BY source_sn
            """)
    List<SettlementOrderItem> selectByOrderSn(@Param("tenantId") Long tenantId,
                                              @Param("settlementOrderSn") String settlementOrderSn);

    @Select("""
            <script>
            SELECT COUNT(*) FROM t_settlement_order_item
            WHERE tenant_id = #{tenantId}
              AND source_type = 'CLEARING_BATCH'
              AND active_source_claim = 1
              AND source_sn IN
              <foreach collection="sourceSns" item="sourceSn" open="(" separator="," close=")">
                #{sourceSn}
              </foreach>
            </script>
            """)
    int countActiveSourceClaims(@Param("tenantId") Long tenantId,
                                @Param("sourceSns") List<String> sourceSns);

    @Update("""
            UPDATE t_settlement_order_item
            SET active_source_claim = NULL
            WHERE tenant_id = #{tenantId} AND settlement_order_sn = #{settlementOrderSn}
              AND active_source_claim = 1
            """)
    int releaseActiveSourceClaims(@Param("tenantId") Long tenantId,
                                  @Param("settlementOrderSn") String settlementOrderSn);
}
