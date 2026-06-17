package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.route.ref.SubjectRef;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.transaction.enums.FundsBenefitLedgerEffect;
import com.wind.funds.transaction.model.dto.FundsBenefitFundingSourceDTO;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 权益让利资金交易请求。
 *
 * <p>该请求表达业务侧已经决策完成的权益让利资金事实，不承载营销实时规则、券包库存或活动生命周期。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Schema(description = "权益让利资金交易请求")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FundsBenefitFundingApplyRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次业务流水号，例如支付、退款、人工补贴或清分调整流水号")
    @NotBlank
    private String businessSn;

    @Schema(description = "原始业务订单号")
    @NotBlank
    private String originalOrderSn;

    @Schema(description = "关联原资金交易流水号，可用于伴随支付、退款、撤销、争议或对账回放")
    private String referenceTransactionSn;

    @Schema(description = "让利、补贴、权益负债或合作方责任承担账务主体")
    @NotNull
    private SubjectRef costBearerSubjectRef;

    @Schema(description = "权益资金影响受益账务主体")
    @NotNull
    private SubjectRef benefitReceiverSubjectRef;

    @Schema(description = "让利资金金额")
    @NotNull
    private Money amount;

    @Schema(description = "权益资金性质")
    @NotNull
    private FundsBenefitFundingNature fundingNature;

    @Schema(description = "账务效果，决定后续是否进入 route/posting 或仅作为解释归因")
    @NotNull
    private FundsBenefitLedgerEffect ledgerEffect;

    @Schema(description = "权益让利来源、规则或外部决策引用列表")
    @NotEmpty
    private List<FundsBenefitFundingSourceDTO> benefitFundingSources;

    @Schema(description = "非关键扩展上下文，不得承载核心金额、规则版本、券包或敏感原文")
    private ReadonlyContextVariables contextVariables;
}
