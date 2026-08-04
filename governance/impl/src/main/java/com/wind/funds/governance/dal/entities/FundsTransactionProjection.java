package com.wind.funds.governance.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 交易只读投影行，使用 scope 隔离正式与影子数据。
 */
@Data
@Table(FundsTransactionProjection.TABLE_NAME)
public class FundsTransactionProjection implements TenantIsolationObject<Long> {

    public static final String TABLE_NAME = "t_funds_transaction_projection";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @Column(tenantId = true)
    private Long tenantId;

    private String viewDomain;

    private String projectionScope;

    private String scopeRef;

    private String projectionSn;

    private String ownerType;

    private String ownerId;

    private String sourceSn;

    private String displayType;

    private String displayStatus;

    private Long amount;

    private String currency;

    private LocalDateTime occurredTime;

    private String payloadJson;

    private String replayTaskSn;

    @Column(version = true)
    private Integer version;
}
