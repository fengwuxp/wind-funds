package com.wind.funds.transaction.dal.entities;

import com.wind.funds.transaction.enums.FundsFrozenOrderState;
import com.wind.funds.route.enums.FundsSubjectType;
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
     * 冻结订单流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 被冻结主体 ID。
     */
    @NotNull
    private String subjectId;

    /**
     * 被冻结主体类型。
     */
    @NotNull
    private FundsSubjectType subjectType;

    /**
     * 冻结类型。
     */
    @NotNull
    private String freezeType;

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
     * 关联资金交易流水号。
     */
    private String transactionSn;

    /**
     * 冻结交易明细流水号。
     */
    private String freezeDetailSn;

    /**
     * 冻结账本交易流水号。
     */
    private String freezeLedgerTransactionSn;

    /**
     * 冻结金额，单位：分。
     */
    @NotNull
    private Long amount;

    /**
     * 已释放金额，单位：分。
     */
    @NotNull
    private Long releasedAmount;

    /**
     * 冻结币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 冻结订单状态。
     */
    @NotNull
    @Column("status")
    private FundsFrozenOrderState state;

    void setStatus(FundsFrozenOrderState state) {
        this.state = state;
    }

    /**
     * 冻结过期时间。
     */
    private LocalDateTime expireTime;

    /**
     * 冻结释放完成时间。
     */
    private LocalDateTime releaseTime;

    /**
     * 冻结订单说明。
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
