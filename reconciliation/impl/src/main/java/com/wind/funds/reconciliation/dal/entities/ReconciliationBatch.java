package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationBatchStatus;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账批次。
 */
@Data
@Table(ReconciliationBatch.TABLE_NAME)
@FieldNameConstants
public class ReconciliationBatch implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -1233111524508731594L;

    public static final String TABLE_NAME = "t_reconciliation_batch";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String reconciliationScopeRef;

    private ReconciliationGateObjectType gateObjectType;

    private String gateObjectSn;

    @NotNull
    private String ruleVersion;

    @NotNull
    private LocalDateTime windowStart;

    @NotNull
    private LocalDateTime windowEnd;

    @NotNull
    private String timezoneId;

    private String previousBatchSn;

    @NotNull
    private ReconciliationBatchStatus status;

    private String runResultSn;

    private String abortedBy;

    private LocalDateTime abortedTime;

    private String abortReason;

    @NotNull
    private String batchDigest;

    @NotNull
    private String createdBy;
}
