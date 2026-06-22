package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.PaymentInstrumentBindingRole;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
import com.wind.funds.wallet.enums.SpendRuleScopeType;
import com.wind.funds.wallet.enums.SpendSubjectFundingRelationType;
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
 * 支出控制准入决策 DTO。
 *
 * @author Codex
 * @date 2026-06-19
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendControlAdmissionDecisionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -9013092242845432258L;

    @Schema(description = "租户 ID")
    private Long tenantId;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    private PaymentInstrumentAction action;

    @Schema(description = "交易金额，最小货币单位")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "支付工具绑定角色")
    private PaymentInstrumentBindingRole bindingRole;

    @Schema(description = "资金责任关系类型")
    private SpendSubjectFundingRelationType relationType;

    @Schema(description = "业务场景")
    private String businessScene;

    @Schema(description = "业务流水号或请求幂等号")
    private String businessSn;

    @Schema(description = "是否允许进入后续交易内核")
    private Boolean admitted;

    @Schema(description = "最终解析出的资金账户或信用账户标识")
    private FundsAccountId targetAccountId;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "Spend Rule 挂载流水号")
    private String spendRuleAssignmentSn;

    @Schema(description = "Spend Rule 控制范围类型")
    private SpendRuleScopeType spendRuleScopeType;

    @Schema(description = "Spend Rule 控制范围标识")
    private String spendRuleScopeId;

    @Schema(description = "Spend Rule 决策流水号")
    private String spendDecisionSn;

    @Schema(description = "Spend Rule 决策结果")
    private SpendControlDecisionResult spendDecisionResult;

    @Schema(description = "Spend Rule 决策摘要，用于幂等、回放和对账追踪")
    private String spendDecisionDigest;

    @Schema(description = "已固化的 Spend Rule 决策日志主键")
    private Long spendDecisionLogId;

    @Schema(description = "预算组或预算控制范围标识")
    private String budgetGroupSn;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "支付工具预交易快照")
    private PaymentInstrumentPreTransactionSnapshotDTO preTransactionSnapshot;
}
