package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(PayoutReceipt.TABLE_NAME)
public class PayoutReceipt implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_payout_receipt";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    private String payoutOrderSn;

    private String channelRef;

    private String externalReceiptRef;

    private String externalReference;

    private PayoutOrderState state;

    private Long amount;

    private CurrencyIsoCode currency;

    private String sourceReceiptDigest;

    private String normalizedReceiptDigest;

    private String evidenceRef;

    private LocalDateTime externalOccurredAt;

    private String receivedBy;
}
