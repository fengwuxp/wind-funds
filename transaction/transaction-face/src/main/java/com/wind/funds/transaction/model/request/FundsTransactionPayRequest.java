package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.spec.transaction.FeeSpec;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

/**
 * 钱包交易支付请求
 *
 * @author wuxp
 * @date 2026-04-21 08:58
 **/
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FundsTransactionPayRequest {

    @Schema(description = "付款账户")
    @NonNull
    private FundsAccountId accountId;

    @Schema(description = "付款交易金额")
    @NonNull
    private TransactionAmount transactionAmount;

    @Schema(description = "本次交易显式手续费规则")
    private FeeSpec feeSpec;

    @Schema(description = "收款账户")
    @NonNull
    private FundsAccountId payeeId;

    @Schema(description = "收款账户Ledger编码")
    @NonNull
    private LedgerSubjectCode payeeLedgerCode = LedgerSubjectCode.SETTLEMENT;

    @Schema(description = "业务流水号，付款凭证，例如：业务订单交易流水号")
    @NonNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NonNull
    private String businessScene;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
