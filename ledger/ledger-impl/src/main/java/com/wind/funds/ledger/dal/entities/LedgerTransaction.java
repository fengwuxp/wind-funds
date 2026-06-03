package com.wind.funds.ledger.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.funds.ledger.enums.LedgerTransactionStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户账本交易
 *
 * @author wuxp
 * @date 2026-04-14 11:28
 **/
@Table(LedgerTransaction.TABLE_NAME)
@Data
@FieldNameConstants
public class LedgerTransaction implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -4011514385689532439L;

    public static final String TABLE_NAME = "t_ledger_transaction";

    @Id(keyType = KeyType.Auto)
    @NotNull
    private Long id;

    @NotNull
    private LocalDateTime gmtCreate;

    @NotNull
    private LocalDateTime gmtModified;

    /**
     * 账户ID
     */
    @NotNull
    @Size(min = 12, max = 50)
    private String sn;

    /**
     * 租户ID
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 事件类型
     */
    @NotNull
    private String eventType;

    /**
     * 业务交易号
     */
    private String fundsTransactionSn;

    /**
     * 指令类型
     */
    private String instructionType;

    /**
     * 交易类型
     */
    private String transactionType;

    /**
     * 交易状态
     */
    @NotNull
    private LedgerTransactionStatus status;

    /**
     * 交易金额，单位：分
     */
    @NotNull
    private Long amount;

    /**
     * 交易币种
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 原始金额，单位：分
     */
    private Long originalAmount;

    /**
     * 原始币种
     */
    private CurrencyIsoCode originalCurrency;

    /**
     * 汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 借方合计，单位：分
     */
    private Long debitAmount;

    /**
     * 贷方合计，单位：分
     */
    private Long creditAmount;

    /**
     * 是否借贷平衡
     */
    @Column("is_balanced")
    private Boolean balanced;

    /**
     * 业务单号
     */
    @Size(min = 10, max = 80)
    private String businessSn;

    /**
     * 业务场景
     */
    @NotNull
    private String businessScene;

    /**
     * 关联账本交易单号
     */
    private String referenceLedgerTransactionSn;

    /**
     * 交易时间
     */
    @NotNull
    private LocalDateTime transactionTime;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 上下文变量
     */
    @Column("context_variables")
    private String contextVariables;

    /**
     * sha256，防止数据篡改
     */
    private String sha256;
}
