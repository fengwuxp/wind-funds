package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 资金交易手续费退回请求。
 *
 * <p>{@link #transactionAmount} 表达本次退回金额及原手续费事实使用的汇率快照，不触发实时询价。
 * 同币种手续费使用 {@link TransactionAmount#sameCurrency(com.wind.transaction.core.Money)} 构造。</p>
 */
@Data
@Accessors(chain = true)
public class FundsTransactionFeeRefundRequest {

    @Schema(description = "手续费退回到账账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "退回手续费交易金额，跨币种时复用原手续费事实的汇率快照")
    @NotNull
    private TransactionAmount transactionAmount;

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
    private ReadonlyContextVariables contextVariables;
}
