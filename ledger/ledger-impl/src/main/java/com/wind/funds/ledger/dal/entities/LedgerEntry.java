package com.wind.funds.ledger.dal.entities;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.funds.ledger.enums.LedgerSubjectCategory;
import com.wind.funds.ledger.enums.EntrySide;
import com.wind.funds.ledger.enums.LedgerReconcileStatus;
import com.wind.funds.ledger.enums.LedgerSettlementStatus;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户账本条目
 *
 * @author wuxp
 * @date 2026-04-14 13:07
 **/
@Data
@Table(LedgerEntry.TABLE_NAME)
@FieldNameConstants
public class LedgerEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = -4018512383389532839L;

    public static final String TABLE_NAME = "t_ledger_entry";

    /**
     * 自增主键
     */
    @Id(keyType = KeyType.Auto)
    @NotNull
    private Long id;

    /**
     * 创建时间
     */
    @NotNull
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    @NotNull
    private LocalDateTime gmtModified;

    /**
     * 分录号
     */
    @NotNull
    private String sn;

    /**
     * 租户 ID
     */
    @NotNull
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 账本交易流水号
     */
    @NotNull
    private String ledgerTransactionSn;

    /**
     * 记账计划流水号
     */
    private String postingPlanSn;

    /**
     * 业务交易号
     */
    private String fundsTransactionSn;

    /**
     * 对应 t_ledger.id
     */
    private Long ledgerId;

    /**
     * 账本周期类型快照
     */
    @NotNull
    private AccountBalancePeriodType periodType;

    /**
     * 账本周期 ID 快照
     */
    @NotNull
    private String periodId;

    /**
     * 账务主体 ID
     */
    @NotNull
    private String subjectId;

    /**
     * 账务主体类型
     */
    @NotNull
    private String subjectType;

    /**
     * 账目编码（会计科目编码）
     * 例如：CASH（现金）、AVAILABLE （余额）、AUTHORIZATION（授权）
     */
    @NotNull
    private LedgerSubjectCode ledgerSubjectCode;

    /**
     * 会计科目类别
     */
    @NotNull
    private LedgerSubjectCategory ledgerSubjectCategory;

    /**
     * 账本分录类型（借贷）
     */
    @NotNull
    @Column("entry_side")
    private EntrySide entrySide;

    /**
     * 本分录余额约束
     */
    private String balanceConstraintType;

    /**
     * 记账意图
     */
    private String intent;

    /**
     * 记账范围
     */
    private String postingScope;

    /**
     * 余额影响语义
     */
    private String balanceEffectType;

    /**
     * 记账阶段
     */
    private String phaseCode;

    /**
     * 业务场景
     */
    @NotNull
    private String businessScene;

    /**
     * 业务 sn（订单号、业务交易流水）
     */
    private String businessSn;

    /**
     * 记账金额，单位：分
     */
    @NotNull
    private Long amount;

    /**
     * 币种
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 原始金额，单位：分
     */
    @NotNull
    private Long originalAmount;

    /**
     * 原始金额币种
     */
    @NotNull
    private CurrencyIsoCode originalCurrency;

    /**
     * 汇率
     */
    @NotNull
    private BigDecimal exchangeRate;

    /**
     * 交易时间
     */
    @NotNull
    private LocalDateTime transactionTime;

    /**
     * 描述
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
    @NotNull
    private String sha256;

    /**
     * 结算状态
     */
    @NotNull
    private LedgerSettlementStatus settlementStatus;

    /**
     * 结算周期（账期）
     */
    private String settlementPeriod;

    /**
     * 结算完成时间
     */
    private LocalDateTime settlementCompletedTime;

    /**
     * 对账状态
     */
    @NotNull
    private LedgerReconcileStatus reconcileStatus;

    /**
     * 对账备注
     */
    private String reconcileRemark;

    /**
     * 对账批次（示例：2026040900015）
     */
    private String reconciliationBatch;

    /**
     * 对账完成时间
     */
    private LocalDateTime reconciliationCompletedTime;
}
