package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.SpendRuleVersionState;
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
 * Spend Rule 版本 DTO。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleVersionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3890268747769865735L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "Spend Rule 标识")
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    private String ruleVersion;

    @Schema(description = "规则规格 JSON")
    private String ruleSpec;

    @Schema(description = "规则规格摘要")
    private String ruleDigest;

    @Schema(description = "版本状态")
    private SpendRuleVersionState state;

    @Schema(description = "操作者")
    private String operatorId;

    @Schema(description = "审计引用")
    private String auditReferenceSn;

    @Schema(description = "描述")
    private String description;
}
