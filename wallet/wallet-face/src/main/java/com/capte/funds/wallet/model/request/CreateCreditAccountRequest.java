package com.capte.funds.wallet.model.request;

import com.wind.integration.funds.ledger.enums.LedgerProfileCode;
import com.wind.integration.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.integration.funds.wallet.enums.FundsAccountStatus;
import com.wind.integration.funds.ledger.enums.AccountBalancePeriodType;
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
 * 创建信用账户请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateCreditAccountRequest {

    @Schema(description = "信用账户号")
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

    @Schema(description = "信用账户类型")
    @NotBlank
    private String accountType;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "账本周期类型")
    private AccountBalancePeriodType periodType;

    @Schema(description = "ledger profile 编码")
    private LedgerProfileCode ledgerProfileCode;

    @Schema(description = "状态")
    private FundsAccountStatus status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
