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
 * 一次成功阶段动作消费的证据；绝不是授权令牌。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Data
@Table(ReconciliationStageGateEvidence.TABLE_NAME)
@FieldNameConstants
public class ReconciliationStageGateEvidence implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 7644053802988599462L;

    public static final String TABLE_NAME = "t_reconciliation_stage_gate_evidence";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @NotNull
    private String sn;

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
    private String requirementSemanticDigest;

    @NotNull
    private String requirementEvidenceBundleDigest;

    @NotNull
    private String consumedPairEvidence;

    @NotNull
    private String decisionDigest;

    @NotNull
    private String evidenceRefs;

    @NotNull
    private String createdBy;
}
