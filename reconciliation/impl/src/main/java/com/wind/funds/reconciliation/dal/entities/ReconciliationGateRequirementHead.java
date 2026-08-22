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
 * 一个精确阶段动作的当前门禁要求指针。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Data
@Table(ReconciliationGateRequirementHead.TABLE_NAME)
@FieldNameConstants
public class ReconciliationGateRequirementHead implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 5945977732619991052L;

    public static final String TABLE_NAME = "t_reconciliation_gate_requirement_head";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String stageKind;

    @NotNull
    private String stageIdentityOwnerNamespace;

    @NotNull
    private String stageIdentityValue;

    @NotNull
    private String currentRequirementIdentityOwnerNamespace;

    @NotNull
    private String currentRequirementIdentityValue;

    @NotNull
    private String currentRequirementVersion;

    @NotNull
    private String currentSemanticDigest;

    @NotNull
    private String currentEvidenceBundleDigest;

    @NotNull
    @Column(version = true)
    private Integer version;
}
