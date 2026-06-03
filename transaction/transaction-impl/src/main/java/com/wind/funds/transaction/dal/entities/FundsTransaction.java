package com.wind.funds.transaction.dal.entities;

import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标准资金交易聚合记录。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(FundsTransaction.TABLE_NAME)
public class FundsTransaction implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -1874805167716741170L;

    public static final String TABLE_NAME = "t_funds_transaction";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private FundsTransactionMode transactionMode;

    @NotNull
    private DefaultFundsTransactionType transactionType;

    @NotNull
    private String businessScene;

    @NotNull
    private String businessSn;

    private String referenceTransactionSn;

    @NotNull
    private FundsTransactionStatus status;

    @NotNull
    private Long amount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private Long authorizedAmount;

    @NotNull
    private Long reversedAmount;

    @NotNull
    private Long settledAmount;

    @NotNull
    private Long refundedAmount;

    @NotNull
    private Long declinedAmount;

    @NotNull
    private Long feeAmount;

    private String routeSnapshot;

    private String description;

    private String contextVariables;

    @Column(version = true)
    private Integer version;
}
