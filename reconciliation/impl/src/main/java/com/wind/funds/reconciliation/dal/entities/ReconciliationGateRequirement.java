package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 不可变门禁要求头部。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Data
@Table(ReconciliationGateRequirement.TABLE_NAME)
@FieldNameConstants
public class ReconciliationGateRequirement implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -3165851626747526713L;

    public static final String TABLE_NAME = "t_reconciliation_gate_requirement";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String stageKind;

    @NotNull
    private String stageIdentityOwnerNamespace;

    @NotNull
    private String stageIdentityValue;

    @NotNull
    private String requirementIdentityOwnerNamespace;

    @NotNull
    private String requirementIdentityValue;

    @NotNull
    private String requirementVersion;

    @NotNull
    private String semanticDigest;

    @NotNull
    private String evidenceRefs;

    @NotNull
    private String evidenceBundleDigest;

    private String previousRequirementIdentityOwnerNamespace;

    private String previousRequirementIdentityValue;

    private String previousRequirementVersion;

    private String previousSemanticDigest;

    private String previousEvidenceBundleDigest;

    @NotNull
    private String createdBy;
}
