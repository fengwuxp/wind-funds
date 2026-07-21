package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * Spend Rule 规则评估决策 DTO。
 *
 * @author Codex
 * @date 2026-06-30
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleEvaluationDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7696604416515434717L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "Spend Rule 标识")
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    private String ruleVersion;

    @Schema(description = "支付工具动作")
    private PaymentInstrumentAction action;

    @Schema(description = "交易金额，最小货币单位")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "规则评估决策结果")
    private SpendControlDecisionResult decisionResult;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "规则评估决策摘要候选")
    private String decisionDigest;
}
