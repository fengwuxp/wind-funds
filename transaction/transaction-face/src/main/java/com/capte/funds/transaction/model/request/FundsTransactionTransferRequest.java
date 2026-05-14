package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 资金交易转账请求
 *
 * @author wuxp
 * @date 2026-04-23 09:02
 **/
@Schema(description = "资金交易转账请求")
@Data
@Accessors(chain = true)
public class FundsTransactionTransferRequest {

    @Schema(description = "出款账户 id")
    @NotNull
    private FundsAccountId payerAccountId;

    @Schema(description = "收款账户 id")
    @NotNull
    private FundsAccountId payeeAccountId;

    @Schema(description = "充值金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务流水号，入账凭证，例如：在线充值流水号，人工入账单流水号、退款单交易流水号")
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
