package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ClearingBatchState;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 单账务主体清算批次。
 */
@Data
@Table(ClearingBatch.TABLE_NAME)
@FieldNameConstants
public class ClearingBatch implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -1847598523312156892L;

    public static final String TABLE_NAME = "t_clearing_batch";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String subjectType;

    @NotNull
    private String subjectId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private String businessLine;

    @NotNull
    private String clearingPeriod;

    @NotNull
    private String clearingRuleCode;

    @NotNull
    private String clearingRuleVersion;

    @NotNull
    private Integer candidateCount;

    @NotNull
    private Long totalAmount;

    @NotNull
    private String amountDigest;

    /**
     * REVIEWING 批次的经济范围占用键；终态或退回草稿后置空。
     */
    private String activeAmountDigest;

    private String fundsTransactionSn;

    @NotNull
    private ClearingBatchState state;

    @NotNull
    private String createdBy;

    private String submittedBy;

    private LocalDateTime submittedTime;

    private String confirmedBy;

    private LocalDateTime confirmedTime;

    private String returnedBy;

    private LocalDateTime returnedTime;

    private String returnReason;

    private String cancelledBy;

    private LocalDateTime cancelledTime;

    private String cancelReason;

    private String failedBy;

    private LocalDateTime failedTime;

    private String failureReason;
}
