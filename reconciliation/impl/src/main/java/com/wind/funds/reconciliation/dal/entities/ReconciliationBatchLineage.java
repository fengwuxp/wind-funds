package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationGateObjectType;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Gate 对账批次血缘的当前头。
 *
 * <p>同一租户和准入对象只保留一个当前批次；对账范围作为血缘事实固化，但不允许据此建立平行血缘。</p>
 */
@Data
@Table(ReconciliationBatchLineage.TABLE_NAME)
@FieldNameConstants
public class ReconciliationBatchLineage implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 6594548093659926784L;

    public static final String TABLE_NAME = "t_reconciliation_batch_lineage";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String reconciliationScopeRef;

    @NotNull
    private ReconciliationGateObjectType gateObjectType;

    @NotNull
    private String gateObjectSn;

    @NotNull
    private String currentBatchSn;
}
