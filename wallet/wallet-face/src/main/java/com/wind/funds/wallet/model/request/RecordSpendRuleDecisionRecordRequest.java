package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * Spend Rule 决策记录记录请求。
 *
 * @author Codex
 * @date 2026-06-22
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordSpendRuleDecisionRecordRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1635838997807266435L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "规则决策流水号，用于幂等、回放和对账追踪")
    @NotBlank
    private String decisionSn;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String ruleVersion;

    @Schema(description = "规则挂载流水号")
    private String spendRuleBindingSn;

    @Schema(description = "控制范围类型")
    @NotNull
    private SpendRuleScopeType scopeType;

    @Schema(description = "控制范围标识")
    @NotBlank
    private String scopeId;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    @NotNull
    private PaymentInstrumentAction action;

    @Schema(description = "交易金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "规则决策结果")
    @NotNull
    private SpendControlDecisionResult decisionResult;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "规则最终决策摘要；"
            + "可代表上游多规则裁决证据摘要，当前公共契约不展开 evaluatedRules、decisionPolicy 或 finalDecision 明细")
    @NotBlank
    private String decisionDigest;
}
