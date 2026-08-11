package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.SpendRuleDefinitionState;
import com.wind.funds.wallet.enums.SpendRuleDomain;
import com.wind.funds.wallet.enums.SpendRuleType;
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
 * Spend Rule 定义 DTO。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleDefinitionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3899578258144274270L;

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

    @Schema(description = "Spend Rule 名称")
    private String ruleName;

    @Schema(description = "Spend Rule 类型")
    private SpendRuleType ruleType;

    @Schema(description = "Spend Rule 规则域")
    private SpendRuleDomain ruleDomain;

    @Schema(description = "规则状态")
    private SpendRuleDefinitionState state;

    @Schema(description = "描述")
    private String description;
}
