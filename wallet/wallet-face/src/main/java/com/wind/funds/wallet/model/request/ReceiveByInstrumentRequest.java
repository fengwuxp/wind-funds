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

/**
 * 支付工具收款请求。
 *
 * <p>该请求是 wallet application 层的外部业务入口模型，用于把支付工具收款语义解析为账户主体型充值交易；
 * 不替代 transaction 层账户主体型充值请求。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
@Schema(description = "支付工具收款请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ReceiveByInstrumentRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "收款金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "收款币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "外部资金来源账户")
    @NotNull
    private FundsAccountId fundsSourceAccountId;

    @Schema(description = "收款渠道编码，对应交易层可识别的资金交易渠道")
    @NotBlank
    private String channelCode;

    @Schema(description = "渠道方交易流水号")
    @NotBlank
    private String channelTransactionSn;

    @Schema(description = "渠道方标识")
    private String channelId;

    @Schema(description = "业务流水号，通常为外部入金流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "期望支付工具绑定版本，用于防止换绑后继续使用旧快照")
    @NotNull
    private Integer expectedBindingVersion;

    @Schema(description = "交易描述")
    private String description;
}
