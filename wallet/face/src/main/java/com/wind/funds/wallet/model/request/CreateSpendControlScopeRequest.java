package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
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
 * 创建支出控制范围请求。
 *
 * <p>目标语义为 Spend Rule 可引用的支出控制 scope，不是账务主体。</p>
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateSpendControlScopeRequest {

    @Schema(description = "支出控制范围标识，不表示账务主体")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支出控制范围归属主体 ID")
    @NotBlank
    private String ownerId;

    @Schema(description = "支出控制范围归属主体类型")
    @NotNull
    private FundsAccountOwnerType ownerType;

    @Schema(description = "支出控制范围业务类型")
    @NotBlank
    private String scopeType;

    @Schema(description = "控制币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "控制周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "周期标识，periodType 不为 LIFETIME 时必填")
    private String periodId;

    @Schema(description = "控制周期策略")
    private String periodPolicy;

    @Schema(description = "状态")
    private FundsAccountState state;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
