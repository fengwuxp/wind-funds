package com.wind.funds.transaction.application.spend;

import com.wind.funds.wallet.FundsAccountId;
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
 * Transaction Provider 内部 Spend Rule 业务确认型退款控制补偿命令。
 *
 * <p>用于业务侧已确认退款可以恢复支付工具周期控制额度，但找不到原控制额度变动流水的场景。
 * 资金退款本身仍由交易服务处理；本请求只写 Spend Rule 控制补偿事实。</p>
 *
 * @author Codex
 * @since 2026-07-10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendControlBusinessConfirmedRefundCompensationRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 3461187182680520721L;

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "本次控制补偿变动流水号")
    @NotBlank
    private String movementSn;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或退款补偿幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "业务侧确认有效的支付工具号")
    @NotBlank
    private String instrumentSn;

    @Schema(description = "控制额度变动目标资金账户或信用账户标识")
    @NotNull
    private FundsAccountId targetAccountId;

    @Schema(description = "补偿控制金额，最小货币单位")
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

    @Schema(description = "控制范围标识")
    private String controlScopeId;

    @Schema(description = "控制周期标识，例如 2026-07")
    @NotBlank
    private String periodId;

    @Schema(description = "业务确认退款补偿原因码")
    @NotBlank
    private String reasonCode;

    @Schema(description = "操作者或系统来源")
    @NotBlank
    private String operatorId;

    @Schema(description = "业务退款决策、审批、凭证或外部审计引用")
    @NotBlank
    private String auditReferenceSn;

    @Schema(description = "控制额度变动说明")
    private String description;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}
