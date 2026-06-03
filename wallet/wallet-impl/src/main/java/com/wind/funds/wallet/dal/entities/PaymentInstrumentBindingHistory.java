package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingChangeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付工具绑定历史。
 *
 * @author Codex
 * @date 2026-05-20
 */
@Data
@Table(PaymentInstrumentBindingHistory.TABLE_NAME)
public class PaymentInstrumentBindingHistory implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 2814961090494978615L;

    public static final String TABLE_NAME = "t_payment_instrument_binding_history";

    @Id(keyType = KeyType.Auto)
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    @NotNull
    private String sn;

    @Column(tenantId = true)
    private Long tenantId;

    @NotNull
    private String bindingSn;

    @NotNull
    private String instrumentSn;

    @NotNull
    private PaymentInstrumentBindingChangeType changeType;

    @NotNull
    private Integer version;

    private String beforeSnapshot;

    @NotNull
    private String afterSnapshot;

    @NotNull
    private String operatorId;

    @NotNull
    private String changeReason;

    private LocalDateTime effectiveAt;

    private String requestSn;

    private String contextVariables;
}
