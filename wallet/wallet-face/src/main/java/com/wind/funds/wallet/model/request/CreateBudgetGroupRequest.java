package com.wind.funds.wallet.model.request;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.funds.ledger.enums.AccountBalancePeriodType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 创建预算组请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateBudgetGroupRequest {

    @Schema(description = "预算组号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "归属主体 ID")
    @NotBlank
    private String ownerId;

    @Schema(description = "归属主体类型")
    @NotNull
    private FundsAccountOwnerType ownerType;

    @Schema(description = "预算类型")
    @NotBlank
    private String budgetType;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "周期标识，periodType 不为 LIFETIME 时必填")
    private String periodId;

    @Schema(description = "周期策略")
    private String periodPolicy;

    @Schema(description = "ledger profile 编码")
    private LedgerProfileCode ledgerProfileCode;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
