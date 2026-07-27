package com.wind.funds.reconciliation.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.reconciliation.enums.ReconciliationDifferenceActionType;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对账差错处理动作事实。
 *
 * <p>每次上层完成的补事实、冲正、调账、挂账、追偿或核销均追加一条记录，不覆盖历史动作。</p>
 */
@Data
@Table(ReconciliationDifferenceAction.TABLE_NAME)
@FieldNameConstants
public class ReconciliationDifferenceAction implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -2326010215242482239L;

    public static final String TABLE_NAME = "t_reconciliation_difference_action";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String differenceSn;

    @NotNull
    private ReconciliationDifferenceActionType actionType;

    @NotNull
    private String adjustmentSn;

    @NotNull
    private String idempotencyKey;

    @NotNull
    private String originalFactRef;

    private String adjustmentTransactionSn;

    @NotNull
    private String approvalRef;

    @NotNull
    private String evidenceRef;

    @NotNull
    private String reason;

    @NotNull
    private String createdBy;
}
