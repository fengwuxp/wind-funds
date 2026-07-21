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
 * 支付工具出款 rail 请求。
 *
 * <p>该请求是 wallet application 层的全球账户出款入口模型，用于承载外部业务语义、支付工具引用、
 * 出款 rail 和收款人引用；不替代 transaction 层账户主体型提现请求。
 * 本请求只适用于外部出款已达到可关闭内部冻结资金的终态成功或等价业务确认；
 * 外部受理、提交或处理中状态只能停在上游出款单、在途或差错链路。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
@Schema(description = "支付工具出款 rail 请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class PayOutByRailRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "出款资金来源账户主体，用于后续校验工具绑定和委派账户主体型提现内核")
    @NotNull
    private FundsAccountId payoutSourceAccountId;

    @Schema(description = "外部收款账户主体，交易内核要求为外部账户")
    @NotNull
    private FundsAccountId payeeAccountId;

    @Schema(description = "提现冻结流水号，引用已确认的出款资金预留")
    @NotBlank
    private String referenceFreezeSn;

    @Schema(description = "出款金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "出款币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "出款 rail 编码，例如 SWIFT、LOCAL、ACH")
    @NotBlank
    private String railCode;

    @Schema(description = "收款人或收款端点引用，由上层业务域负责维护明细")
    @NotBlank
    private String receiverReference;

    @Schema(description = "外部出款请求或渠道流水号")
    @NotBlank
    private String externalPayoutSn;

    @Schema(description = "外部出款终态状态，只有 SUCCEEDED、PAID、SETTLED、COMPLETED 可关闭内部冻结资金")
    @NotBlank
    private String externalPayoutStatus;

    @Schema(description = "业务流水号，通常为出款订单号或请求幂等号")
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
