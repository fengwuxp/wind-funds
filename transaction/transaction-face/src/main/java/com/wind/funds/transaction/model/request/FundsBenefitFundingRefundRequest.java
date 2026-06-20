package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 权益让利退款请求。
 *
 * <p>退款、业务取消、人工纠错或反向冲销都用于对已经入账的权益让利资金交易进行反向资金影响，
 * 必须引用原权益让利资金交易流水号。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Schema(description = "权益让利退款请求")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FundsBenefitFundingRefundRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "被退款的原权益让利资金交易流水号")
    @NotBlank
    private String referenceBenefitTransactionSn;

    @Schema(description = "关联原主资金交易流水号，可用于支付退款或争议退款的跨事实追踪")
    private String referenceTransactionSn;

    @Schema(description = "退款金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次退款业务流水号")
    @NotBlank
    private String businessSn;

    @Schema(description = "原始业务订单号")
    @NotBlank
    private String originalOrderSn;

    @Schema(description = "退款、业务取消、人工纠错或反向冲销原因")
    private String refundReason;

    @Schema(description = "非关键扩展上下文，不得承载核心金额、规则版本、券包或敏感原文")
    private ReadonlyContextVariables contextVariables;
}
