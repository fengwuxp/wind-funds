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
import org.jspecify.annotations.NonNull;

/**
 * 直接交易退款请求。
 *
 * <p>填写 {@code referenceTransactionSn} 时按原交易 route snapshot 回放；未填写时为业务决策型直接退款，
 * 调用方必须明确给出退款到账账户、出资账户和出资账目。</p>
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

    @Schema(description = "退款交易金额")
    @NotNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易显式手续费规则")
    private FeeSpec feeSpec;

    @Schema(description = "退款出资账户；业务决策型直接退款时由调用方明确给出")
    @NotNull
    private FundsAccountId payerId;

    @Schema(description = "退款出资账户账目编码；业务决策型直接退款时由调用方明确给出")
    @NonNull
    private LedgerSubjectCode payerLedgerCode = LedgerSubjectCode.SETTLEMENT;

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
