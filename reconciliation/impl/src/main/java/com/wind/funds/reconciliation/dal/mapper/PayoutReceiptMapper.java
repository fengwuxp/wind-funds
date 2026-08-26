package com.wind.funds.reconciliation.dal.mapper;

import com.mybatisflex.core.BaseMapper;
import com.wind.funds.reconciliation.dal.entities.PayoutReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PayoutReceiptMapper extends BaseMapper<PayoutReceipt> {

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, payout_order_sn, channel_ref, external_receipt_ref,
                   external_reference, state, amount, currency, source_receipt_digest, normalized_receipt_digest,
                   evidence_ref, external_occurred_at, received_by FROM t_payout_receipt
            WHERE tenant_id = #{tenantId}
              AND channel_ref = #{channelRef}
              AND external_receipt_ref = #{externalReceiptRef}
            """)
    PayoutReceipt selectBySource(@Param("tenantId") Long tenantId,
                                 @Param("channelRef") String channelRef,
                                 @Param("externalReceiptRef") String externalReceiptRef);

    @Select("""
            SELECT id, gmt_create, gmt_modified, sn, tenant_id, payout_order_sn, channel_ref, external_receipt_ref,
                   external_reference, state, amount, currency, source_receipt_digest, normalized_receipt_digest,
                   evidence_ref, external_occurred_at, received_by FROM t_payout_receipt
            WHERE tenant_id = #{tenantId}
              AND channel_ref = #{channelRef}
              AND external_receipt_ref = #{externalReceiptRef}
            FOR UPDATE
            """)
    PayoutReceipt selectBySourceForUpdate(@Param("tenantId") Long tenantId,
                                          @Param("channelRef") String channelRef,
                                          @Param("externalReceiptRef") String externalReceiptRef);
}
