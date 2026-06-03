package com.capte.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 钱包账户提现请求
 *
 * @author wuxp
 * @date 2026-04-21 13:37
 **/
@Data
@Accessors(chain = true)
public class FundsTransactionWithdrawRequest {

    @Schema(description = "提现账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "收款账户（外部账户）")
    @NotNull
    private FundsAccountId payeeId;

    @Schema(description = "提现冻结流水号")
    @NotNull
    private String referenceFreezeSn;

    @Schema(description = "提现交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易显式手续费规则")
    private FeeSpec feeSpec;

    @Schema(description = "业务流水号，提现凭证，例如：提现申请单流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
