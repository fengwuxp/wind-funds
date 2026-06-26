package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付工具绑定并发保护行。
 *
 * @author Codex
 * @date 2026-06-25
 */
@Data
@Table(PaymentInstrumentBindingGuard.TABLE_NAME)
public class PaymentInstrumentBindingGuard implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 2296732734861553961L;

    public static final String TABLE_NAME = "t_payment_instrument_binding_guard";

    /**
     * 自增主键。
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 创建时间。
     */
    private LocalDateTime gmtCreate;

    /**
     * 最后修改时间。
     */
    private LocalDateTime gmtModified;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 支付工具流水号。
     */
    @NotNull
    private String instrumentSn;

    /**
     * 绑定角色。
     */
    @NotNull
    private PaymentInstrumentBindingRole bindingRole;

    /**
     * 绑定币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 保护类型。
     */
    @NotNull
    private String guardType;
}
