package com.capte.funds.transaction.model.request;


import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 资金交易充值请求
 *
 * @author wuxp
 * @date 2026-04-21 08:57
 **/
@Data
@Accessors(chain = true)
public class FundsTransactionTopupRequest {

    @Schema(description = "充值入账账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "资金来源账户（外部账户）")
    @NotNull
    private FundsAccountId fundsSourceAccountId;

    @Schema(description = "渠道")
    @NotNull
    private FundsTransactionChannel channel;

    @Schema(description = "渠道方交易流水号")
    @Size(max = 80)
    @NotNull
    private String channelTransactionSn;

    @Schema(description = "渠道方标识")
    private String channelId;

    @Schema(description = "充值交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易显式手续费规则")
    private FeeSpec feeSpec;

    @Schema(description = "业务流水号，充值入账凭证，例如：在线充值流水号、人工入账单流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;

    public FundsTransactionTopupRequest setContextVariables(WritableContextVariables contextVariables) {
        this.contextVariables = FundsRequestContextVariables.snapshot(contextVariables);
        return this;
    }

}
