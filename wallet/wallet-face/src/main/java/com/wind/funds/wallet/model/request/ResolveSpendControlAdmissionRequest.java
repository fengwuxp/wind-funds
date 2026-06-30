package com.wind.funds.wallet.model.request;

import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
import com.wind.transaction.core.enums.CurrencyIsoCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 支出控制准入解析请求。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class ResolveSpendControlAdmissionRequest {

    @Schema(description = "租户 ID")
    @NotNull
    private Long tenantId;

    @Schema(description = "支付工具号")
    @NotBlank
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

    @Schema(description = "期望支付工具绑定角色")
    @NotNull
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "期望支付工具绑定版本，用于防止换绑后继续使用旧快照")
    private Integer expectedBindingVersion;

    @Schema(description = "资金责任关系类型")
    @NotNull
    private SpendSubjectFundingRelationType relationType;

    @Schema(description = "业务场景")
    @NotBlank
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    @NotBlank
    private String businessSn;

    @Schema(description = "Spend Rule 标识")
    @NotBlank
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    @NotBlank
    private String spendRuleVersion;

    @Schema(description = "Spend Rule 挂载流水号，可为空；为空时只记录规则和控制范围")
    private String spendRuleAssignmentSn;

    @Schema(description = "Spend Rule 控制范围类型")
    @NotNull
    private SpendRuleScopeType spendRuleScopeType;

    @Schema(description = "Spend Rule 控制范围标识")
    @NotBlank
    private String spendRuleScopeId;

    @Schema(description = "Spend Rule 决策流水号")
    @NotBlank
    private String spendDecisionSn;

    @Schema(description = "Spend Rule 决策结果")
    @NotNull
    private SpendControlDecisionResult spendDecisionResult;

    @Schema(description = "Spend Rule 决策摘要，用于幂等、回放和对账追踪")
    @NotBlank
    private String spendDecisionDigest;

    @Schema(description = "预算控制范围标识，兼容字段名为预算组号，可为空")
    private String budgetGroupSn;

    @Schema(description = "拒绝原因，仅 Spend Rule 决策拒绝时必填")
    private String rejectReason;
}
