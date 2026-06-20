package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 交易结果消费支出控制活动请求。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendControlTransactionConsumptionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -6977114450567710286L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "本次控制消费、释放或补偿活动流水号")
    @NotBlank
    private String activitySn;

    @Schema(description = "原支出控制活动流水号")
    @NotBlank
    private String originalActivitySn;

    @Schema(description = "已存在的资金交易流水号")
    @NotBlank
    private String transactionSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "控制活动目标资金账户或信用账户标识")
    @NotNull
    private FundsAccountId targetAccountId;

    @Schema(description = "控制金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "控制活动摘要，用于幂等、回放和审计追踪")
    @NotBlank
    private String activityDigest;

    @Schema(description = "控制活动说明")
    private String description;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}
