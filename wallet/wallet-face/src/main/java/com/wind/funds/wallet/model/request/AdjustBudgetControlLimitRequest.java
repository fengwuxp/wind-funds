package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
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
 * 预算控制额度调整请求。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class AdjustBudgetControlLimitRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -5677038236698716798L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "预算控制额度调整变动流水号，用于幂等、回放和审计追踪")
    @NotBlank
    private String movementSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "预算控制范围标识，历史字段名 budgetGroupSn，不表示账务主体")
    private String budgetGroupSn;

    @Schema(description = "控制范围标识，目标语义名；当前兼容映射到历史字段 budgetGroupSn")
    private String controlScopeId;

    @Schema(description = "控制周期标识，例如 2026-07；用于周期额度刷新和历史追溯")
    private String periodId;

    @Schema(description = "预算控制额度影响的资金账户或信用账户标识")
    @NotNull
    private FundsAccountId targetAccountId;

    @Schema(description = "调整金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String spendRuleVersion;

    @Schema(description = "是否调增，true 表示调增，false 表示调减")
    @NotNull
    private Boolean increase;

    @Schema(description = "调整原因码")
    @NotBlank
    private String reasonCode;

    @Schema(description = "审批、凭证、规则发布或外部审计引用")
    @NotBlank
    private String auditReferenceSn;

    @Schema(description = "控制额度变动摘要，用于幂等、回放和审计追踪")
    @NotBlank
    private String movementDigest;

    @Schema(description = "调整说明")
    private String description;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}
