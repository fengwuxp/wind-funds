package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.enums.SpendRuleAssignmentStatus;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Spend Rule 挂载查询条件。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleAssignmentQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 4177807300369381796L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "规则挂载流水号")
    private String assignmentSn;

    @Schema(description = "Spend Rule 标识")
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    private String ruleVersion;

    @Schema(description = "挂载范围类型")
    private SpendRuleScopeType scopeType;

    @Schema(description = "挂载范围标识")
    private String scopeId;

    @Schema(description = "挂载状态")
    private SpendRuleAssignmentStatus status;

    @Schema(description = "是否只查询指定时间点有效的挂载")
    private Boolean effectiveOnly;

    @Schema(description = "有效性评估时间，不传使用当前时间")
    private LocalDateTime effectiveAt;
}
