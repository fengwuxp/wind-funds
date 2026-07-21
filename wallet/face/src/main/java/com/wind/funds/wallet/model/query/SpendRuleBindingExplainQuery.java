package com.wind.funds.wallet.model.query;

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
 * Spend Rule 挂载解释查询条件。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleBindingExplainQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1554872230993303283L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "规则挂载流水号")
    @NotBlank
    private String sn;

    @Schema(description = "解释评估时间，不传使用当前时间")
    private LocalDateTime explainAt;
}
