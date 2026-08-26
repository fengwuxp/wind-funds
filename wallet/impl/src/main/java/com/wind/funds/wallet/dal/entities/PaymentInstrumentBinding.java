package com.wind.funds.wallet.dal.entities;

import com.wind.funds.route.enums.FundsSubjectType;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingState;
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
 * 支付工具和资金主体绑定关系。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(PaymentInstrumentBinding.TABLE_NAME)
@FieldNameConstants
public class PaymentInstrumentBinding implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -3840825799484566395L;

    public static final String TABLE_NAME = "t_payment_instrument_binding";

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
     * 绑定关系流水号。
     */
    @NotNull
    private String sn;

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
     * 绑定资金主体 ID。
     */
    @NotNull
    private String subjectId;

    /**
     * 绑定资金主体类型。
     */
    @NotNull
    private FundsSubjectType subjectType;

    /**
     * 绑定币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 同角色绑定优先级。
     */
    @NotNull
    private Integer priority;

    /**
     * 是否默认绑定。
     */
    @NotNull
    @Column("is_default")
    private Boolean defaultBinding;

    /**
     * 绑定生命周期状态。
     */
    @NotNull
    private PaymentInstrumentBindingState state;

    /**
     * 绑定版本号。
     */
    @NotNull
    private Integer version;

    /**
     * 绑定生效时间。
     */
    private LocalDateTime validFrom;

    /**
     * 绑定失效时间。
     */
    private LocalDateTime validTo;

    /**
     * 绑定说明。
     */
    private String description;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;
}
