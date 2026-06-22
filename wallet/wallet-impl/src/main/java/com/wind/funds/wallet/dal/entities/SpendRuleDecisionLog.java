package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.integration.core.model.TenantIsolationObject;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 决策日志。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@Table(SpendRuleDecisionLog.TABLE_NAME)
public class SpendRuleDecisionLog implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 3126579531735774083L;

    public static final String TABLE_NAME = "t_spend_rule_decision_log";

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
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * 规则决策流水号。
     */
    @NotNull
    private String decisionSn;

    /**
     * Spend Rule 标识。
     */
    @NotNull
    private String ruleId;

    /**
     * Spend Rule 版本。
     */
    @NotNull
    private String ruleVersion;

    /**
     * 规则挂载流水号。
     */
    private String assignmentSn;

    /**
     * 控制范围类型。
     */
    @NotNull
    private SpendRuleScopeType scopeType;

    /**
     * 控制范围标识。
     */
    @NotNull
    private String scopeId;

    /**
     * 支付工具号。
     */
    private String instrumentSn;

    /**
     * 支付工具动作。
     */
    @NotNull
    private PaymentInstrumentAction action;

    /**
     * 交易金额，最小货币单位。
     */
    @NotNull
    private Long amount;

    /**
     * 币种。
     */
    @NotNull
    private CurrencyIsoCode currency;

    /**
     * 业务场景。
     */
    @NotNull
    private String businessScene;

    /**
     * 业务流水号或请求幂等号。
     */
    @NotNull
    private String businessSn;

    /**
     * 规则决策结果。
     */
    @NotNull
    private SpendControlDecisionResult decisionResult;

    /**
     * 拒绝原因。
     */
    private String rejectReason;

    /**
     * 规则决策摘要。
     */
    @NotNull
    private String decisionDigest;
}
