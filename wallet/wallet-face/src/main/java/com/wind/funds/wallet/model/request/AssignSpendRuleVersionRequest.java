package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.SpendRuleConflictPolicy;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Spend Rule 版本挂载请求。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class AssignSpendRuleVersionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2079566949699936611L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "规则挂载流水号，用于幂等和审计追踪")
    @NotBlank
    private String assignmentSn;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "挂载范围类型")
    @NotNull
    private SpendRuleScopeType scopeType;

    @Schema(description = "挂载范围标识")
    @NotBlank
    private String scopeId;

    @Schema(description = "挂载优先级")
    @NotNull
    private Integer priority;

    @Schema(description = "挂载冲突策略")
    @NotNull
    private SpendRuleConflictPolicy conflictPolicy;

    @Schema(description = "挂载生效开始时间")
    @NotNull
    private LocalDateTime effectiveFrom;

    @Schema(description = "挂载生效结束时间")
    @NotNull
    private LocalDateTime effectiveTo;

    @Schema(description = "描述")
    private String description;
}
