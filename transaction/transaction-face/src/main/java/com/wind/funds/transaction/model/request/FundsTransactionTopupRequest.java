package com.wind.funds.transaction.model.request;


import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.route.ref.PaymentInstrumentRefSpec;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.wallet.FundsAccountId;
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

    @Schema(description = "渠道方或外部 rail 标识，写入 route snapshot 的 externalAccountRef.providerCode")
    private String channelId;

    @Schema(description = "充值交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易新增收费规则；手续费从充值到账 FundingAccount 的 AVAILABLE 扣取，与充值本金原子入账")
    private FeeSpec feeChargeSpec;

    @Schema(description = "触发本次充值的支付工具引用快照，仅用于路由追溯和投影解释，不作为账务主体")
    private PaymentInstrumentRefSpec paymentInstrumentRef;

    @Schema(description = "业务流水号，充值入账凭证，例如：在线充值流水号、人工入账单流水号")
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
