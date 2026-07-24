package com.wind.funds.transaction.dal.entities;

import com.wind.funds.transaction.enums.FundsTransactionMode;
import com.wind.funds.transaction.enums.FundsTransactionStatus;
import com.wind.funds.transaction.enums.FundsEffectType;
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
     * 资金交易流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 交易模式。
     */
    @NotNull
    private FundsTransactionMode transactionMode;

    /**
     * 交易类型。
     */
    @NotNull
    private DefaultFundsTransactionType transactionType;

    /**
     * 业务场景。
     */
    @NotNull
    private String businessScene;

    /**
     * 业务流水号。
     */
    @NotNull
    private String businessSn;

    /**
     * 外部资金事实来源编码。
     */
    private String externalSourceCode;

    /**
     * 外部资金事实流水号。
     */
    private String externalFundsFactSn;

    /**
     * 外部资金事实作用类型。
     */
    private FundsEffectType externalFundsEffectType;

    /**
     * 外部资金事实不可变载荷摘要。
     */
    private String externalFundsFactDigest;

    /**
     * 关联原交易流水号。
     */
    private String referenceTransactionSn;

    /**
     * 资金交易状态。
     */
    @NotNull
    private FundsTransactionStatus status;

    /**
     * 交易请求金额，单位：分。
     */
    @NotNull
    private Long amount;

    /**
     * 交易币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 已授权金额，单位：分。
     */
    @NotNull
    private Long authorizedAmount;

    /**
     * 已撤销金额，单位：分。
     */
    @NotNull
    private Long reversedAmount;

    /**
     * 已完成金额，单位：分。
     */
    @NotNull
    private Long completedAmount;

    /**
     * 已退款金额，单位：分。
     */
    @NotNull
    private Long refundedAmount;

    /**
     * 已拒绝金额，单位：分。
     */
    @NotNull
    private Long declinedAmount;

    /**
     * 手续费金额，单位：分。
     */
    @NotNull
    private Long feeAmount;

    /**
     * 路由快照 JSON。
     */
    private String routeSnapshot;

    /**
     * 交易说明。
     */
    private String description;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;

    /**
     * 乐观锁版本号。
     */
    @Column(version = true)
    private Integer version;
}
