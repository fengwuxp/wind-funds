package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 钱包授权交易结算（完成）请求
 *
 * @author wuxp
 * @date 2026-04-21 09:02
 **/
@Data
@Accessors(chain = true)
@Schema(description = "钱包授权交易结算（完成）请求")
public class FundsAuthorizationTransactionSettleRequest {

    @Schema(description = "账户 id")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "结算金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "本次结算业务流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "原授权资金交易号")
    @NotNull
    private String authorizationTransactionSn;

    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;
}
