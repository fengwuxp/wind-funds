package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预算控制投影 DTO。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class BudgetControlProjectionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6088012310511043169L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "预算组或预算控制范围标识")
    private String budgetGroupSn;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "控制活动目标资金账户或信用账户标识，空表示预算控制范围级聚合投影")
    private FundsAccountId targetAccountId;

    @Schema(description = "控制占用金额，最小货币单位")
    private Long reservedAmount;

    @Schema(description = "控制消耗金额，最小货币单位")
    private Long consumedAmount;

    @Schema(description = "控制释放金额，最小货币单位")
    private Long releasedAmount;

    @Schema(description = "剩余控制金额，最小货币单位")
    private Long remainingControlAmount;

    @Schema(description = "最后一笔控制活动流水号")
    private String lastActivitySn;

    @Schema(description = "最后一笔控制活动时间")
    private LocalDateTime lastActivityAt;
}
