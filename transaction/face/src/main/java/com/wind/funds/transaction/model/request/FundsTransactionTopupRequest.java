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

    @Schema(description = "本次外部交易实际使用的 rail 编码，写入 route snapshot 的 externalAccountRef.channelCode")
    private String externalRailCode;

    @Schema(description = "渠道方交易流水号")
    @Size(max = 80)
    @NotNull
    private String channelTransactionSn;

    @Schema(description = "外部资金提供方或接入方编码，写入 route snapshot 的 externalAccountRef.providerCode")
    private String providerCode;

    @Schema(description = "可信上游适配层归一的外部资金事实来源命名空间；与 externalFundsFactSn 同时提供时启用外部资金事实级去重")
    @Size(max = 128)
    private String externalSourceCode;

    @Schema(description = "外部资金事实流水号；只表示一次外部资金变动，不替代业务流水号或渠道通知流水")
    @Size(max = 128)
    private String externalFundsFactSn;

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
