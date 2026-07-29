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
 * 清分批次明细关系。
 */
@Data
@Table(ClearingSplitBatchDetail.TABLE_NAME)
@FieldNameConstants
public class ClearingSplitBatchDetail implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -4049395722879732419L;

    public static final String TABLE_NAME = "t_clearing_split_batch_detail";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String splitBatchSn;

    @NotNull
    private String splittableDetailSn;

    /**
     * 有效占用键；批次取消后置空，历史成员关系继续保留。
     */
    private String activeSplittableDetailSn;

    @NotNull
    private String createdBy;
}
