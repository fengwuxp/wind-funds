package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ClearingCandidateState;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 清算候选准入事实。
 */
@Data
@Table(ClearingCandidate.TABLE_NAME)
@FieldNameConstants
public class ClearingCandidate implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -1375456269094267522L;

    public static final String TABLE_NAME = "t_clearing_candidate";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String splitResultSn;

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
    private String clearingPeriod;

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
    private LocalDateTime clearingAvailableTime;

    @NotNull
    private String clearingRuleCode;

    @NotNull
    private String clearingRuleVersion;

    @NotNull
    private String gateEvidenceRef;

    @NotNull
    private String reconciliationEvidenceRefs;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String candidateDigest;

    private String activeSplittableDetailSn;

    @NotNull
    @Column("status")
    private ClearingCandidateState state;

    void setStatus(ClearingCandidateState state) {
        this.state = state;
    }

    private String blockReason;

    private String exclusionReason;

    private String lockedClearingBatchSn;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime statusChangedTime;
}
