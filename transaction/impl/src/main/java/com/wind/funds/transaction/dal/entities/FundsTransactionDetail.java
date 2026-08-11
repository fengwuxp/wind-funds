package com.wind.funds.transaction.dal.entities;

import com.wind.funds.transaction.enums.FundsEffectType;
import com.wind.funds.transaction.enums.FundsTransactionDetailState;
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
     * 交易明细流水号。
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 资金交易流水号。
     */
    @NotNull
    private String transactionSn;

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
     * 交易类型。
     */
    @NotNull
    private DefaultFundsTransactionType transactionType;

    /**
     * 交易事件类型。
     */
    @NotNull
    private FundsTransactionEventType eventType;

    /**
     * 交易参与主体 ID。
     */
    @NotNull
    private String subjectId;

    /**
     * 交易参与主体类型。
     */
    @NotNull
    private String subjectType;

    /**
     * 路由参与方角色。
     */
    @NotNull
    private RouteParticipantRole participantRole;

    /**
     * 请求幂等摘要。
     */
    @NotNull
    private String requestHash;

    /**
     * 资金影响类型。
     */
    @NotNull
    private FundsEffectType fundsEffectType;

    /**
     * 关联账本交易流水号。
     */
    private String ledgerTransactionSn;

    /**
     * 关联原交易明细流水号。
     */
    private String referenceDetailSn;

    /**
     * 关联原账本交易流水号。
     */
    private String referenceLedgerTransactionSn;

    /**
     * 明细金额，单位：分。
     */
    @NotNull
    private Long amount;

    /**
     * 明细币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 交易明细状态。
     */
    @NotNull
    @Column("status")
    private FundsTransactionDetailState state;

    void setStatus(FundsTransactionDetailState state) {
        this.state = state;
    }

    /**
     * 失败错误码。
     */
    private String errorCode;

    /**
     * 失败错误信息。
     */
    private String errorMessage;

    /**
     * 明细说明。
     */
    private String description;

    /**
     * 扩展上下文变量。
     */
    private String contextVariables;
}
