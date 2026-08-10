package com.wind.funds.transaction.model.request;

import com.wind.core.ReadonlyContextVariables;
import com.wind.funds.ledger.enums.LedgerSubjectCode;
import com.wind.funds.transaction.enums.FundsBenefitFundingNature;
import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 让利出资记账交易结算请求。
 *
 * <p>该请求表达业务侧已经决策完成、需要确认入账的优惠券、代金券、支付立减或多方让利出资责任事实，
 * 不承载营销实时规则、券包库存、活动生命周期、返利、佣金或分润。</p>
 *
 * @author Codex
 * @date 2026-06-16
 */
@Schema(description = "让利出资记账交易结算请求")
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class FundsBenefitContributionSettleRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "本次让利出资结算业务流水号，例如平台补贴、商户让利、合作方补贴或清分调整流水号")
    @NotBlank
    private String businessSn;

    @Schema(description = "原始业务订单号")
    @NotBlank
    private String originalOrderSn;

    @Schema(description = "关联原资金交易流水号，可用于伴随支付、争议或对账回放")
    private String referenceTransactionSn;

    @Schema(description = "平台、商户或合作方让利责任承担资金账户")
    @NotNull
    private FundsAccountId costBearerAccountId;

    @Schema(description = "让利账务承接资金账户，例如用户或订单让利归集账户、商户清结算账户或等价被补足账户")
    @NotNull
    private FundsAccountId benefitReceiverAccountId;

    @Schema(description = "让利承接目标账目；平台补足商户使用 CLEARING，用户或订单归集使用 SETTLEMENT")
    @NotNull
    private LedgerSubjectCode benefitReceiverLedgerSubjectCode;

    @Schema(description = "本出资方承担的让利金额")
    @NotNull
    private Money amount;

    @Schema(description = "让利资金性质")
    @NotNull
    private FundsBenefitFundingNature fundingNature;

    @Schema(description = "非关键扩展上下文，不得承载核心金额、出资分摊、券、活动、规则来源或敏感原文")
    private ReadonlyContextVariables contextVariables;
}
