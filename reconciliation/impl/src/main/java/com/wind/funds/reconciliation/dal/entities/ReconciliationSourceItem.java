package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账来源快照成员。
 */
@Data
@Table(ReconciliationSourceItem.TABLE_NAME)
@FieldNameConstants
public class ReconciliationSourceItem implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -7187290668537788925L;

    public static final String TABLE_NAME = "t_reconciliation_source_item";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String sourceSnapshotSn;

    @NotNull
    private String sourceFactOwnerNamespace;

    @NotNull
    private String sourceFactIdentityValue;

    @NotNull
    private String comparisonOwnerNamespace;

    @NotNull
    private String comparisonIdentityValue;

    @NotNull
    private Long amount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private String ruleNamespace;

    @NotNull
    private String ruleIdentity;

    @NotNull
    private String ruleVersion;

    @NotNull
    private String comparisonStatusCode;

    @NotNull
    private Boolean comparisonProven;

    @NotNull
    private String claimKind;

    @NotNull
    private String economicComponent;

    @NotNull
    private String direction;

    @NotNull
    private String normalizationVersion;

    @NotNull
    private String semanticDigest;

    @NotNull
    private String evidenceBundleDigest;

    @NotNull
    private String evidenceRefs;

    @NotNull
    private String createdBy;
}
