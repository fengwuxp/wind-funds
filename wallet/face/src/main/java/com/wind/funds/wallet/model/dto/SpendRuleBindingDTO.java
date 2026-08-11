package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleBindingState;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 挂载 DTO。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleBindingDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3621330559126513154L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "规则挂载流水号")
    private String sn;

    @Schema(description = "Spend Rule 标识")
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    private String ruleVersion;

    @Schema(description = "挂载范围类型")
    private SpendRuleScopeType scopeType;

    @Schema(description = "挂载范围标识")
    private String scopeId;

    @Schema(description = "挂载优先级")
    private Integer priority;

    @Schema(description = "挂载冲突策略")
    private SpendRuleConflictPolicy conflictPolicy;

    @Schema(description = "挂载生效开始时间")
    private LocalDateTime effectiveFrom;

    @Schema(description = "挂载生效结束时间")
    private LocalDateTime effectiveTo;

    @Schema(description = "挂载状态")
    private SpendRuleBindingState state;

    @Schema(description = "创建挂载的审计引用")
    private String auditReferenceSn;

    @Schema(description = "描述")
    private String description;
}
