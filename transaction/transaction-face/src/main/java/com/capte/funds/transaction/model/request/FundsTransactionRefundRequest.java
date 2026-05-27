package com.capte.funds.transaction.model.request;

import com.capte.funds.transaction.enums.FundsTransactionChannel;
import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.spec.transaction.FeeSpec;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.integration.funds.ledger.enums.LedgerSubjectCode;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

/**
 * 钱包交易退款请求
 *
 * @author wuxp
 * @date 2026-04-21 08:57
 **/
@Data
@Accessors(chain = true)
public class FundsTransactionRefundRequest {

    @Schema(description = "退款到账账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "退款金额")
    @NotNull
    private Money amount;

    @Schema(description = "本次交易显式手续费规则")
    private FeeSpec feeSpec;

    @Schema(description = "退款出资（原收款方）账户")
    @NotNull
    private FundsAccountId payerId;

    @Schema(description = "退款出资账户 Ledger编码")
    @NonNull
    private LedgerSubjectCode payerLedgerCode = LedgerSubjectCode.SETTLEMENT;

    @Schema(description = "业务流水号，退款凭证，例如：在线退款流水号、人工退款单流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "渠道")
    private FundsTransactionChannel channel;

    @Schema(description = "渠道方标识")
    private String channelId;

    /**
     * 渠道方交易流水号，可空
     * 如果是通过第三方平台做的退款，则必填
     */
    @Size(max = 80)
    private String channelTransactionSn;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;

    public FundsTransactionRefundRequest setContextVariables(WritableContextVariables contextVariables) {
        this.contextVariables = FundsRequestContextVariables.snapshot(contextVariables);
        return this;
    }
}
