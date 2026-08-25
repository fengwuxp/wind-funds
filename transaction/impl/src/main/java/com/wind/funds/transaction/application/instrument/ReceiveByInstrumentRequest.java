package com.wind.funds.transaction.application.instrument;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Transaction Provider 内部支付工具收款命令。
 *
 * <p>仅供 transaction-impl 把已归一支付工具收款事实解析为账户主体型充值交易，
 * 不属于 Wallet 或 Public API。</p>
 *
 * @author Codex
 * @since 2026-06-21
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

    @Schema(description = "本次外部交易实际使用的 rail 编码，例如 ACH、WIRE、DIGITAL_WALLET")
    @NotBlank
    private String externalRailCode;

    @Schema(description = "渠道方交易流水号")
    @NotBlank
    private String channelTransactionSn;

    @Schema(description = "可信上游适配层归一的外部资金事实来源编码，标识外部资金事实号的唯一性命名空间")
    @NotBlank
    @Size(max = 128)
    private String externalSourceCode;

    @Schema(description = "外部资金事实流水号，标识一次可入账的外部资金变动，不等同于渠道通知流水")
    @NotBlank
    @Size(max = 128)
    private String externalFundsFactSn;

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
