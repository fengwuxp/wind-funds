package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.wallet.FundsAccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;

/**
 * 钱包授权交易撤销请求
 *
 * @author wuxp
 * @date 2026-04-21 09:01
 **/
@Schema(description = "钱包授权交易撤销请求")
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class FundsAuthorizationTransactionReversalRequest {

    @Schema(description = "原授权主账户，必须与原授权 RouteSnapshot 一致")
    @NotNull
    @NonNull
    private FundsAccountId accountId;

    @Schema(description = "撤销交易金额，主金额应和账户币种一致")
    @NotNull
    @NonNull
    private TransactionAmount transactionAmount;

    @Schema(description = "业务场景")
    @NotNull
    @NonNull
    private String businessScene;

    @Schema(description = "本次撤销业务流水号")
    @NonNull
    @NotNull
    private String businessSn;

    @Schema(description = "原授权资金交易号")
    @NonNull
    @NotNull
    private String authorizationTransactionSn;

    @Schema(description = "交易撤销时间")
    private LocalDateTime reversalTime;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
