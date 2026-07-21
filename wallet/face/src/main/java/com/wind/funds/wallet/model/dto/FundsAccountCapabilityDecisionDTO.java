package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.FundsAccountCapability;
import com.wind.funds.wallet.enums.FundsAccountStatus;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 资金账户能力准入决策 DTO。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class FundsAccountCapabilityDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -1379483235982622608L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "资金账户或信用账户标识")
    private FundsAccountId accountId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "账户状态")
    private FundsAccountStatus status;

    @Schema(description = "当前账户能力集合")
    private Set<FundsAccountCapability> capabilities;

    @Schema(description = "是否允许收款")
    private Boolean canReceive;

    @Schema(description = "是否允许付款")
    private Boolean canPay;

    @Schema(description = "是否允许提现")
    private Boolean canWithdraw;

    @Schema(description = "能力来源，例如 LEDGER_PROFILE、CONTEXT_VARIABLES")
    private String capabilitySource;
}
