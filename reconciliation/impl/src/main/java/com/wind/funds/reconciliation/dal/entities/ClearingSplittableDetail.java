package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ClearingSplittableAdmissionResult;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionResult;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可清分明细准入事实。
 */
@Data
@Table(ClearingSplittableDetail.TABLE_NAME)
@FieldNameConstants
public class ClearingSplittableDetail implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 7960793348173312737L;

    public static final String TABLE_NAME = "t_clearing_splittable_detail";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

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
    private String subjectType;

    @NotNull
    private String subjectId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private Long amount;

    @NotNull
    private Long refundAmount;

    @NotNull
    private String businessLine;

    @NotNull
    private String splitPeriod;

    @NotNull
    private String splitRuleCode;

    @NotNull
    private String splitRuleVersion;

    @NotNull
    @Column("status")
    private ClearingSplittableAdmissionResult admissionResult;

    void setStatus(ClearingSplittableAdmissionResult admissionResult) {
        this.admissionResult = admissionResult;
    }

    private ClearingSplittableExclusionReason exclusionReason;

    @NotNull
    @Column("reconciliation_decision_status")
    private ReconciliationGateDecisionResult reconciliationDecisionResult;

    void setReconciliationDecisionStatus(ReconciliationGateDecisionResult reconciliationDecisionResult) {
        this.reconciliationDecisionResult = reconciliationDecisionResult;
    }

    private String gateEvidenceRef;

    @NotNull
    private String reconciliationEvidenceRefs;

    private String routeSnapshotDigest;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String createdBy;
}
