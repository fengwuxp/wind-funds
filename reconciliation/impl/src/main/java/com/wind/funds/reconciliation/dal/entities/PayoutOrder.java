package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.PayoutOrderStatus;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(PayoutOrder.TABLE_NAME)
public class PayoutOrder implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_payout_order";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    private String settlementOrderSn;

    private String settlementSubjectType;

    private String settlementSubjectId;

    private Long amount;

    private CurrencyIsoCode currency;

    private PayoutOrderStatus status;

    private String payoutAccountRef;

    private String payeeEndpointRef;

    private String channelRef;

    private String approvalRef;

    private String externalRuleEvidenceDigest;

    private String reconciliationRunResultSn;

    private String reconciliationResultDigest;

    private String admissionDecisionDigest;

    private String admissionEvidenceRefs;

    private String submitDigest;

    private String externalReference;

    private String completionFundsTransactionSn;

    private String rollbackFundsTransactionSn;

    private String lastReceiptDigest;

    private String failureCode;

    private String failureReason;

    private String createdBy;

    private String submittedBy;

    private LocalDateTime submittedTime;

    private LocalDateTime completedTime;

    @Column(version = true)
    private Integer version;
}
