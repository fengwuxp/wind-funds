package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.wallet.FundsAccountId;
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

    @Schema(description = "本次交易新增收费规则；手续费从 payerAccountId 对应 FundingAccount 的 AVAILABLE 扣取，与转账本金原子入账")
    private FeeSpec feeChargeSpec;

    @Schema(description = "业务流水号，转账凭证，例如：账户转账单流水号、内部调拨单流水号")
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
