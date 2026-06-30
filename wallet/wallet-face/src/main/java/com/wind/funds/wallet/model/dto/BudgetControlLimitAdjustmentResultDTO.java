package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.SpendControlMovementType;
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
 * 预算控制额度调整结果 DTO。
 *
 * @author Codex
 * @date 2026-06-21
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class BudgetControlLimitAdjustmentResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3762686718054235431L;

    @Schema(description = "控制额度变动流水自增主键")
    private Long movementId;

    @Schema(description = "控制额度变动流水流水号")
    private String movementSn;

    @Schema(description = "控制额度变动流水类型")
    private SpendControlMovementType movementType;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "控制范围标识，目标语义名；当前兼容映射到历史字段 budgetGroupSn")
    private String controlScopeId;

    @Schema(description = "预算控制范围标识，历史字段名 budgetGroupSn，不表示账务主体")
    private String budgetGroupSn;

    @Schema(description = "控制周期标识，例如 2026-07")
    private String periodId;

    @Schema(description = "预算控制额度影响的资金账户或信用账户标识")
    private FundsAccountId targetAccountId;

    @Schema(description = "调整金额，最小货币单位")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "是否调增，true 表示调增，false 表示调减")
    private Boolean increase;

    @Schema(description = "调整原因码")
    private String reasonCode;

    @Schema(description = "操作者或系统来源")
    private String operatorId;

    @Schema(description = "审批、凭证、规则发布或外部审计引用")
    private String auditReferenceSn;

    @Schema(description = "控制额度变动摘要")
    private String movementDigest;

    @Schema(description = "预算控制投影")
    private BudgetControlProjectionDTO projection;
}
