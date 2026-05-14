package com.capte.funds.transaction.model.request;

import com.wind.core.WritableContextVariables;
import com.wind.integration.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 钱包账户余额解冻请求
 *
 * @author wuxp
 * @date 2026-04-21 09:06
 **/
@Data
@Accessors(chain = true)
public class FundsBalanceUnfreezeRequest {

    @Schema(description = "解冻账户")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "解冻金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务流水号，解冻凭证，例如：解冻申请单流水号，风控处理流水号")
    @NotNull
    private String businessSn;

    @Schema(description = "业务场景")
    @NotNull
    private String businessScene;

    @Schema(description = "关联的冻结记录流水号")
    private String referenceFreezeSn;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private WritableContextVariables contextVariables;
}
