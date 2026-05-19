package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
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

    @Schema(description = "转账交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易显式手续费规则")
    private FeeSpec feeSpec;

    @Schema(description = "业务流水号，转账凭证，例如：账户转账单流水号、内部调拨单流水号")
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
