package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.SettlementDestination;
import com.wind.funds.reconciliation.enums.SettlementMode;
import com.wind.funds.reconciliation.enums.SettlementOrderState;
import com.wind.funds.reconciliation.enums.SettlementReleaseDisposition;
import com.wind.funds.reconciliation.enums.SettlementTriggerMode;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内部结算锁定单。
 */
@Data
@Table(SettlementOrder.TABLE_NAME)
@FieldNameConstants
public class SettlementOrder implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -2792779076803576284L;

    public static final String TABLE_NAME = "t_settlement_order";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String settlementSubjectType;

    @NotNull
    private String settlementSubjectId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private String settlementPeriod;

    @NotNull
    private SettlementMode settlementMode;

    @NotNull
    private SettlementDestination settlementDestination;

    @NotNull
    private SettlementTriggerMode triggerMode;

    @NotNull
    private String timezone;

    @NotNull
    private String cutoff;

    @NotNull
    private Long totalAmount;

    @NotNull
    private Long addAmount;

    @NotNull
    private Long deductAmount;

    @NotNull
    private Long reserveAmount;

    @NotNull
    private Long netAmount;

    @NotNull
    private SettlementOrderState state;

    private String settlementApprovalRef;

    private String lockFundsTransactionSn;

    private String releaseFundsTransactionSn;

    private String releaseFreezeOrderSn;

    private SettlementReleaseDisposition releaseDisposition;

    private String releaseDigest;

    private String releaseGateEvidenceRef;

    private String releaseCurrentLineageBatchSn;

    private String releaseSourceClosureDigest;

    private String releaseAuthorityDecisionDigest;

    private String releaseAuthorityEvidenceRefs;

    private String releaseApprovalRef;

    private String releaseReason;

    private String releasedBy;

    private LocalDateTime releasedTime;

    private String lockGateEvidenceRef;

    @NotNull
    private String ruleCode;

    @NotNull
    private String ruleVersion;

    private String policyApprovalRef;

    @NotNull
    private String amountDigest;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String policySnapshotDigest;

    @NotNull
    private String orderDigest;

    private String activeOrderDigest;

    @NotNull
    private String createdBy;

    private String submittedBy;

    private LocalDateTime submittedTime;

    private String approvedBy;

    private LocalDateTime approvedTime;

    private String lockedBy;

    private LocalDateTime lockedTime;

    private String returnedBy;

    private LocalDateTime returnedTime;

    private String returnReason;

    private String cancelledBy;

    private LocalDateTime cancelledTime;

    private String cancelReason;

    private String failedBy;

    private LocalDateTime failedTime;

    private String failureReason;

    @Column(version = true)
    private Integer version;
}
