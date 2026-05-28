package com.capte.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 钱包授权交易退款
 *
 * @author wuxp
 * @date 2026-04-21 09:04
 **/
@Data
@Accessors(chain = true)
@Schema(description = "钱包授权交易退款")
public class FundsAuthorizationTransactionRefundRequest {

    @Schema(description = "账户 id")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "退款金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "本次退款业务流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "原授权资金交易号")
    @NotNull
    private String authorizationTransactionSn;

    @Schema(description = "退款时间")
    private LocalDateTime refundTime;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
