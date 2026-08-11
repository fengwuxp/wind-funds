package com.wind.funds.reconciliation.model.request;

import com.wind.funds.reconciliation.enums.PayoutOrderState;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 外部出款回执处理请求。
 *
 * @author wuxp
 * @since 2026-07-30
 */
@Schema(description = "外部出款回执处理请求")
@Data
@Accessors(chain = true)
public class HandlePayoutReceiptRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5779825888828459962L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "出款单流水号")
    @NotBlank
    private String payoutOrderSn;

    @Schema(description = "出款通道稳定引用")
    @NotBlank
    private String channelRef;

    @Schema(description = "外部回执唯一引用")
    @NotBlank
    private String externalReceiptRef;

    @Schema(description = "外部通道业务引用")
    @NotBlank
    private String externalReference;

    @Schema(description = "回执确认的出款事实状态")
    @NotNull
    private PayoutOrderState state;

    @Schema(description = "回执金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "回执币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "原始回执稳定摘要")
    @NotBlank
    private String sourceReceiptDigest;

    @Schema(description = "外部回执证据引用")
    @NotBlank
    private String evidenceRef;

    @Schema(description = "外部事件发生时间")
    @NotNull
    private LocalDateTime externalOccurredAt;

    @Schema(description = "外部失败码")
    private String failureCode;

    @Schema(description = "外部失败原因")
    private String failureReason;
}
