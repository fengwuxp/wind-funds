package com.wind.funds.wallet.model.dto;

import com.wind.funds.wallet.FundsAccountId;
import com.wind.funds.wallet.enums.PaymentInstrumentAction;
import com.wind.funds.wallet.enums.SpendControlMovementType;
import com.wind.funds.wallet.enums.SpendControlDecisionResult;
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
 * 控制额度变动流水 DTO。
 *
 * @author Codex
 * @date 2026-06-20
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
public class SpendControlMovementDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -6508030574142134935L;

    @Schema(description = "自增主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    @Schema(description = "最后修改时间")
    private LocalDateTime gmtModified;

    @Schema(description = "租户 ID")
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

    @Schema(description = "已存在的资金交易流水号")
    private String transactionSn;

    @Schema(description = "支付工具号")
    private String instrumentSn;

    @Schema(description = "支付工具动作")
    private PaymentInstrumentAction action;

    @Schema(description = "控制额度变动目标资金账户或信用账户标识")
    private FundsAccountId targetAccountId;

    @Schema(description = "控制金额，最小货币单位")
    private Long amount;

    @Schema(description = "币种")
    private CurrencyIsoCode currency;

    @Schema(description = "Spend Rule 标识")
    private String spendRuleId;

    @Schema(description = "Spend Rule 版本")
    private String spendRuleVersion;

    @Schema(description = "Spend Rule 决策流水号")
    private String spendDecisionSn;

    @Schema(description = "Spend Rule 决策结果")
    private SpendControlDecisionResult spendDecisionResult;

    @Schema(description = "Spend Rule 决策摘要")
    private String spendDecisionDigest;

    @Schema(description = "预算控制范围标识，历史字段名 budgetGroupSn，不表示账务主体")
    private String budgetGroupSn;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "调整原因码")
    private String reasonCode;

    @Schema(description = "操作者或系统来源")
    private String operatorId;

    @Schema(description = "审批、凭证、规则发布或外部审计引用")
    private String auditReferenceSn;

    @Schema(description = "控制额度变动摘要")
    private String movementDigest;

    @Schema(description = "控制额度变动说明")
    private String description;

    @Schema(description = "扩展上下文变量")
    private String contextVariables;
}
