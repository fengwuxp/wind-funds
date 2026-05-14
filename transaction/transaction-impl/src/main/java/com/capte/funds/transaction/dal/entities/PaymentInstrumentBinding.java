package com.capte.funds.transaction.dal.entities;

import com.wind.integration.funds.route.enums.FundsSubjectType;
import com.capte.funds.transaction.enums.PaymentInstrumentBindingRole;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付工具和资金主体绑定关系。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(PaymentInstrumentBinding.TABLE_NAME)
public class PaymentInstrumentBinding implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -3840825799484566395L;

    public static final String TABLE_NAME = "t_payment_instrument_binding";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String instrumentSn;

    @NotNull
    private PaymentInstrumentBindingRole bindingRole;

    @NotNull
    private String subjectId;

    @NotNull
    private FundsSubjectType subjectType;

    @NotNull
    private CurrencyIsoCode currency;

    @NotNull
    private Integer priority;

    @NotNull
    @Column("is_default")
    private Boolean defaultBinding;

    @NotNull
    private FundsAccountStatus status;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private String description;

    private String contextVariables;
}
