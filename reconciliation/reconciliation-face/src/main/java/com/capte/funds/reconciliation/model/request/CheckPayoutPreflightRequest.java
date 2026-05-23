package com.capte.funds.reconciliation.model.request;

import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 出款前准入检查请求。
 *
 * <p>职责：承载结算单、出款单、金额、出款账户、收款端点、通道、幂等和准入证据引用。</p>
 *
 * <p>边界：请求只用于出款提交前的准入判断，不包含通道真实回执、账务分录或出款生命周期事实。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class CheckPayoutPreflightRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = -4386909509534583269L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "结算单号")
    @NotBlank
    private String settlementSn;

    @Schema(description = "出款单号")
    @NotBlank
    private String payoutSn;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "出款金额，最小货币单位")
    @NotNull
    @Positive
    private Long amount;

    @Schema(description = "出款账户引用")
    private String payoutAccountRef;

    @Schema(description = "收款端点引用")
    private String payeeEndpointRef;

    @Schema(description = "通道引用")
    private String channelRef;

    @Schema(description = "幂等键")
    @NotBlank
    private String idempotencyKey;

    @Schema(description = "外部规则核验证据引用")
    private String ruleEvidenceRef;

    @Schema(description = "审批证据引用")
    private String approvalRef;
}
