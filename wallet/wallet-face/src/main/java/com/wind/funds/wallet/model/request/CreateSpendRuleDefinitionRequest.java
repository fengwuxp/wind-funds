package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleType;
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
 * Spend Rule 定义创建请求。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateSpendRuleDefinitionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 2968324855220386122L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String ruleId;

    @Schema(description = "Spend Rule 名称")
    @NotBlank
    private String ruleName;

    @Schema(description = "Spend Rule 类型")
    @NotNull
    private SpendRuleType ruleType;

    @Schema(description = "Spend Rule 规则域")
    @NotNull
    private SpendRuleDomain ruleDomain;

    @Schema(description = "描述")
    private String description;
}
