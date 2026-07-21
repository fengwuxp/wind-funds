package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.funds.reconciliation.enums.ReconciliationRunResultStatus;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账运行结果。
 */
@Data
@Table(ReconciliationRunResult.TABLE_NAME)
@FieldNameConstants
public class ReconciliationRunResult implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -6890953651804099698L;

    public static final String TABLE_NAME = "t_reconciliation_run_result";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String reconciliationBatchSn;

    @NotNull
    private ReconciliationGateObjectType gateObjectType;

    @NotNull
    private String gateObjectSn;

    @NotNull
    private ReconciliationRunResultStatus status;

    @NotNull
    private String ruleVersion;

    @NotNull
    private String internalSourceDigest;

    @NotNull
    private String externalSourceDigest;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String resultDigest;

    @NotNull
    private Integer totalCount;

    @NotNull
    private Integer matchedCount;

    @NotNull
    private Integer differenceCount;

    @NotNull
    private String evidenceRefs;

    @NotNull
    private String createdBy;
}
