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

    @Schema(description = "Spend Rule 标识，可选回显字段；wallet 以 decisionSn 回读结果为准")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本，可选回显字段；wallet 以 decisionSn 回读结果为准")
    private String spendRuleVersion;

    @Schema(description = "Spend Rule 挂载流水号，可选回显字段；适用挂载由 wallet 解析")
    private String spendRuleBindingSn;

    @Schema(description = "Spend Rule 控制范围类型，可选回显字段；适用范围由 wallet 解析")
    private SpendRuleScopeType spendRuleScopeType;

    @Schema(description = "Spend Rule 控制范围标识，可选回显字段；适用范围由 wallet 解析")
    private String spendRuleScopeId;

    @Schema(description = "Spend Rule 决策引用；存在适用挂载时必填，wallet 必须可回读并验真")
    private String spendDecisionSn;

    @Schema(description = "Spend Rule 决策结果，可选回显字段，不作为准入依据")
    private SpendControlDecisionResult spendDecisionResult;

    @Schema(description = "Spend Rule 最终决策摘要，可选回显字段，不作为准入依据；"
            + "wallet 以 decisionSn 回读的已固化摘要为准")
    private String spendDecisionDigest;

    @Schema(description = "控制范围标识，可为空")
    private String controlScopeId;

    @Schema(description = "拒绝原因，可选回显字段，不作为准入依据")
    private String rejectReason;
}
