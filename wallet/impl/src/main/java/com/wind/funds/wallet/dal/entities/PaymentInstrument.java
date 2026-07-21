package com.wind.funds.wallet.dal.entities;

import com.wind.funds.wallet.enums.PaymentInstrumentFlowDirection;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

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
@ToString(exclude = {"instrumentNo", "externalInstrumentId"})
@Table(PaymentInstrument.TABLE_NAME)
public class PaymentInstrument implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -4709316715914927935L;

    public static final String TABLE_NAME = "t_payment_instrument";

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
     * 支付工具流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 工具归属主体 ID。
     */
    @NotNull
    private String ownerId;

    /**
     * 工具归属主体类型。
     */
    @NotNull
    private FundsAccountOwnerType ownerType;

    /**
     * 支付工具类型。
     */
    @NotNull
    private String instrumentType;

    /**
     * 工具资金流向。
     */
    @NotNull
    @Column("instrument_direction")
    private PaymentInstrumentFlowDirection flowDirection;

    /**
     * 工具号码或账号标识。
     */
    @NotNull
    private String instrumentNo;

    /**
     * 工具所属渠道编码。
     */
    private String channelCode;

    /**
     * 外部渠道工具 ID。
     */
    private String externalInstrumentId;

    /**
     * 工具币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 支付工具状态。
     */
    @NotNull
    private FundsAccountStatus status;

    /**
     * 工具生效时间。
     */
    private LocalDateTime validFrom;

    /**
     * 工具失效时间。
     */
    private LocalDateTime validTo;

    /**
     * 工具说明。
     */
    private String description;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;
}
