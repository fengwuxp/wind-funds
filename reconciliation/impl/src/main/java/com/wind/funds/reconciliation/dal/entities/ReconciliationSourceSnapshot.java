package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账来源快照。
 */
@Data
@Table(ReconciliationSourceSnapshot.TABLE_NAME)
@FieldNameConstants
public class ReconciliationSourceSnapshot implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 2547403002610205634L;

    public static final String TABLE_NAME = "t_reconciliation_source_snapshot";

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
    private ReconciliationSourceRole sourceRole;

    @NotNull
    private String sourceNamespace;

    @NotNull
    private String snapshotOwnerNamespace;

    @NotNull
    private String snapshotIdentityValue;

    @NotNull
    private String snapshotVersion;

    @NotNull
    private Boolean coverageComplete;

    private String coverageWatermark;

    @NotNull
    private Integer coverageMemberCount;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String semanticDigest;

    @NotNull
    private String evidenceBundleDigest;

    @NotNull
    private String evidenceRefs;

    @NotNull
    private String createdBy;
}
