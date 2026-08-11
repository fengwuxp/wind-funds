package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ClearingSplitBatchState;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 单账务主体清分批次。
 */
@Data
@Table(ClearingSplitBatch.TABLE_NAME)
@FieldNameConstants
public class ClearingSplitBatch implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 7727985743282635504L;

    public static final String TABLE_NAME = "t_clearing_split_batch";

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
    private String splitPeriod;

    @NotNull
    private String splitRuleCode;

    @NotNull
    private String splitRuleVersion;

    @NotNull
    private Integer detailCount;

    @NotNull
    private Long totalAmount;

    @NotNull
    private String memberDigest;

    @NotNull
    private String batchDigest;

    /**
     * 有效批次幂等键；批次取消后置空，历史摘要继续保留。
     */
    private String activeBatchDigest;

    @NotNull
    @Column("status")
    private ClearingSplitBatchState state;

    void setStatus(ClearingSplitBatchState state) {
        this.state = state;
    }

    @NotNull
    private String createdBy;

    private String submittedBy;

    private LocalDateTime submittedTime;

    private String confirmedBy;

    private LocalDateTime confirmedTime;

    private String cancelledBy;

    private LocalDateTime cancelledTime;

    private String cancelReason;
}
