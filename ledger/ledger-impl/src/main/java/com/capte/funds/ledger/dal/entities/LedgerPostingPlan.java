package com.capte.funds.ledger.dal.entities;

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
 * 账本记账计划。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@Table(LedgerPostingPlan.TABLE_NAME)
@FieldNameConstants
public class LedgerPostingPlan implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 1845684579157582661L;

    public static final String TABLE_NAME = "t_ledger_posting_plan";

    /**
     * 主键
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
     * 记账计划号
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
     * 账本交易号
     */
    @NotNull
    private String ledgerTransactionSn;

    /**
     * 业务交易号
     */
    private String fundsTransactionSn;

    /**
     * 来源 route leg
     */
    private String routeLegId;

    /**
     * 记账意图
     */
    @NotNull
    private String intent;

    /**
     * 记账范围
     */
    @NotNull
    private String postingScope;

    /**
     * 余额影响语义
     */
    @NotNull
    private String balanceEffectType;

    /**
     * 记账阶段
     */
    @NotNull
    private String phaseCode;

    /**
     * 计划金额
     */
    @NotNull
    private Long amount;

    /**
     * 币种
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 借方合计
     */
    @NotNull
    private Long debitAmount;

    /**
     * 贷方合计
     */
    @NotNull
    private Long creditAmount;

    /**
     * 是否借贷平衡
     */
    @NotNull
    @Column("is_balanced")
    private Boolean balanced;

    /**
     * 描述
     */
    private String description;

    /**
     * 扩展上下文
     */
    private String contextVariables;

    /**
     * 防篡改摘要
     */
    @NotNull
    private String sha256;
}
