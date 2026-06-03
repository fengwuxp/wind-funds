package com.capte.funds.wallet.model.query;

import com.wind.funds.wallet.enums.PlatformFundingAccountRole;
import com.wind.funds.wallet.enums.FundsAccountOwnerType;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 真实资金账户查询条件。
 *
 * @author Codex
 * @date 2026-05-07
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundingAccountQuery {

    @Schema(description = "资金账户号")
    private String sn;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "账户归属主体 ID")
    private String ownerId;

    @Schema(description = "账户归属主体类型")
    private FundsAccountOwnerType ownerType;

    @Schema(description = "资金账户类型")
    private String accountType;

    @Schema(description = "平台账户角色")
    private PlatformFundingAccountRole accountRoleCode;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "状态")
    private FundsAccountStatus status;
}
