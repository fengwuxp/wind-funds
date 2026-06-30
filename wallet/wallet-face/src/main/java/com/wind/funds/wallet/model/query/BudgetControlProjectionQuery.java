package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 预算控制投影查询条件。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class BudgetControlProjectionQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = -4567049496355767574L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "控制范围标识，目标语义名；当前兼容映射到历史字段 budgetGroupSn")
    private String controlScopeId;

    @Schema(description = "预算控制范围标识，历史字段名 budgetGroupSn，不表示账务主体")
    private String budgetGroupSn;

    @Schema(description = "控制周期标识，例如 2026-07；用于周期额度当前与历史查询")
    private String periodId;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "控制额度变动目标资金账户或信用账户标识，不传时按预算控制范围聚合")
    private FundsAccountId targetAccountId;
}
