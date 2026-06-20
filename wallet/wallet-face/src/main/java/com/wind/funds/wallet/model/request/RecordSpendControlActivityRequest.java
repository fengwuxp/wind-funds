package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
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
 * 支出控制活动记录请求。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class RecordSpendControlActivityRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 5797637413300774738L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支出控制活动流水号，用于幂等、回放和审计追踪")
    @NotBlank
    private String activitySn;

    @Schema(description = "支出控制活动类型")
    @NotNull
    private SpendControlActivityType activityType;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    @NotNull
    private PaymentInstrumentAction action;

    @Schema(description = "控制活动目标资金账户或信用账户标识")
    @NotNull
    private FundsAccountId targetAccountId;

    @Schema(description = "控制金额，最小货币单位")
    @NotNull
    private Long amount;

    @Schema(description = "币种")
    @NotNull
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String spendRuleVersion;

    @Schema(description = "Spend Rule 决策流水号")
    @NotBlank
    private String spendDecisionSn;

    @Schema(description = "Spend Rule 决策结果")
    @NotNull
    private SpendControlDecisionResult spendDecisionResult;

    @Schema(description = "Spend Rule 决策摘要，用于幂等、回放和对账追踪")
    @NotBlank
    private String spendDecisionDigest;

    @Schema(description = "预算组或预算控制范围标识")
    private String budgetGroupSn;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "控制活动摘要，用于幂等、回放和审计追踪")
    @NotBlank
    private String activityDigest;

    @Schema(description = "控制活动说明")
    private String description;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}
