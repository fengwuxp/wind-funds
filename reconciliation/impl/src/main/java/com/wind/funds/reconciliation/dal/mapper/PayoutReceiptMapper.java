package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.PayoutReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PayoutReceiptMapper extends BaseMapper<PayoutReceipt> {

    @Select("""
            SELECT * FROM t_payout_receipt
            WHERE tenant_id = #{tenantId}
              AND channel_ref = #{channelRef}
              AND external_receipt_ref = #{externalReceiptRef}
            """)
    PayoutReceipt selectBySource(@Param("tenantId") Long tenantId,
                                 @Param("channelRef") String channelRef,
                                 @Param("externalReceiptRef") String externalReceiptRef);

    @Select("""
            SELECT * FROM t_payout_receipt
            WHERE tenant_id = #{tenantId}
              AND channel_ref = #{channelRef}
              AND external_receipt_ref = #{externalReceiptRef}
            FOR UPDATE
            """)
    PayoutReceipt selectBySourceForUpdate(@Param("tenantId") Long tenantId,
                                          @Param("channelRef") String channelRef,
                                          @Param("externalReceiptRef") String externalReceiptRef);
}
