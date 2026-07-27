package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ClearingSplittableDetailStatus;
import com.wind.funds.reconciliation.enums.ClearingSplittableExclusionReason;
import com.wind.funds.reconciliation.enums.ReconciliationGateDecisionStatus;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 可清分明细准入事实。
 */
@Data
@Table(ClearingSplittableDetail.TABLE_NAME)
@FieldNameConstants
public class ClearingSplittableDetail implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 7960793348173312737L;

    public static final String TABLE_NAME = "t_clearing_splittable_detail";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String fundsTransactionSn;

    /**
     * 识别候选时冻结的来源资金交易版本；最终清分命令必须重新核对。
     */
    @NotNull
    private Integer sourceTransactionVersion;

    @NotNull
    private String fundsTransactionDetailSn;

    @NotNull
    private String ledgerTransactionSn;

    @NotNull
    private String postingPlanSn;

    @NotNull
    private String ledgerEntrySn;

    @NotNull
    private String subjectType;

    @NotNull
    private String subjectId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private Long principalAmount;

    @NotNull
    private Long refundAmount;

    @NotNull
    private String clearingPeriod;

    @NotNull
    private String ruleCode;

    @NotNull
    private String ruleVersion;

    @NotNull
    private ClearingSplittableDetailStatus status;

    private ClearingSplittableExclusionReason exclusionReason;

    @NotNull
    private ReconciliationGateDecisionStatus reconciliationDecisionStatus;

    /**
     * 清分前消费的对账运行结果流水号。
     */
    @NotNull
    private String reconciliationRunResultSn;

    /**
     * 清分前消费的对账运行结果 SHA-256。
     */
    private String reconciliationResultDigest;

    @NotNull
    private String reconciliationEvidenceRefs;

    @NotNull
    private String sourceDigest;

    @NotNull
    private String createdBy;
}
