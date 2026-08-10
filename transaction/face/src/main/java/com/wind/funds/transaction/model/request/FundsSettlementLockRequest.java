package com.wind.funds.transaction.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 结算资金锁定请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "结算资金锁定请求")
@Data
@Accessors(chain = true)
public class FundsSettlementLockRequest {

    @Schema(description = "结算资金账户标识")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "结算锁定金额")
    @NotNull
    private Money amount;

    @Schema(description = "归属结算单流水号")
    @NotBlank
    private String settlementOrderSn;

    @Schema(description = "结算锁定资金交易说明")
    private String description;
}
