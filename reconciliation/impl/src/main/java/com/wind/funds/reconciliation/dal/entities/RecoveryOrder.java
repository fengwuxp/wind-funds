package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.RecoveryOrderStatus;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(RecoveryOrder.TABLE_NAME)
public class RecoveryOrder implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_recovery_order";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    private String sourceType;

    private String sourceSn;

    private String responsibleSubjectType;

    private String responsibleSubjectId;

    private Long expectedAmount;

    private Long recoveredAmount;

    private CurrencyIsoCode currency;

    private RecoveryOrderStatus status;

    private String sourceDigest;

    private String orderDigest;

    private String approvalRef;

    private String evidenceRef;

    private String lastFundsTransactionSn;

    private String createdBy;

    private LocalDateTime recoveredTime;

    @Column(version = true)
    private Integer version;
}
