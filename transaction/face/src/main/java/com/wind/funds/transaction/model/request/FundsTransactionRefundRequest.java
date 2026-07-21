package com.wind.funds.transaction.model.request;

import com.wind.funds.transaction.enums.FundsTransactionChannel;
import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 直接交易退款请求。
 *
 * <p>填写 {@code referenceTransactionSn} 时，原交易 route snapshot 是唯一退款路由事实源，
 * 不得再填写 {@code accountId}、{@code payerId} 或 {@code payerLedgerSubjectCode}；未填写原交易流水时为
 * 业务确认型直接退款，调用方必须明确给出退款到账账户、出资账户和出资账目。</p>
 *
 * @author wuxp
 * @date 2026-04-21 08:57
 **/
@Data
@Accessors(chain = true)
public class FundsTransactionRefundRequest {

    @Schema(description = "退款到账账户")
    private FundsAccountId accountId;

    @Schema(description = "退款交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易新增收费规则；手续费从退款路径中唯一真实资金受益 FundingAccount 的 AVAILABLE 扣取，"
            + "不得从 CreditAccount 额度扣取，与退款本金原子入账")
    private FeeSpec feeChargeSpec;

    @Schema(description = "退款出资账户；业务决策型直接退款时由调用方明确给出")
    private FundsAccountId payerId;

    @Schema(description = "退款出资账户账目编码；业务决策型直接退款时由调用方明确给出")
    private LedgerSubjectCode payerLedgerSubjectCode;

    @Schema(description = "原资金交易流水号；填写后按原交易 route snapshot 回放，未填写表示业务决策型直接退款")
    @Size(max = 80)
    private String referenceTransactionSn;

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
    private ReadonlyContextVariables contextVariables;
}
