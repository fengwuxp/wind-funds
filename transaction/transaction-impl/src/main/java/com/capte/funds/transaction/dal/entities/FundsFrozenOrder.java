package com.capte.funds.transaction.dal.entities;

import com.capte.funds.transaction.enums.FundsFrozenOrderStatus;
import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资金冻结订单。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(FundsFrozenOrder.TABLE_NAME)
public class FundsFrozenOrder implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 2777635217409420157L;

    public static final String TABLE_NAME = "t_funds_frozen_order";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String subjectId;

    @NotNull
    private FundsSubjectType subjectType;

    @NotNull
    private String freezeType;

    @NotNull
    private String businessScene;

    @NotNull
    private String businessSn;

    private String transactionSn;

    private String freezeDetailSn;

    private String freezeLedgerTransactionSn;

    @NotNull
    private Long amount;

    @NotNull
    private Long releasedAmount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private FundsFrozenOrderStatus status;

    private LocalDateTime expireTime;

    private LocalDateTime releaseTime;

    private String description;

    private String contextVariables;

    @Column(version = true)
    private Integer version;
}
