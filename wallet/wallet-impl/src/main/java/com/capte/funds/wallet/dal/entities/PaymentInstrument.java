package com.capte.funds.wallet.dal.entities;

import com.wind.integration.funds.wallet.enums.PaymentInstrumentDirection;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付或收款工具。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(PaymentInstrument.TABLE_NAME)
public class PaymentInstrument implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -4709316715914927935L;

    public static final String TABLE_NAME = "t_payment_instrument";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String ownerId;

    @NotNull
    private FundsAccountOwnerType ownerType;

    @NotNull
    private String instrumentType;

    @NotNull
    private PaymentInstrumentDirection instrumentDirection;

    @NotNull
    private String instrumentNo;

    private String channelCode;

    private String externalInstrumentId;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private FundsAccountStatus status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String description;

    private String contextVariables;
}
