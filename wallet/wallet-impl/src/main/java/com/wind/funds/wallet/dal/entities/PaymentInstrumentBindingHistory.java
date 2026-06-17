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
     * 绑定历史流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 绑定关系流水号。
     */
    @NotNull
    private String bindingSn;

    /**
     * 支付工具流水号。
     */
    @NotNull
    private String instrumentSn;

    /**
     * 绑定变更类型。
     */
    @NotNull
    private PaymentInstrumentBindingChangeType changeType;

    /**
     * 变更后的绑定版本号。
     */
    @NotNull
    private Integer version;

    /**
     * 变更前快照。
     */
    private String beforeSnapshot;

    /**
     * 变更后快照。
     */
    @NotNull
    private String afterSnapshot;

    /**
     * 操作人 ID。
     */
    @NotNull
    private String operatorId;

    /**
     * 变更原因。
     */
    @NotNull
    private String changeReason;

    /**
     * 变更生效时间。
     */
    private LocalDateTime effectiveAt;

    /**
     * 幂等请求流水号。
     */
    private String requestSn;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;
}
