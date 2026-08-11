package com.wind.funds.wallet.model.request;

import com.wind.funds.ledger.enums.LedgerProfileCode;
import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountState;
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
 * 创建真实资金账户请求。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CreateFundingAccountRequest {

    @Schema(description = "资金账户号")
    @NotBlank
    private String sn;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "账户归属主体 ID")
    @NotBlank
    private String ownerId;

    @Schema(description = "账户归属主体类型")
    @NotNull
    private FundsAccountOwnerType ownerType;

    @Schema(description = "资金账户类型")
    @NotBlank
    private String accountType;

    @Schema(description = "是否平台账户")
    private Boolean platform;

    @Schema(description = "平台账户角色")
    private PlatformFundingAccountRole accountRoleCode;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "ledger profile 编码")
    private LedgerProfileCode ledgerProfileCode;

    @Schema(description = "状态")
    private FundsAccountState state;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "扩展上下文")
    private String contextVariables;
}
