package com.wind.funds.wallet.model.query;

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

    @Schema(description = "预算组或预算控制范围标识")
    @NotBlank
    private String budgetGroupSn;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;
}
