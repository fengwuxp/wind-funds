package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 清分确认生成的不可变结果快照。
 */
@Data
@Table(ClearingSplitResultSnapshot.TABLE_NAME)
@FieldNameConstants
public class ClearingSplitResultSnapshot implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -2090773374728247190L;

    public static final String TABLE_NAME = "t_clearing_split_result_snapshot";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String splitBatchSn;

    @NotNull
    private String splittableDetailSn;

    @NotNull
    private String subjectType;

    @NotNull
    private String subjectId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private String businessLine;

    @NotNull
    private String splitPeriod;

    @NotNull
    private Long amount;

    @NotNull
    private String fundsTransactionSn;

    @NotNull
    private String fundsTransactionDetailSn;

    @NotNull
    private String ledgerTransactionSn;

    @NotNull
    private String postingPlanSn;

    @NotNull
    private String ledgerEntrySn;

    @NotNull
    private String routeSnapshotDigest;

    @NotNull
    private String splitRuleCode;

    @NotNull
    private String splitRuleVersion;

    @NotNull
    private String reconciliationRunResultSn;

    @NotNull
    private String reconciliationResultDigest;

    @NotNull
    private String reconciliationEvidenceRefs;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String snapshotDigest;

    @NotNull
    private String createdBy;
}
