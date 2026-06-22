package com.wind.funds.wallet.dal.entities;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.integration.core.model.TenantIsolationObject;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 挂载。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@Table(SpendRuleAssignment.TABLE_NAME)
public class SpendRuleAssignment implements Serializable, TenantIsolationObject<Long> {

    @Serial
    private static final long serialVersionUID = -8956553106315399779L;

    public static final String TABLE_NAME = "t_spend_rule_assignment";

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
     * 规则挂载流水号。
     */
    @NotNull
    private String assignmentSn;

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
     * 挂载范围类型。
     */
    @NotNull
    private SpendRuleScopeType scopeType;

    /**
     * 挂载范围标识。
     */
    @NotNull
    private String scopeId;

    /**
     * 挂载优先级。
     */
    @NotNull
    private Integer priority;

    /**
     * 挂载状态。
     */
    @NotNull
    private SpendRuleAssignmentStatus status;

    /**
     * 挂载说明。
     */
    private String description;
}
