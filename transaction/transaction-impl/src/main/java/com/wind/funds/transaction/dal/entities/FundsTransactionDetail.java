package com.wind.funds.transaction.dal.entities;

import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionDetailStatus;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.route.enums.RouteParticipantRole;
import com.wind.funds.transaction.enums.DefaultFundsTransactionType;
import com.wind.funds.transaction.enums.FundsTransactionEventType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标准资金交易生命周期明细。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(FundsTransactionDetail.TABLE_NAME)
public class FundsTransactionDetail implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 875722632137901821L;

    public static final String TABLE_NAME = "t_funds_transaction_detail";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String transactionSn;

    @NotNull
    private String businessScene;

    @NotNull
    private String businessSn;

    @NotNull
    private DefaultFundsTransactionType transactionType;

    @NotNull
    private FundsTransactionEventType eventType;

    @NotNull
    private String subjectId;

    @NotNull
    private String subjectType;

    @NotNull
    private RouteParticipantRole participantRole;

    @NotNull
    private String requestHash;

    @NotNull
    private FundsEffectType fundsEffectType;

    private String ledgerTransactionSn;

    private String referenceDetailSn;

    private String referenceLedgerTransactionSn;

    @NotNull
    private Long amount;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private FundsTransactionDetailStatus status;

    private String errorCode;

    private String errorMessage;

    private String description;

    private String contextVariables;
}
