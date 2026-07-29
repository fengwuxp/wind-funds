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
 * 清算批次候选绑定和提交快照。
 */
@Data
@Table(ClearingBatchDetail.TABLE_NAME)
@FieldNameConstants
public class ClearingBatchDetail implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 6490038615267097067L;

    public static final String TABLE_NAME = "t_clearing_batch_detail";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String clearingBatchSn;

    @NotNull
    private String candidateSn;

    @NotNull
    private String splitBatchSn;

    @NotNull
    private String splittableDetailSn;

    @NotNull
    private String fundsTransactionDetailSn;

    @NotNull
    private String ledgerEntrySn;

    @NotNull
    private Long amount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private String createdBy;
}
