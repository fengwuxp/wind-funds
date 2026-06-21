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
 * 外部资金事件消费请求。
 *
 * <p>该请求用于承载 ACH、银行文件、渠道回调或第三方钱包回调等外部资金事件的最小资金域语义；
 * 不替代交易层账户主体型请求，也不表示银行文件批次本身可以进入资金内核。</p>
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

    @Schema(description = "外部资金事件类型，例如 ACH_CREDIT_CONFIRMED、BANK_DEBIT_CONFIRMED")
    @NotBlank
    private String externalEventType;

    @Schema(description = "外部事件影响的内部资金账户或信用账户主体")
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
