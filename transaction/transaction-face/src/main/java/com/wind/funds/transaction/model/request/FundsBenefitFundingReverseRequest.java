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
 * 权益让利撤销请求。
 *
 * <p>撤销用于业务取消、人工纠错或准实时反向冲销场景，必须引用原权益让利资金交易流水号。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Schema(description = "权益让利撤销请求")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FundsBenefitFundingReverseRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "被撤销的原权益让利资金交易流水号")
    @NotBlank
    private String referenceBenefitTransactionSn;

    @Schema(description = "关联原主资金交易流水号，可用于支付撤销或业务取消的跨事实追踪")
    private String referenceTransactionSn;

    @Schema(description = "撤销金额")
    @NotNull
    private Money amount;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次撤销业务流水号")
    @NotBlank
    private String businessSn;

    @Schema(description = "原始业务订单号")
    @NotBlank
    private String originalOrderSn;

    @Schema(description = "撤销原因")
    private String reverseReason;

    @Schema(description = "非关键扩展上下文，不得承载核心金额、规则版本、券包或敏感原文")
    private ReadonlyContextVariables contextVariables;
}
