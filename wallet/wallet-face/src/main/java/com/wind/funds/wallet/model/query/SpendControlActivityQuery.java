package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.SpendControlActivityType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 支出控制活动查询条件。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendControlActivityQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 3347119266449879712L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支出控制活动流水号")
    private String activitySn;

    @Schema(description = "支出控制活动类型")
    private SpendControlActivityType activityType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "原支出控制活动流水号")
    private String originalActivitySn;

    @Schema(description = "资金交易流水号")
    private String transactionSn;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "控制活动目标资金账户或信用账户标识")
    private FundsAccountId targetAccountId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "预算组或预算控制范围标识")
    private String budgetGroupSn;
}
