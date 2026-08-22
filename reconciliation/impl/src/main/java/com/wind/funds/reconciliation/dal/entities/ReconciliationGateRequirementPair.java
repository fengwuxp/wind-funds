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
 * 不可变门禁要求中的一个必需范围与对账对。
 *
 * @author wuxp
 * @since 2026-08-19
 */
@Data
@Table(ReconciliationGateRequirementPair.TABLE_NAME)
@FieldNameConstants
public class ReconciliationGateRequirementPair implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -6741216082592454940L;

    public static final String TABLE_NAME = "t_reconciliation_gate_requirement_pair";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String requirementIdentityOwnerNamespace;

    @NotNull
    private String requirementIdentityValue;

    @NotNull
    private String scopeOwnerNamespace;

    @NotNull
    private String scopeIdentityValue;

    @NotNull
    private String pairOwnerNamespace;

    @NotNull
    private String pairIdentityValue;

    @NotNull
    private String ruleNamespace;

    @NotNull
    private String ruleIdentity;

    @NotNull
    private String ruleVersion;
}
