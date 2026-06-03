package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 钱包授权交易过期释放请求。
 */
@Data
@Accessors(chain = true)
@Schema(description = "钱包授权交易过期释放请求")
public class FundsAuthorizationTransactionExpireRequest {

    @Schema(description = "账户 id")
    @NotNull
    private FundsAccountId accountId;

    @Schema(description = "过期释放金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次过期释放业务流水号")
    @NotBlank
    private String businessSn;

    @Schema(description = "原授权资金交易号")
    @NotBlank
    private String authorizationTransactionSn;

    @Schema(description = "过期时间")
    private LocalDateTime expiredTime;

    @Schema(description = "过期原因")
    private String expireReason;

    @Schema(description = "交易描述")
    private String description;

    @Schema(description = "上下文变量")
    private ReadonlyContextVariables contextVariables;
}
