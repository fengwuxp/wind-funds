package com.wind.funds.wallet.model.request;

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
 * Spend Rule 版本发布请求。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PublishSpendRuleVersionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5705789695698944994L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "规则规格 JSON")
    @NotBlank
    private String ruleSpec;

    @Schema(description = "规则规格摘要，用于版本不可变校验")
    @NotBlank
    private String ruleDigest;

    @Schema(description = "操作者")
    @NotBlank
    private String operatorId;

    @Schema(description = "审计引用")
    @NotBlank
    private String auditReferenceSn;

    @Schema(description = "描述")
    private String description;
}
