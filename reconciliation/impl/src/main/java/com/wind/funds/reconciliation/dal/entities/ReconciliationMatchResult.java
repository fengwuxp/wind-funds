package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationMatchResultKind;
import com.wind.funds.reconciliation.enums.ReconciliationSourceRole;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账匹配结果明细。
 */
@Data
@Table(ReconciliationMatchResult.TABLE_NAME)
@FieldNameConstants
public class ReconciliationMatchResult implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -1910403067195739363L;

    public static final String TABLE_NAME = "t_reconciliation_match_result";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String reconciliationRunResultSn;

    @NotNull
    private String reconciliationBatchSn;

    private String referenceFactOwnerNamespace;

    private String referenceFactIdentityValue;

    private String comparisonFactOwnerNamespace;

    private String comparisonFactIdentityValue;

    @NotNull
    private String comparisonOwnerNamespace;

    @NotNull
    private String comparisonIdentityValue;

    @NotNull
    private ReconciliationMatchResultKind resultKind;

    private CurrencyIsoCode absoluteDifferenceCurrency;

    private Long absoluteDifferenceAmount;

    private ReconciliationSourceRole largerSide;

    @NotNull
    private String evidenceRefs;

    @NotNull
    private String matchIdentityDigest;

    @NotNull
    private String resultDigest;

    @NotNull
    private String createdBy;
}
