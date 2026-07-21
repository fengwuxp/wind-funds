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
public class CreateSpendRuleBindingRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -2079566949699936611L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "挂载范围类型。payment card 映射 PAYMENT_INSTRUMENT；"
            + "financial account 映射 FUNDING_ACCOUNT 或 CREDIT_ACCOUNT；"
            + "authorized user、cardholder、员工或账户层级映射 ACCOUNT_HIERARCHY；"
            + "card product 可按产品侧稳定场景映射 BUSINESS_SCENE。"
            + "scope 只表达控制范围，不是资金主体或账本主体。")
    @NotNull
    private SpendRuleScopeType scopeType;

    @Schema(description = "挂载范围标识。只能使用系统内稳定引用，"
            + "不得传入卡号、PAN、CVV、邮编、街道地址或外部账户敏感原文。")
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

    @Schema(description = "创建挂载的审计引用，参与业务幂等。必须来自上层业务事实或审批事实，不是外部请求流水号。")
    @NotBlank
    private String auditReferenceSn;

    @Schema(description = "描述")
    private String description;
}
