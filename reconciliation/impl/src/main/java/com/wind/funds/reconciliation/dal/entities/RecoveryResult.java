package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(RecoveryResult.TABLE_NAME)
public class RecoveryResult implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_recovery_result";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    private String recoveryOrderSn;

    private String fundsTransactionSn;

    private Long amount;

    private CurrencyIsoCode currency;

    private String idempotencyKey;

    private String resultDigest;

    private String approvalRef;

    private String evidenceRef;

    private String recordedBy;
}
