package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 预算控制范围查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class BudgetGroupQuery {

    @Schema(description = "预算控制范围标识，兼容字段名为预算组号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "预算控制范围归属主体 ID")
    private String ownerId;

    @Schema(description = "预算控制范围归属主体类型")
    private FundsAccountOwnerType ownerType;

    @Schema(description = "预算控制范围业务类型")
    private String budgetType;

    @Schema(description = "控制币种")
    private CurrencyIsoCode currency;

    @Schema(description = "控制周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "控制周期标识")
    private String periodId;

    @Schema(description = "状态")
    private FundsAccountStatus status;
}
