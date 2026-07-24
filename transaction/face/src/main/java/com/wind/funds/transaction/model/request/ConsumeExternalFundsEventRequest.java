package com.wind.funds.transaction.model.request;

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
 * 外部资金事件消费请求。
 *
 * <p>该请求承载外部确认入金事件的最小交易语义。当前仅支持 confirmed credit 入金到内部资金账户，
 * 并委派标准 topup 内核。外部扣账、return/NOC/reversal、信用账户还款或调额、差错调整等场景，
 * 必须先明确事件方向、原交易引用和账务主体语义后再扩展。</p>
 *
 * @author Codex
 * @date 2026-06-21
 */
@Schema(description = "外部资金事件消费请求")
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ConsumeExternalFundsEventRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "外部资金事件流水号，用于事件幂等、回放和对账追踪")
    @NotBlank
    private String externalEventSn;

    @Schema(description = "可信上游适配层归一的外部资金事实来源编码，标识外部资金事实号的唯一性命名空间")
    @NotBlank
    @Size(max = 128)
    private String externalSourceCode;

    @Schema(description = "外部资金事实流水号，标识一次可入账的外部资金变动，不等同于通知事件流水")
    @NotBlank
    @Size(max = 128)
    private String externalFundsFactSn;

    @Schema(description = "外部资金事件类型，例如 ACH_CREDIT_CONFIRMED、BANK_CREDIT_CONFIRMED；大小写和横线由外部事件消费入口归一")
    @NotBlank
    private String externalEventType;

    @Schema(description = "已确认外部入金影响的内部资金账户主体，仅支持 FUNDING_ACCOUNT")
    @NotNull
    private FundsAccountId targetAccountId;

    @Schema(description = "外部事件金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "外部事件币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "原资金交易流水号，可用于退款、撤销、退票或差错处理")
    private String originalTransactionSn;

    @Schema(description = "对账差异流水号，可用于把外部事件消费回链到差异处理")
    private String reconciliationDifferenceSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号，通常为外部事件归一后的内部幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "事件描述")
    private String description;
}
