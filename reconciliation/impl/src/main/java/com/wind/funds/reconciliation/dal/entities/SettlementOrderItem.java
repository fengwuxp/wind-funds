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
 * 结算金额项和来源快照。
 */
@Data
@Table(SettlementOrderItem.TABLE_NAME)
@FieldNameConstants
public class SettlementOrderItem implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 2266034613341325865L;

    public static final String TABLE_NAME = "t_settlement_order_item";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String settlementOrderSn;

    @NotNull
    private String itemType;

    @NotNull
    private String direction;

    @NotNull
    private String sourceType;

    @NotNull
    private String sourceSn;

    @NotNull
    private Long amount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private String sourceAmountDigest;

    private Integer activeSourceClaim;

    @NotNull
    private String createdBy;
}
