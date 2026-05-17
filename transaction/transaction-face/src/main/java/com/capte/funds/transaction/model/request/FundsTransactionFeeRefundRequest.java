package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 资金交易手续费退回请求。
 */
@Data
@Accessors(chain = true)
public class FundsTransactionFeeRefundRequest {

    @Schema(description = "手续费退回到账账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "退回手续费金额")
    @NotNull
    private Money amount;

    @Schema(description = "原手续费事实所在资金交易流水号")
    @NotNull
    private String feeSourceTransactionSn;

    @Schema(description = "业务流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;
}
