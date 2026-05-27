package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 钱包交易手续费扣款请求
 *
 * @author wuxp
 * @date 2026-04-21 10:47
 **/
@Data
@Accessors(chain = true)
public class FundsTransactionFeeRequest {

    @Schema(description = "支出账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "手续费金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务流水号，手续费收取凭证，例如：手续费单流水号、小额交易手续费流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "手续费类型")
    @NotNull
    private String feeType;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;

    public FundsTransactionFeeRequest setContextVariables(WritableContextVariables contextVariables) {
        this.contextVariables = FundsRequestContextVariables.snapshot(contextVariables);
        return this;
    }
}
