package com.wind.funds.wallet.model.query;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.SpendControlMovementType;
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
import java.time.LocalDateTime;

/**
 * 控制额度变动流水查询条件。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendControlMovementQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 3347119266449879712L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "控制额度变动流水流水号")
    private String movementSn;

    @Schema(description = "控制额度变动流水类型")
    private SpendControlMovementType movementType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "原控制额度变动流水流水号")
    private String originalMovementSn;

    @Schema(description = "资金交易流水号")
    private String transactionSn;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "控制额度变动目标资金账户或信用账户标识")
    private FundsAccountId targetAccountId;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "控制范围标识，目标语义名；当前兼容映射到历史字段 budgetGroupSn")
    private String controlScopeId;

    @Schema(description = "预算控制范围标识，历史字段名 budgetGroupSn，不表示账务主体")
    private String budgetGroupSn;

    @Schema(description = "控制周期标识，例如 2026-07")
    private String periodId;

    @Schema(description = "创建时间最小值，含边界")
    private LocalDateTime gmtCreateMin;

    @Schema(description = "创建时间最大值，含边界")
    private LocalDateTime gmtCreateMax;
}
