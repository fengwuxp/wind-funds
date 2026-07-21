package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceSeverity;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceType;
import com.wind.funds.reconciliation.enums.ReconciliationMatchStrength;
import com.wind.funds.reconciliation.enums.ReconciliationSourceQuality;
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

    private String internalSourceRef;

    private String externalSourceRef;

    @NotNull
    private ReconciliationSourceQuality sourceQuality;

    @NotNull
    private ReconciliationMatchStrength matchStrength;

    private ReconciliationDifferenceType differenceType;

    private ReconciliationDifferenceSeverity severity;

    private CurrencyIsoCode currency;

    private Long differenceAmount;

    @NotNull
    private String evidenceRef;

    @NotNull
    private String matchIdentityDigest;

    @NotNull
    private String matchDigest;

    @NotNull
    private String createdBy;
}
