package com.capte.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 账户余额冻结请求
 *
 * @author wuxp
 * @date 2026-04-21 09:05
 **/
@Data
@Accessors(chain = true)
public class FundsBalanceFreezeRequest {

    @Schema(description = "冻结账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "冻结金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务流水号，冻结凭证，例如：冻结申请单流水号，风控处理流水号")
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
