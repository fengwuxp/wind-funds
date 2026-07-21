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
 * 交易结果消费控制额度变动流水请求。
 *
 * <p>本请求用于已存在原控制额度变动流水的交易消费和退款补偿链路。业务确认型退款找不到原控制事实时，
 * 不应通过资金退款入口自动回补周期额度；应由业务侧确认支付工具存在且状态有效，并显式给出周期和金额等控制补偿依据。</p>
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

    @Schema(description = "本次控制消费或补偿变动流水号")
    @NotBlank
    private String movementSn;

    @Schema(description = "原控制额度变动流水流水号")
    @NotBlank
    private String originalMovementSn;

    @Schema(description = "已存在的资金交易流水号")
    @NotBlank
    private String transactionSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "控制额度变动目标资金账户或信用账户标识")
    @NotNull
    private FundsAccountId targetAccountId;

    @Schema(description = "控制金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "控制额度变动摘要，用于幂等、回放和审计追踪")
    @NotBlank
    private String movementDigest;

    @Schema(description = "控制额度变动说明")
    private String description;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}
