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

/**
 * Spend Rule 决策解释查询条件。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleDecisionExplainQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 8451297182934804240L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "规则决策流水号")
    @NotBlank
    private String decisionSn;
}
