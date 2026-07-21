package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
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
 * Spend Rule 决策记录查询条件。
 *
 * @author Codex
 * @date 2026-06-23
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendRuleDecisionRecordQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = -1343117785267888144L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "规则决策流水号")
    private String decisionSn;

    @Schema(description = "Spend Rule 标识")
    private String ruleId;

    @Schema(description = "Spend Rule 版本")
    private String ruleVersion;

    @Schema(description = "规则挂载流水号")
    private String spendRuleBindingSn;

    @Schema(description = "控制范围类型")
    private SpendRuleScopeType scopeType;

    @Schema(description = "控制范围标识")
    private String scopeId;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    private PaymentInstrumentAction action;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "规则决策结果")
    private SpendControlDecisionResult decisionResult;
}
