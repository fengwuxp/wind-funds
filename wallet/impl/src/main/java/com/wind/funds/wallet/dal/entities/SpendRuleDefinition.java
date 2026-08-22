package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.wallet.enums.SpendRuleDefinitionState;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleType;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 定义。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@Table(SpendRuleDefinition.TABLE_NAME)
public class SpendRuleDefinition implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = 432277269348430464L;

    public static final String TABLE_NAME = "t_spend_rule_definition";

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
     * 租户 ID。
     */
    @Column(tenantId = true)
    private Long tenantId;

    /**
     * Spend Rule 标识。
     */
    @NotNull
    private String ruleId;

    /**
     * Spend Rule 名称。
     */
    @NotNull
    private String ruleName;

    /**
     * Spend Rule 类型。
     */
    @NotNull
    private SpendRuleType ruleType;

    /**
     * Spend Rule 规则域。
     */
    @NotNull
    private SpendRuleDomain ruleDomain;

    /**
     * 规则定义状态。
     */
    @NotNull
    @Column("status")
    private SpendRuleDefinitionState state;

    /**
     * 规则定义说明。
     */
    private String description;
}
